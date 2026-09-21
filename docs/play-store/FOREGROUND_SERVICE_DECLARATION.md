# Play Console Declaration: Foreground Service (Location)

> **DRAFT, needs my review**  
> *Notice: This document provides the text answers required in the Google Play Console under Policy > App content > Foreground service permissions, along with a complete video demonstration script.*

---

## 1. Play Console Questionnaire Answers

### Question 1: What is the core feature that requires a foreground service of type `location`?
> **Answer**:  
> MotoLog is a motorcycle ride tracking and speedometer application. The core feature of the app is recording live GPS ride metrics—including accurate distance, continuous live speed smoothing, moving time, stopped time, and split statistics—while the motorcycle is in motion. Riders mount the phone on their handlebar or place it inside a jacket pocket, frequently with the phone screen turned off or while switching between MotoLog and navigation apps. The location foreground service ensures that GPS tracking continues uninterrupted during the ride and provides glanceable stats and glove-friendly Pause/Stop controls directly from the lock-screen notification.

---

### Question 2: Why can this task not be performed using a background job (such as WorkManager or JobScheduler)?
> **Answer**:  
> Motorcycle ride tracking requires high-frequency, low-latency GPS fixes (1-second intervals) to calculate real-time speedometer readings, detect stationary stops (auto-pause), and capture accurate cornering geometry. Background jobs (WorkManager or JobScheduler) are throttled or deferred by Android battery-saving policies and cannot execute continuous 1 Hz location sampling. Deferring or interrupting location updates would cause severe route distortion, incorrect odometer distance accumulation, and loss of real-time speed monitoring, which defeats the primary purpose of the application.

---

### Question 3: Describe how the user initiates the foreground service.
> **Answer**:  
> The service is strictly user-initiated. The service **never** starts automatically at device boot, upon geofence triggers, or when the app is launched. It starts **only** when the user opens MotoLog and explicitly taps the large "Start Ride" button on the Live screen. Before starting for the first time, the user is presented with a prominent in-app disclosure explaining that location tracking will run as a foreground service with an ongoing notification until they stop the ride.

---

### Question 4: Describe the user-facing persistent notification.
> **Answer**:  
> While tracking is active, MotoLog displays an ongoing persistent notification in the status bar and lock screen with:
> 1. Real-time ride statistics: Current speed (km/h or mph), elapsed distance, and moving duration.
> 2. Glove-friendly interactive action buttons: "Pause / Resume" and "Stop (In-App)".
> 3. Clear status badges indicating whether tracking is currently recording, auto-paused, or manual-paused.  
> When the user stops the ride, the foreground service immediately ceases location updates, dismisses the notification, and releases all GPS resources.

---

## 2. Video Demonstration Script (Required for Play Console Submission)

Google requires an unlisted video link (e.g. uploaded to YouTube) demonstrating the complete user journey. Record the phone screen while performing the following sequence (total length: ~45–60 seconds):

| Step | Action on Device | What to Show on Screen | Commentary / Text Overlay |
| :--- | :--- | :--- | :--- |
| **1. Launch** | Open the MotoLog app. | Live speedometer screen with Start button. | "User opens MotoLog. No location tracking runs in the background." |
| **2. Initiate** | Tap the large "Start Ride" button. | Prominent disclosure dialog appears explaining location permission and foreground tracking. | "User explicitly taps Start. An in-app disclosure explains why location is needed." |
| **3. Permission** | Tap "Grant Permission" and accept system prompt. | System permission prompt granted. Status shows "Waiting for GPS" then transitions to "RECORDING". | "Precise location permission granted. Tracking session begins." |
| **4. Notification** | Pull down the Android notification shade. | Ongoing notification visible showing live speed, distance, time, and "Pause" / "Stop" buttons. | "A persistent ongoing notification shows live ride stats and lock-screen controls." |
| **5. Backgrounding** | Press the Home button or lock the screen. | App moves to background; status bar shows active location and notification icon. Move phone or simulate motion for 5 seconds. | "Tracking continues reliably while the app is in the background or screen is off." |
| **6. Return & Pause** | Return to MotoLog; tap "Pause". | Speedometer status transitions to "PAUSED"; notification updates to "PAUSED". | "User can pause tracking at any time." |
| **7. Complete Stop** | Tap and hold the "Hold to Stop" button for 2 seconds. | Stop progress ring fills; ride summary is saved. | "2-second hold-to-stop prevents accidental termination." |
| **8. Verification** | Pull down notification shade; observe status bar. | Notification is completely dismissed; location icon vanishes from status bar. History screen shows the saved ride. | "Stop completely stops the service, dismisses the notification, and releases GPS." |
