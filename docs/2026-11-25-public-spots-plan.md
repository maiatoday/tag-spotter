# Issue #92: Accessing Other Users' Public Spots (Read-Only)

**Date**: 2026-11-25  
**Issue**: [#92 — Investigate how to access public spots](https://github.com/maiatoday/tag-spotter/issues/92)  
**Board status**: **In progress** in *TagSpotter and TagAlong*.  
**Execution workflow**: Stop after each phase so context can be compacted before continuing.

---

## 1. Locked-In Product Decisions

| Area | Decision |
|---|---|
| **Toggle & Persistence** | Persisted `showPublicSpots` setting in `SettingsRepository` (default `true`); toggling it off is remembered across sessions. |
| **Switch Placement** | Shown in **both** `FilterBottomSheet` (next to *Starred Spots*) and `Settings`, backed by the same `SettingsRepository` flow. |
| **Signed-Out Behavior** | Public spots are **also fetched and shown when signed out** (using the open read rule on `/public_spots`). They remain strictly read-only (`Spot.isEditableBy(null) == false` whenever `ownerUid` is set). |
| **Freshness / Sync Trigger** | **Pull-based**: fetch `/public_spots` on app start, on manual sync, and whenever the user toggles *Show Public Spots* ON. |
| **Platform Scope** | **All platforms**: Android, iOS, Desktop (JVM), and Wasm/Web. |
| **UI & Source Filter** | Subtle community/public badge on other users' spots in Gallery & Map + fourth **`Public`** chip in `SOURCE` (`All` / `My Spots` / `Public` / `Imported`). When *Show Public Spots* is OFF, hide/disable the `Public` chip and reset `SOURCE` to `All` if `Public` was selected. |
| **Storage Security** | Harden `storage.rules` now by scoping public thumbnails to `public_spots/{userId}/{uuid}/thumbnails/{imageFile}` with `allow write: if request.auth != null && request.auth.uid == userId;`. *(Requires deploying `storage.rules` to Firebase.)* |

---

## 2. What Already Exists

- **Firestore Rules**: `firestore.rules` (L106–L122) already enforces open `read` and author-only `create` / `update` / `delete` on `/public_spots/{uuid}`.
- **Read-Only Model & Detail UI** (PR #63): `Spot.isEditableBy` (`core/model/.../Spot.kt`) and `DetailViewModel.canEdit` (`feature/detail/.../DetailViewModel.kt`) already hide edit/delete controls and block mutations on non-author spots while allowing local starring.
- **Dual-Write & Fetch (Partial)**: `NonWebSyncManager.kt` and `JvmSyncManager.kt` already dual-write to `/public_spots/{uuid}` and implement `fetchPublicSpots(...)`, currently only consumed by TagAlong (`HybridSpotRepository`).

---

## 3. Bugs & Gaps to Fix Along the Way

1. **Public thumbnail upload bug (Android/iOS & Desktop)**:
   - `NonWebSyncManager.kt` (L72–L124) and `JvmSyncManager.kt` (L150–L205) overwrite `thumbnailPath` (and `imagePath` on JVM) with private `users/{uid}/...` HTTPS URLs *before* uploading public thumbnails — causing 403s for other users (and skipping upload completely on JVM).
2. **Unauthenticated public fetch on Desktop (JVM)**:
   - `JvmSyncManager.kt` (L512–L513) uses `client.authenticatedClient` for `fetchPublicSpots`, which must work unauthenticated when signed out.
3. **Room scoping & ownership preservation**:
   - `SpotDao.kt` (L62–L82) excludes rows with another user's `ownerUid`.
   - `LocalSpotRepository.saveSyncedSpot` (L378–L387) overwrites `ownerUid = activeUid` (must NOT be used for community public spots).
4. **Gallery bulk/multi-select delete guard**:
   - `GalleryViewModel.deleteSpots` (L298–L306) has no `isEditableBy` check.
5. **Wasm/Web parity**:
   - `WasmSyncManager.kt` and JS helpers in `webApp/src/wasmJsMain/resources/index.html` lack `/public_spots` dual-write, public thumbnail upload, and `fetchPublicSpots()`.

---

## 4. Phased Implementation Plan

### Phase 1 — Storage Rules & Public Thumbnail Publishing
- Update `storage.rules`:
  - Add `/public_spots/{userId}/{uuid}/thumbnails/{imageFile}` with `allow read: if true; allow write: if request.auth != null && request.auth.uid == userId;`.
  - Keep read-only compatibility on the legacy path `/public_spots/{uuid}/thumbnails/{imageFile}` (`allow read: if true; allow write: if false;`) so any previously uploaded public thumbnails keep loading.
- Fix thumbnail publishing in `NonWebSyncManager.kt` and `JvmSyncManager.kt`:
  - Read bytes from the original `localDetail.images` before private URL rewriting; if local bytes aren't on-device, fall back to downloading bytes from the author's private storage path.
  - Upload to `public_spots/$userId/$uuid/thumbnails/${image.uuid}.jpg` and set both `thumbnailPath` and `imagePath` on the `/public_spots/{uuid}` doc to the public download URL (never leak local file paths or private URLs).

### Phase 2 — Local Storage & Sync for Public Spots (All Platforms)
- **Settings (`:core:settings`)**:
  - Add `showPublicSpots: StateFlow<Boolean>` (default `true`) and `updateShowPublicSpots(show: Boolean)` to `SettingsRepository` and platform implementations.
- **Database (`:core:database`)**:
  - Add `savePublicSpot(spotDetails: SpotDetails)` and `pruneRemovedPublicSpots(activePublicUuids: Set<String>, currentUserUid: String?)` to `SpotRepository`, `LocalSpotRepository`, `WasmSpotRepository`, and `FakeSpotRepository`:
    - Preserve remote `ownerUid` (never overwrite with `activeUid`), keep `isSynced = true`, and preserve existing local `isStarred` on refresh.
    - Never overwrite a spot owned by `activeUid`.
  - Update `SpotDao.kt` read queries (`getAllSpotsDetails`, `getAllSpotsDetailsByCategory`, `getAllUsedTags`, `getStarredSpots`, `getStarredSpotsCount`) to include `OR isPublic = 1`.
  - Ensure `WasmSpotRepository` preserves `ownerUid` and respects visibility scoping consistently with `LocalSpotRepository`.
- **Sync (`:core:sync`)**:
  - Add `suspend fun syncPublicSpots()` to `SyncManager` (works both signed-in and signed-out):
    - Pulls `/public_spots`, skips entries where `ownerUid == activeUserId` (managed by personal sync), calls `repository.savePublicSpot(...)`, and prunes removed/unpublished public spots.
    - Called from `syncNow()`, on app start in `MainViewModel` (when `showPublicSpots` is `true`, even if `user == null`), and when toggling `showPublicSpots` ON.
  - In `JvmSyncManager.kt`: ensure `fetchPublicSpots()` works unauthenticated when signed out.
  - In `WasmSyncManager.kt` + `webApp/src/wasmJsMain/resources/index.html`:
    - Add JS helpers for dual-writing/deleting `/public_spots/{uuid}`, uploading public thumbnails to `public_spots/{userId}/{uuid}/thumbnails/{imageUuid}.jpg`, and fetching `/public_spots` (unauthenticated-capable).

### Phase 3 — UI, Filtering, Badges & Read-Only Guards
- **Persisted Switch**:
  - Add *Show Public Spots* switch to `SettingsScreen` / `SettingsViewModel` and `FilterBottomSheet` (next to *Starred Spots*).
  - Turning it ON triggers `syncManager.syncPublicSpots()`.
  - Turning it OFF hides other users' public spots in Gallery & Map (and resets `SOURCE` to `"All"` if `"Public"` was active).
- **Source Filtering (`GalleryViewModel` & `MapViewModel`)**:
  - Inject `AuthService` and `SettingsRepository` (`showPublicSpots`).
  - Filter out other users' public spots when `!showPublicSpots`.
  - Support 4 `SOURCE` chips in `FilterBottomSheet.kt`:
    - `"All"`: user's own spots + imported spots + (if `showPublicSpots`) other users' public spots.
    - `"My Spots"`: `spot.isEditableBy(currentUserUid)`.
    - `"Public"` (shown when `showPublicSpots` is ON): `spot.isPublic && !spot.isEditableBy(currentUserUid)`.
    - `"Imported"`: `spot.isImported`.
- **Visual Badges**:
  - Add a subtle community/public badge on other users' public spots in `GalleryScreen` cards and `MapScreen` markers.
- **Delete Guard**:
  - Guard `GalleryViewModel.deleteSpots(...)` with `detail.spot.isEditableBy(currentUserUid)` (and reflect non-deletable items in multi-select).

### Phase 4 — Verification
- **Automated Tests**:
  - `LocalSpotRepository` / `SpotDao` tests: ownership preserved, `isEditableBy(viewerUid) == false`, local `isStarred` preserved on refresh, excluded from `getUnsyncedSpots()`, pruned when unpublished.
  - `GalleryViewModel` & `MapViewModel` tests: `showPublicSpots` toggle, 4 source filters (`All`, `My Spots`, `Public`, `Imported`), and `deleteSpots` blocking non-author spots.
  - `NonWebSyncManager` / `JvmSyncManager` tests: public thumbnail path (`public_spots/{userId}/{uuid}/thumbnails/...`) and unauthenticated `fetchPublicSpots()`.
- **Manual E2E**:
  - Deploy updated `storage.rules`.
  - User A marks a spot Public & syncs → User B (signed in) and signed-out user both see it with its image, can open Detail and star it, cannot edit/delete it, and can hide it via the *Show Public Spots* switch (persisted across restart).
