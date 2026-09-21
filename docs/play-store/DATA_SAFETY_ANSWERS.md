# Google Play Data Safety Form: Answers and Reasoning

> **DRAFT, needs my review**  
> *Notice: This document provides suggested answers and exact reasoning based on Google Play's official Data Safety definitions and MotoLog's codebase. Review each answer in Google Play Console under Policy > App content > Data safety.*

---

## Section 1: Data Collection and Security

### Question 1: Does your app collect or share any of the required user data types?
- **Answer**: **No** *(See Interpretation Note below)*
- **Reasoning**:
  - Google Play defines data as **"collected"** only if it is transmitted off the user's device (to developer servers or third-party servers) and retained for longer than ephemerally.
  - MotoLog stores all GPS location points, speeds, bike odometers, maintenance items, and fuel logs **strictly on the user's physical device** in a private SQLite database. No data is transmitted to any developer server or cloud account.
  - *Interpretation Note on Map Tiles*: MotoLog's map view fetches vector map tiles from OpenFreeMap over HTTPS. When fetching map tiles, standard HTTP GET requests transmit the device IP address and the requested map tile coordinates (zoom/X/Y) to the OpenFreeMap CDN. Because this is an ephemeral HTTP request strictly to retrieve map display assets and is not stored, tracked, or linked to user identity, Google's guidance generally treats standard CDN web browsing / asset loading as not constituting data collection. However, if Play Console reviewers interpret tile requests as location collection, consult the alternative answer in the Appendix.

---

### Question 2: Is all of the user data collected by your app encrypted in transit?
- **Answer**: **Yes** (or N/A if "No data collected" is selected).
- **Reasoning**: All network requests made by MotoLog (specifically HTTPS tile downloads from OpenFreeMap and links to OpenStreetMap copyright pages) use TLS/HTTPS encryption. Cleartext HTTP traffic is explicitly blocked (`android:usesCleartextTraffic="false"`).

---

### Question 3: Do you provide a way for users to request that their data be deleted?
- **Answer**: **Yes**
- **Reasoning**:
  - Users can delete individual rides at any time in the app (via the trash icon on the History screen with undo support), which immediately purges the ride and all GPS coordinates from the device.
  - Users can delete all data at any time by selecting "Clear Storage / Clear Data" in Android system settings or by uninstalling the application.

---

## Section 2: Data Types (If "No" selected in Q1)

If Question 1 is answered **No**, the Data Types section is skipped by Google Play Console. The public Data Safety label on the Play Store listing will state:
> *"No data collected: The developer says this app does not collect user data."*  
> *"No data shared with third parties: The developer says this app does not share user data with other companies or organizations."*

---

## Appendix: Alternative Classification (If Reviewer Flags Map Requests)

If Google Play policy reviewers request explicit disclosure of location due to the map tile CDN requests:

### Data Type: Location -> Approximate Location
- **Collected**: Yes (ephemerally processed for map display)
- **Shared**: Yes (transmitted to OpenFreeMap CDN when fetching vector tiles)
- **Processed ephemerally?**: Yes (tiles are requested in real time to render the map view and not retained or profiled)
- **Is this data required or optional?**: Required for map rendering feature (rides can still be recorded without viewing maps)
- **Purpose**: App functionality (displaying route lines over map tiles)

### Data Type: Device or other identifiers -> IP Address
- **Collected**: Yes (ephemerally processed by CDN)
- **Shared**: Yes (OpenFreeMap CDN)
- **Processed ephemerally?**: Yes
- **Purpose**: App functionality (standard internet protocol transmission)

*Recommendation*: Submit with **"No data collected"** initially, as MotoLog contains no tracking SDKs, no ads, and no user accounts, and all ride logs remain entirely on the local device.
