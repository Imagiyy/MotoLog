# MotoLog — Progress Tracker

## Stage 0: Project Setup
**Status:** COMPLETED

### Built
- [x] Gradle setup (AGP 9.4.0, Kotlin 2.4.20, KSP 2.3.12, Gradle 9.7.1)
- [x] Version catalog (`libs.versions.toml`)
- [x] `.gitignore` for Android
- [x] App module with `compileSdk=37`, `targetSdk=36`, `minSdk=26`
- [x] R8/minification enabled for release builds
- [x] Hilt setup (`MotoLogApp`, `MainActivity`, DI modules)
- [x] Room database with entities: `BikeEntity`, `RideEntity`, `RidePointEntity`
- [x] Room DAOs: `BikeDao`, `RideDao`, `RidePointDao`
- [x] Room schema export to `schemas/`
- [x] DataStore Preferences with `SettingsRepository`
- [x] Material 3 theme (Light, Dark, AMOLED variants)
- [x] Navigation Compose with Live and History screens
- [x] `TrackingConstants.kt` with all AGENTS.md thresholds
- [x] Unit tests for constants and database entities
- [x] GitHub Actions CI workflow (`.github/workflows/ci.yml`)
- [x] ProGuard/R8 rules for release builds
- [x] Vector adaptive launcher icons (`ic_launcher.xml`, `ic_launcher_round.xml`)
- [x] Service package placeholder

### Verified
- [x] `./gradlew assembleDebug` passes
- [x] `./gradlew test` (unit tests) passes
- [x] R8 rules and Hilt bytecode transformations succeed
- [ ] App launches on physical device — PENDING (Stage 1 manual test)

### Open Issues
- None for Stage 0. Ready for Stage 1.

---

## Stage 1: Start/Stop + Live Speed
**Status:** BUILT & TESTED (PENDING DEVICE VERIFICATION)

### Built
- [x] Pure Kotlin `LocationPoint` domain model (no Android framework imports)
- [x] `LocationSource` domain interface for clean layer abstraction
- [x] `FusedLocationSource` using Google's `FusedLocationProviderClient` (1s interval, 1s fastest, high accuracy)
- [x] Hilt `LocationModule` providing `FusedLocationProviderClient` and binding `LocationSource`
- [x] Location permissions declared in manifest: `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` only
- [x] Contextual permission request flow on Start with rationale, approximate location warning, and deep link to system settings for permanent denial
- [x] One-time rider safety disclaimer dialog with DataStore persistence (`SettingsRepository.disclaimerAccepted`)
- [x] Keep-screen-on setting integration (`FLAG_KEEP_SCREEN_ON`) via DataStore
- [x] Glanceable live speed screen with 110sp bold digits and 3 metric placeholders (Distance, Moving Time, Avg Speed)
- [x] "Waiting for GPS" state with target threshold (≤ 25 m per `TrackingConstants.MIN_GPS_ACCURACY_METERS`) and live accuracy readout
- [x] Stationary noise filter: GPS speeds < 1.5 km/h displayed as 0 km/h
- [x] Glove-friendly 2-second Hold-to-Stop button with circular progress ring animation and haptic feedback
- [x] Complete release of location updates on Stop and ViewModel `onCleared`
- [x] `LiveViewModelTest` unit tests with `FakeLocationSource` covering all state transitions

### Verified
- [x] `./gradlew test` (unit tests: state transitions, GPS gating, stationary speed filtering, stop cleanup) — VERIFIED
- [x] `./gradlew assembleDebug` (debug APK build passes with 0 errors) — VERIFIED
- [x] `./gradlew lintDebug` (Android Lint passes with 0 errors) — VERIFIED
- [ ] Speed updates on physical device while moving — NEEDS DEVICE TEST
- [ ] Location icon completely disappears from status bar on Stop — NEEDS DEVICE TEST
- [ ] 2-second Hold-to-Stop gesture feel with riding gloves — NEEDS DEVICE TEST

### Open Issues
- Real-device field test needed to confirm GPS fix acquisition and status bar icon dismissal.

---

## Stage 2: Calculation Engine
**Status:** BUILT & VERIFIED VIA TESTS

### Built
- [x] Pure Kotlin `GpsPoint` domain class (no Android framework dependencies, JVM testable)
- [x] `RideStats` immutable statistics snapshot model
- [x] `PointFilterResult` sealed interface (`Accepted`, `Rejected` with `RejectionReason`)
- [x] `RideCalculator` streaming processor with batch `processAll` companion support
- [x] Haversine distance formula with mean earth radius $R = 6,371,000.0\text{ m}$
- [x] Speed fallback logic: prefers GPS speed, falls back to $\Delta d / \Delta t$ if missing, zeroes noise $< 1.5\text{ km/h}$
- [x] Strict point filtering pipeline:
  - Reject accuracy $> 25.0\text{ m}$ (`MIN_GPS_ACCURACY_METERS`)
  - Reject non-monotonic timestamps ($\Delta t \le 0$)
  - Reject implied speed spikes $> 250.0\text{ km/h}$ (`MAX_PLAUSIBLE_SPEED_KMH`)
  - Reject implied acceleration spikes $> 15.0\text{ m/s}^2$ (`MAX_ACCELERATION_MS2`)
  - Signal gap detection: $\Delta t > 10.0\text{ s}$ (`GAP_THRESHOLD_MS`), $0\text{ m}$ distance added across gaps
  - Low-speed stationary jitter filter: speed $< 3.0\text{ km/h}$ and $\Delta d < \text{accuracy radius} \implies 0\text{ m}$ distance added
