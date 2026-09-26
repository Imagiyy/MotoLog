# MotoLog — Motorcycle Ride Tracker

[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20iOS%20(Shared)-3DDC84.svg?style=flat&logo=android)](https://www.android.com/)
[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin%20Multiplatform-2.4.20-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org/docs/multiplatform.html)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-KMP%20%2B%20Clean%20MVVM-FF6F00.svg?style=flat)]()
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

**MotoLog** is a modern, high-precision motorcycle ride tracking application built for handlebar mounts and pocket storage. Designed from the ground up for motorcycle riders wearing gloves, MotoLog delivers glanceable telemetry, customizable cockpit dashboards, resilient background tracking, and complete data ownership.

MotoLog uses a **Kotlin Multiplatform (KMP)** architecture where 100% of the mathematical calculation engine, GPS filtering, auto-pause state machine, unit conversion, formatting, and garage management logic live in a pure shared domain module (`:shared`), reusable across Android and iOS.

---

## 🏍️ Key Highlights

- **Kotlin Multiplatform Core:** Pure Kotlin domain engine (`:shared`) shared across Android and iOS with zero platform lock-in.
- **Glanceable Telemetry:** Ultra-high contrast, large typography designed for fast visual scanning while riding at speed.
- **5 Custom Themed Cockpits:** Dynamic instruments tailored to your motorcycle's personality:
  - 🏁 **Track Day:** Superbike TFT display inspired by MotoGP/WorldSBK instrumentation (Ducati Panigale / Yamaha R1) featuring dynamic LED shift-light strip and race telemetry.
  - ⚡ **Neon Cyber:** High-tech digital HUD with glowing hexagonal tachometer dial and neon cyan/magenta styling.
  - 🏜️ **Desert Rally:** Dakar-style navigation roadbook and digital rally tripmaster with desert gold accents.
  - ☕ **Cafe Racer:** Twin Smiths chronometric analog chrome instruments with warm vintage illumination.
  - 🕰️ **Vintage Retro:** Classic 1970s analog speedometer dial with mechanical rolling odometer drum and incandescent jewel indicator lamps.
- **Immediate Theme Preview:** Cockpit dashboards render instantly in the idle/standby state before starting a ride.
- **Immersive Full-Screen Landscape Mode:** When mounted horizontally on handlebars, navigation clutter is hidden to maximize speedometer gauge size. Swiping down or tapping the pull tab reveals an animated telemetry drawer with detailed numerical statistics and controls.
- **Glove-Friendly Ergonomics:** Minimum 56dp touch targets, high contrast, and a **2-second Hold-to-Stop** gesture with an animated progress ring to prevent accidental cancellation from road bumps or vibration.
- **Foreground Tracking Engine:** Continuous background tracking with persistent notification controls (Pause / Resume / Stop), crash/kill recovery on reboot, and batch persistence to Room.
- **High-Precision GPS Engine:** Pure domain math engine:
  - Rejects GPS multipath jitter and accuracy readings > 25m.
  - Clamps stationary noise (< 1.5 km/h) to zero.
  - Implied speed (> 250 km/h) and acceleration (> 15 m/s²) spike filtering.
  - Intelligent auto-pause (< 3 km/h for 8s) and auto-resume (> 5 km/h).
  - Separate moving average vs. overall average speed tracking.
  - 100-meter start confirmation gate preventing driveway drift accumulation.
- **Motorcycle Garage & Maintenance:** Multi-bike management with cumulative odometer tracking, calibration offset adjustments, maintenance interval alerts (km / days), and a standard full-tank fuel mileage calculator.
- **Hardware-Aware Offline Maps:** MapLibre Native OpenGL ES vector rendering with speed-colored route lines, elevation profiles, and fallback handling for entry-level devices.
- **100% Privacy & Device-Only Data:** All location data, rides, and routes stay on your device. Zero cloud accounts, tracking, or telemetry analytics. Full GPX 1.1 streaming export/import and ZIP backup/restore.

---

## 📱 Cockpit Dashboards

| Theme | Inspiration | Distinct Features |
|---|---|---|
| **Track Day** | WorldSBK / MotoGP TFT | Dynamic RPM-style shift light bar, session time, top speed peak, circuit map switcher |
| **Neon Cyber** | Cyberpunk HUD | Hexagonal angular tachometer dial, pulsing boost meters, grid accents |
| **Desert Rally** | Dakar Rally Tripmaster | Dual digital tripmeters, stage waypoint progress, compass bearing ribbon |
| **Cafe Racer** | 1960s British Twin Smiths | Dual chrome-bezeled dials (Speed + Tachometer), mechanical needle smoothing |
| **Vintage Retro** | 1970s Classic Analog | Brass-rimmed dial, mechanical rolling odometer drum, jewel indicator lamps |

---

## 📐 Architecture & Modular Structure

MotoLog follows Clean Architecture with a strict modular separation:

