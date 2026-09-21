# Permissions Justification: MotoLog

> **DRAFT, needs my review**  
> *Notice: This document details every permission requested in MotoLog's merged AndroidManifest.xml, the component requesting it, and its precise functional justification.*

---

### Permissions Requested Directly by MotoLog

| Permission | Protection Level | Purpose & Functional Justification | When Requested |
| :--- | :--- | :--- | :--- |
| `android.permission.ACCESS_FINE_LOCATION` | Dangerous (Runtime) | **Core Feature**: Needed to acquire precise GPS coordinates, Doppler GPS speed, altitude, and heading to calculate motorcycle speed, distance, per-km/mi splits, and route geometry. | In-context only when the rider taps "Start Ride". Never requested at app launch. |
| `android.permission.ACCESS_COARSE_LOCATION` | Dangerous (Runtime) | Android platform requirement paired with fine location. Used during initial GPS lock before full 3D satellite constellation fix. | Requested alongside fine location when rider taps "Start Ride". |
| `android.permission.FOREGROUND_SERVICE` | Normal | Allows the app to run `TrackingService` in the foreground so motorcycle ride tracking continues with the screen turned off or in a pocket. | Granted at install time. |
| `android.permission.FOREGROUND_SERVICE_LOCATION` | Normal (Target 34+) | Required by Android 14+ for foreground services with `foregroundServiceType="location"`. Mandates a persistent ongoing notification and user-visible tracking. | Granted at install time. |
| `android.permission.POST_NOTIFICATIONS` | Dangerous (Runtime, Android 13+) | Allows MotoLog to post the persistent ongoing notification during active tracking. Critical for motorcycle rider safety: provides lock-screen speed display and Pause/Stop controls without unlocking the phone or navigating menus with gloves. | Requested in-context before starting a ride on Android 13+ (API 33+). |
| `android.permission.VIBRATE` | Normal | Provides non-blocking haptic feedback when triggering speed alerts and when confirming the 2-second Hold-to-Stop gesture. | Granted at install time. |
| `android.permission.INTERNET` | Normal | Allows MapLibre to fetch public vector map tiles, styles, glyphs, and sprites from OpenFreeMap CDN servers over HTTPS to render route maps. | Granted at install time. |
| `android.permission.ACCESS_NETWORK_STATE` | Normal | Used by MapLibre and the app to check network availability before initiating map tile downloads, preventing unnecessary battery drain when offline. | Granted at install time. |

---

### Permissions Injected by AndroidX Libraries

| Permission | Source Library | Justification |
| :--- | :--- | :--- |
| `android.permission.WAKE_LOCK` | AndroidX WorkManager & MapLibre | Allows `MaintenanceCheckWorker` to briefly evaluate periodic maintenance reminders once every 24 hours, and MapLibre to process rendering frames. |
| `android.permission.RECEIVE_BOOT_COMPLETED` | AndroidX WorkManager | Allows WorkManager to reschedule the daily periodic maintenance check worker following a device reboot. |

---

### Restricted Permissions Explicitly EXCLUDED from MotoLog

MotoLog deliberately **avoids** requesting sensitive or high-risk permissions that are unnecessary for its core functionality:

- **NO `ACCESS_BACKGROUND_LOCATION`**: MotoLog does **not** track location in the background when no ride is in progress. All tracking is bound to the user-initiated foreground service (`FOREGROUND_SERVICE_LOCATION`), completely avoiding strict Google Play background location review.
- **NO Storage Permissions (`READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, `MANAGE_EXTERNAL_STORAGE`)**: MotoLog exclusively uses the Storage Access Framework (`ACTION_CREATE_DOCUMENT` and `ACTION_OPEN_DOCUMENT`) and `FileProvider` for GPX/CSV export and backup/restore, requiring zero storage permissions.
- **NO Camera or Microphone Permissions**: No media hardware is accessed.
- **NO Phone State or Contacts Permissions**: No personal communication identifiers are read.
