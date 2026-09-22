# MotoLog — Motorcycle Ride Tracker

[![Android](https://img.shields.io/badge/Platform-Android-3DDC84.svg?style=flat&logo=android)](https://www.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF.svg?style=flat&logo=kotlin)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4.svg?style=flat&logo=jetpackcompose)](https://developer.android.com/jetpack/compose)
[![Architecture](https://img.shields.io/badge/Architecture-MVVM%20%2B%20Clean-FF6F00.svg?style=flat)]()
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

**MotoLog** is a modern, high-precision Android application designed specifically for motorcycle riders. Built from the ground up for handlebar mounts and pocket storage, MotoLog delivers glanceable telemetry, glove-friendly ergonomics, and resilient background tracking.

---

## 🏍️ Key Highlights

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
- **High-Precision GPS Engine:** Pure Kotlin domain math engine:
  - Rejects GPS multipath jitter and accuracy readings > 25m.
  - Clamps stationary noise (< 1.5 km/h) to zero.
  - Implied speed (> 250 km/h) and acceleration (> 15 m/s²) spike filtering.
  - Intelligent auto-pause (< 3 km/h for 8s) and auto-resume (> 5 km/h).
  - Separate moving average vs. overall average speed tracking.
- **Hardware-Aware Offline Maps:** MapLibre Native OpenGL ES vector rendering with fallback handling for entry-level devices.
- **100% Privacy & Device-Only Data:** All location data, rides, and routes stay on your device. No cloud accounts, tracking, or ads.

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

## 📐 Architecture & Technology Stack

MotoLog is built according to modern Android development standards and Clean Architecture principles:

- **Language:** Kotlin 2.0+ with Kotlin Coroutines and StateFlow.
- **UI Framework:** Jetpack Compose (Material 3) with custom Canvas graphics, hardware-accelerated draw calls, and zero allocations inside `DrawScope`.
- **Dependency Injection:** Dagger Hilt (`@HiltAndroidApp`, `@HiltViewModel`).
- **Persistence:**
  - **Room Database:** Continuous batching of GPS coordinates (`RidePoint`), rides (`Ride`), bikes (`Bike`), maintenance logs, and fuel entries.
  - **Jetpack DataStore:** Asynchronous preferences for settings, unit preferences (km/h vs. mph), themes, and speed alert thresholds.
- **Location:** Google Play Services `FusedLocationProviderClient` with `PRIORITY_HIGH_ACCURACY` and 1-second cadence.
- **Background Processing:** Android Foreground Service (`ServiceType.LOCATION`) with ongoing lock-screen notifications and `NotificationCompat.Action` buttons.
- **Mapping:** MapLibre Native SDK (OpenGL ES) with vector tile styling and device compatibility validation.
- **Domain Layer:** 100% pure Kotlin domain calculation engine with zero Android framework dependencies, fully validated against real-world GPX trace fixtures.

---

## 🛠️ Build & Installation

### Prerequisites
- **Android Studio:** Ladybug (2024.2+) or newer
- **JDK:** OpenJDK 17 or higher
- **Android SDK:**
  - Minimum SDK: `API 26` (Android 8.0 Oreo)
  - Target/Compile SDK: `API 35` (Android 15)

### Clone & Build
```bash
# Clone the repository
git clone https://github.com/your-username/motolog.git
cd motolog

# Run all unit tests (Domain math engine, GPX replay fixtures, ViewModels)
./gradlew testDebugUnitTest

# Assemble Debug APK
./gradlew assembleDebug
```

The compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔒 Permissions

MotoLog follows strict privacy and Play Store compliance standards:
- `ACCESS_FINE_LOCATION` & `ACCESS_COARSE_LOCATION`: Requested in-context only when the rider taps **Start Ride**.
- `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_LOCATION`: Powers uninterrupted tracking while the screen is off or another app is open.
- `POST_NOTIFICATIONS`: Android 13+ requirement to display the persistent tracking notification with lock-screen Pause/Stop actions.
- *Note:* `ACCESS_BACKGROUND_LOCATION` is **never requested**, guaranteeing full user transparency and compliant Play Store review.

---

## 🧪 Testing

The domain math engine contains comprehensive unit test suites covering real-world riding scenarios:
- **GPX Fixtures:** Highway rides, stop-and-go city traffic, mountain switchbacks, and tunnel GPS signal gaps.
- **Jitter Suppression:** Confirms a stationary phone resting for 5 minutes accumulates under 10 meters of phantom distance.
- **Spike Rejection:** Verifies filtering of erroneous GPS multipath speed leaps.
- Run tests via terminal:
  ```bash
  ./gradlew testDebugUnitTest --info
  ```

---

## 📄 License

This project is licensed under the Apache License 2.0. See the [LICENSE](LICENSE) file for details.
