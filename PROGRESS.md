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
**Status:** NOT STARTED

### Built
### Verified
### Open Issues

---

## Stage 4: Auto-Pause and Signal Handling
**Status:** NOT STARTED

### Built
### Verified
### Open Issues

---

## Stage 5: History and Ride Detail
**Status:** NOT STARTED

### Built
### Verified
### Open Issues

---

## Stage 6: Garage (Bikes, Odometer, Maintenance, Fuel)
**Status:** NOT STARTED

### Built
### Verified
### Open Issues

---

## Stage 7: Map and Visuals
**Status:** NOT STARTED

### Built
### Verified
### Open Issues

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
