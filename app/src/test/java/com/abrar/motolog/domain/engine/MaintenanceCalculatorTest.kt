package com.abrar.motolog.domain.engine

import com.abrar.motolog.data.local.entity.MaintenanceItemEntity
import com.abrar.motolog.domain.model.MaintenanceStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class MaintenanceCalculatorTest {

    private val oneDayMs = TimeUnit.DAYS.toMillis(1)

    @Test
    fun `evaluation by km only - OK, Due Soon, Overdue`() {
        val item = MaintenanceItemEntity(
            id = 1,
            bikeId = 1,
            name = "Engine Oil",
            intervalKm = 5_000.0,
            intervalDays = null,
            lastDoneOdometerKm = 10_000.0,
            lastDoneDateEpochMs = 0L
        )

        // 1. Current odo 14_000 -> 1000 km remaining -> OK (> 100 km)
        val okEval = MaintenanceCalculator.evaluate(item, 14_000.0, 0L)
        assertEquals(MaintenanceStatus.OK, okEval.status)
        assertEquals(1000.0, okEval.remainingKm!!, 0.001)

        // 2. Current odo 14_950 -> 50 km remaining -> DUE_SOON (<= 100 km)
        val dueSoonEval = MaintenanceCalculator.evaluate(item, 14_950.0, 0L)
        assertEquals(MaintenanceStatus.DUE_SOON, dueSoonEval.status)
        assertEquals(50.0, dueSoonEval.remainingKm!!, 0.001)

        // 3. Current odo 15_010 -> -10 km remaining -> OVERDUE
        val overdueEval = MaintenanceCalculator.evaluate(item, 15_010.0, 0L)
        assertEquals(MaintenanceStatus.OVERDUE, overdueEval.status)
        assertEquals(-10.0, overdueEval.remainingKm!!, 0.001)
    }

    @Test
    fun `evaluation by days only - OK, Due Soon, Overdue`() {
        val lastDoneMs = 1_000_000_000L
        val item = MaintenanceItemEntity(
            id = 2,
            bikeId = 1,
            name = "Brake Fluid",
            intervalKm = null,
            intervalDays = 365,
            lastDoneOdometerKm = 0.0,
            lastDoneDateEpochMs = lastDoneMs
        )

        // 1. 200 days elapsed -> 165 days remaining -> OK (> 7 days)
        val okEval = MaintenanceCalculator.evaluate(item, 0.0, lastDoneMs + (200 * oneDayMs))
        assertEquals(MaintenanceStatus.OK, okEval.status)
        assertEquals(165, okEval.remainingDays)

        // 2. 360 days elapsed -> 5 days remaining -> DUE_SOON (<= 7 days)
        val dueSoonEval = MaintenanceCalculator.evaluate(item, 0.0, lastDoneMs + (360 * oneDayMs))
        assertEquals(MaintenanceStatus.DUE_SOON, dueSoonEval.status)
        assertEquals(5, dueSoonEval.remainingDays)

        // 3. 370 days elapsed -> -5 days remaining -> OVERDUE
        val overdueEval = MaintenanceCalculator.evaluate(item, 0.0, lastDoneMs + (370 * oneDayMs))
        assertEquals(MaintenanceStatus.OVERDUE, overdueEval.status)
        assertEquals(-5, overdueEval.remainingDays)
    }

    @Test
    fun `evaluation by whichever comes first - km triggers first`() {
        val lastDoneMs = 1_000_000_000L
        val item = MaintenanceItemEntity(
            id = 3,
            bikeId = 1,
            name = "General Service",
            intervalKm = 6_000.0,
            intervalDays = 180,
            lastDoneOdometerKm = 10_000.0,
            lastDoneDateEpochMs = lastDoneMs
        )

        // Only 30 days elapsed (days OK), but km is 15_950 (50 km remaining -> due soon)
        val eval = MaintenanceCalculator.evaluate(item, 15_950.0, lastDoneMs + (30 * oneDayMs))
        assertEquals(MaintenanceStatus.DUE_SOON, eval.status)
        assertEquals(50.0, eval.remainingKm!!, 0.001)
        assertEquals(150, eval.remainingDays)
    }

    @Test
    fun `evaluation by whichever comes first - days triggers first`() {
        val lastDoneMs = 1_000_000_000L
        val item = MaintenanceItemEntity(
            id = 4,
            bikeId = 1,
            name = "General Service",
            intervalKm = 6_000.0,
            intervalDays = 180,
            lastDoneOdometerKm = 10_000.0,
            lastDoneDateEpochMs = lastDoneMs
        )

        // Only 500 km ridden (km OK), but 178 days elapsed (2 days remaining -> due soon)
        val eval = MaintenanceCalculator.evaluate(item, 10_500.0, lastDoneMs + (178 * oneDayMs))
        assertEquals(MaintenanceStatus.DUE_SOON, eval.status)
        assertEquals(5500.0, eval.remainingKm!!, 0.001)
        assertEquals(2, eval.remainingDays)
    }

    @Test
    fun `reminder de-duplication logic - prevents spamming within 7 days unless newly due`() {
        val now = 2_000_000_000L
        val sixDaysAgo = now - TimeUnit.DAYS.toMillis(6)
        val eightDaysAgo = now - TimeUnit.DAYS.toMillis(8)
        val antiSpamDurationMs = TimeUnit.DAYS.toMillis(7)

        val baseItem = MaintenanceItemEntity(
            id = 5,
            bikeId = 1,
            name = "Chain Lube",
            intervalKm = 500.0,
            intervalDays = 30,
            lastDoneOdometerKm = 1000.0,
            lastDoneDateEpochMs = now - TimeUnit.DAYS.toMillis(40),
            lastNotifiedDueEpochMs = null
        )

        // Case 1: Status is OK -> Never notify
        assertFalse(MaintenanceCalculator.shouldNotify(baseItem, MaintenanceStatus.OK, now, antiSpamDurationMs))

        // Case 2: Never notified -> Should notify for DUE_SOON and OVERDUE
        assertTrue(MaintenanceCalculator.shouldNotify(baseItem, MaintenanceStatus.DUE_SOON, now, antiSpamDurationMs))
        assertTrue(MaintenanceCalculator.shouldNotify(baseItem, MaintenanceStatus.OVERDUE, now, antiSpamDurationMs))

        // Case 3: Notified 6 days ago -> Suppress notification
        val notifiedRecently = baseItem.copy(lastNotifiedDueEpochMs = sixDaysAgo)
        assertFalse(MaintenanceCalculator.shouldNotify(notifiedRecently, MaintenanceStatus.DUE_SOON, now, antiSpamDurationMs))

        // Case 4: Notified 8 days ago -> Notify again (weekly reminder)
        val notifiedLongAgo = baseItem.copy(lastNotifiedDueEpochMs = eightDaysAgo)
        assertTrue(MaintenanceCalculator.shouldNotify(notifiedLongAgo, MaintenanceStatus.DUE_SOON, now, antiSpamDurationMs))

        // Case 5: Serviced after last notification -> Always notify for new due event
        val servicedAfterNotified = baseItem.copy(
            lastNotifiedDueEpochMs = sixDaysAgo,
            lastDoneDateEpochMs = sixDaysAgo + 1000L
        )
        assertTrue(MaintenanceCalculator.shouldNotify(servicedAfterNotified, MaintenanceStatus.OVERDUE, now, antiSpamDurationMs))
    }
}
