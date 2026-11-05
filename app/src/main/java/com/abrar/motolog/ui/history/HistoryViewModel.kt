package com.abrar.motolog.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abrar.motolog.data.local.entity.RideEntity
import com.abrar.motolog.domain.repository.RideRepository
import com.abrar.motolog.data.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * ViewModel for the Ride History list screen.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val rideRepository: RideRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private companion object {
        const val PAGE_SIZE = 100
    }

    val useMetricUnits: StateFlow<Boolean> = settingsRepository.useMetricUnits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), true)

    private val _recentlyDeletedRide = MutableStateFlow<RideEntity?>(null)
    private var pendingDeleteJob: Job? = null

    private val _uiState = MutableStateFlow(HistoryUiState(isLoading = true))
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var loadedPageCount = 0

    init {
        loadPage(reset = true)
    }

    private fun loadPage(reset: Boolean = false) {
        if (_uiState.value.isLoadingMore) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val pageIndex = if (reset) 0 else loadedPageCount
            val page = rideRepository.getFinishedRidesPage(
                offset = pageIndex * PAGE_SIZE,
                limit = PAGE_SIZE
            ).first()
            val combined = if (reset) page else (_uiState.value.rides + page).distinctBy { it.id }
            loadedPageCount = pageIndex + 1
            _uiState.update {
                it.copy(
                    isLoading = false,
                    rides = combined,
                    canLoadMore = page.size == PAGE_SIZE,
                    isLoadingMore = false,
                    recentlyDeletedRide = _recentlyDeletedRide.value
                )
            }
        }
    }

    fun loadMore() {
        loadPage()
    }

    /**
     * Hides the ride from the list immediately and opens a 5-second undo window.
     */
    fun deleteRide(ride: RideEntity) {
        val currentPending = _recentlyDeletedRide.value
        if (currentPending != null && currentPending.id != ride.id) {
            pendingDeleteJob?.cancel()
            viewModelScope.launch {
                rideRepository.commitDeletion(currentPending.id)
            }
        }

        _recentlyDeletedRide.value = ride
        _uiState.update {
            it.copy(
                recentlyDeletedRide = ride,
                rides = it.rides.filterNot { visibleRide -> visibleRide.id == ride.id }
            )
        }
        viewModelScope.launch {
            rideRepository.markForDeletion(ride.id)
        }

        pendingDeleteJob?.cancel()
        pendingDeleteJob = viewModelScope.launch {
            delay(5_000L)
            commitPendingDeletion()
        }
    }

    /**
     * Cancels pending deletion and instantly restores the ride.
     */
    fun undoDelete() {
        val rideToRestore = _recentlyDeletedRide.value ?: return
        pendingDeleteJob?.cancel()
        _recentlyDeletedRide.value = null
        _uiState.update { state ->
            state.copy(
                recentlyDeletedRide = null,
                rides = (state.rides + rideToRestore).sortedByDescending { it.startTime }
            )
        }
        viewModelScope.launch {
            rideRepository.undoDeletion(rideToRestore.id)
        }
    }

    /**
     * Commits pending deletion immediately when snackbar is manually dismissed.
     */
    fun dismissSnackbar() {
        commitPendingDeletion()
    }

    private fun commitPendingDeletion() {
        val rideToDelete = _recentlyDeletedRide.value ?: return
        pendingDeleteJob?.cancel()
        _recentlyDeletedRide.value = null
        _uiState.update { it.copy(recentlyDeletedRide = null) }
        viewModelScope.launch {
            rideRepository.commitDeletion(rideToDelete.id)
        }
    }

    override fun onCleared() {
        val rideToDelete = _recentlyDeletedRide.value
        if (rideToDelete != null) {
            viewModelScope.launch {
                rideRepository.commitDeletion(rideToDelete.id)
            }
        }
    }
}
