package com.abrar.motolog.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.abrar.motolog.data.local.entity.RideEntity
import com.abrar.motolog.domain.repository.RideRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Ride History list screen.
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val rideRepository: RideRepository
) : ViewModel() {

    private val _recentlyDeletedRide = MutableStateFlow<RideEntity?>(null)
    private var pendingDeleteJob: Job? = null

    private val _uiState = MutableStateFlow(HistoryUiState(isLoading = true))
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                rideRepository.getFinishedRides(),
                _recentlyDeletedRide
            ) { rides, deletedRide ->
                val visibleRides = if (deletedRide != null) {
                    rides.filter { it.id != deletedRide.id }
                } else {
                    rides
                }
                HistoryUiState(
                    isLoading = false,
                    rides = visibleRides,
                    recentlyDeletedRide = deletedRide
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
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
        viewModelScope.launch {
            rideRepository.commitDeletion(rideToDelete.id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        val rideToDelete = _recentlyDeletedRide.value
        if (rideToDelete != null) {
            viewModelScope.launch {
                rideRepository.commitDeletion(rideToDelete.id)
            }
        }
    }
}
