# Follow-Up Optional Firebase Backup & Sync Plan

This document details the refined technical implementation plan for closing all remaining gaps in our **Optional Firebase Backup & Sync** integration. To support multi-turn execution across clean contexts, the plan is structured into four distinct, independent, and sequential phases.

---

## Technical Alignment Summary

*   **JVM Credentials**: Dynamically load API keys and Project IDs on JVM using standard local properties / environmental variables.
*   **Logical DB Scoping**: Use a single Room SQLite DB with an `ownerUid` column to cleanly query, isolate, and list spots.
*   **JWT Silent Refresh**: Integrate Ktor's native `BearerAuth` plugin to handle automatic silent token refreshes on JVM.
*   **Strictly Local Spots**: Tag legacy/offline spots that users decline to sync with `ownerUid = "local_only"` to prevent future syncs.
*   **Shared Spot Interactions**: **Fork / Duplicate to Edit**. Users view other photographers' spots as read-only but can duplicate them locally.
*   **Cloud Packs (Approach A Refined)**: Share specific collections using self-contained Cloud Pack documents (`SharedPack`) in Firestore. Each pack contains a copyable 6-character alphanumeric code, generates a deep link, and renders a vector QR code on a Compose canvas for direct phone-to-phone sharing.

---

## Sequenced Implementation Phases

```mermaid
gantt
    title Detailed Implementation Timeline
    dateFormat  YYYY-MM-DD
    section Phase 1
    Phase 1: Real JVM REST Firebase      :active, p1, 2026-07-05, 3d
    section Phase 2
    Phase 2: Remote Thumbnail Sync      : p2, after p1, 2d
    section Phase 3
    Phase 3: Multi-User Isolation       : p3, after p2, 2d
    section Phase 4
    Phase 4: Cloud Packs & QR Sharing   : p4, after p3, 3d
```

---

### Phase 1: Real Firebase Connections on Desktop JVM (Ktor REST Client)

Replaces the mock JVM simulation with actual connections to Firebase services using safe client-side REST APIs over Ktor.

#### Key Tasks
1.  **Ktor HTTP Configuration**: Setup Ktor client inside `:core:sync` JVM sources, configuring the standard `BearerAuth` plugin for automatic silent token refreshes using short-lived ID tokens and long-lived Refresh tokens.
2.  **Auth REST Client**: Implement `signInWithEmailAndPassword`, `signUpWithEmailAndPassword`, and `sendPasswordResetEmail` targeting Google's public Identity Toolkit endpoints.
3.  **Firestore REST Client**: Implement Document GET/SET calls mapping `SpotDetails` models to and from Firestore JSON structures.
4.  **Storage REST Client**: Implement upload binary streams to Cloud Storage folders.
5.  **DI Configuration**: Update Koin inside `:desktopApp` to instantiate the new REST-based clients instead of the simulation when credentials exist.

#### 🧪 Phase 1 Manual Verification
1.  Add real Firebase credentials to `local.properties`.
2.  Launch the desktop app, go to Settings, and log in with a real Firebase email/password.
3.  Verify in the logs that a real token is fetched. Create a spot and ensure it saves to your live Cloud Firestore.

#### ⏭️ Continuation Prompt for Phase 2
Copy-paste this prompt to start the next phase:
```text
Please resume the Tag Spotter Firebase integration at Phase 2: Remote Thumbnail Synchronization & Rendering. Refer to docs/follow-up-firebase-optional-backup-plan.md for technical requirements.
```

---

### Phase 2: Remote Thumbnail Synchronization & Rendering

Ensures thumbnails taken on other devices are successfully synchronized, resolved, and rendered over the internet.

#### Key Tasks
1.  **Secure URL Resolution**: In `SyncManager.kt` (during pull/snapshot updates), scan pulled spots. If an image's `thumbnailPath` does not exist on the local device, fetch its secure download HTTPS URL from Firebase Storage.
2.  **Path Updating**: Save the remote HTTPS download URL directly inside the local database `thumbnailPath` field.
3.  **Coil Loading**: Verify that Coil `AsyncImage` cells correctly intercept, render, and cache these remote HTTPS URLs.
4.  **Disk Optimization**: Store downloaded remote thumbnails into `/thumbnails/{ownerUid}/{imageUuid}.jpg` subdirectories on local storage.