```
ride_tracker/
├── shared/                          # Kotlin Multiplatform Shared Module
│   ├── src/commonMain/kotlin/       # Pure Kotlin domain (models, engines, calculators)
│   │   └── com/abrar/motolog/shared/domain/
│   │       ├── engine/              # RideCalculator, SplitCalculator, AutoPause, Odometer...
│   │       ├── model/               # LocationPoint, GpsPoint, RideStats, MaintenanceTask...
│   │       ├── time/                # Clock abstraction backed by kotlinx-datetime
│   │       └── util/                # PlatformFormatter (expect), RideNameGenerator...
│   ├── src/androidMain/kotlin/      # Android-specific actuals (Locale.US string formatting)
│   ├── src/iosMain/kotlin/          # iOS-specific actuals (NSNumberFormatter RoundHalfUp)
│   └── src/commonTest/kotlin/       # Multiplatform tests, boundary checks & GPX fixtures
└── app/                             # Android Application Module
    ├── src/main/java/               # UI, DI, Storage, and Android Services
    │   └── com/abrar/motolog/
    │       ├── data/                # Room DB (v4), DataStore Preferences, Repositories
    │       ├── di/                  # Dagger Hilt Modules
    │       ├── service/             # TrackingService (Location Foreground Service)
    │       └── ui/                  # Jetpack Compose UI (Cockpits, History, Garage, Settings)
    └── src/test/java/               # Android unit tests & ViewModel tests
```

### Technology Stack
- **Kotlin:** 2.4.20 (Kotlin Multiplatform)
- **Multiplatform Libraries:** `kotlinx-datetime` (0.6.2), `kotlinx-coroutines-core` (1.10.1), `kotlinx-serialization-json` (1.7.3)
- **Android UI:** Jetpack Compose (Material 3) with hardware-accelerated Canvas graphics and zero allocations inside `DrawScope`
- **Dependency Injection:** Dagger Hilt (`@HiltAndroidApp`, `@HiltViewModel`)
- **Persistence:**
  - **Room Database (v4):** Continuous batching of GPS coordinates (`RidePoint`), rides (`Ride`), bikes (`Bike`), maintenance logs, and fuel entries
  - **Jetpack DataStore:** Asynchronous preferences for themes, units, tracking mode, and speed alerts
- **Location:** Google Play Services `FusedLocationProviderClient` with `PRIORITY_HIGH_ACCURACY` and 1-second cadence
- **Background Processing:** Android Foreground Service (`ServiceType.LOCATION`) with ongoing lock-screen notifications and `NotificationCompat.Action` buttons
- **Mapping:** MapLibre Native SDK (OpenGL ES) with OpenFreeMap vector tile styling
- **iOS Target:** Compiles to static framework `MotoLogShared` (`iosArm64`, `iosSimulatorArm64`, `iosX64`)

---

## 🛠️ Build & Installation

### Prerequisites
- **Android Studio:** Ladybug (2024.2+) or newer
- **JDK:** OpenJDK 17 or higher
- **Android SDK:**
  - Minimum SDK: `API 26` (Android 8.0 Oreo)
  - Target SDK: `API 36` (Android 15+)
  - Compile SDK: `API 37`

### Clone & Build
```bash
# Clone the repository
git clone https://github.com/your-username/motolog.git
cd motolog

# Run all unit tests (Shared KMP engine + Android App)
./gradlew test

# Run shared KMP common tests on JVM host
./gradlew :shared:testAndroidHostTest

# Compile iOS KLIBs (can run on Linux and macOS)
./gradlew :shared:compileKotlinIosSimulatorArm64

# Assemble Debug APK
./gradlew assembleDebug

# Build Production Release App Bundle (.aab)
./gradlew bundleRelease
```

The compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔒 Permissions & Privacy

MotoLog is built with strict privacy and Play Store compliance:
- `ACCESS_FINE_LOCATION` & `ACCESS_COARSE_LOCATION`: Requested in-context only when the rider taps **Start Ride**.
- `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_LOCATION`: Powers uninterrupted tracking while the screen is off or another app is open.
- `POST_NOTIFICATIONS`: Displays the persistent tracking notification with lock-screen Pause/Stop actions.
- `VIBRATE`: Provides haptic feedback for the 2-second Hold-to-Stop gesture and non-blocking speed alert notifications.
- **Never Requested:** `ACCESS_BACKGROUND_LOCATION` is **never requested**, guaranteeing full user transparency and avoiding strict Play Store review friction.
- **100% On-Device:** Location points and routes are stored locally in Room. Cloud auto-backup rules explicitly exclude the database.

---

## 🧪 Testing

The calculation engine and application are covered by comprehensive unit tests:
- **Shared Domain Tests (`:shared:testAndroidHostTest`):**
  - **Boundary & Rounding Tests:** Hardcoded expectations for half-way and binary-imprecise floats (`1.005`, `1.05`, `2.675`, `0.145`) validating `PlatformFormatter`.
  - **Deterministic GPX Fixtures:** Synthetic simulations of highway rides, stop-and-go city traffic, mountain switchbacks, and 45s tunnel signal gaps.
  - **Jitter & Drift Suppression:** Confirms a stationary phone resting for 5 minutes accumulates strictly 0.0 m of phantom distance.
  - **Spike Rejection:** Implied speeds > 250 km/h and accelerations > 15 m/s² are rejected.
  - **Auto-Pause & Recovery:** Validates the auto-pause state machine and crash-recovery reconstruction.
- **App & ViewModel Tests (`:app:testDebugUnitTest`):**
  - Database schema migrations (v1 → v2, v2 → v3, v3 → v4).
  - Lifecycle state transitions, permissions flow, and repository coordination.

---

## 📄 License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.

