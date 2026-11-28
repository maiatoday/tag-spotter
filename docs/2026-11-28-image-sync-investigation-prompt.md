# Image Update & Sync Reliability Investigation Prompt

Investigate and design a reliable, unified image update & sync pipeline across Android, iOS, Desktop (JVM), and Wasm in `/Users/maia/workspace/maiatoday/tag-spotter`.

## Context & Current State
- Canonical Firestore collection: `/spots/{uuid}` (`FirestoreSpotDocument` with top-level `ownerUid`, `isPublic`, `spot`, `images`, `notes`).
- Canonical Storage path: `spots/{ownerUid}/{spotUuid}/thumbnails/{imageUuid}.jpg`.
- Room v15 schema (`SpotEntity`, `SpotImageEntity` with `imagePath`, `thumbnailPath`, `remoteThumbnailUrl`, and `SpotOrigin`: `OWN`, `IMPORTED`, `PUBLIC_CACHE`).
- Open PR #101 (`fix/shared-device-public-spots`) adds shared-device public visibility (`isPublic = 1` in `SpotDao`), sign-out `syncPublicSpots()`, and healing stale non-local image paths on synced `OWN` rows during `savePublicSpot`.

## Why Image Updates Currently Feel Flaky (Observed Symptoms to Audit)
1. **Path overloading & display fallback inconsistencies**:
   - `SpotImage` has `imagePath`, `thumbnailPath`, and `remoteThumbnailUrl`, but UI code (`formatImageModel` in `feature/gallery` and `feature/detail`) only receives `(imagePath, thumbnailPath)` and doesn't consistently fall back to `remoteThumbnailUrl` when a local path is missing/deleted on another device.
2. **Inconsistent sync & caching across platforms**:
   - `NonWebSyncManager` (Android/iOS), `JvmSyncManager` (Desktop), and `WasmSyncManager` (Web) each have their own upload, download-token, local-disk caching (`~/Pictures/TagSpotter/thumbnails/...` on JVM), and `resolveRemoteThumbnails` logic.
   - `JvmSyncManager` doesn't implement `forceBackupSpot` / `forceBackupAllImages`.
   - `NonWebSyncManager.forceBackupAllImages` / `forceBackupSpot` skips images when `readBytesFromFile` is null (e.g. if the device only has an old/broken remote URL).
3. **Stale image UUIDs & silent failure modes**:
   - Different devices can end up with different `SpotImage.uuid` values for the same spot (e.g. after imports/re-saves), breaking UUID-matched updates.
   - Coil / Compose image loaders on Desktop & Android cache 403/failed responses or stale URLs without clear retry/invalidation when `remoteThumbnailUrl` updates.
   - Auth/API key failures or Storage upload errors fail silently in logs without surfacing actionable status to the user.

## Key Files to Inspect
- `core/model/src/commonMain/kotlin/net/maiatoday/tagspotter/core/model/Spot.kt`
- `core/database/src/nonWebMain/kotlin/net/maiatoday/tagspotter/core/database/LocalSpotRepository.kt`
- `core/sync/src/nonWebMain/kotlin/net/maiatoday/tagspotter/core/sync/NonWebSyncManager.kt`
- `core/sync/src/jvmMain/kotlin/net/maiatoday/tagspotter/core/sync/JvmSyncManager.kt`
- `core/sync/src/wasmJsMain/kotlin/net/maiatoday/tagspotter/core/sync/WasmSyncManager.kt`
- `feature/gallery/src/*/kotlin/net/maiatoday/tagspotter/feature/gallery/res/Resources.*.kt`
- `feature/detail/src/*/kotlin/net/maiatoday/tagspotter/feature/detail/res/Resources.*.kt`

## Goal
Audit the full lifecycle of a `SpotImage` (create → local thumbnail → upload → Firestore metadata → pull/public sync → local cache → UI display → update/replace/delete), identify every failure mode causing missing or stale images, and propose a minimal, rock-solid fix plan before editing code.

---

## Phased Fix Plan & Resumable Todo List

> **Note for clean-context sessions**: PR #101 (`fix/shared-device-public-spots`) is already merged into `main`. Check off items below as each phase lands so subsequent sessions can resume directly from the next unchecked phase.

