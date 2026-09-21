# Privacy Policy for MotoLog

> **DRAFT, needs my review**  
> *Notice: This document is a draft prepared for developer review and customization. You must review and approve this policy before publishing it to your website or Google Play Console.*

**Effective Date:** [Date — Placeholder, e.g., October 1, 2026]  
**Last Updated:** [Date — Placeholder, e.g., October 1, 2026]  
**Contact Email:** [support@example.com — Placeholder]

---

### 1. Introduction

MotoLog ("the App", "we", "us", or "our") is an Android application designed for motorcycle riders to track ride distance, live speed, moving time, and motorcycle maintenance. 

We strongly believe that your motorcycle ride history and locations belong exclusively to you. MotoLog is built with an **on-device-only architecture**: your rides, routes, GPS coordinates, garage information, and fuel logs remain on your physical device. We do not operate user accounts, do not maintain remote cloud servers for ride storage, and do not track or profile you.

---

### 2. Information Handled by MotoLog and Where It Is Stored

All personal and ride data recorded by MotoLog is stored locally in an encrypted or private SQLite/Room database on your device:

1. **Precise GPS Location Data**:
   - **What is recorded**: While an active ride is recording (between pressing "Start" and "Stop"), MotoLog records latitude, longitude, timestamp, GPS speed, accuracy radius, and elevation.
   - **Storage**: Stored locally on your device in the app's internal database (`rides` and `ride_points` tables).
   - **When location is accessed**: Location updates run **only** while a ride is active. When you tap "Stop", location updates cease immediately, the foreground service is terminated, and the location icon is released. MotoLog never accesses location in the background when no ride is actively in progress.
2. **Motorcycle & Garage Data**:
   - Make, model, custom nickname, and odometer readings.
   - Maintenance tasks (intervals, last completed dates, and odometers).
   - Fuel logs (date, odometer reading, liters filled, fuel cost, and notes).
   - **Storage**: Stored locally on your device in the app's internal database.
3. **App Preferences & Settings**:
   - Display units (Metric vs Imperial), theme preference, auto-pause toggle, tracking mode, and speed alert threshold.
   - **Storage**: Stored locally on your device via Android Jetpack DataStore Preferences.

---

### 3. Network Connections & Third-Party Services

MotoLog does not send your ride coordinates, statistics, or personal data to any developer servers. The **only** network activity initiated by the app is for downloading public map display assets:

1. **Vector Map Tiles (OpenFreeMap / OpenMapTiles)**:
   - When you view a route map on the Ride Detail screen, MotoLog connects over secure HTTPS to OpenFreeMap CDN servers to download static vector map tiles, map fonts (glyphs), and map icons (sprites).
   - **What the map provider can see**: Like any standard web request, OpenFreeMap servers observe your device's public IP address and the specific map tile coordinates (bounding box / zoom level) requested to display the map. OpenFreeMap is an open-source, non-tracking service that does not require an account, API key, or tracking cookies.
   - **Attribution**: Map data © [OpenStreetMap](https://www.openstreetmap.org/copyright) contributors, styles and tiles by [OpenFreeMap](https://openfreemap.org) and OpenMapTiles.
2. **Crash Reporting & Diagnostics**:
   - MotoLog **does not include any third-party analytics or crash tracking SDKs** (such as Firebase Crashlytics, Facebook SDK, or Google Analytics).
   - Crash diagnostics are limited strictly to **Android Vitals** provided automatically by the Android operating system to the Google Play Console if you have opted in to share diagnostic data with Google in your Android system settings. Android Vitals reports contain stack traces and device models, and never contain your GPS coordinates or route history.

---

### 4. User-Controlled Exports & Backups

You have complete ownership of your data and may export it at any time:

1. **GPX and CSV Exports**:
   - You can export individual rides or all rides as standard GPX 1.1 or CSV files using Android's Storage Access Framework. The destination folder (local storage, SD card, or user-selected cloud drive) is chosen entirely by you.
2. **Full Database Backup (ZIP Archive)**:
   - You can create a complete ZIP backup containing your rides, points, bikes, and logs.
   - *Security Note*: Backup files are unencrypted files containing your raw GPS route history. You should store backup files only in locations and cloud accounts you trust.
3. **Android Cloud Auto Backup (Google Drive)**:
   - MotoLog explicitly **excludes the ride and location database from Google's automated cloud backups** (`data_extraction_rules.xml` and `backup_rules.xml`). Your GPS coordinates will never be silently uploaded to Google Drive.
   - The database is only included during direct device-to-device transfers (such as transferring apps and data between two phones over a USB cable or Wi-Fi Direct).

---

### 5. Advertisements & Data Selling

- **No Ads**: MotoLog contains zero advertisements.
- **No Data Selling**: We do not sell, rent, monetize, or trade your personal data, location history, or usage statistics to data brokers, advertisers, or third parties under any circumstances.

---

### 6. Data Retention and How to Delete Your Data

Because all data is stored exclusively on your device:
- **In-App Deletion**: You can delete individual rides at any time from the History screen. Deleting a ride permanently purges the ride and all associated GPS track points from your device's database.
- **Complete Deletion**: You can delete all data at once by selecting "Clear Storage / Clear Data" in your device's Android App Settings for MotoLog, or by uninstalling the MotoLog app. Uninstalling removes all databases, settings, and cached map tiles immediately.

---

### 7. Children's Privacy

MotoLog is an application designed for licensed motorcycle operators. We do not knowingly collect, store, or solicit information from children under the age of 13 (or under 16 in the European Union).

---

### 8. Changes to This Privacy Policy

We may update this Privacy Policy from time to time to reflect changes in app functionality or regulatory obligations. Any updates will be posted at the public URL linked in the app and on the Google Play Store.

---

### 9. Contact Us

If you have questions, feedback, or concerns regarding this Privacy Policy, please contact:
- **Developer / Support**: [Your Name / Organization — Placeholder]
- **Email**: [support@example.com — Placeholder]
