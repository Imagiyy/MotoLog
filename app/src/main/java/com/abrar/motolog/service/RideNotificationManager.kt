package com.abrar.motolog.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.abrar.motolog.MainActivity
import com.abrar.motolog.R
import com.abrar.motolog.domain.TrackingConstants
import com.abrar.motolog.domain.model.RideStats
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages notification channel creation and ongoing notifications for the TrackingService.
 *
 * Configured with:
 * - Low importance channel (no sound/vibration churn)
 * - Persistent notification with live speed, distance, and moving time
 * - Quick Pause/Resume lock-screen action
 * - Safe Stop action opening the in-app 2-second Hold-to-Stop screen
 */
@Singleton
class RideNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                TrackingConstants.NOTIFICATION_CHANNEL_ID,
                "Ride Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live motorcycle ride tracking stats and controls"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun buildNotification(
        stats: RideStats,
        isPaused: Boolean,
        accuracyMeters: Float,
        isWaitingGps: Boolean
    ): Notification {
        // Content Intent: Open MainActivity
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Pause/Resume Intent
        val pauseResumeAction = if (isPaused) {
            val resumeIntent = Intent(context, TrackingService::class.java).apply {
                action = TrackingService.ACTION_RESUME
            }
            val resumePendingIntent = PendingIntent.getService(
                context,
                1,
                resumeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_play,
                "Resume",
                resumePendingIntent
            ).build()
        } else {
            val pauseIntent = Intent(context, TrackingService::class.java).apply {
                action = TrackingService.ACTION_PAUSE
            }
            val pausePendingIntent = PendingIntent.getService(
                context,
                2,
                pauseIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            NotificationCompat.Action.Builder(
                android.R.drawable.ic_media_pause,
                "Pause",
                pausePendingIntent
            ).build()
        }

        // Stop Intent: Opens MainActivity so rider can perform the 2-second Hold-to-Stop gesture
        val stopAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_REQUEST_STOP", true)
        }
        val stopPendingIntent = PendingIntent.getActivity(
            context,
            3,
            stopAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val stopAction = NotificationCompat.Action.Builder(
            android.R.drawable.ic_menu_close_clear_cancel,
            "Stop (In-App)",
            stopPendingIntent
        ).build()

        val title = when {
            isWaitingGps -> "MotoLog: Waiting for GPS"
            isPaused -> "MotoLog: PAUSED"
            else -> "MotoLog: Recording Ride"
        }

        val contentText = if (isWaitingGps) {
            val acc = if (accuracyMeters < Float.MAX_VALUE) "${accuracyMeters.toInt()}m" else "--"
            "Waiting for precise fix (current accuracy: ±$acc)"
        } else {
            val totalSec = stats.movingTimeMs / 1000
            val mins = (totalSec % 3600) / 60
            val secs = totalSec % 60
            val hrs = totalSec / 3600
            val timeStr = if (hrs > 0) {
                String.format(Locale.US, "%02d:%02d:%02d", hrs, mins, secs)
            } else {
                String.format(Locale.US, "%02d:%02d", mins, secs)
            }
            String.format(
                Locale.US,
                "Speed: %.0f km/h  •  Dist: %.1f km  •  Time: %s",
                stats.currentSpeedKmh,
                stats.totalDistanceMeters / 1000.0,
                timeStr
            )
        }

        val builder = NotificationCompat.Builder(context, TrackingConstants.NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(contentText)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)

        if (!isWaitingGps) {
            builder.addAction(pauseResumeAction)
            builder.addAction(stopAction)
        }

        return builder.build()
    }
}
