# MotoLog Pilot Testing Guide: Destination Tracking & Route Navigation

## 1. Overview
This guide provides instructions for internal pilot testers and release engineers evaluating the **MotoLog Destination Tracking and Route Navigation Subsystem** (Version 1.0 Release Candidate).

Navigation operates alongside MotoLog's core ride telemetry and foreground service, preserving strict on-device privacy, glove-friendly usability, and offline autonomy.

---

## 2. Feature Flag & Emergency Controls
- **Location**: Navigate to **Settings** > **Navigation & Routing**.
- **Control**: **Enable Navigation Subsystem** toggle switch.
- **Behavior**:
  - When **Enabled**: Destination search, interactive map-pin selection, planned route previews, and turn-by-turn guidance are active in the cockpit.
  - When **Disabled**: The navigation subsystem and live "NAV" entry points are safely hidden without impacting standard ride recording, GPS speed tracking, or garage logs.

---

## 3. Test Scenarios for Pilot Riders

### Scenario A: Destination Search & Route Planning
1. Open MotoLog and navigate to the **Live** screen.
2. Tap the cyan **NAV** button in the lower status bar or top header.
3. In the search box, enter a landmark or street name.
4. Verify candidates appear promptly with locality descriptions.
5. Tap **Choose Pin on Map**, pan the crosshair, and confirm location via the 56dp button.
6. Verify route geometry, total distance, expected duration (ETA), and alternatives are displayed.
7. Tap **Start Navigation** (56dp target). Verify navigation starts **without** inadvertently stopping or resetting ride recording.

### Scenario B: Live Route Matching & Telemetry
1. Mount the phone to the handlebar in portrait or landscape orientation.
2. Begin riding. Verify the blue planned route line, next maneuver banner, and distance-to-next-turn card update smoothly every second.
3. Switch camera modes between **Follow Head-Up** (3D bearing rotation) and **Follow North-Up** using the floating camera button.
4. Drag the map to inspect surroundings (camera switches to **Free** mode).
5. Tap the **Recenter** button and confirm camera snaps back onto current position with auto-follow engaged.

### Scenario C: Off-Route Detection & Recalculation
1. Deliberately take an alternate turn away from the planned route.
2. At 35–50 meters off-track for 5 seconds, verify the yellow **Off-Route Warning** banner appears.
3. Continue off-track beyond 75 meters for 8–12 seconds.
4. Verify the system enters **Rerouting...** state and automatically calculates a new route from your current coordinate.
5. Confirm the previous route remains visible until the new route calculation succeeds, and that subsequent reroute requests respect the 15-second cooldown timer.

### Scenario D: Bluetooth Helmet Audio & Voice Guidance
1. Connect a Bluetooth intercom / helmet headset (e.g., Sena, Cardo).
2. Verify audio prompts announce maneuvers with speed-adaptive advance timing (e.g. 200m at highway speed, 50m in urban traffic).
3. Tap the speaker icon on the navigation map to test **Mute**. Confirm voice audio ceases immediately.
4. Tap unmute and confirm guidance resumes.

### Scenario E: Offline Autonomy & Tunnel Resilience
1. Start navigation, then switch device to **Airplane Mode** (disabling cellular/Wi-Fi).
2. Continue riding along the planned route.
3. Verify progress matching, distance-to-next-turn, and ETA continue updating offline using on-device GPS matching.
4. Pass through an underpass or tunnel (GPS signal loss).
5. Confirm the amber **GPS SIGNAL LOST** indicator displays, progress calculation safely pauses, and zero phantom distance is added across the blackout.

---

## 4. Submitting Anonymized Diagnostics
Pilot riders who experience routing errors, unexpected reroutes, or GPS dropouts can easily export non-sensitive diagnostic telemetry:

1. Open **Settings** > **Navigation & Routing**.
2. Scroll to the **Pilot Diagnostics & Telemetry** section.
3. Review the aggregated metrics:
   - **Requests**: Total route calculations requested, succeeded, and failed.
   - **Reroutes**: Automatic and manual recalculation counts.
   - **Arrivals**: Confirmed destination arrivals.
   - **GPS Loss**: Number of signal degradation events.
4. Tap **Copy JSON**.
5. Paste the clipboard contents into an email or pilot issue tracker.

> **Privacy Guarantee**: The exported JSON document contains **ZERO** GPS coordinates, search queries, user identifiers, or API credentials. It contains only numerical failure categories and counters.

---

## 5. Exit & Promotion Criteria
Before promoting the pilot build from the Closed Testing track to Production:
- [ ] At least 10 hours of active motorcycle road testing recorded without unhandled app crashes.
- [ ] No false-arrival triggers or infinite reroute calculation loops reported.
- [ ] Screen-off tracking confirmed stable across Samsung, Xiaomi, and OnePlus devices with battery optimization guidance applied.
- [ ] Redacted diagnostics export verified by release team with zero PII or coordinates detected.
