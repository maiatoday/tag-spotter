# Remove Yandex and Implement Interactive Leaflet.js Maps on Web & Desktop

This plan outlines the removal of the non-interactive Yandex Static Maps from the Web (Wasm-JS) target and replaces it with a fully interactive Leaflet.js Map (OpenStreetMap). It also updates the multiplatform Google Maps export functions to support single-spot navigation and multi-spot walking routes on both Desktop (JVM) and Web (Wasm-JS) platforms.

## User Review Required

> [!NOTE]
> For the Web (Wasm-JS) platform, we will implement Leaflet.js via an `iframe` with a dynamic `srcdoc` rendered inside Compose's standard `@OptIn(ExperimentalComposeUiApi::class) HtmlView`. 
>
> This design choice has significant advantages:
> 1. **Complete Isolation**: Leaflet’s styles and global scope are isolated inside the iframe, preventing conflicts with the main page canvas.
> 2. **Clean JS-Interop**: Communicating events (like map clicks or readiness) back to Compose is done via highly robust, standard `window.parent.postMessage` events.
> 3. **Consistency**: Both Web and Desktop will use HTML-based Leaflet wrappers, ensuring uniform marker styling, map centers, and CartoDB Dark Matter tile configurations.

---

## Proposed Changes

### 1. Web Interactive Mapping

#### [MODIFY] [SpotMapView.wasmJs.kt](file:///Users/maia/workspace/maiatoday/tag-spotter/core/ui/src/wasmJsMain/kotlin/net/maiatoday/tagspotter/core/ui/SpotMapView.wasmJs.kt)
- Rewrite `SpotMapView` to use `HtmlView` hosting a Leaflet.js map.
- Implement an iframe `srcdoc` template that loads Leaflet CSS/JS via unpkg CDN.
- Use `window.parent.postMessage` from the iframe to notify Compose of `map_click` (to update coordinates) and `map_ready` events.
- Setup a `DisposableEffect` to add/remove the message listener.
- Use `iframe.contentWindow?.postMessage` inside the `update` block of `HtmlView` to update the map center, zoom, active pins, and dark map styling dynamically on state change without reloading the iframe.

---

### 2. Multiplatform Google Maps Exports (Navigation & Walking Routes)

#### [MODIFY] [GalleryPlatformHelper.jvm.kt](file:///Users/maia/workspace/maiatoday/tag-spotter/feature/gallery/src/jvmMain/kotlin/net/maiatoday/tagspotter/feature/gallery/res/GalleryPlatformHelper.jvm.kt)
- Implement `getRoute(spots: List<SpotDetails>)` using Java's `java.awt.Desktop` API.
- Construct the correct Google Maps URL containing the target destination, walking mode (`&travelmode=walking`), and the intermediate waypoints separated by `|`.
- Launch the default platform browser.

#### [MODIFY] [GalleryPlatformHelper.wasmJs.kt](file:///Users/maia/workspace/maiatoday/tag-spotter/feature/gallery/src/wasmJsMain/kotlin/net/maiatoday/tagspotter/feature/gallery/res/GalleryPlatformHelper.wasmJs.kt)
- Implement `getRoute(spots: List<SpotDetails>)` using browser's `kotlinx.browser.window.open`.
- Construct the same walking-focused Google Maps Directions URL and open it in a new browser tab (`_blank`).

---

## Verification Plan

### Automated Tests
- Run Gradle builds for Desktop and Web to verify compilation:
  - JVM Desktop: `./gradlew :desktopApp:run`
  - Wasm Web: `./gradlew :webApp:wasmJsBrowserDevelopmentRun` (or compile verification)

### Manual Verification
1. **Interactive Pins & Filtering**:
   - Open Map screen on both Desktop and Web.
   - Toggle filters and ensure Leaflet map markers filter instantly.
2. **Mini Maps on Details**:
   - Open any spot's details screen.
   - Verify that the card displays a mini Leaflet map with a single pin centered on the spot's coordinates.
3. **Coordinate Picker**:
   - Open the "Update Location" map dialog on Web/Desktop.
   - Click/tap anywhere on the map and verify that the neon marker snaps to the tapped location and coordinates update correctly on confirm.
4. **Google Maps Route Export**:
   - Go to Gallery Screen, select multiple spots, and click "Get Route in Google Maps".
   - Verify that it opens Google Maps in a browser showing walking directions connecting the selected spots.
