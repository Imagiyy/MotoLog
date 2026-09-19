---
trigger: always_on
---

# Motorcycle Ride Tracker: Project Rules

## Product goal
Android app for motorcycle riders that tracks ride distance, live speed,
and average speed. Tracking runs ONLY between the user pressing Start and
Stop. Target: Play Store release, so privacy, permissions, and stability
are first-class requirements, not afterthoughts.

Primary user: a rider with the phone mounted on the handlebar or in a
pocket, wearing gloves, often with the screen off. Design for glanceable,
glove-friendly, minimal-interaction use. Never require the rider to unlock
the phone or navigate menus mid-ride.

## Stack
- Kotlin, Jetpack Compose (Material 3), single-activity
- MVVM with unidirectional state (UI observes ViewModel state)
- Hilt for dependency injection
- Fused Location Provider for GPS
- Foreground service (type: location) for tracking
- Room for ride data, DataStore for settings
- Maps: MapLibre primary, with an extensible MapProvider abstraction interface supporting osmdroid as well (avoid Google Maps API key/billing).
- Min SDK 26. Target the latest API level Play requires (verify in
  Play Console docs, do not assume).
- Ask before adding any dependency not listed here.

## Architecture rules
- UI layer: composables only render state and forward events. No logic.
- Domain layer: ride math lives in pure Kotlin functions with no Android
  imports (distance, speed smoothing, moving time, splits). Fully unit
  tested.
- Data layer: Room DAOs and repositories. Location points are written to
  the database continuously during a ride (batch every few seconds), never
  only at Stop.
- Service layer: the foreground service owns the tracking session. UI
  binds to it or observes shared state; the service must work with no UI.
- All tunable thresholds (below) live in ONE constants file.

## Tracking behavior rules
- No location updates before Start or after Stop. Stop must remove
  location updates, stop the service, and dismiss the notification.
- Wait for a usable GPS fix (accuracy <= 25 m) before starting the clock.
  Show a "Waiting for GPS" state.
- Update interval: 1 s, fastest interval 1 s, high accuracy priority.
- Point filtering (initial values, keep configurable):
  - Reject points with accuracy worse than 25 m
  - Treat GPS speed below 1.5 km/h as zero (stationary noise)
  - Do not add distance for segments shorter than the point's accuracy
    radius while speed is under 3 km/h
  - Reject spikes: implied speed above 250 km/h, or implied acceleration
    above ~15 m/s^2 between consecutive points
- Speed: prefer the GPS-provided speed (Location.speed); fall back to
  distance / time only when speed is unavailable.
- Distance: sum of distances between accepted consecutive points.
- Auto-pause: pause moving time when speed is below 3 km/h for 8 s;
  resume when speed exceeds 5 km/h. Toggleable in settings; operates
  dynamically alongside manual pause. Show a visible "Auto-paused" state.
- Average speeds: store BOTH average moving speed
  (distance / moving time) and overall average (distance / elapsed time).
  Show moving average as the primary figure, with stats cards toggleable to
  switch between moving and overall elapsed metrics.
- Also track: max speed, moving time, stopped time, elapsed time.
- Signal loss: show a visible "GPS signal lost" state; do not invent
  distance across gaps; mark the gap in stored data.
- Crash and kill recovery: on next launch, detect an unfinished ride and
  offer to recover or discard it.

## Permissions (Play Store sensitive)
- Request permissions in context (when the user taps Start), with a short
  explanation screen first. Never request at app launch.
- Needed: ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION,
  FOREGROUND_SERVICE, FOREGROUND_SERVICE_LOCATION, POST_NOTIFICATIONS
  (Android 13+ mandatory before starting a ride to guarantee lock-screen controls).
- Do NOT request ACCESS_BACKGROUND_LOCATION. Start the foreground service
  only while the app is visible (user taps Start); this works with
  "while using the app" permission and avoids strict Play review.
- Handle every state gracefully: denied, denied permanently, approximate
  only, notification denied. Explain and deep-link to settings.
- Guide the user to exempt the app from battery optimization on OEMs that
  kill background apps (Xiaomi, Oppo, Vivo, Samsung, OnePlus, Realme).

## Rider-safety and UX rules
- Live screen: very large speed digits, high contrast, dark/AMOLED
  friendly, optional keep-screen-on, buttons at least 56 dp for gloves.
  Displays 4 primary glanceable metrics (Speed, Distance, Moving Time, Moving Avg Speed) with toggleable stat views.
