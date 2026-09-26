package com.abrar.motolog.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.abrar.motolog.MainActivity
import com.abrar.motolog.R
import com.abrar.motolog.data.local.dao.BikeDao
import com.abrar.motolog.data.local.dao.MaintenanceDao
import com.abrar.motolog.data.local.dao.RideDao
import com.abrar.motolog.domain.engine.evaluate
import com.abrar.motolog.domain.engine.shouldNotify
import com.abrar.motolog.shared.domain.TrackingConstants
import com.abrar.motolog.shared.domain.engine.MaintenanceCalculator
import com.abrar.motolog.shared.domain.engine.OdometerCalculator
import com.abrar.motolog.shared.domain.model.MaintenanceStatus
import com.abrar.motolog.domain.repository.TrackingRepository
import com.abrar.motolog.shared.domain.repository.TrackingSessionState
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MaintenanceNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val bikeDao: BikeDao,
    private val rideDao: RideDao,
    private val maintenanceDao: MaintenanceDao,
    private val trackingRepository: TrackingRepository
) {

    init {
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                TrackingConstants.MAINTENANCE_NOTIFICATION_CHANNEL_ID,
                "Maintenance Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies when motorcycle maintenance tasks are due soon or overdue."
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    /**
     * Check maintenance items and send notifications for any due items.
     * Can target a specific bikeId (e.g. after ride completion) or all active bikes (e.g. daily background check).
     * Skipped if tracking is currently active to avoid disturbing the rider mid-ride.
     */
    suspend fun checkAndNotify(targetBikeId: Long? = null) {
        // Reminders must never fire during an active ride
        val currentState = trackingRepository.sessionState.value
        if (currentState is TrackingSessionState.Tracking || currentState is TrackingSessionState.WaitingForGps) {
            return
        }

        val notificationManager = NotificationManagerCompat.from(context)
        if (!notificationManager.areNotificationsEnabled()) {
            return
        }

        val bikes = if (targetBikeId != null) {
            val b = bikeDao.getBikeById(targetBikeId)
            if (b != null && !b.isArchived) listOf(b) else emptyList()
        } else {
            bikeDao.getActiveBikesOnce()
        }

        val nowEpochMs = System.currentTimeMillis()
        val antiSpamDurationMs = 7L * 24 * 60 * 60 * 1000 // 7 days

        for (bike in bikes) {
            val totalDistanceMeters = rideDao.getTotalDistanceMetersForBikeOnce(bike.id) ?: 0.0
            val currentOdoKm = OdometerCalculator.calculateCurrentOdometerKm(
                initialOdometerKm = bike.initialOdometerKm,
                recordedDistanceMeters = totalDistanceMeters,
                odometerOffsetKm = bike.odometerOffsetKm
            )

            val items = maintenanceDao.getItemsForBikeOnce(bike.id)
            for (item in items) {
                val eval = MaintenanceCalculator.evaluate(
                    item = item,
                    currentOdometerKm = currentOdoKm,
                    currentEpochMs = nowEpochMs
                )

                if (eval.status == MaintenanceStatus.OVERDUE || eval.status == MaintenanceStatus.DUE_SOON) {
                    val shouldNotify = MaintenanceCalculator.shouldNotify(
                        item = item,
                        status = eval.status,
                        currentEpochMs = nowEpochMs,
                        antiSpamDurationMs = antiSpamDurationMs
                    )

                    if (shouldNotify) {
                        showNotification(
                            bikeId = bike.id,
                            bikeName = bike.name,
                            itemId = item.id,
                            taskName = item.name,
                            status = eval.status,
                            reason = eval.summary
                        )
                        maintenanceDao.updateLastNotified(item.id, nowEpochMs)
                    }
                }
            }
        }
    }

    private fun showNotification(
        bikeId: Long,
        bikeName: String,
        itemId: Long,
        taskName: String,
        status: MaintenanceStatus,
        reason: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_NAV_BIKE_ID", bikeId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            (bikeId * 1000 + itemId).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (status == MaintenanceStatus.OVERDUE) {
            "Maintenance Overdue: $taskName"
        } else {
            "Maintenance Due Soon: $taskName"
        }

        val content = "$bikeName: $reason"

        val notification = NotificationCompat.Builder(context, TrackingConstants.MAINTENANCE_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            val notificationId = (TrackingConstants.MAINTENANCE_NOTIFICATION_ID_BASE + (itemId % 10000)).toInt()
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Notification permission denied or revoked
        }
    }
}
