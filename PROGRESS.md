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
**Status:** NOT STARTED

### Built
### Verified
### Open Issues

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
