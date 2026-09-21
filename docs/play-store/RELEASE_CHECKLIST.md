# Release Checklist: MotoLog Play Store Launch

> **DRAFT, needs my review**  
> *Notice: This checklist covers every step required across the codebase and inside Google Play Console for a successful production release.*

---

## Part 1: Local Codebase & Build Preparation

### 1. Keystore Generation (Do This Once)
- [ ] Generate your release upload keystore securely in a terminal:
  ```bash
  keytool -genkey -v -keystore motolog-upload-key.jks -alias motolog-upload -keyalg RSA -keysize 2048 -validity 10000
  ```
- [ ] Back up `motolog-upload-key.jks` and its passwords to a password manager or secure offline backup drive.
- [ ] Create `keystore.properties` in the project root (gitignored):
  ```properties
  storeFile=/absolute/path/to/motolog-upload-key.jks
  storePassword=YOUR_KEYSTORE_PASSWORD
  keyAlias=motolog-upload
  keyPassword=YOUR_KEY_PASSWORD
  ```
- [ ] Verify `keystore.properties` and `*.jks` are NOT tracked by Git (`git status`).

### 2. Privacy Policy URL Hosting
- [ ] Review `docs/play-store/PRIVACY_POLICY.md` and fill in placeholders (contact email, effective date).
- [ ] Host the privacy policy at a public URL (e.g. your personal domain or public GitHub markdown page).
- [ ] Update `PRIVACY_POLICY_URL` in `SettingsScreen.kt` if different from the default.

### 3. Versioning
- [ ] Confirm `applicationId = "com.abrar.motolog"` in `app/build.gradle.kts`.
- [ ] Set `versionCode = 1` and `versionName = "1.0.0"`.

### 4. Verification & Testing
- [ ] Run automated unit tests:
  ```bash
  ./gradlew testDebugUnitTest
  ```
- [ ] Run release Android Lint:
  ```bash
  ./gradlew lintRelease
  ```
- [ ] Assemble release APK:
  ```bash
  ./gradlew assembleRelease
  ```
- [ ] Install release APK on a physical device and perform manual ride/screen walkthrough:
  ```bash
  adb install app/build/outputs/apk/release/app-release.apk
  ```

### 5. Generate Production App Bundle (.aab)
- [ ] Build the release bundle:
  ```bash
  ./gradlew bundleRelease
  ```
- [ ] Output bundle file location:
  `app/build/outputs/bundle/release/app-release.aab`
- [ ] Verify bundle contents:
  - Check targetSdk is 36.
  - Verify native libraries in `.aab` have 16 KB segment alignment.

---

## Part 2: Google Play Console Setup

### 1. Create Application
- [ ] In Google Play Console, click **Create app**.
- [ ] App Name: `MotoLog: Motorcycle Tracker`
- [ ] Default language: English (United States)
- [ ] App or game: App
- [ ] Free or paid: Free

### 2. Complete App Content Declarations (Policy > App content)
- [ ] **Privacy Policy**: Enter the public URL hosting your Privacy Policy.
- [ ] **Ads**: Select "No, my app does not contain ads".
- [ ] **App Access**: Select "All functionality is available without special access restrictions".
- [ ] **Content Rating**: Complete questionnaire per `APP_CONTENT_FORMS.md` (rated Everyone / 3+).
- [ ] **Target Audience**: Select "18 and over".
- [ ] **News Apps**: Select "No".
- [ ] **COVID-19 Apps**: Select "No".
- [ ] **Data Safety**: Complete per `DATA_SAFETY_ANSWERS.md` ("No data collected").
- [ ] **Government Apps**: Select "No".
- [ ] **Financial Features**: Select "None".
- [ ] **Foreground Service Permissions**:
  - Select `Location`.
  - Enter description, user trigger, and notification answers from `FOREGROUND_SERVICE_DECLARATION.md`.
  - Provide link to recorded unlisted YouTube demo video.

### 3. Store Listing Setup (Grow > Store presence > Main store listing)
- [ ] Title: `MotoLog: Motorcycle Tracker` (27 chars)
- [ ] Short description: `Motorcycle ride tracking, live speed, garage logs, and maps. 100% on-device.` (77 chars)
- [ ] Full description: Copy text from `STORE_LISTING.md`.
- [ ] App icon: Upload 512x512 px 32-bit PNG.
- [ ] Feature graphic: Upload 1024x500 px banner.
- [ ] Screenshots: Upload at least 4 phone screenshots (1080x1920 or higher).
- [ ] Category: Auto & Vehicles.
- [ ] Contact details: Enter developer contact email.

### 4. Release Track Deployment
- [ ] Enable **Google Play App Signing**.
- [ ] Under **Testing > Internal testing**, create a new release and upload `app-release.aab`.
- [ ] Invite testers to verify installation from Play Store and check Google's automated **Pre-launch Report** for crashes or display bugs.
- [ ] Advance release to **Closed Testing** (20 testers for 14 days if personal account required by Google Play policy).
- [ ] Promote to **Production** track after testing period finishes.