- Notification actions: Pause/Resume and Stop, usable from lock screen.
- Stop mechanism: 2-second Hold-to-Stop gesture with visual progress ring animation to prevent accidental stops from bumps while wearing gloves.
- Display a one-time disclaimer: speed is GPS-derived and is not a legal
  speedometer; the app must be operated only when safe; do not interact
  while riding.
- No blocking dialogs or animations during an active ride.

## Data model (initial)
- Bike: id, name, make/model, odometer offset, created date
- Ride: id, bikeId, start/end time, distance, elapsed time, moving time,
  avg moving speed, overall avg speed, max speed, elevation gain, name,
  status (active, completed, recovered)
- RidePoint: rideId, timestamp, lat, lon, speed, accuracy, altitude,
  flags (paused, gap)
- MaintenanceItem: bikeId, name, interval km and/or days, last done
  odometer/date
- FuelLog: bikeId, date, odometer, litres, cost, full-tank flag

## Build stages (do one at a time, do not build ahead)

### Stage 0: Project setup
- Gradle, Hilt, Room, DataStore, navigation, theme, CI (GitHub Actions
  build + tests).
- Done when: app launches, `./gradlew build` and unit tests pass in CI.

### Stage 1: Start/Stop + live speed
- Permission flow, "Waiting for GPS" state, live speed on screen, Start
  and Stop buttons. Foreground-only at this stage.
- Done when: speed updates on device; nothing runs after Stop (verify
  location icon disappears).

### Stage 2: Calculation engine
- Pure functions for filtering, distance, moving time, averages, max
  speed. Tests built from recorded GPX fixtures (city ride, highway ride,
  stop-and-go, tunnel gap, stationary jitter).
- Done when: fixtures produce expected values within tolerance; a
  stationary phone for 5 minutes adds under 10 m of distance.

### Stage 3: Foreground service + persistence
- Move tracking into the foreground service (type location), persistent
  notification with live stats and Pause/Stop actions, points saved to
  Room continuously, crash recovery.
- Done when: a 30-minute ride works with screen off; force-killing the
  app mid-ride offers recovery; Stop fully releases GPS and notification.

### Stage 4: Auto-pause and signal handling
- Auto-pause/resume, GPS-lost state, gap marking.
- Done when: emulator replay of a stop-and-go route shows correct moving
  vs stopped time.

### Stage 5: History and ride detail
- Ride list, summary screen (distance, times, averages, max speed,
  per-km splits), delete with undo, rename.
- Done when: rides persist across restarts and totals match the engine.

### Stage 6: Garage (bikes, odometer, maintenance, fuel)
- Multiple bikes, ride assigned to a bike, odometer per bike, service
  reminders by km/days, fuel log with automatic km/l.
- Done when: completing a ride updates odometer and triggers a due
  maintenance notification.

### Stage 7: Map and visuals
- Route on map colored by speed, speed-over-time graph, elevation
  profile (use barometer when available).
- Done when: routes render smoothly for a 2-hour ride (downsample points
  for display, keep full data stored).

### Stage 8: Data ownership and settings
- GPX and CSV export, GPX import, backup/restore, units (km/mi), theme,
  accuracy vs battery mode, speed alert threshold.
- Done when: an exported GPX opens correctly in another app and a
  backup restores on a fresh install.

### Stage 9: Play Store readiness
- Privacy policy (location is collected and stored on device only, state
  this clearly), Data safety form answers, foreground service location
  declaration with justification video/text, target API compliance,
  release signing, app bundle (.aab), crash reporting, store listing,
  internal testing then closed testing track (personal accounts may need
  a minimum tester period; verify current Play requirements).
- Done when: pre-launch report has no critical issues and a release
  build passes a full manual ride test.

## Later (do not build until asked)
Voice announcements, home-screen widget and Quick Settings tile, live
location sharing, physical crash/fall detection with emergency SMS (note: app process crash/kill recovery remains in v1 Stage 3), privacy zones
for sharing, Wear OS, Strava/Health Connect, Bluetooth sensors.

## Out of scope (do not add)
Cloud accounts, servers, social features, ads, analytics that send
location data off the device.

## Working agreements for agents
- One stage per session; state what you will change before changing it.
- After every change run `./gradlew build` and unit tests; fix failures
  before continuing. Run lint before finishing a stage.
- Use current, non-deprecated Android APIs; consult official docs if
  unsure. Do not guess permission or foreground service behavior.
- Never log or transmit location data outside the device.
- Commit after each working stage with a clear message.
- If a requirement here conflicts with what you find, stop and ask rather
  than silently deviating.
- Real-device testing is required for GPS accuracy, battery drain, and
  OEM background-kill behavior; emulator route replay is only for logic.