- [x] Robust max speed algorithm: rejects unconfirmed single-point speed glitches unless Doppler GPS confidence $\le 1.5\text{ m/s}$
- [x] Test-only `GpxParser` for track point ingestion
- [x] Deterministic programmatic test fixtures (`stationary_jitter.gpx`, `highway_ride.gpx`, `stop_and_go.gpx`, `tunnel_gap.gpx`, `city_ride.gpx`)
- [x] Unit test suite (`RideCalculatorTest`) asserting analytical ground truths
- [x] Wired `RideCalculator` into `LiveViewModel` and `LiveScreen` metric cards with toggleable Moving vs Overall elapsed views (Distance in km, Moving/Elapsed Time, Moving/Overall Avg Speed in km/h)
- [x] Stationary distance decoupling fix: speed < 1.5 km/h strictly suppresses distance addition, preventing drift accumulation while stationary
- [x] Early-ride speed stabilization: gracefully returns current speed during first 5 seconds to prevent small-denominator GPS quantization noise
- [x] Monotonic hardware clock in `FusedLocationSource`: switched to `location.elapsedRealtimeNanos / 1_000_000L` to eliminate wall-clock UTC jitter

### Verified
- [x] `./gradlew test` (35 unit tests pass) — VERIFIED
- [x] Stationary jitter fixture (5 minutes of GPS drift adds under 10 m: strictly 0.0 m added) — VERIFIED
- [x] Stationary drift test: zero speed does not add distance even if displacement exceeds accuracy radius — VERIFIED
- [x] Traffic light stop test: moving average speed does not increase while stopped — VERIFIED
- [x] Highway ride fixture (sustained 90 km/h computes 30 km and 90 km/h average) — VERIFIED
- [x] Stop-and-go fixture (correctly separates moving vs stopped time) — VERIFIED
- [x] Tunnel gap fixture (gap marked, zero distance added across the 45s blackout) — VERIFIED
- [x] City ride fixture (urban route with traffic lights calculates accurately) — VERIFIED
- [x] Edge case tests (speed spike rejection, acceleration spike rejection, non-monotonic timestamps, poor accuracy rejection, fallback speed) — VERIFIED
- [x] `./gradlew assembleDebug` (debug APK packaging passes with 0 errors) — VERIFIED
- [x] `./gradlew lintDebug` (0 lint errors) — VERIFIED
- [ ] Real-world live speed smoothing on physical device — PENDING USER RETEST

### Thresholds & Judgment Calls
- **Earth Radius:** Used $R = 6,371,000.0\text{ m}$ (WGS-84 mean radius).
- **Max Speed Robustness:** Required either Doppler GPS confidence (`speedAccuracyMps` $\le 1.5\text{ m/s}$) or confirmation over 2 consecutive readings within 15% tolerance.
- **Acceleration Spike:** Computed between consecutive speeds $\Delta v / \Delta t$ against $15.0\text{ m/s}^2$.
- **Signal Gap:** Points separated by $> 10.0\text{ s}$ marked as gaps with zero distance accumulated across them.
- **Average Speed Stability:** Stationary speed (< 1.5 km/h) enforces 0.0 m distance added; moving time and distance strictly coupled; early ride (< 5s) displays current speed to prevent discretization swings.

---

## Stage 3: Foreground Service + Persistence
**Status:** BUILT & TESTED (PENDING DEVICE VERIFICATION)

