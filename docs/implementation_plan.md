# Map Architecture Modernization, AI Security & UI Performance Measurement Plan

This plan addresses three critical technical objectives for **Tag Spotter**:
1. **Map Engine Architecture Strategy**: Replacing the suboptimal JavaFX `WebView` Desktop map implementation with a native Compose/Skia tile renderer.
2. **AI Access Restriction Strategy**: Enforcing strict platform boundaries so AI capabilities remain disabled on Desktop and Web to eliminate API key exposure risks.
3. **Multiplatform UI Performance Profiling & Benchmarking**: Measuring RAM footprint, GC pressure, frame rendering times, and startup latency across platforms before and after refactoring to provide data for **Blog Post #4**.

---

## User Review Required

> [!IMPORTANT]
> **Recommendation on Google Maps API vs Open-Source Maps**:
> We strongly recommend **AGAINST switching to Google Maps API** for the following reasons:
> 1. **API Key Security Risk on Desktop & Web**: Google Maps API keys embedded in Desktop binaries or Web Wasm bundles cannot be restricted by Android SHA1 fingerprints or iOS bundle IDs, leaving them vulnerable to extraction and quota abuse.
> 2. **Desktop WebView Issue Unsolved**: Google Maps has no native JVM Desktop SDK. Using Google Maps on Desktop would STILL require loading the Google Maps JS API inside a JavaFX `WebView`, failing to resolve the Desktop performance overhead.
> 3. **Cost & Billing Lock-in**: Google Maps requires Google Cloud billing and charges beyond free quotas.
> 
> **Proposed Map Solution**:
> - **Desktop (JVM)**: Replace the heavy JavaFX `WebView` / `JFXPanel` setup with a **pure Compose Skia OpenStreetMap raster tile renderer**. This renders map tiles natively on desktop using Skia/OpenGL, removing JavaFX dependencies and dramatically reducing memory footprint (~50MB vs ~570MB) and startup lag.
> - **Android**: Retain **OSMDroid** for offline-first OpenStreetMap rendering.
> - **iOS**: Retain native **Apple MapKit** (`MKMapView` via `UIKitView`), which is 100% free, keyless, and hardware-accelerated.
> - **Web (WasmJS)**: Replace static map image with a Leaflet / WebGL canvas interop.

> [!IMPORTANT]
> **AI Access Policy**:
> - **Desktop & Web**: AI access will be **hard-disabled**. The Koin modules for Desktop and Web will explicitly bind `UnsupportedAiRecognitionService()`. `isAiAugmentationAvailable` in `DetailViewModel` will return `false`, automatically hiding AI identification buttons and suggestion cards. In `SettingsScreen`, the AI Artist Recognition switch will be completely hidden.
> - **Android & iOS**: Retain `AndroidFirebaseAiService` via Firebase Vertex AI with App Check security.

---

## Technical Comparison Matrix

| Criteria | Google Maps API | Recommended Open-Source Strategy (Compose Skia / Native KMP) |
| :--- | :--- | :--- |
| **API Key Required** | ❌ Yes (Requires Google Cloud billing) | ✅ **No** (Keyless OpenStreetMap vector/raster tiles) |
| **Desktop Performance** | ❌ Poor (Requires JavaFX `WebView` JS wrapper) | ✅ **High** (Native Skia / OpenGL hardware rendering) |
| **Desktop RAM Footprint** | ❌ ~570 MB (High JavaFX WebKit overhead) | ✅ **~50 MB** (Pure Compose Skia canvas) |
| **Rendering Stability** | ❌ **Broken Grid Glitches & Thread Deadlocks** | ✅ **100% Reliable Native Compose Canvas** |
| **Desktop Security** | ❌ Key exposed in desktop executable | ✅ **Secure** (No API keys to leak) |
| **iOS Performance** | ✅ Native Maps SDK | ✅ **Native MapKit** (Hardware-accelerated) |
| **Offline Capabilities** | ⚠️ Limited caching | ✅ **Full local disk tile caching** |

---

## Pre-Refactoring Investigation & Visual Bug Analysis 🐛

### Desktop Map Failure Investigation

During Step 0 testing, opening the map screen and panning on macOS revealed severe rendering corruption and functional failure:

#### Observed Rendering Bugs
1. **Fragmented Tile Grid**: Map tile images render disconnected across offset grid locations.
2. **Large Gray Canvas Patch Holes**: Large rectangular gray voids appear where tile images fail to download, place, or align into DOM container elements.
3. **Displaced Attribution Overlay**: Leaflet attribution text gets displaced and overlaps misplaced tile fragments.
4. **Failure to Unmount / Thread Deadlock**: Navigating back from Map to Gallery fails to release off-heap WebKit memory, leaving the process stuck at **567.2 MB RAM**.

