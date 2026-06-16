# Implementation Plan - Hybrid Architecture & TS Pack Support for WASM Web App (Approved Decisions)

This plan details the technical implementation of **Hybrid Architecture (Option C)** for the WASM web application based on our aligned design decisions:
1. **Hybrid Local Persistence**:
   - **Spot Metadata & Database JSON**: Persisted in browser `localStorage` for lightweight, fast text retrieval.
   - **Binary Image Blobs**: Persisted in browser `IndexedDB` (natively supports raw binary Blobs without Base64 overhead).
2. **TS Pack Import & Export**:
   - Pack files (`.ts_pack` / ZIP format) will be unzipped and zipped directly in the browser.
   - **JSZip** will be integrated as a fast, reliable, zero-config CDN library loaded in `index.html`.
3. **Optimized Image Processing**:
   - Imported images will be compressed and scaled to a maximum boundary of 1080p at 80% JPEG quality to ensure low memory footprint and high browser performance.
4. **Local-First Pre-requisite Phase**:
   - Focus strictly on robust local browser storage and fully working `.ts_pack` backup import/export, with clean repository abstractions so that Firebase Cloud Sync can be easily layered on top as an optional toggle later.

---

## Detailed Technical Design

### 1. Browser Storage & Lifecycle
* **App Launch**: `WasmSpotRepository` loads serialized spot metadata from `localStorage`. It restores image blobs from `IndexedDB` and creates temporary browser-session-bound **Object URLs** (`blob:http://...`) using `URL.createObjectURL(blob)` so the Coil image loader can seamlessly display them.
* **Saving a Spot**: Write the updated database JSON to `localStorage`, and save the associated image binary blob to `IndexedDB`.
* **Page Refresh**: Since the spots JSON is in `localStorage` and image blobs are in `IndexedDB`, the application remains fully persistent across browser restarts, page refreshes, and tabs.

### 2. JSZip Library Integration
* Add JSZip CDN script tag to `index.html`'s `<head>`:
  ```html
  <script src="https://cdnjs.cloudflare.com/ajax/libs/jszip/3.10.1/jszip.min.js"></script>
  ```
* In Kotlin/Wasm platform-specific code, we will write a clean, type-safe JS interop external class or small `js("...")` wrappers to call `JSZip` to read/write zip entry arrays.

### 3. File Import & Export Flow
* **File Picker**: We'll implement `rememberImportLauncher` to open the native OS file picker. On change, the picked file's `ArrayBuffer` is read and unzipped using `JSZip`.
* **File Downloader**: We'll implement `rememberLauncher` and `exportPack` to generate a compiled zip blob, and trigger a programmatical download using a virtual `<a>` element.

---

## Proposed Changes

### Component: Core Database (`:core:database`)

#### [MODIFY] [WasmSpotRepository.kt](file:///Users/maia/workspace/maiatoday/tag-spotter/core/database/src/wasmJsMain/kotlin/net/maiatoday/tagspotter/core/database/WasmSpotRepository.kt)
* Implement local persistence read/write routines utilizing `localStorage` and `IndexedDB`.
* Implement `importPack` using our JSZip wrappers to parse `spots.json` and persist unpacked image/thumbnail Blobs.

---

### Component: Gallery UI Helpers (`:feature:gallery`)

#### [MODIFY] [GalleryPlatformHelper.wasmJs.kt](file:///Users/maia/workspace/maiatoday/tag-spotter/feature/gallery/src/wasmJsMain/kotlin/net/maiatoday/tagspotter/feature/gallery/res/GalleryPlatformHelper.wasmJs.kt)
* **`rememberImportLauncher`**: Create and trigger a hidden `<input type="file" accept=".ts_pack, .zip">` DOM element, passing the unpacked result to `WasmSpotRepository`.
* **`rememberLauncher`**: Provide a standard callback launcher for trigger flows.
* **`exportPack`**: Bundle the spots data and image Blobs into a zip file using JSZip and trigger a native browser download (`tagspotter_backup.ts_pack`).

---

### Component: Web App Entry (`:webApp`)

#### [MODIFY] [index.html](file:///Users/maia/workspace/maiatoday/tag-spotter/webApp/src/wasmJsMain/resources/index.html)
* Append the JSZip CDN script tag inside the `<head>` block.

---

## Verification Plan

### Manual Verification
1. **Verify Startup & Local Persistence**:
   - Add several spots. Refresh the browser tab or open it in a new window.
   - Verify that all metadata, thumbnails, and images are fully persistent and render instantly.
2. **Verify TS Pack Backup Export**:
   - Navigate to the Gallery screen and tap the Export button.
   - Verify a `.ts_pack` archive downloads containing valid `spots.json`, `images/`, and `thumbnails/` directories.
3. **Verify TS Pack Backup Import**:
   - Clear browser storage (or use an Incognito tab) to start with an empty database.
   - Open the hamburger menu, tap **Import Pack**, and pick the downloaded `.ts_pack` file.
   - Verify that all spots, coordinates, descriptions, and thumbnails are successfully unzipped, restored, and displayed on both the Gallery and Map.
