# Phase 4: Manual Testing Guide

This guide outlines step-by-step procedures to manually verify the Authentication and Reactive Synchronization flows implemented in Phase 4 once the UI is connected in Phase 5.

---

## Test Scenario 1: Authentication State Flows
Verify that the `AuthService` transitions and handles login states correctly on different platforms.

### Step 1: Anonymous State Verification
1. Launch the application (either JVM Desktop, Android, iOS, or Web/WasmJs).
2. Ensure you are in "Anonymous / Offline Mode" by default.
3. Add a new local Spot.
4. **Verification**: 
   - The spot is saved in the local database.
   - No remote sync calls are executed.
   - `isSyncing` flow remains `false`.

### Step 2: Sign-In Flow
1. Navigate to the login screen or trigger the sign-in option.
   - *JVM Desktop fallback*: Log in using Email/Password.
   - *Android, iOS, Web*: Use Google Sign-In.
2. Sign in with a test account.
3. **Verification**:
   - `AuthService.authState` emits a non-null `FirebaseUserWrapper`.
   - The user profile photo, email, and ID are successfully resolved.
   - An immediate incremental sync (`syncNow()`) is triggered in the background.

---

## Test Scenario 2: Bidirectional Cloud Synchronization
Verify that local changes sync to the cloud and remote updates pull down locally.

### Step 1: Push Sync (Local -> Cloud)
1. Add/edit several Spots while in anonymous/offline mode.
2. Ensure at least one Spot has a thumbnail attachment.
3. Sign in to your Firebase account.
4. **Verification**:
   - `SyncManager.isSyncing` transitions to `true`, then `false`.
   - Open your Firebase Console -> **Firestore Database**.
   - Check the `/users/{userId}/spots` collection. You should see all local spots uploaded with their correct attributes.
   - Open **Firebase Storage**. Verify that the image thumbnail has been uploaded to `/users/{userId}/thumbnails/${imageUuid}.jpg`.

### Step 2: Last-Write-Wins (LWW) Resolution
1. Choose a Spot that exists both locally and in Firestore.
2. Edit the spot locally with a timestamp of `T1`.
3. In the Firebase Console, manually edit the same spot's fields and set the `lastEditedAt` field to `T2` (where `T2 > T1`).
4. Trigger a sync.
5. **Verification**:
   - The local database should be updated to match the Firestore values (since Firestore had the newer timestamp).
6. Now, edit the local spot with a timestamp of `T3` (where `T3 > T2`).
7. Trigger a sync.
8. **Verification**:
   - The cloud database should receive the local update (since the local update had the newer timestamp).

### Step 3: Real-Time Updates
1. Stay signed in to your account inside the app.
2. In the Firebase Console Firestore browser, manually add a new document inside your user's `/spots` collection or modify an existing document.
3. **Verification**:
   - The real-time listener receives the update immediately.
   - The Spot updates or appears in your local list on the UI without requiring a manual refresh or a manual `syncNow()` call.

---

## Test Scenario 3: Sign-Out & Account Switching
Verify that user sessions tear down gracefully without leaking state or data.

1. Sign out of your account inside the app.
2. **Verification**:
   - `SyncManager.stopRealtimeSync()` is called.
   - Real-time listeners are unregistered.
   - `AuthService.authState` emits `null`.
3. Sign in with a *different* test account.
4. **Verification**:
   - The app triggers synchronization for the new user ID.
   - No data from the previous user's Firestore collection is pulled into the current user's local database unless explicitly shared.
