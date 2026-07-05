# Tag Spotter: Firebase Backup & Sync Manual Testing Guide

This guide outlines step-by-step procedures to manually verify the Authentication, Bidirectional REST Synchronization, Multi-User Data Isolation, and Cloud Pack Sharing flows.

---

## 🔑 Test Scenario 1: Desktop JVM REST Authentication State Flows
Verify that the REST-based `AuthService` dynamically resolves config credentials and handles login states on Desktop JVM.

### Step 1.1: anonymous / offline State Verification
1. Launch the application with:
   ```bash
   rtk ./gradlew :desktopApp:run
   ```
2. Ensure you are in "Anonymous / Offline Mode" (not logged in).
3. Create a new local spot.
4. **Verification**: 
   - The spot is saved in the local Room SQLite database with `ownerUid == null`.
   - Check the terminal logs to verify no remote sync calls are executed.

### Step 1.2: Sign-In / Sign-Up Flow
1. Navigate to **Settings** and trigger the cloud sign-in.
2. Enter your Firebase email and password (or click sign up to create a new account).
3. **Verification**:
   - A POST request is successfully made to `identitytoolkit.googleapis.com` to sign in.
   - The short-lived `idToken` and long-lived `refreshToken` are successfully parsed and encrypted into secure system preferences storage.
   - The terminal prints confirmation logs, and the app dashboard updates to a logged-in state displaying your email.

---

## 🔄 Test Scenario 2: Bidirectional REST Cloud Sync & Thumbnail Fetching
Verify that local changes sync to the cloud, and remote updates (including thumbnail images) are successfully downloaded and rendered.

### Step 2.1: Push Sync (Local -> Cloud)
1. Log in on your Desktop JVM client.
2. Add a new spot with a detailed description and attach a thumbnail photo.
3. Trigger a manual sync or wait for the automatic coroutine polling sync.
4. **Verification**:
   - The spot metadata is serialized into Firestore JSON format and PATCHed to `/users/{userId}/spots/{uuid}`.
   - The binary stream of the thumbnail is uploaded to Firebase Storage under `/users/{userId}/thumbnails/{imageUuid}.jpg`.
   - Log into the **[Firebase Console](https://console.firebase.google.com/)** and verify that both Firestore document metadata and the Storage JPG image appear under your User's UID directory.

### Step 2.2: Pull Sync & Thumbnail Resolution (Cloud -> Local)
1. Log in with the same account on a *different* device or emulator.
2. Verify that the spot you just uploaded appears in the list.
3. **Verification**:
   - The sync manager scans pulled spots and detects that the thumbnail is missing locally.
   - It queries the storage object metadata to fetch its secure HTTPS download URL.
   - It downloads the thumbnail bytes and saves them locally at `~/Pictures/TagSpotter/thumbnails/{ownerUid}/{imageUuid}.jpg`.
   - The Gallery displays the thumbnail beautifully, rendered from the newly resolved path.

---

## 🛡️ Test Scenario 3: Secure Account Switching & Multi-User Isolation
Verify that user sessions are fully isolated and switching accounts handles local data adoption and cache purging cleanly.

### Step 3.1: Guest Spot Adoption Prompt
1. Log out of the app.
2. While signed out, create 2-3 local spots. They are saved in the database with `ownerUid == null`.
3. Sign in to your Firebase account.
4. **Verification**:
   - Immediately upon sign-in, the **Adoption Prompt card** appears: *"We found offline spots on this device. Do you want to back them up to [your email] or keep them strictly local?"*
5. Test **Back Up**:
   - Tap **Back Up**. The database column `ownerUid` is updated to your user UID, and they automatically upload to Firestore.
6. Test **Keep Strictly Local**:
   - Tap **Keep Strictly Local**. The database column `ownerUid` is updated to `"local_only"`, excluding them from cloud sync and keeping them purely on this device.

### Step 3.2: Multi-User Data Isolation
1. While logged in as `userA@example.com`, create a spot named "User A Private Spot".
2. Log out and choose **Keep cached files**.
3. Log in as `userB@example.com`.
4. **Verification**:
   - The Gallery and Map are completely empty (or only show spots scoped to User B or strictly local spots).
   - "User A Private Spot" is completely hidden, proving database query level isolation.

### Step 3.3: Logout & Cache Purging
1. Log back in as `userA@example.com`.
2. Tap **Log Out**. In the confirmation dialog, select **Clear Cache**.
3. **Verification**:
   - All database records in `spots`, `spot_images`, and `spot_notes` matching `ownerUid == userA_uid` are deleted.
   - The corresponding local thumbnail and image files are completely deleted from the local disk filesystem.

---

## 📦 Test Scenario 4: Direct Cloud Pack Sharing via codes, Links & QR Codes
Verify that collections can be bundled, uploaded, rendered as a QR code, and imported without duplication.

### Step 4.1: Cloud Pack Bundling & Sharing Dialog
1. Log in and navigate to the **Gallery**.
2. Multi-select 2 or 3 spots, and choose the **Share Collection (Cloud)** action.
3. Enter a title (e.g., "Milano Street Art") and description, then confirm.
4. **Verification**:
   - A beautiful share dialog appears showing a 6-character alphanumeric code (e.g., `B3K8XP`, excluding `I, O, 1, 0, L`).
   - A copyable deep link `https://tagspotter.net/import?pack=[CODE]` is displayed.
   - A high-fidelity, vector QR code of the deep link is drawn live on a Compose Canvas.
   - The pack document is successfully uploaded to Firestore under `/shared_packs/{packId}`.

### Step 4.2: Importing a Cloud Pack & Thumbnail Fetching
1. On a second device/account, tap the **Import Cloud Pack (Arrow Downward)** icon on the top bar.
2. Enter the 6-character code and tap **Fetch**.
3. **Verification**:
   - The modal retrieves the metadata from Firestore and previews "Milano Street Art" with the creator's name and total spots count.
4. Tap **Import**.
5. **Verification**:
   - The spots load into your Gallery feed.
   - The sync client detects the original creator's UID, downloads the thumbnails from their Storage folders, and renders them successfully in the feed.

### Step 4.3: Refreshing and Unloading Packs (Deduplication)
1. Navigate to **Manage Loaded Packs (Settings/Tune icon)** on the Gallery top bar.
2. Tap **Refresh** next to the imported pack.
3. **Verification**:
   - The spots update in-place using their unique UUIDs, performing an `UPDATE` rather than creating duplicate spots or images.
4. Tap the **Unload (Bin)** button next to the pack.
5. **Verification**:
   - The pack metadata record is deleted.
   - All local spots associated with that pack ID are cascadingly deleted and disappear from the Gallery.