#### Root Cause Analysis
- **Swing / JavaFX HiDPI Scaling Misalignment**: Compose Multiplatform uses HiDPI logical density scaling on macOS Retina screens. JavaFX `WebView` inside `JFXPanel` fails to recalculate DOM container pixel dimensions dynamically when Compose re-layouts `SwingPanel`.
- **Missing Container Resize Signals**: Leaflet JS requires explicit `map.invalidateSize()` events when its parent container dimensions change. JavaFX `WebView` wrapped in Swing does not trigger window resize events down to JavaScript, leaving Leaflet's internal pixel bounds permanently broken.
- **AWT / FX Thread Race Conditions**: AWT Event Dispatch Thread (EDT) and JavaFX Application Thread get out of sync during panning or view recomposition, resulting in failed tile requests and thread deadlocks.

#### Architectural Solution (Step 1)
- Completely remove `JFXPanel`, JavaFX `WebView`, and `SwingPanel`.
- Build a native Compose Skia OpenStreetMap tile renderer in `SpotMapView.jvm.kt` drawing tiles directly on Compose's native `Canvas`. Skia handles HiDPI Retina scaling natively, eliminating container resize glitches, thread deadlocks, and grid misalignment.

---

## Phased Execution & UI Performance Benchmark Steps

### Step 0: Pre-Refactoring Performance Baseline Measurement 📊 (Completed)
1. **Desktop JVM Baseline (JavaFX WebView)**:
   - Initial Splash / Login: **442.0 MB RSS**
   - WebKit Init Spike: **260.9% CPU surge**
   - Peak Map Footprint: **570.8 MB RSS** (+128.8 MB off-heap native memory)
   - Settled / Return to Gallery: **567.2 MB RSS** (Failed to release native memory)

---

### Step 1: Code Refactoring (Map Engine & AI Restrictions) 🛠️

#### Core UI Module (`:core:ui`)
- **`SpotMapView.jvm.kt`**:
  - Remove JavaFX `JFXPanel`, `WebView`, and `WebBridge` Swing panel wrappers.
  - Implement native Compose Skia OpenStreetMap tile renderer with mouse drag to pan, scroll-wheel to zoom, and custom marker pins.
  - Add in-memory + local disk tile cache in app data directory.
- **`build.gradle.kts` (`:core:ui`)**:
  - Remove `org.openjfx:javafx-swing` and `org.openjfx:javafx-web` dependencies from `jvmMain`.
- **`SpotMapView.wasmJs.kt`**:
  - Replaced static Yandex fallback map URL with an interactive Compose Multiplatform tile grid renderer powered by Coil `AsyncImage` and Compose `Canvas`.
- **`GalleryPlatformHelper.jvm.kt` & `GalleryPlatformHelper.wasmJs.kt`**:
  - Implemented `getRoute(spots)` walking directions export for Desktop JVM (`java.awt.Desktop.getDesktop().browse()`) and Web WasmJS (`window.open()`).

#### Core AI & Settings Modules (`:core:ai`, `:feature:settings`)
- **Desktop/Web Koin DI Modules**:
  - Bind `single<AiRecognitionService> { UnsupportedAiRecognitionService() }`.
- **`SettingsScreen.kt`**:
  - Hide AI Artist Recognition toggle switch when `!aiRecognitionService.isSupported` (on Desktop and Web).

---

### Step 2: Post-Refactoring Performance Measurement & Validation 📈
Repeat profiling to compile comparative data for **Blog Post #4**:
1. **Desktop JVM Measurement (Compose Skia Native)**:
   - Launch `./gradlew :desktopApp:run`.
   - Run `jcmd <pid> GC.heap_info` and `jcmd <pid> VM.native_memory summary`.
   - Validate target metric: **<60MB RAM footprint (>80% reduction)**, zero grid misalignment, and 60fps fluid map interaction.
2. **Compile Comparative Benchmark Table**:
   - Save metrics into a markdown table comparing JavaFX WebView vs Compose Skia Native across RAM, startup time, and FPS.

---

## Verification Plan

### Automated Tests
- Run project-wide unit tests to ensure DI bindings and ViewModels behave as expected:
  ```bash
  ./gradlew test
  ```
- Test `DetailViewModelTest` on JVM to confirm `isAiAugmentationAvailable` is `false` when AI service is unsupported.

### Manual Verification & Profiling Commands
1. **Desktop Profiling (`:desktopApp`)**:
   - `jps -l` (Get Desktop PID)
   - `jcmd <PID> GC.heap_info`
   - `jstat -gc <PID> 1000`
2. **Android Profiling (`:androidApp`)**:
   - `adb shell dumpsys meminfo net.maiatoday.tagspotter`
   - `adb shell dumpsys gfxinfo net.maiatoday.tagspotter`