#### 🧪 Phase 2 Manual Verification
1.  Sign in on both an Android device and a Wasm browser tab.
2.  Create a spot with a photo on mobile. Wait for it to sync.
3.  Open the gallery on the Web/Wasm or Desktop app, verify that the spot card displays the thumbnail, and check that Coil fetches it over HTTPS.

#### ⏭️ Continuation Prompt for Phase 3
Copy-paste this prompt to start the next phase:
```text
Please resume the Tag Spotter Firebase integration at Phase 3: Secure Account Switching & Multi-User Scoping. Refer to docs/follow-up-firebase-optional-backup-plan.md for technical requirements.
```

---

### Phase 3: Secure Account Switching & Logical Multi-User Scoping

Enforces absolute privacy and data isolation when logging out and logging in as different users.

#### Key Tasks
1.  **Room Schema Migration**: Add nullable `ownerUid: String?` column to `spots`, `spot_images`, and `spot_notes` tables in Room SQLite. Implement migration `MIGRATION_11_12`.
2.  **Adoption Prompt UI**: Upon first successful cloud sign-in, if local spots have `ownerUid == NULL`, show a card prompting: *"We found offline spots on this device. Do you want to back them up to [email] or keep them strictly local?"*
3.  **Owner Assignment**:
    *   If **Back Up**: Update `ownerUid` to the logged-in `userUid`.
    *   If **Strictly Local**: Update `ownerUid` to `"local_only"`.
4.  **Logical Filtering**: Restrict all Gallery, Map, and Sync database queries to only retrieve spots where `ownerUid == activeUid OR ownerUid IS NULL` (if offline).
5.  **Clean Logout UI**: When logging out, offer the user a clear dialog to either:
    *   **Keep cached files** (keeps spots locked to previous UID locally but hides them from active dashboard).
    *   **Clear cache** (deletes all spots where `ownerUid == activeUid` and clears their specific subdirectory folder).

#### 🧪 Phase 3 Manual Verification
1.  Create 5 spots offline. Log in to Account A, select **Back Up**, and verify they upload to Account A's Firestore.
2.  Log out, select **Keep cached files**. Log in to Account B. Verify that the Gallery and Map are completely empty (Account A's spots are hidden).
3.  Log out, select **Clear cache**, and verify that Account A's cache is purged.

#### ⏭️ Continuation Prompt for Phase 4
Copy-paste this prompt to start the next phase:
```text
Please resume the Tag Spotter Firebase integration at Phase 4: Direct Cloud Pack Sharing. Refer to docs/follow-up-firebase-optional-backup-plan.md for technical requirements.
```

---

### Phase 4: Direct Cloud Pack Sharing via codes, Links & QR Codes

Builds our self-contained, lightweight sharing framework to exchange collections with friends seamlessly.

#### Key Tasks
1.  **Metadata Database**: Add a local Room table `loaded_packs` containing `packId`, `title`, `authorName`, `description`, `importedAt`, and `lastRefreshedAt`.
2.  **Pack Bundler**: Add a **"Share Collection"** option to Gallery search results/starred lists. Compile selected spots into a Firestore `/shared_packs/{packId}` document.
3.  **6-Char Generator**: Generate highly reliable 6-character uppercase alphanumeric codes (A-Z, 0-9) excluding easily confused characters to identify the Firestore document.
4.  **Deep Link & QR UI**:
    *   In the sharing dialog, display a copyable share code and deep link `https://tagspotter.net/import?pack=[CODE]`.
    *   Write a pure-Kotlin QR code renderer to draw the deep link as a vector **QR Code on a Compose Canvas** directly on the screen.
5.  **Import Modal**: Build an input dialog where users can paste a code or scan a QR code. Download the `SharedPack` and save the spots locally with `parentPackId = [CODE]`.
6.  **Refresh & Unload Buttons**: Provide simple controls next to loaded packs to instantly **Refresh** (re-pull document with LWW resolution) or **Unload** (cascadingly delete all local records associated with that pack ID).

#### 🧪 Phase 4 Manual Verification
1.  Select several starred spots and tap "Share". Give it a title, e.g. "Milano Tour", and generate the pack.
2.  Open the QR sharing card. Scan it with another device's camera (or paste the generated 6-character code).
3.  Verify the import modal pops up on the second device with "Milano Tour by Alice". Agree to load.
4.  Confirm the spots load into the feed, and thumbnails display correctly. Tap "Refresh" and verify it runs smoothly.