### Built
- [x] Manifest declarations: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_LOCATION`, `POST_NOTIFICATIONS` (No `ACCESS_BACKGROUND_LOCATION`)
- [x] `TrackingService` declared with `android:foregroundServiceType="location"` and `android:exported="false"`
- [x] `TrackingSessionState` sealed interface modeling session lifecycle (`Idle`, `WaitingForGps`, `Tracking`, `Stopped`)
- [x] `TrackingRepository` singleton domain interface and `TrackingRepositoryImpl` bridging `TrackingService` and `LiveViewModel`
- [x] `RideNotificationManager` creating low-importance persistent notification channel (`motolog_tracking_channel`)
- [x] Persistent ongoing notification showing live speed, distance, moving time, Pause/Resume action, and Stop action
- [x] Android 14+ `ServiceCompat.startForeground` location type flag compliance
- [x] Room continuous persistence:
  - Creates `RideEntity` with status `ACTIVE` upon session start
  - Buffer queue (`pointChannel`) with background coroutine writing points every 3 seconds (`DB_BATCH_WRITE_INTERVAL_MS = 3_000L`) via `ridePointDao.insertAll`
  - Periodic summary checkpointing to `RideEntity` every 10 seconds (`CHECKPOINT_INTERVAL_MS = 10_000L`)
  - Guaranteed buffer flush upon Stop before marking status `COMPLETED`
- [x] Manual Pause / Resume:
  - Freezes distance and moving time accumulation while keeping service, notification, and session active
  - Points recorded while paused marked with `isPaused = true`
  - Glove-friendly in-app toggle button (>= 56dp) and lock-screen notification action
- [x] Crash & force-kill recovery:
  - Startup detection of unfinished `ACTIVE` rides via `rideDao.findActiveRide()`
  - Reconstructs accurate stats from stored points using `RideCalculator.processAll()`
  - Sets ride status to `RECOVERED` with end time set to the last stored point
  - Unmissable UI recovery prompt modal with "Recover" and "Discard" options (no silent auto-resumption)
- [x] Battery optimization guidance:
  - First-ride explanatory dialog detailing aggressive OEM background task termination (Samsung, Xiaomi, OnePlus, etc.)
  - Deep-link to Android battery settings (`Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS`) without requesting restricted permissions
- [x] Edge case handling: duplicate Start taps ignored; Stop cleanly ceases location updates, flushes pending writes, dismisses notification, and stops service
- [x] 100m Start Confirmation Gate:
  - Added `START_CONFIRMATION_DISTANCE_METERS = 100.0` in `TrackingConstants.kt`
  - In `RideCalculator.kt`, speed and distance display remain 0.0 while straight-line displacement or cumulative motion is under 100 m
  - Discard drift/miscalculation before bike starts rolling: pending movement and moving time are buffered
  - Once 100 m is reached, `isConfirmed` triggers and retroactively credits all buffered distance and time into `totalDistanceMeters` and `movingTimeMs` so no ride distance is lost
- [x] Comprehensive test suite:
  - `RideCalculatorTest.startConfirmation_suppressesSpeedAndDistanceUnder100mAndCreditsOnCrossingThreshold` verifying 0.0 under 100m and full credit on crossing 100m
  - `RideRecoveryTest` verifying crash recovery point reconstruction and accurate summary recalculation
  - `LiveViewModelTest` with `FakeTrackingRepository` covering all session transitions, recovery prompt, and settings toggles

### Verified
- [x] `./gradlew test` (43 unit tests pass across calculation engine, fixtures, recovery, and ViewModel) — VERIFIED
- [x] `./gradlew lintDebug` (Android Lint passes with 0 errors) — VERIFIED
- [x] `./gradlew assembleDebug` (Debug APK built with 0 errors, copied to `MotoLog.apk`) — VERIFIED
- [ ] 30-minute ride tracking with screen turned off on physical device — NEEDS DEVICE TEST
- [ ] Notification Pause/Resume and Stop actions from lock screen — NEEDS DEVICE TEST
- [ ] Force-killing the app mid-ride and verifying Recovery prompt on relaunch — NEEDS DEVICE TEST
- [ ] Status bar GPS icon dismissal immediately upon Stop — NEEDS DEVICE TEST

### Thresholds & Judgment Calls
- **100m Start Confirmation Gate:** Set to 100.0 meters (`START_CONFIRMATION_DISTANCE_METERS`). Prevents GPS jitter, driveway stationary drift, or smartphone handling from starting speed/distance calculations prematurely. Once 100 meters is confirmed, the full 100 meters and corresponding moving time are credited immediately.
- **Notification Update Rate:** Throttled to 2 seconds (`NOTIFICATION_UPDATE_INTERVAL_MS = 2_000L`) to eliminate system UI churn and battery consumption.
- **Stop Action Protection:** In notification, "Stop (In-App)" opens the application directly to the 2-second Hold-to-Stop screen to guarantee 100% protection against accidental termination from pocket bumps or gloves.
- **Batch Interval:** 3.0 seconds (`DB_BATCH_WRITE_INTERVAL_MS`) offloaded to `Dispatchers.IO` using an unlimited memory queue to prevent point drops during slow flash writes.
- **Checkpoint Interval:** 10.0 seconds (`CHECKPOINT_INTERVAL_MS`) updating summary fields on the active `rides` row.
- **Crash Recovery Policy:** Explicitly offers "Recover" or "Discard" rather than silently auto-resuming tracking. Silently resuming after a multi-hour process death could invent false gaps or drain battery while parked.

---

## Stage 4: Auto-Pause and Signal Handling
**Status:** BUILT & TESTED (PENDING DEVICE VERIFICATION)

### Built
- [x] Pure Kotlin `AutoPauseStateMachine` domain class (no Android framework imports, JVM testable)
- [x] Auto-pause state transitions based on `TrackingConstants`:
  - Enters `AUTO_PAUSED` when filtered speed stays below 3.0 km/h (`AUTO_PAUSE_SPEED_THRESHOLD_KMH`) for 8 seconds (`AUTO_PAUSE_DELAY_SECONDS`)
  - Moving time and distance accumulation strictly freeze while paused; stopped time accumulates instead
  - Auto-resumes to `RECORDING` when speed exceeds 5.0 km/h (`AUTO_RESUME_SPEED_THRESHOLD_KMH`)
  - Hysteresis: slow crawl speeds between 3.0 and 5.0 km/h stay in `AUTO_PAUSED` without flapping in and out of pause
- [x] Manual pause & auto-pause coexistence rules:
  - Manual pause always wins and overrides auto-pause
  - Auto-resume never overrides manual pause (high speeds while manually paused remain paused)
  - Manually resuming while stationary resets the 8-second stationary timer, preventing immediate re-pausing
  - Auto-pause toggle off in settings bypasses auto-pause and adheres strictly to Stage 2 calculation behavior
- [x] Settings integration:
  - Added `SettingsScreen` with high-contrast Material 3 design and back navigation
  - Added `SettingsRoute` and bottom navigation / top-bar gear icon
  - DataStore-backed `autoPauseEnabled` toggle (enabled by default)
  - Dynamic propagation to `TrackingService` and `RideCalculator`
- [x] GPS Signal Loss detection:
  - Continuous 10-second timeout monitor (`GPS_SIGNAL_LOST_TIMEOUT_SECONDS`) detecting absence of valid location fixes
  - Hardware availability callback monitor via `FusedLocationSource.getLocationAvailability()`
  - High-contrast visual status badge ("GPS SIGNAL LOST" in red/amber) and satellite warning banner on Live screen
  - Notification updates immediately to reflect "MotoLog: GPS Signal Lost"
- [x] Gap handling & Signal re-acquisition:
  - Points separated by $> 10\text{ s}$ marked with `isGap = true` in Room database
  - Zero distance added across gaps; zero moving time added across gaps (moving state during blackout is unknown)
  - Elapsed time continues to tick monotonically
  - When fix returns, requires accuracy fix $\le 25.0\text{ m}$ before feeding to engine
  - First point after gap acts as fresh anchor without phantom distance or speed spike
- [x] Injectable `Clock` abstraction in `com.abrar.motolog.domain.time`:
  - Allows JVM unit tests to simulate auto-pause delay and signal loss timeouts deterministically with zero real-time waiting
- [x] Crash Recovery compatibility:
  - Updated `TrackingRepositoryImpl.recoverActiveRide()` and `RideCalculator.processAll()` to respect stored `isPaused` and `isGap` flags
  - Reconstructed rides faithfully reflect recorded pauses and signal gaps
- [x] UI & Notification updates:
  - Distinct status badges: "RECORDING" (Green), "AUTO-PAUSED" (Amber), "PAUSED" (Orange), "GPS SIGNAL LOST" (Red)
  - Speedometer subtitle dynamically shows "KM/H (AUTO-PAUSED)" vs "KM/H (PAUSED)" vs "KM/H"
  - Notification displays exact state and keeps glove-friendly lock-screen Pause/Resume and Stop actions

### Verified
- [x] `./gradlew test` (53 unit tests pass across calculation engine, fixtures, recovery, auto-pause, and ViewModel) — VERIFIED
- [x] `./gradlew lintDebug` (Android Lint passes with 0 errors) — VERIFIED
- [x] `./gradlew assembleDebug` (Debug APK built with 0 errors, refreshed at `MotoLog.apk`) — VERIFIED
- [x] `AutoPauseTest.stopAndGoFixture_expectedMovingVsStoppedTimeWithinTolerance`: 300s moving, 300s stopped within tolerance — VERIFIED
- [x] `AutoPauseTest.stationaryPhone_goesToAutoPausedAfter8SecondsAndStaysThere`: 8s transition and 5 min stay — VERIFIED
- [x] `AutoPauseTest.slowCrawlBetween3And5Kmh_noFlappingHysteresis`: 3–5 km/h holds auto-pause; > 5 km/h resumes — VERIFIED
- [x] `AutoPauseTest.manualPausePlusAutoPauseCombinations`: manual wins, high speed ignored, stationary resume resets timer — VERIFIED
- [x] `AutoPauseTest.signalLostAfterTimeout_recoversWithoutPhantomDistance`: 45s blackout adds 0 distance and 0 speed spike — VERIFIED
- [x] `AutoPauseTest.tunnelGapFixture_fromStage2StillPasses`: 45s gap handled accurately — VERIFIED
- [x] `AutoPauseTest.autoPauseToggleOff_matchesStage2Results`: matches Stage 2 baseline identically — VERIFIED
- [x] `AutoPauseTest.recoveryCompatibility_reflectsAutoPauseAndGaps`: stored flags accurately reconstructed — VERIFIED
- [x] `LiveViewModelTest`: UI state mapping for `AUTO_PAUSED` and `isGpsLost` — VERIFIED
- [ ] Emulator stop-and-go route replay — NEEDS DEVICE TEST (no active emulator attached)
- [ ] Real-device motorcycle test for auto-pause and tunnel recovery — NEEDS DEVICE TEST

### Thresholds & Judgment Calls
- **Gap Moving Time Policy:** Moving time during a GPS blackout gap is treated as strictly 0.0 seconds (state unknown). Stored elapsed time continues to accumulate normally.
- **Stationary Resume Buffer:** Tapping resume while stationary resets the 8-second delay timer so the rider is not trapped in an instant auto-pause loop while pulling out of an intersection or driveway.
- **Hysteresis Band:** Speed between 3.0 km/h and 5.0 km/h does not flap state. Requires $> 5.0\text{ km/h}$ to resume and $< 3.0\text{ km/h}$ for 8s to pause.

---

## Stage 5: History and Ride Detail
**Status:** BUILT & TESTED (PENDING DEVICE VERIFICATION)

### Built
- [x] Pure Kotlin `RideSplit` domain model (unit-agnostic distance, moving duration, average speed, partial split indicator)
- [x] Pure Kotlin `SplitCalculator` domain engine:
  - Unit-agnostic default split distance (1,000.0 m for km, expandable to 1,609.344 m for miles in Stage 8)
  - Strict filtering: drops points with accuracy > 25.0 m
  - Respects pauses and gaps: points with `isPaused == true` or `isGap == true` add 0 distance and 0 moving time
  - Linear boundary interpolation: splits close cleanly at exact distance thresholds, carrying residual remainder to next split
  - Final partial split computation (emits incomplete trailing segment if >= 1.0 m)
  - Zero/negative division safeguards (duration 0 or distance 0 safely returns 0.0 km/h, preventing NaN or Infinity)
- [x] `RideNameGenerator` utility:
  - Deterministic time-of-day name generation based on start timestamp local hour (Morning: 05:00–11:59, Afternoon: 12:00–16:59, Evening: 17:00–20:59, Night: 21:00–04:59)
  - Requires no network, no GPS geocoding, and no external services
  - Sanitization logic: trims whitespace, falls back to default name on blank/empty input, truncates to 50 characters max
- [x] Room & Database additions:
  - `RideDao.getFinishedRides()`: reactive Flow querying `status IN ('COMPLETED', 'RECOVERED') ORDER BY startTime DESC` (guaranteeing `ACTIVE` rides never appear in finished history)
  - `RideDao.updateRideName()`: updates ride name
  - Populates default time-of-day name in `TrackingService.handleStop()` and `TrackingRepositoryImpl.recoverActiveRide()` if ride name is blank
  - Clean cascade deletion: `RidePointEntity` cascades deletion via ForeignKey, complemented by explicit repository point deletion ensuring zero orphaned points
- [x] `RideRepository` domain interface & `RideRepositoryImpl`:
  - Manages reactive finished rides stream
  - Non-destructive delayed hard delete with in-memory pending deletions set for undo support
  - Converts Room entities to domain `GpsPoint`s for off-thread calculation
- [x] History list screen (`HistoryScreen`):
  - Replaces Stage 0 placeholder in bottom navigation
  - Newest rides first with glanceable cards (Name, Date/Time, Distance, Moving Time, Avg Moving Speed)
  - Small amber `RECOVERED` badge on crash-recovered rides (unmarked on normal completed rides)
  - Trash icon action triggering immediate list removal and 5-second Undo snackbar
  - Friendly empty state when no rides exist
- [x] Ride detail screen (`RideDetailScreen`):
  - Type-safe navigation route: `RideDetailRoute(val rideId: Long)`
  - Displays Total Distance hero card
  - Glanceable Moving vs Overall toggle pill (Moving Time & Moving Avg Speed vs Elapsed Time & Overall Avg Speed)
  - Secondary metrics: Peak Max Speed, Stopped Time, Elapsed Time, and optional Elevation Gain
  - Per-kilometre splits table (Split #, Distance, Time, Avg Speed) computed off the main thread on `Dispatchers.Default`
  - Inline rename dialog with trimming, length limit, and default name fallback
- [x] Comprehensive test suite (74 unit tests passing):
  - `SplitCalculatorTest`: steady 5 km ride, 60s pause handling, 45s tunnel gap handling, partial final split, < 1 km short ride, stationary noise
  - `RideNameGeneratorTest`: morning/afternoon/evening/night time buckets, sanitization, whitespace trim, length limit
  - `RideRepositoryImplTest`: reactive flow, pending deletion hiding, undo restoration, cascade delete leaving no orphans, entity-to-domain mapping
  - `HistoryViewModelTest`: finished rides loading, immediate card dismissal on delete, undo restoration, 5s timeout database commit
  - `RideDetailViewModelTest`: detail loading, off-thread splits calculation, moving vs overall toggle, rename dialog validation and fallback

### Verified
- [x] `./gradlew test --rerun-tasks` (all 74 unit tests pass) — VERIFIED
- [x] `./gradlew lintDebug` (Android Lint passes with 0 errors) — VERIFIED
- [x] `./gradlew assembleDebug` (Debug APK built with 0 errors, refreshed at `MotoLog.apk`) — VERIFIED
- [ ] Persistence across app restarts on physical device — NEEDS DEVICE TEST
- [ ] Tap-to-rename and keyboard input with motorcycle gloves — NEEDS DEVICE TEST
- [ ] Delete with Undo snackbar interaction on physical phone — NEEDS DEVICE TEST

### Thresholds & Judgment Calls
- **Source of Truth for Summary Numbers:** Stored `RideEntity` summary columns (total distance, elapsed time, moving time, stopped time, average speeds, max speed) are the **single source of truth**. Primary stats on the detail screen are read directly from the database rather than re-calculated from raw GPS points. This guarantees a 100% exact numerical match with what the rider saw on their handlebar when they pressed Stop, avoiding floating-point drift or discrepancies from the initial 100m start confirmation gate.
- **Delete-with-Undo Design:** Implemented as a **Delayed Hard Delete with an In-Memory Pending Deletion Set in `RideRepository`**. When deleted, the ride is immediately removed from the active list Flow and held in `_pendingDeletionRideIds`. If "Undo" is tapped or cancelled, the ID is removed and the card reappears instantly with zero database operations. If the 5-second window expires, a database transaction purges both the ride and its points. If the app process is terminated mid-undo by the OS or battery manager, the database was untouched—the ride remains completely safe and intact on next launch. This avoids adding a soft-delete tombstone column (`isDeleted`) to Room and keeps the database schema cleanly at version 1.
- **Partial Split Threshold:** Set to 1.0 meter (`currentSplitDist >= 1.0`). Any leftover distance equal to or exceeding 1 meter at the end of a ride produces a partial split (e.g. 0.42 km) with its respective moving duration and average speed, while sub-meter floating-point discretization errors are cleanly ignored.
- **Default Names:** Purely time-of-day based (Morning/Afternoon/Evening/Night ride), requiring zero network connectivity, zero geocoding latency, and zero background permissions.

### Manual Phone Checklist for Stage 5
1. **Ride List & Empty State:**
   - Launch fresh app -> tap **History** tab -> verify friendly empty state ("No Rides Yet. Start recording your motorcycle rides from the Live tab").
   - Record a short ride on the **Live** tab, hold Stop for 2 seconds -> switch to **History** tab.
   - Verify the new ride appears at the top of the list with formatted date/time, distance, moving time, and average moving speed.
   - Verify active rides do NOT appear in the History list.
2. **Crash Recovery Label:**
   - Force kill the app mid-ride -> reopen -> tap "Recover Ride" on prompt -> check **History** tab.
   - Verify the recovered ride displays the amber "RECOVERED" badge.
3. **Ride Detail & Moving vs Overall Toggle:**
   - Tap any ride in the history list -> verify navigation to Ride Detail screen.
   - Verify the distance hero card and metrics match the summary shown at Stop.
   - Tap the **Moving** / **Overall** toggle:
     - In **Moving** mode: verify Moving Time and Moving Avg Speed are shown.
     - In **Overall** mode: verify Elapsed Time and Overall Avg Speed are shown.
   - Verify Peak Max Speed and Stopped Time display expected values.
4. **Per-Km Splits Table:**
   - Scroll down to the **Per-Kilometre Splits** section.
   - For rides > 1 km, verify full 1.00 km splits with time and average speed.
   - Verify the last split is marked as partial if the distance didn't end on an exact kilometre boundary.
5. **Rename Ride:**
   - Tap the Edit (pencil) icon in the top app bar -> enter a new name -> tap Save.
   - Verify title updates immediately and persists when returning to the History list.
   - Edit again, clear the text completely -> tap Save -> verify it falls back to the default time-of-day name.
6. **Delete with Undo:**
   - In History list, tap the trash can icon on a ride card.
   - Verify the card disappears immediately from the list.
   - Verify the "Ride deleted" snackbar appears with the "Undo" action.
   - Tap "Undo" -> verify the ride instantly reappears in its exact position.
   - Delete another ride -> let the 5-second snackbar dismiss naturally -> restart app -> verify the ride is permanently gone and no points remain.

---

## Stage 6: Garage (Bikes, Odometer, Maintenance, Fuel)
**Status:** BUILT & TESTED (PENDING DEVICE VERIFICATION)

### Built
- [x] Room Database Migration (Version 1 → 2):
  - Added `MaintenanceItemEntity` table (`maintenance_items`) with indices on `bikeId` and foreign key cascade deletion
  - Added `FuelLogEntity` table (`fuel_logs`) with indices on `bikeId` and foreign key cascade deletion
  - Added `odometerOffsetKm` and `isArchived` columns to `bikes` table with non-destructive defaults
  - Exported schema JSON (`app/schemas/com.abrar.motolog.data.local.MotoLogDatabase/2.json`)
  - Added migration test (`DatabaseMigrationTest`) verifying existing unassigned and assigned rides survive non-destructively
- [x] Room DAOs & Domain Interfaces:
  - `BikeDao` and `RideDao` methods for active/archived bikes, ride counts, and cumulative bike distance queries
  - `MaintenanceDao`: full CRUD, `markDone(id, odo, date)`, `updateLastNotified(id, time)`
  - `FuelLogDao`: full CRUD, reactive log streams, `getLatestLog(bikeId)`
  - `GarageRepository` interface & `GarageRepositoryImpl` bridging Room and domain flows
- [x] Pure Kotlin Domain Calculators (no Android framework imports, JVM testable):
  - `OdometerCalculator`: `calculateCurrentOdometerKm(initialOdo, recordedDistanceMeters, offsetKm)` and `calculateCalibrationOffset(initialOdo, recordedDistanceMeters, targetOdo)`
  - `MaintenanceCalculator`: evaluates item status (`OK`, `DUE_SOON`, `OVERDUE`) by km, days, or dual "whichever comes first" intervals; pure `shouldNotify` de-duplication helper
  - `FuelMileageCalculator`: Standard Full-Tank Method for motorcycle mileage; handles single fills, partial fill accumulation, and missed fills
- [x] Bikes Management & Selection:
  - Add, edit, and archive/delete bikes (name, make/model, initial odometer reading)
  - Prevent hard deletion of bikes with recorded rides; archives them instead
  - Multi-bike support with "current bike" selection remembered in DataStore Preferences (`currentBikeId`)
- [x] Ride Assignment:
  - Tag new rides with current bike at Start (non-blocking if no bike set up; prompt gently)
  - Existing rides from earlier stages left unassigned (`bikeId = null`) without data loss
  - Reassign a finished ride's bike dynamically from the Ride Detail screen (`ChangeBikeDialog`)
- [x] Maintenance Items & Notifications:
  - Presets: Engine Oil, Chain Lube & Tension, Brake Fluid, Tyre Pressure, Air Filter, Spark Plugs, Coolant, General Service
  - Status thresholds: OK, Due Soon (≤ 100 km or ≤ 7 days per `TrackingConstants`), Overdue
  - Anti-spam de-duplication: notifies once per due event; suppresses duplicate alerts within 7 days unless marked done and due again
  - Dedicated notification channel: `motolog_maintenance_channel` (`IMPORTANCE_DEFAULT`)
  - Suppressed during active rides to prevent rider disturbance mid-ride
  - Post-ride evaluation: automatically triggered in `TrackingService` on completion and in `TrackingRepositoryImpl` on recovery
  - Periodic daily evaluation: WorkManager `MaintenanceCheckWorker` scheduled once per 24 hours on app startup
- [x] Fuel Log & Full-Tank Mileage:
  - Full CRUD entries (date, odometer reading, litres, total cost, full-tank flag, notes)
  - Input validation: positive litres and cost, warning if odometer lower than previous fill
  - Detailed stats: Average Mileage, Best Fill, Worst Fill, Latest Fill, Total Spent, Total Litres
- [x] UI & Navigation:
  - Added `Garage` to bottom navigation bar (`GarageScreen`, `GarageViewModel`)
  - Bike Detail screen (`BikeDetailScreen`, `BikeDetailViewModel`) with tabbed interface: Overview, Maintenance, Fuel Log
  - Glove-friendly forms and dialogs usable with keyboard open and surviving configuration rotation
  - Updated Ride Detail screen with motorcycle assignment pill and reassignment dialog
- [x] Comprehensive Test Suite (98 unit tests passing across the project):
  - `OdometerCalculatorTest`: initial reading, distance accumulation, calibration offsets, deleted rides, bike reassignment
  - `MaintenanceCalculatorTest`: km intervals, days intervals, dual intervals, and reminder de-duplication
  - `FuelMileageCalculatorTest`: normal sequence, partial fills, single entry, missed fill, out-of-order dates
  - `DatabaseMigrationTest`: Room 1 → 2 migration preserving pre-existing rides and settings
  - `GarageViewModelTest`: active bikes loading, current bike selection, add bike dialog, archive/delete rules
  - `BikeDetailViewModelTest`: tabs, odometer calibration, add/edit/delete/mark done maintenance, add/delete fuel log
  - `RideDetailViewModelTest`: bike assignment and reassignment flow

### Verified
- [x] `./gradlew test` (all 98 unit tests pass) — VERIFIED
- [x] `./gradlew lintDebug` (0 lint errors) — VERIFIED
- [x] `./gradlew assembleDebug` (Debug APK built with 0 errors) — VERIFIED
- [x] Room migration test with pre-existing rides — VERIFIED
- [x] Odometer calculation and calibration test — VERIFIED
- [x] Fuel mileage standard full-tank calculator test — VERIFIED
- [x] Maintenance status and de-duplication test — VERIFIED
- [ ] Post-ride due-maintenance notification on physical device — NEEDS DEVICE TEST
- [ ] Daily WorkManager periodic execution on OEM background-restricted device — NEEDS DEVICE TEST
- [ ] Hold-to-Stop completing ride and assigning to current bike on real ride — NEEDS DEVICE TEST

### Thresholds & Judgment Calls
- **Odometer Calculation & Calibration:**
  $$\text{Current Odometer} = \text{Initial Odometer} + \left(\frac{\text{Total Recorded Distance Meters}}{1,000.0}\right) + \text{Odometer Offset}$$
  When the rider enters a real-world odometer reading $T$ to correct for drift or unrecorded rides:
  $$\text{Odometer Offset} = T - \left(\text{Initial Odometer} + \frac{\text{Total Recorded Distance Meters}}{1,000.0}\right)$$
  This maintains mathematical consistency when rides are subsequently deleted or reassigned without mutating historical ride data.
- **Handling Existing Rides from Previous Stages:**
  Left `bikeId = null` (unassigned). Fabricating a default bike would make assumptions about the rider's garage. Riders can reassign any past ride to a specific motorcycle at any time with one tap on the Ride Detail screen.
- **Bike Archiving vs Deletion:**
  Bikes with 0 recorded rides can be permanently deleted. Bikes with 1 or more recorded rides cannot be hard-deleted (which would orphan or cascade-delete ride history); they are archived instead, hiding them from the active garage and selection menus while preserving their ride history.
- **Standard Full-Tank Mileage Calculation:**
  Distance is calculated between consecutive full-tank fills:
  $$\text{Mileage} = \frac{\text{Odometer}_{\text{current full}} - \text{Odometer}_{\text{previous full}}}{\sum \text{Litres added at all fills since previous full (including current)}}$$
  Litres from the initial full fill are excluded. Partial fills accumulate their litres until the next full fill. Missed fills reset the measurement baseline to prevent distorted consumption figures.
- **Maintenance Anti-Spam & Disturbance Policy:**
  Reminders use a separate `IMPORTANCE_DEFAULT` channel (`motolog_maintenance_channel`). Notifications are suppressed during active rides to prevent lock-screen interference. An anti-spam window of 7 days prevents repeating alerts for the same due condition unless the task was marked done and entered a new cycle.

### Manual Phone Checklist for Stage 6
1. **Garage Tab & Bike Creation:**
   - Launch app -> tap **Garage** tab in bottom navigation.
   - Tap "Add Bike" / "Add Your First Bike".
   - Enter Name ("Honda CB300R"), Make/Model ("Honda"), and Current Odometer ("12500"). Tap Save.
   - Verify the bike card appears with "12,500 km", "Current Bike" badge, and "0 rides recorded".
2. **Current Bike Selection:**
   - Add a second bike ("Royal Enfield Himalayan", "RE", "8000").
   - Tap "Set as Current" on the second bike.
   - Verify the "Current Bike" badge moves to the Himalayan.
3. **Ride Assignment & Odometer Increment:**
   - Switch to **Live** tab -> verify current bike name is shown in the header/status banner.
   - Start a ride, move > 100 m, hold Stop for 2 seconds to complete.
   - Return to **Garage** tab -> verify the current bike's odometer increased by the recorded distance.
   - Open History -> tap the new ride -> verify the bike badge shows the assigned bike.
   - Tap "Change Bike" -> select the other bike -> verify the ride moves and both bikes' odometers update accordingly.
4. **Odometer Manual Calibration:**
   - Tap on a bike to open the **Bike Detail** screen.
   - Tap "Calibrate Odometer" / Edit Odometer -> enter a new value (e.g. 13,000 km) -> tap Save.
   - Verify the odometer updates immediately to 13,000 km and the offset is retained.
5. **Maintenance Items & Mark as Done:**
   - On Bike Detail screen, switch to **Maintenance** tab.
   - Tap "Add Maintenance Task" -> select preset "Chain Lube & Tension" (500 km / 30 days) -> tap Save.
   - Verify the task card displays with remaining km/days and an "OK" status badge.
   - Add a task with a low interval that triggers "Due Soon" or "Overdue" (e.g. 10 km).
   - Verify the amber or red warning badge appears.
   - Tap "Mark Done" -> enter current odometer -> tap Confirm -> verify status resets to "OK" and last-done date updates.
6. **Fuel Log & Mileage:**
   - Switch to **Fuel** tab -> tap "Add Fuel Fill".
   - Add first entry: 12,500 km, 10.0 L, Cost 1000, Full Tank checked. Tap Save.
     - Verify it shows "First fill (baseline set)".
   - Add second entry: 12,800 km, 8.5 L, Cost 850, Full Tank checked. Tap Save.
     - Verify mileage computes to: (12,800 - 12,500) / 8.5 = 300 / 8.5 ≈ 35.3 km/L.
     - Verify average and latest mileage stats cards update.
7. **Archive vs Delete Safety:**
   - On a bike with 0 rides: tap delete -> verify it asks for permanent deletion.
   - On a bike with recorded rides: tap archive -> verify it archives the bike without deleting ride history.

---

## Stage 7: Map and Visuals
**Status:** BUILT & TESTED (PENDING DEVICE VERIFICATION)

### Built
- [x] MapLibre Compose updated to 0.17.0 (Android support is upstream Beta)
- [x] `MapProvider` abstraction with MapLibre implementation and injectable `MapStyleProvider`
- [x] OpenFreeMap Liberty/light and Dark styles
- [x] OpenStreetMap/OpenMapTiles attribution visible and tappable on every route map
- [x] Route rendering with fixed five-bucket slow-to-fast speed colors, start/end markers, camera fit, and smooth camera animation
- [x] GPS gaps and paused intervals rendered as separate route segments with no connecting line
- [x] Ramer-Douglas-Peucker route downsampling with endpoint, structural, and speed-extreme preservation
- [x] Compose Canvas speed-over-time and elevation graphs with pause/gap breaks, max/average markers, and touch inspection
- [x] Pressure barometer sampling in the foreground service at approximately 1 Hz with FIFO latency and complete unregister on stop
- [x] GPS altitude fallback when no pressure reading is available; actual elevation source is recorded
- [x] Existing Room v3 elevation schema used without destructive migration; old rides without elevation remain valid
- [x] Pure Kotlin route, graph, speed-scale, elevation, and downsampling tests
- [x] First app network access documented: only map style, vector tile, glyph, and sprite requests from OpenFreeMap/MapLibre
- [x] No ride coordinates, ride identifiers, analytics, or telemetry are sent by app code
### Verified
- [x] `./gradlew :app:compileDebugKotlin` — VERIFIED
- [x] `./gradlew :app:testDebugUnitTest` — VERIFIED (129 tests passed)
- [x] `./gradlew build` — VERIFIED (debug/release build and tests passed)
- [x] `./gradlew lintDebug` — VERIFIED (0 lint errors)
- [x] 7,200-point synthetic two-hour route timing — VERIFIED (passed under the 250 ms test budget; observed 127 ms on the initial run)
- [ ] Map rendering, camera smoothness, lifecycle/rotation, and offline fallback on phone — NEEDS DEVICE TEST
- [ ] Barometer availability, elevation accuracy, and battery impact on phone — NEEDS DEVICE TEST
### Open Issues
- MapLibre Compose is Beta; the app is isolated behind `MapProvider` but future minor releases may change its API.
- OpenFreeMap public hosting has no SLA; the route remains visible when style/tile loading fails, but the basemap is unavailable.

### Manual Phone Checklist
1. Install the updated debug APK and open History.
2. Open a completed ride with at least two valid GPS points. Confirm the map shows the route, start/end markers, fixed speed legend, and tappable attribution.
3. Toggle light, dark, and AMOLED themes; confirm the map style follows the theme and the route remains visible.
4. Disable network access, reopen the ride detail screen, and confirm the route and graphs remain usable while the basemap is unavailable.
5. Confirm zoom and pan remain smooth, then leave and re-enter ride detail repeatedly to check for lifecycle or memory issues.
6. Complete a ride on a phone with a pressure sensor and confirm the summary source says barometer; repeat on a phone without one and confirm GPS fallback.
7. During a ride, pass through a tunnel or pause for several seconds and confirm route and graphs do not draw connecting lines across the gap or pause.
8. Confirm elevation gain/loss and the elevation profile are absent, not broken, for older rides without altitude data.
9. Observe battery behavior during a long screen-off ride and confirm barometer sampling stops immediately after Stop.

### Stage 7 Decisions
- Fixed speed scale is used for cross-ride comparison. P5/P95 bounds from the robust Stage 2 speed data prevent one noisy point from stretching the scale.
- Paused and GPS-gap segments are omitted from the active route line rather than implying movement through unknown intervals.
- Graphs use Compose Canvas; no chart dependency was added.
- Pressure altitude is relative and may have an absolute offset because standard sea-level pressure is used. Gain/loss uses smoothing and a dead-band threshold.
- OpenFreeMap permits commercial use and requires attribution: `OpenFreeMap © OpenMapTiles Data from OpenStreetMap`. MapLibre also supports automatic attribution; MotoLog renders its own visible attribution control and links to the OSM copyright page.

---

## Stage 8: Data Ownership and Settings
**Status:** NOT STARTED

### Built
### Verified
### Open Issues

---

## Stage 9: Play Store Readiness
**Status:** NOT STARTED

### Built
### Verified
### Open Issues
