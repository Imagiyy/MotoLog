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
**Status:** NOT STARTED

### Built
### Verified
### Open Issues

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