### Phase 1 — Single Image Display & Fallback Pipeline
- [x] Consolidate all 4 duplicated `formatImageModel` implementations (`core/ui`, `feature/gallery`, `feature/detail`, `feature/map`) into one canonical API in `core/ui/image/ImageFormat.kt` accepting `(imagePath, thumbnailPath, remoteThumbnailUrl = "", preferFullImage = false)` (plus `SpotImage` overloads).
- [x] Enforce consistent fallback order on **all** platforms (Android, JVM, iOS, Wasm):
  - Thumbnails: existing local `thumbnailPath` → existing local `imagePath` → `remoteThumbnailUrl` → `http`/`data:` in `thumbnailPath`/`imagePath` → `content://` URI.
  - Full-screen (`preferFullImage = true`): existing local `imagePath` → existing local `thumbnailPath` → `remoteThumbnailUrl`.
  - Check local file existence on Android, JVM, and iOS (with `resolveLocalPath` for iOS `/Documents/` rebasing) so missing/other-device local paths always fall back to `remoteThumbnailUrl`.
- [x] Update `GalleryScreen`, `MapScreen`, `DetailHeroSection`, `DetailPhotoTimeline`, `DetailScreen`, and `ZoomableImageOverlay` to pass `remoteThumbnailUrl` and use the unified `core/ui` helper.
- [x] Add unit tests for image model resolution (local exists, local missing → remote fallback, `preferFullImage`, `http`/`data:`/`content://` handling).

### Phase 2 — Fix Sync Merge, UUID Healing & Public Sync
- [x] Extract shared `shouldApplyRemoteUpdate(localMatch, remoteDetail)` into `core/sync/src/commonMain` and use it in `NonWebSyncManager`, `JvmSyncManager`, and `WasmSyncManager`. Trigger update when `remote.lastEditedAt > local.lastEditedAt`, image/note UUID sets differ (`localUuids != remoteUuids`), or any matched image's `remoteThumbnailUrl` or `isMain` differs.
- [x] Fix `LocalSpotRepository.saveSyncedSpot` (`LocalSpotRepository.kt`) to prefer `resolvedRemoteUrl.ifBlank { image.thumbnailPath }` (and same for `imagePath`) when local files do not exist on disk, preventing stale other-device local paths in Firestore from shadowing valid remote URLs.
- [x] Guard `WasmSpotRepository.saveSyncedSpot` against overwriting unsynced local edits (`!existing.spot.isSynced && remote.lastEditedAt <= existing.spot.lastEditedAt`).
- [x] Call `resolveRemoteThumbnails` (using `detail.spot.ownerUid`) during `syncPublicSpots()` in `NonWebSyncManager`, `JvmSyncManager`, and `WasmSyncManager` when any public spot image lacks an `http` `remoteThumbnailUrl`.
- [x] Add unit tests for `shouldApplyRemoteUpdate` (mismatched UUIDs, `isMain` change, URL change, unsynced local protection) and `LocalSpotRepository.saveSyncedSpot` (broken other-device path healed by `remoteThumbnailUrl`).

### Phase 3 — Platform Parity (Thumbnails, iOS Paths, JVM Cache & Force Backup)
- [x] Fix iOS path rebasing in `core/sync/src/iosMain/.../FileHelper.ios.kt` (`readBytesFromFile`) and `core/photo/src/iosMain/.../RealPhotoProcessor.kt` (`fileExists`) by calling `resolveLocalPath`.
- [x] Generate real 480px JPEG thumbnails on JVM (`core/photo/src/jvmMain/.../RealPhotoProcessor.kt` using existing `decodeScaledBitmap`) and iOS (`core/photo/src/iosMain/.../RealPhotoProcessor.kt`) instead of uploading full-resolution originals.
- [x] Fix `JvmSyncManager.resolveRemoteThumbnails`: invalidate local cache when `remoteThumbnailUrl` / download token changes (or rely on Coil disk cache instead of manual `~/Pictures/TagSpotter/thumbnails` caching), fix cache-hit `imagePath` consistency, and parallelize downloads.
- [x] Never persist tokenless `?alt=media` fallback URLs for private spots in `JvmSyncManager` (treat missing `downloadTokens` as an error or fetch fresh metadata).
- [x] Implement `forceBackupSpot` / `forceBackupAllImages` in `JvmSyncManager` and return a structured `BackupResult(uploaded, refreshedUrls, failed, skippedNoLocalFile)` across all `SyncManager` implementations so UI gives accurate feedback instead of false *"Spot photos already backed up"*.

### Phase 4 — Coil Resilience & Actionable Sync Error Reporting
- [x] Configure shared Coil 3 `ImageLoader` / `SpotAsyncImage` wrapper with error logging, broken-image placeholder/retry UI, and cache keys incorporating `remoteThumbnailUrl` / local file mtime.
- [x] Add `syncError: StateFlow<String?>` (or `SyncStatus` sealed state) to `SyncManager` and surface actionable errors in Settings / Detail instead of silently deferring failed uploads.
- [ ] (Optional cleanup) Delete orphaned Firebase Storage thumbnails when a spot or image is deleted.
