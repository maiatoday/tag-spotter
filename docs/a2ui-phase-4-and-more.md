# Phase 4 and Beyond: Single-App Consolidation, Real A2UI Libraries & Test Plan

> **Document:** `docs/a2ui-phase-4-and-more.md`  
> **Master Intent Document:** [a2ui-kmp-intent-document.md](file:///Users/maia/workspace/maiatoday/tag-spotter/docs/a2ui-kmp-intent-document.md)  
> **Task Tracker:** [tagalong-task-tracker.md](file:///Users/maia/workspace/maiatoday/tag-spotter/docs/tagalong-task-tracker.md)  
> **ADK Runtime Guide:** [tagalong-adk-runtime-guide.md](file:///Users/maia/workspace/maiatoday/tag-spotter/docs/tagalong-adk-runtime-guide.md)  
> **Date:** 2026-11-16  
> **Workspace:** `/Users/maia/workspace/maiatoday/tag-spotter`

---

## 📑 Table of Contents

1. [Executive Summary & Resolved Design Decisions](#1-executive-summary--resolved-design-decisions)
2. [Single-App Architecture & In-App Mode Switching](#2-single-app-architecture--in-app-mode-switching)
   - [2.1 Why Retire `:tagalongApp:android` and `:tagalongApp:desktop`?](#21-why-retire-tagalongappandroid-and-tagalongappdesktop)
   - [2.2 Target Module Graph (Single App Shell)](#22-target-module-graph-single-app-shell)
   - [2.3 KMP-Safe In-App Switching via Composable Slot (`:feature:main`)](#23-kmp-safe-in-app-switching-via-composable-slot-featuremain)
   - [2.4 Desktop vs. Mobile Map Layout & Android App Shortcut](#24-desktop-vs-mobile-map-layout--android-app-shortcut)
3. [Side-by-Side A2UI Library Architecture (`androidx.a2ui` vs `a2ui-4k`)](#3-side-by-side-a2ui-library-architecture-androidxa2ui-vs-a2ui-4k)
   - [3.1 Eliminating the Hand-Rolled Parser & Router](#31-eliminating-the-hand-rolled-parser--router)
   - [3.2 Head-to-Head Library Comparison Matrix (Blog Post Reference)](#32-head-to-head-library-comparison-matrix-blog-post-reference)
   - [3.3 Topic 1: Hybrid Catalog (Basic Primitives + 4 Custom Domain Cards) & `toJsonSchemaString()`](#33-topic-1-hybrid-catalog-basic-primitives--4-custom-domain-cards--tojsonschemastring)
   - [3.4 Topic 2: Inline Surface Render Loop, `updateDataModel` & Native `functionCall` vs `event` Routing](#34-topic-2-inline-surface-render-loop-updatedatamodel--native-functioncall-vs-event-routing)
   - [3.5 Unified `A2UIRendererEngine` Contract & 2-State Toggle](#35-unified-a2uirendererengine-contract--2-state-toggle)
4. [UI Component Reuse & Design System Unification (`:core:ui`)](#4-ui-component-reuse--design-system-unification-coreui)
   - [4.1 Shared Stateless Compose Widgets Across Engines and Modes](#41-shared-stateless-compose-widgets-across-engines-and-modes)
   - [4.2 Component Inventory & Sharing Matrix](#42-component-inventory--sharing-matrix)
   - [4.3 Bi-directional Reuse (`DetailScreen` Using Shared Cards)](#43-bi-directional-reuse-detailscreen-using-shared-cards)
5. [Data Access, Auth & Persistence in the Unified App](#5-data-access-auth--persistence-in-the-unified-app)
   - [5.1 Shared Local Storage (`SpotDatabase` & `TagAlongDatabase`)](#51-shared-local-storage-spotdatabase--tagalongdatabase)
   - [5.2 Desktop JVM Database Path Fix (`JvmSpotDatabase.kt`)](#52-desktop-jvm-database-path-fix-jvmspotdatabasekt)
   - [5.3 Shared Firebase Auth, App Check & `HybridSpotRepository`](#53-shared-firebase-auth-app-check--hybridspotrepository)
   - [5.4 Complete End-to-End Data Flow Diagram](#54-complete-end-to-end-data-flow-diagram)
6. [TagAlong UX Features & Session Architecture](#6-tagalong-ux-features--session-architecture)
   - [6.1 Light and Dark Mode Switching](#61-light-and-dark-mode-switching)
   - [6.2 "New Conversation" Lifecycle](#62-new-conversation-lifecycle)
   - [6.3 Conversation History Plan & Multi-Session Surface Replay](#63-conversation-history-plan--multi-session-surface-replay)
7. [Multimodal Photo Bytes, Category Mapping & 1-Tap Spot Creation](#7-multimodal-photo-bytes-category-mapping--1-tap-spot-creation)
   - [7.1 Fixing the Phase 3 Multimodal Photo Bug (Sending Real Image Bytes)](#71-fixing-the-phase-3-multimodal-photo-bug-sending-real-image-bytes)
   - [7.2 Category vs. Style Tags Alignment (`Spot.CATEGORIES`)](#72-category-vs-style-tags-alignment-spotcategories)
   - [7.3 Complete 1-Tap Agentic Spot Creation (`@Tool saveNewSpot` + Missing-Field A2UI Form)](#73-complete-1-tap-agentic-spot-creation-tool-savenewspot--missing-field-a2ui-form)
8. [Phase 4 Implementation Roadmap & Next Steps](#8-phase-4-implementation-roadmap--next-steps)
   - [Step 4.1: Retire `:tagalongApp:*` & Integrate TagAlong Tab into `:androidApp` & `:desktopApp`](#step-41-retire-tagalongapp--integrate-tagalong-tab-into-androidapp--desktopapp)
   - [Step 4.2: Shared UI Primitives & Stateless Card Consolidation (`:core:ui`)](#step-42-shared-ui-primitives--stateless-card-consolidation-coreui)
   - [Step 4.3: Replace Hand-Rolled A2UI with Hybrid `androidx.a2ui` & `a2ui-4k` Engines (`:core:a2ui`)](#step-43-replace-hand-rolled-a2ui-with-hybrid-androidxa2ui--a2ui-4k-engines-corea2ui)
   - [Step 4.4: Wire Agent Prompts to `toJsonSchemaString()`, Fix Photo Bytes & Connect Render Loop](#step-44-wire-agent-prompts-to-tojsonschemastring-fix-photo-bytes--connect-render-loop)
   - [Step 4.5: Theme Switching, New Conversation Controls & Desktop DB Persistence Fix](#step-45-theme-switching-new-conversation-controls--desktop-db-persistence-fix)
   - [Step 4.6: 1-Tap Agentic Spot Saving, `Spot.CATEGORIES` Mapping & Two-Way DataModel Actions](#step-46-1-tap-agentic-spot-saving-spotcategories-mapping--two-way-datamodel-actions)
   - [Step 4.7: Conversation History Drawer / Sheet Implementation](#step-47-conversation-history-drawer--sheet-implementation)
   - [Step 4.8: Full Monorepo Regression Gate](#step-48-full-monorepo-regression-gate)
9. [Future Phases Horizon (Phase 5+)](#9-future-phases-horizon-phase-5)
10. [Comprehensive Test Plan](#10-comprehensive-test-plan)
11. [Clean Context Handoff & Execution Prompt](#11-clean-context-handoff--execution-prompt)

---

## 1. Executive Summary & Resolved Design Decisions

Through our Phase 4 design review, we locked in **9 concrete architectural decisions**:

| # | Decision Area | Locked-In Design Choice |
| :-: | :--- | :--- |
| **1** | **App Consolidation** | Retire `:tagalongApp:android` and `:tagalongApp:desktop`. Embed `:feature:tagalong` directly into **`:androidApp`** and **`:desktopApp`** via an in-app `Tab.TagAlong` (using a KMP-safe composable slot in `:feature:main`) + a static Android App Shortcut (`res/xml/shortcuts.xml`). Shares one `SpotDatabase`, photo directory, and Firebase App Check setup. |
| **2** | **No Hand-Rolled A2UI** | Delete `A2UIPayloadParser.kt`, `A2UIComponents.kt`, and the manual `when` router in `NeonA2UICatalog.kt`. Implement official Jetpack **`androidx.a2ui` (`1.0.0-alpha02`)** in `androidMain` and KMP **`com.contextable:a2ui-4k` (`0.10.0`)** in `commonMain`. |
| **3** | **Protocol Emission** | Match Jetpack `androidx.a2ui`'s canonical design: inject `catalog.toJsonSchemaString()` into the system prompt so the agent emits standard A2UI v0.9 JSON envelopes (`createSurface`, `updateDataModel`, `updateComponents`) parsed by `A2uiJsonMessageParser` / `SurfaceStateManager`. |
| **4** | **Hybrid Catalog Strategy** | Combine **4 Custom Domain Components** (`SpotCard`, `TourCard`, `ArtistBioCard`, `MapPinOverlayWidget` backed by stateless composables in `:core:ui`) with the **Built-in Basic Catalog Primitives** (`materialA2uiBasicCatalogV1` / `CoreCatalog`: `Card`, `Column`, `Row`, `Text`, `Button`, `CheckBox`, `ChoicePicker`, `TextField`). Interactive quizzes, community voting, and missing-field forms are composed from Basic Catalog primitives + two-way data bindings. |
| **5** | **Inline Surface Rendering** | Render A2UI surfaces **inline inside the chat stream**, keyed by `surfaceId`. Subsequent `updateDataModel(surfaceId, ...)` messages or local two-way bindings mutate the existing surface in place without duplicating cards. |
| **6** | **Native Action Routing (`functionCall` vs `event`)** | Use A2UI v0.9's built-in `Action` split (`A2uiActionSchema`):<br>• **`functionCall`**: Executed locally on the client via `A2uiCatalog.functions` (`focusMap`, `openSpotDetail`).<br>• **`event`**: Emitted via `processor.outboundEvents` (`A2uiClientEventMessage` with `clientDataModel`) to the ADK `Runner` or handled by `TagAlongViewModel` for `SpotRepository` updates. |
| **7** | **Engine Switcher** | Simple **2-State Toggle (`ANDROIDX_JETPACK_OFFICIAL` ↔ `A2UI_KMP_4K`)** in the TagAlong top bar on Android, replaying the active session's A2UI v0.9 envelopes when toggled. |
| **8** | **Real Multimodal Bytes, 1-Tap Save & `Spot.CATEGORIES`** | • Fix Phase 3's bug that sent `"User attached photo URI: ..."` as text—pass real compressed image bytes (`Blob` / `inlineData`) via `:core:photo`.<br>• Support 1-tap direct save (`@Tool saveNewSpot` / `save_spot` event) once all required `Spot` fields are gathered (via EXIF + GPS + Gemini vision, or a Basic Catalog `TextField` form card if info is missing).<br>• Keep `spot.category` strictly within `Spot.CATEGORIES` (`"graffiti"`, `"sculpture"`, `"nature"`, `"architecture"`, `"public_place"`, `"food"`) and put street-art styles (`"mural"`, `"stencil"`, `"sticker"`, `"paste-up"`) in `spot.tags`. |
| **9** | **Map Layout (Desktop vs. Mobile)** | • **Desktop (`>= 760.dp`):** 2-pane split (Chat + A2UI left, live synchronized `SpotMapView` right).<br>• **Mobile (`< 760.dp`):** Remove the redundant inner `"Live Map"` sub-tab inside `TagAlongScreen`; render single-column Chat + A2UI, and when the user triggers *"View on Map"*, switch `MainContainer` to the main `Map` tab centered on that spot. |

---

## 2. Single-App Architecture & In-App Mode Switching

### 2.1 Why Retire `:tagalongApp:android` and `:tagalongApp:desktop`?

| Concern | Separate Apps (`:tagalongApp:*`) | Unified App with In-App Mode Switching (`:androidApp` & `:desktopApp`) |
| :--- | :--- | :--- |
| **Local `SpotDatabase` Access** | ❌ Isolated by Android OS sandbox; TagAlong cannot read/write Tag Spotter's local SQLite DB. | ✅ **Single shared `SpotDatabase` instance** injected by Koin in the same process. |
| **Local Photo Files** | ❌ Locked inside Tag Spotter's private `filesDir`. | ✅ Directly readable by Coil, `PhotoProcessor`, and Gemini multimodal prompts. |
| **Firebase App Check & Auth** | ⚠️ Requires registering a second Android package, second debug token, and duplicate sign-in. | ✅ Reuses `:androidApp`'s existing `google-services.json`, App Check attestation, and Google Sign-In session. |
| **Spot Detail & Map Navigation** | ⚠️ Requires cross-app `Intent` / URI deep-link handoff. | ✅ Direct in-process navigation (`backStack.add(DetailKey(spotId))` and switching to `Tab.Map`). |

---

### 2.2 Target Module Graph (Single App Shell)

```mermaid
flowchart TD
    subgraph AppEntrypoints["App Targets"]
        AndroidApp[":androidApp<br><i>Android (Gallery, Map, TagAlong + App Check)</i>"]
        DesktopApp[":desktopApp<br><i>Desktop JVM (Gallery, Map, TagAlong)</i>"]
        IOSApp["iosApp<br><i>iOS (Gallery, Map)</i>"]
        WebApp[":webApp<br><i>WasmJs (Gallery, Map)</i>"]
    end

    subgraph FeatureLayer["Feature Modules"]
        FeatMain[":feature:main<br><i>Unified Shell + Optional tagAlongContent Slot</i>"]
        FeatGallery[":feature:gallery"]
        FeatMap[":feature:map"]
        FeatDetail[":feature:detail"]
        FeatSettings[":feature:settings"]
        FeatTagAlong[":feature:tagalong<br><i>Android & JVM (Chat + A2UIRendererHost + Desktop MapPane)</i>"]
    end

    subgraph EngineModules["A2UI & Agent Engines (Android & JVM)"]
        CoreA2UI[":core:a2ui<br><i>JetpackA2UIEngine (androidx.a2ui) + KmpA2UIEngine (a2ui-4k)</i>"]
        CoreAgent[":core:agent<br><i>Google ADK Kotlin + @Tool StreetArtTools</i>"]
    end

    subgraph SharedCore["Shared Core Modules (All Targets)"]
        CoreUI[":core:ui<br><i>Stateless Neon Cards, Chips, Map, ZoomOverlay</i>"]
        CoreDB[":core:database<br><i>SpotDatabase v14 + TagAlongDatabase</i>"]
        CoreSync[":core:sync<br><i>HybridSpotRepository + SyncManager</i>"]
        CoreModel[":core:model"]
        CoreLoc[":core:location"]
        CorePhoto[":core:photo"]
        CoreSettings[":core:settings"]
    end

    AndroidApp --> FeatMain & FeatTagAlong & CoreA2UI & CoreAgent
    DesktopApp --> FeatMain & FeatTagAlong & CoreA2UI & CoreAgent
    IOSApp --> FeatMain
    WebApp --> FeatMain

    FeatMain --> FeatGallery & FeatMap & FeatDetail & FeatSettings
    FeatTagAlong --> CoreA2UI & CoreAgent & CoreUI & CoreDB & CoreSync & CorePhoto
    CoreA2UI --> CoreUI & CoreModel
    CoreAgent --> CoreSync & CoreDB & CoreLoc & CorePhoto & CoreModel
```

---

### 2.3 KMP-Safe In-App Switching via Composable Slot (`:feature:main`)

Because [feature/main/build.gradle.kts](file:///Users/maia/workspace/maiatoday/tag-spotter/feature/main/build.gradle.kts) compiles for `android`, `jvm`, `ios*`, and `wasmJs`, while `:feature:tagalong` targets `android` and `jvm` (matching `google/adk-kotlin`), `:feature:main` (`commonMain`) does **not** depend on `:feature:tagalong` at the Gradle level.

Instead, `MainNavigation` and `MainContainer` expose an optional composable slot:

```kotlin
// In :feature:main (commonMain)
enum class Tab {
    Gallery,
    Map,
    TagAlong // Shown in NavigationBar and NavigationRail when tagAlongContent != null
}

@Composable
fun MainNavigation(
    initialSpotId: Long? = null,
    initialTab: Tab = Tab.Gallery,
    onNavigateToSpotHandled: () -> Unit = {},
    onTriggerFiles: (onPhotoPicked: (String) -> Unit) -> Unit,
    versionName: String,
    showToast: (String) -> Unit,
    onGoogleSignInClick: (() -> Unit)? = null,
    tagAlongContent: (@Composable (
        onOpenSpotDetail: (Long) -> Unit,
        onViewOnMainMap: (Double, Double) -> Unit
    ) -> Unit)? = null
)
```

- **On `:androidApp` and `:desktopApp`:** They pass `tagAlongContent = { onOpenSpotDetail, onViewOnMainMap -> TagAlongScreen(...) }`. Both the portrait `NavigationBar` and landscape/Desktop `NavigationRail` render the **TagAlong** tab (`Icons.Default.AutoAwesome`) right beside `Gallery` and `Map`.
- **On `iosApp` and `:webApp`:** `tagAlongContent` defaults to `null`, so only `Gallery` and `Map` are rendered and `iOS`/`wasmJs` builds require zero changes.

---

### 2.4 Desktop vs. Mobile Map Layout & Android App Shortcut

1. **Desktop / Wide Viewports (`maxWidth >= 760.dp`):**
   - `TagAlongScreen` renders its adaptive **2-pane split layout**:
     - **Left Pane (56%):** Conversational stream, inline A2UI surfaces, and multimodal input bar.
     - **Right Pane (44%):** Live synchronized `SpotMapView` showing active tour waypoints and spot markers.
2. **Mobile / Compact Viewports (`maxWidth < 760.dp`):**
   - Removes the redundant inner `"Live Map"` `TabRow` that existed in Phase 3.
   - Renders a clean, full-height single-column Chat + inline A2UI stream (including inline `MapPinOverlayWidget` mini-map cards).
   - When a user taps *"View on Map"* on a card on mobile, `onViewOnMainMap(lat, lon)` switches `MainContainer`'s `selectedTab` to `Tab.Map` centered on those coordinates.
3. **Android Static App Shortcut (`res/xml/shortcuts.xml`):**
   - Declares a static shortcut for **"TagAlong AI Guide"** targeting `tagspotter://tagalong` (`initialTab = Tab.TagAlong`). Users can long-press the Tag Spotter icon or pin TagAlong as a standalone shortcut icon on their home screen.

---

## 3. Side-by-Side A2UI Library Architecture (`androidx.a2ui` vs `a2ui-4k`)

### 3.1 Eliminating the Hand-Rolled Parser & Router

The following hand-rolled files in `:core:a2ui` will be **deleted**:
- ❌ `core/a2ui/src/commonMain/.../parser/A2UIPayloadParser.kt`
- ❌ `core/a2ui/src/commonMain/.../model/A2UIComponents.kt`
- ❌ `core/a2ui/src/commonMain/.../catalog/NeonA2UICatalog.kt`
- ❌ Hardcoded JSON schema strings in `TagAlongPrompts.kt`

---

### 3.2 Head-to-Head Library Comparison Matrix (Blog Post Reference)

| Dimension | Official Jetpack `androidx.a2ui` (`1.0.0-alpha02`) | KMP `Contextable/a2ui-4k` (`0.10.0`) |
| :--- | :--- | :--- |
| **Maven Artifacts** | `androidx.a2ui:a2ui-model`<br>`androidx.a2ui:a2ui-engine`<br>`androidx.a2ui.compose:compose-runtime`<br>`androidx.a2ui.compose:compose-ui`<br>`androidx.compose.material3:material3-a2ui` | `com.contextable:a2ui-4k` |
| **Supported Targets** | **Android Only** (`.aar` packages) | **Kotlin Multiplatform** (`android`, `jvm`, `ios*`, `wasmJs`) |
| **Protocol Version** | A2UI v0.9 (`createSurface`, `updateComponents`, `updateDataModel`, `deleteSurface`) | A2UI v0.9 (with transparent v0.8 transcoding) |
| **Built-In Basic Catalog** | `materialA2uiBasicCatalogV1(...)` (`Text`, `Image`, `Icon`, `Row`, `Column`, `List`, `Card`, `Tabs`, `Modal`, `Divider`, `Button`, `TextField`, `CheckBox`, `ChoicePicker`, `Slider`, `DateTimeInput`) | `CoreCatalog` (`Text`, `Image`, `Row`, `Column`, `Card`, `Button`, `TextField`, `CheckBox`, `ChoicePicker`, etc.) |
| **Custom Component API** | Implement `A2uiComponent` (`name`, `description`, `properties: List<A2uiProperty<*>>`, `@Composable fun A2uiComponentScope.Content(...)`) | Register custom widget renderer in a `Catalog` extending `CoreCatalog` |
| **Catalog Schema Generation** | **`catalog.toJsonSchemaString()`** via `A2uiCoreCatalogSerializer` (JSON Schema Draft 2020-12) | Catalog schema serializer / `A2UiRenderTool.parameters` |
| **Render Loop Engine** | **`A2uiMessageProcessor(catalogs)`** running `suspend fun collectMessages()` | **`SurfaceStateManager()`** processing inbound `JsonElement` messages via `processMessage(...)` |
| **Surface Host Composable** | **`A2uiSurface(surfaceModel, modifier, loadingContent, errorContent, transitionSpec)`** | **`A2UISurface(manager, surfaceId, catalog, onEvent)`** |
| **Reactive Data Model** | `A2uiDataModel` backed by Compose `SnapshotStateMap` & `SnapshotStateSparseList` | Path-based JSON Pointer reactive state store inside `SurfaceStateManager` |
| **1-Way & 2-Way Data Binding** | `properties.bind(DynamicA2uiProperty)` (read) and **`properties.bindUpdater(DynamicA2uiProperty)`** (write lambda `(T?) -> Unit`) | JSON Pointer `{ "path": "/..." }` resolution + `DataChangeEvent` |
| **Action Routing (`functionCall` vs `event`)** | • **`functionCall`**: Executed locally via `A2uiCatalog.functions`<br>• **`event`**: Emits `A2uiClientEventMessage` (with `clientDataModel`) to `processor.outboundEvents` | `onEvent` callback receives interaction events; `event.toClientMessage(version = "v0.9")` formats wire message |

---

### 3.3 Topic 1: Hybrid Catalog (Basic Primitives + 4 Custom Domain Cards) & `toJsonSchemaString()`

To showcase both **built-in A2UI primitives** (recursive child composition + two-way form bindings) and **custom domain components** in the blog post, our catalog uses a **Hybrid Catalog Strategy**:

1. **Built-In Basic Catalog Primitives (`materialA2uiBasicCatalogV1` / `CoreCatalog`):**
   - `Card`, `Column`, `Row`, `Text`, `Image`, `Divider`, `Button`, `CheckBox`, `ChoicePicker`, `TextField`.
   - Used by the agent to compose:
     - **Interactive Style Quizzes** (`Card` -> `Column` -> question `Text`, `ChoicePicker` bound to `/quiz/selectedAnswer`, and `Button` dispatching `{"event": {"name": "submit_quiz"}}`).
     - **Community Voting Cards** (`Card` -> `Column` -> option `Button`s / `ChoicePicker` bound to `/vote/selection`).
     - **Missing-Metadata Spot Forms** (`Card` -> `Column` -> `TextField`s bound to `/draft/description`, `/draft/artist`, and a Save `Button`).
2. **4 Custom Domain Components (Backed by Shared `:core:ui` Stateless Composables):**
   - **`SpotCard`**: Renders `NeonSpotCard` from `:core:ui` with two-way star binding (`isStarred`: `{"path": "/spots/{id}/isStarred"}`).
   - **`TourCard`**: Renders `NeonTourCard` from `:core:ui` with interactive waypoint list and route summary.
   - **`ArtistBioCard`**: Renders `NeonArtistBioCard` from `:core:ui` with style chips and notable works.
   - **`MapPinOverlay`**: Renders `NeonMapPinOverlayWidget` from `:core:ui` with embedded mini-map pins.
3. **Custom Client-Side `A2uiFunction`s Registered in the Catalog:**
   - **`focusMap(latitude, longitude, title)`**: Local `functionCall` that pans the Desktop `MapPane` or switches Mobile to `Tab.Map` without round-tripping to the server.
   - **`openSpotDetail(spotId)`**: Local `functionCall` that navigates directly to `DetailScreen(spotId)`.

```kotlin
// core/a2ui/src/androidMain/kotlin/net/maiatoday/tagspotter/core/a2ui/jetpack/JetpackNeonCatalog.kt
fun createJetpackHybridCatalog(
    context: Context,
    onFocusMap: (Double, Double, String) -> Unit,
    onOpenSpotDetail: (String) -> Unit
): A2uiCatalog {
    val basicM3Catalog = materialA2uiBasicCatalogV1(
        image = MaterialA2uiBasicCatalogV1Defaults.image { url, contentDesc, _, modifier, _ ->
            AsyncImage(model = url, contentDescription = contentDesc, modifier = modifier)
        },
        video = MaterialA2uiBasicCatalogV1Defaults.video { _, _, _ -> },
        audioPlayer = MaterialA2uiBasicCatalogV1Defaults.audioPlayer { _, _, _, _ -> },
        functions = A2uiBasicCatalogV1.Functions(context)
    )

    val customComponents = listOf(
        JetpackSpotCardComponent,
        JetpackTourCardComponent,
        JetpackArtistBioCardComponent,
        JetpackMapPinOverlayComponent
    )

    val customFunctions = listOf(
        FocusMapA2uiFunction(onFocusMap),
        OpenSpotDetailA2uiFunction(onOpenSpotDetail)
    )

    return A2uiCatalog(
        catalogId = "https://tagspotter.maiatoday.net/a2ui/neon_hybrid_catalog/v1/catalog.json",
        components = basicM3Catalog.components.toList() + customComponents,
        functions = basicM3Catalog.functions.toList() + customFunctions,
        isInline = true
    )
}
```

Calling **`hybridCatalog.toJsonSchemaString()`** automatically serializes all Basic Catalog primitives, all 4 custom Neon components, and all client functions into a single Draft 2020-12 JSON Schema string for the agent's system prompt!

---

### 3.4 Topic 2: Inline Surface Render Loop, `updateDataModel` & Native `functionCall` vs `event` Routing

#### A. Inline Surface Rendering by `surfaceId`
- Each `TagAlongMessage` in the chat stream stores `surfaceIds: List<String>` (extracted from any `createSurface` envelopes in that turn).
- Inside `ChatMessageItem`, for each `surfaceId`, we call `engine.RenderSurface(surfaceId = surfaceId)`.
- If a subsequent turn or user interaction sends `updateDataModel(surfaceId = "quiz-1", path = "/quiz/feedback", value = "Correct! Notice the multi-layer bridges.")`, `A2uiMessageProcessor` / `SurfaceStateManager` mutates `"quiz-1"`'s reactive data model and the existing card in the chat stream updates in place.

#### B. Native `functionCall` vs `event` Split (`A2uiActionSchema`)
In `androidx.a2ui` (`A2uiActionHandler.kt`), every component `Action` is a `oneOf`:
1. **Local Client Function (`{"functionCall": {"call": "focusMap", "args": {"lat": 52.49, "lon": 13.24, "title": "Teufelsberg"}}}`):**
   - Executed immediately on-device by `A2uiActionHandler` via `executionContext.executeFunction(...)`.
2. **Server/Agent Event (`{"event": {"name": "submit_quiz", "context": {"answer": {"path": "/quiz/selectedAnswer"}}}}`):**
   - `A2uiActionHandler` resolves the dynamic `{ "path": "/quiz/selectedAnswer" }` against the surface's `A2uiDataModel`, attaches `clientDataModel` (when `shouldSendDataModel = true`), and emits an `A2uiClientEventMessage` onto `processor.outboundEvents`.
   - `TagAlongViewModel` collects `engine.outboundEvents`:
     - For `toggle_star` and `save_spot`, it updates `SpotRepository` and confirms via `engine.updateDataModel(...)`.
     - For conversational events (`submit_quiz`, `cast_vote`, etc.) and validation errors (`A2uiClientErrorMessage`), it forwards the event JSON back to the Google ADK `Runner` so the agent responds or sends a delta `updateDataModel` message.

```mermaid
sequenceDiagram
    autonumber
    participant Agent as Google ADK Agent
    participant VM as TagAlongViewModel
    participant Engine as A2UIRendererEngine<br>(androidx.a2ui / a2ui-4k)
    participant DataModel as Reactive DataModel<br>(A2uiDataModel / SurfaceStateManager)
    participant UI as Inline Chat Surface<br>(Basic Primitives + :core:ui Cards)

    Agent->>VM: Streams prose + ```a2ui [createSurface, updateDataModel, updateComponents] ```
    VM->>Engine: processProtocolMessage(envelopeJson)
    Engine->>DataModel: Hydrate "/spot" or "/quiz" state tree
    DataModel-->>UI: RenderSurface(surfaceId) recomposes inline in chat bubble

    alt Local Client Action ("functionCall": "focusMap" / "openSpotDetail")
        UI->>Engine: User taps "View on Map"
        Engine->>VM: Executes registered A2uiFunction locally
        VM-->>UI: Pans Desktop MapPane OR switches Mobile to Tab.Map
    else Two-Way Binding + Server Event ("event": "submit_quiz" / "toggle_star")
        UI->>DataModel: User selects option -> bindUpdater("/quiz/selected")("Stencil")
        UI->>Engine: User taps Submit -> dispatchAction({"event": {"name": "submit_quiz", ...}})
        Engine-->>VM: Emits A2uiClientEventMessage on outboundEvents (with resolved context & DataModel)
        VM->>Agent: Sends client event to ADK Runner
        Agent->>VM: Returns {"updateDataModel": {"surfaceId": "quiz-1", "path": "/quiz/result", "value": "🎯 Spot on!"}}
        VM->>Engine: processProtocolMessage(updateDataModel)
        DataModel-->>UI: Existing "quiz-1" card updates in place!
    end
```

---

### 3.5 Unified `A2UIRendererEngine` Contract & 2-State Toggle

```kotlin
package net.maiatoday.tagspotter.core.a2ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.Flow

interface A2UIRendererEngine {
    val engineType: A2UIEngineType

    /** Returns the JSON Schema (Draft 2020-12) describing all registered catalog components & functions. */
    fun getCatalogSchemaJson(): String

    /** Starts the engine's message collection loop (suspends while active; required by androidx.a2ui). */
    suspend fun runRenderLoop()

    /** Clears all active surfaces (called when starting a New Conversation or switching sessions). */
    fun clearSurfaces()

    /** Feeds an inbound A2UI v0.9 JSON message or array of messages, returning any created/updated surfaceIds. */
    fun processProtocolMessage(json: String): List<String>

    /** Mutates a JSON Pointer path in the surface's reactive data model directly. */
    fun updateDataModel(surfaceId: String, path: String, value: Any?)

    /** Outbound stream of client events and validation errors emitted by the engine. */
    val outboundEvents: Flow<A2UIClientEvent>

    /** Renders the active A2UI surface for [surfaceId] inline in the chat list. */
    @Composable
    fun RenderSurface(
        surfaceId: String,
        modifier: Modifier = Modifier
    )
}

enum class A2UIEngineType {
    A2UI_KMP_4K,
    ANDROIDX_JETPACK_OFFICIAL
}
```

- **2-State Engine Toggle on Android:** Tapping the engine badge in the TagAlong header switches between `ANDROIDX_JETPACK_OFFICIAL` and `A2UI_KMP_4K`, calling `clearSurfaces()` and replaying the current session's stored `a2uiPayloadJson` messages into the active engine.

---

## 4. UI Component Reuse & Design System Unification (`:core:ui`)

### 4.1 Shared Stateless Compose Widgets Across Engines and Modes

All visual domain components are written once in `:core:ui` as pure, stateless `@Composable` functions using our Neon design system tokens:

```mermaid
flowchart TD
    subgraph CoreUI[":core:ui (Single Source of Truth for Compose Widgets)"]
        SpotCard["NeonSpotCard"]
        TourCard["NeonTourCard"]
        ArtistCard["NeonArtistBioCard"]
        MiniMap["NeonMapPinOverlayWidget"]
        ZoomOverlay["ZoomableImageOverlay"]
        Chips["NeonTagChip & NeonCategoryBadge"]
    end

    subgraph CoreA2UI[":core:a2ui (Hybrid Catalogs: Basic Primitives + 4 Custom Wrappers)"]
        JetpackCat["JetpackHybridCatalog (androidMain)<br><i>materialA2uiBasicCatalogV1 + 4 A2uiComponent wrappers</i>"]
        KmpCat["KmpHybridCatalog (commonMain)<br><i>CoreCatalog + 4 a2ui-4k Catalog wrappers</i>"]
    end

    subgraph AppModes["Unified App Modes"]
        TagAlongMode["TagAlong Tab<br>(:feature:tagalong)"]
        TagSpotterMode["Gallery & Detail Screens<br>(:feature:detail, :feature:gallery)"]
    end

    JetpackCat --> CoreUI
    KmpCat --> CoreUI
    TagAlongMode --> CoreA2UI
    TagAlongMode --> CoreUI
    TagSpotterMode --> CoreUI
```

### 4.2 Component Inventory & Sharing Matrix

| UI Component | Location | How It Is Rendered in TagAlong | Reused in Tag Spotter (`Gallery` / `Detail`)? |
| :--- | :--- | :--- | :---: |
| **`NeonSpotCard`** | `:core:ui` | Custom `SpotCard` component in both A2UI catalogs wrapping `NeonSpotCard` | ✅ Yes |
| **`NeonTourCard`** | `:core:ui` | Custom `TourCard` component in both A2UI catalogs wrapping `NeonTourCard` | ✅ Yes |
| **`NeonArtistBioCard`** | `:core:ui` | Custom `ArtistBioCard` component in both A2UI catalogs wrapping `NeonArtistBioCard` | ✅ Yes (`DetailScreen`) |
| **`NeonMapPinOverlayWidget`** | `:core:ui` | Custom `MapPinOverlay` component in both A2UI catalogs wrapping `NeonMapPinOverlayWidget` | ✅ Yes (unifies `DetailMiniMapCard`) |
| **Style Quiz & Community Voting** | Built-in A2UI Basic Catalog | Composed dynamically by the agent using `Card`, `Column`, `Row`, `Text`, `ChoicePicker`, `CheckBox`, `Button` from `materialA2uiBasicCatalogV1` / `CoreCatalog` | ➖ N/A (Dynamic A2UI) |
| **`ZoomableImageOverlay`** | `:core:ui` | Used for full-screen pinch-to-zoom on chat & card images | ✅ Yes (`DetailScreen`) |
| **`NeonTagChip` & `NeonCategoryBadge`** | `:core:ui` | Used inside `NeonSpotCard`, `NeonArtistBioCard`, and `NeonTourCard` | ✅ Yes (`GalleryScreen`, `DetailMetadataCard`) |

---

## 5. Data Access, Auth & Persistence in the Unified App

### 5.1 Shared Local Storage (`SpotDatabase` & `TagAlongDatabase`)
- **`SpotDatabase` (`SpotDao.kt`):** Single in-process SQLite database (`spot_database`). Every spot captured in Tag Spotter or imported from `.zip` packs is immediately available to TagAlong's `StreetArtTools`.
- **`TagAlongDatabase` (`ChatDao.kt`):** Stores `chat_sessions` and `chat_messages` (including the A2UI v0.9 JSON envelopes per turn).

### 5.2 Desktop JVM Database Path Fix (`JvmSpotDatabase.kt`)
- Update [JvmSpotDatabase.kt](file:///Users/maia/workspace/maiatoday/tag-spotter/core/database/src/jvmMain/kotlin/net/maiatoday/tagspotter/core/database/JvmSpotDatabase.kt#L11) to store `spot_database.db` and `tagalong_database.db` in `File(System.getProperty("user.home"), ".tagspotter/")` (creating the directory if needed) instead of `java.io.tmpdir` (`/tmp`), so data persists across OS reboots.

### 5.3 Shared Firebase Auth, App Check & `HybridSpotRepository`
- `:androidApp` already configures Firebase and App Check in `TagSpotterApplication.kt`. Both `:core:ai` and `:core:agent` share this attestation automatically.
- `HybridSpotRepository` merges local `SpotRepository` records with Firestore `/public_spots/{uuid}`, deduplicating by `uuid` and prioritizing local image paths.

### 5.4 Complete End-to-End Data Flow Diagram

```mermaid
flowchart TD
    subgraph UnifiedApp["Tag Spotter + TagAlong Unified App (:androidApp / :desktopApp)"]
        GalleryVM["GalleryViewModel & DetailViewModel<br><i>Gallery / Map / Detail</i>"]
        ChatVM["TagAlongViewModel<br><i>TagAlong Tab</i>"]
        AgentRunner["Google ADK Runner"]
        Tools["StreetArtTools (@Tool)"]
        Engine["A2UIRendererEngine<br>(androidx.a2ui / a2ui-4k)"]
    end

    subgraph CoreUI[":core:ui"]
        SharedCards["Shared Stateless Neon Composables"]
    end

    subgraph CoreHybrid[":core:sync (Hybrid Data Layer)"]
        HybridRepo["HybridSpotRepository"]
        SyncMgr["SyncManager"]
    end

    subgraph CoreDB[":core:database (In-Process Shared Room SQLite)"]
        LocalRepo["LocalSpotRepository"]
        SpotDB[("SpotDatabase v14<br><i>spots, images, tags</i>")]
        ChatRepo["RoomChatRepository"]
        ChatDB[("TagAlongDatabase<br><i>chat_sessions, chat_messages</i>")]
    end

    ChatVM --> ChatRepo --> ChatDB
    ChatVM --> Engine --> SharedCards
    ChatVM --> AgentRunner --> Tools --> HybridRepo
    GalleryVM --> LocalRepo
    GalleryVM --> SharedCards
    HybridRepo --> LocalRepo --> SpotDB
    HybridRepo --> SyncMgr
```

---

## 6. TagAlong UX Features & Session Architecture

### 6.1 Light and Dark Mode Switching
- Add `ThemeMode { SYSTEM, LIGHT, DARK }` in `:core:settings` and a theme toggle control in the top bar / settings, switching `MyApplicationTheme` between `UrbanDarkColorScheme` and `UrbanLightColorScheme`.

### 6.2 "New Conversation" Lifecycle
- Add a `+` ("New Chat") button in the TagAlong header calling `TagAlongViewModel.startNewConversation()`.
- Calls `engine.clearSurfaces()`, inserts a new `ChatSession` in `TagAlongDatabase`, and posts a fresh welcome message while preserving past sessions in SQLite.

### 6.3 Conversation History Plan & Multi-Session Surface Replay
- Expose `allSessions: StateFlow<List<ChatSession>>` in `TagAlongViewModel`.
- Provide a History Sheet/Drawer in `TagAlongScreen` to switch between past sessions (`switchSession(sessionId)` clears the engine and replays all `a2uiPayloadJson` envelopes from that session's messages into the active `A2UIRendererEngine`) or delete sessions (`deleteSessionById(sessionId)`).

---

## 7. Multimodal Photo Bytes, Category Mapping & 1-Tap Spot Creation

### 7.1 Fixing the Phase 3 Multimodal Photo Bug (Sending Real Image Bytes)
In Phase 3, `TagAlongViewModel.kt` sent `"User attached photo URI: $currentPhoto"` as plain text instead of image bytes.
- **Fix in Phase 4:** When `photoUri` is attached, use `:core:photo` (`PhotoProcessor`) to read/compress the image into a `ByteArray` and pass a real binary image `Part` (`Part.fromBytes(bytes, "image/jpeg")` / `InlineData`) into `runner.runAsync(...)` so Gemini Vision analyzes the actual mural photo.

### 7.2 Category vs. Style Tags Alignment (`Spot.CATEGORIES`)
To ensure 100% compatibility with Tag Spotter's Gallery and Map filters ([Spot.kt](file:///Users/maia/workspace/maiatoday/tag-spotter/core/model/src/commonMain/kotlin/net/maiatoday/tagspotter/core/model/Spot.kt#L29)):
- **`spot.category`** is strictly constrained to `Spot.CATEGORIES`:
  `["graffiti", "sculpture", "nature", "architecture", "public_place", "food"]` (street art murals/tags default to `"graffiti"`, 3D installations to `"sculpture"`).
- **Street-art styles and techniques** (`"mural"`, `"stencil"`, `"sticker"`, `"paste-up"`, `"wildstyle"`, `"throw-up"`) are stored in **`spot.tags: List<String>`**.

### 7.3 Complete 1-Tap Agentic Spot Creation (`@Tool saveNewSpot` + Missing-Field A2UI Form)

When a user attaches a photo in TagAlong and asks to identify or save it:
1. **Automatic Metadata Extraction (`:core:photo` + `:core:location` + `:core:settings`):**
   - `PhotoProcessor.extractMetadataFromUri(photoUri)` extracts EXIF `latitude`, `longitude`, and `timestamp`.
   - If EXIF GPS is absent, `LocationProvider.getCurrentLocation()` provides fallback coordinates.
   - `PhotoProcessor.createThumbnailFromUri(photoUri)` generates the local `thumbnailPath`.
   - `SettingsRepository.photographerName` and `SpotRepository.activeUid` supply `photographer` and `ownerUid`.
2. **Gemini Vision Analysis:**
   - Identifies `description` (title/summary), `artists` (`List<String>`), `category` (from `Spot.CATEGORIES`, e.g. `"graffiti"`), and `tags` (including style tags like `"mural"`, `"stencil"`).
3. **Completeness Check (1-Tap Save vs. A2UI Form Card):**
   - **If all required fields (`imagePath`, `thumbnailPath`, valid `latitude`/`longitude`, `description`, `category`) are present:**
     - The agent renders a preview `SpotCard` (or calls `@Tool saveNewSpot`) allowing **1-tap direct save** into `SpotRepository.saveSpot(...)`, updating `/spot/isSaved` to `true` in the surface data model.
   - **If required fields are missing (e.g., no EXIF/device GPS or ambiguous details):**
     - The agent renders an interactive **A2UI Form Card** using Basic Catalog `TextField` and `ChoicePicker` primitives bound to `/draftSpot/*` paths so the user can fill in the missing field and tap `[Save Spot]`.

---

## 8. Phase 4 Implementation Roadmap & Next Steps

```mermaid
flowchart TD
    subgraph Phase 4: Single-App Consolidation, Real A2UI Libraries & UX
        S41["Step 4.1: Retire :tagalongApp:* & Wire In-App TagAlong Tab<br>into :feature:main, :androidApp & :desktopApp"]
        S42["Step 4.2: Shared Stateless UI Consolidation in :core:ui"]
        S43["Step 4.3: Hybrid androidx.a2ui & a2ui-4k Engines in :core:a2ui<br>(Delete hand-rolled parser/DTOs)"]
        S44["Step 4.4: Wire Prompts to toJsonSchemaString(), Fix Photo Bytes<br>& Connect Inline Surface Render Loop"]
        S45["Step 4.5: Theme Switching, New Chat Controls & Desktop DB Fix"]
        S46["Step 4.6: 1-Tap Spot Saving, Spot.CATEGORIES & DataModel Actions"]
        S47["Step 4.7: Conversation History Drawer / Sheet with Surface Replay"]
        S48["Step 4.8: Full Monorepo Regression Gate"]
    end

    S41 --> S42 --> S43 --> S44 --> S45 --> S46 --> S47 --> S48
```

### Step 4.1: Retire `:tagalongApp:*` & Integrate TagAlong Tab into `:androidApp` & `:desktopApp`
- **Goal:** Consolidate TagAlong into the main Tag Spotter app on Android and Desktop so both modes share one `SpotDatabase`, one photo storage directory, and one Firebase App Check configuration.
- **Work Items:**
  1. Add `Tab.TagAlong` and an optional `tagAlongContent` composable slot to `MainContainer` and `MainNavigation` in `:feature:main` (`commonMain`), keeping `iOS` and `wasmJs` untouched.
  2. Wire `:feature:tagalong`, `:core:a2ui`, and `:core:agent` dependencies and Koin modules into `:androidApp` and `:desktopApp`, passing `TagAlongScreen` into `MainNavigation`'s `tagAlongContent` slot.
  3. Remove the redundant inner `"Live Map"` `TabRow` on mobile viewports (`< 760.dp`) in `TagAlongScreen.kt`, routing mobile *"View on Map"* actions to `MainContainer`'s `Tab.Map`.
  4. Add a static Android App Shortcut (`res/xml/shortcuts.xml` for `tagspotter://tagalong`) in `:androidApp`.
  5. Remove `:tagalongApp:android` and `:tagalongApp:desktop` from `settings.gradle.kts` and delete their module directories.
- **Verification:**
  - `./gradlew :feature:main:allTests :androidApp:assembleDebug :desktopApp:assemble`

### Step 4.2: Shared UI Primitives & Stateless Card Consolidation (`:core:ui`)
- **Goal:** Move all reusable Compose UI components into `:core:ui` as stateless, library-agnostic composables shared between Tag Spotter mode, TagAlong mode, `androidx.a2ui`, and `a2ui-4k`.
- **Work Items:**
  1. Move `ZoomableImageOverlay.kt` from `:feature:detail` to `:core:ui/src/commonMain/.../image/`.
  2. Create `NeonTagChip` and `NeonCategoryBadge` in `:core:ui/src/commonMain/.../chips/` using `CategoryExtensions.kt`.
  3. Move the 4 custom domain composables (`NeonSpotCard`, `NeonTourCard`, `NeonArtistBioCard`, `NeonMapPinOverlayWidget`) from `:core:a2ui` into `:core:ui/src/commonMain/.../components/`, refactored to take pure primitive parameters and event lambdas.
  4. Refactor `DetailMetadataCard.kt` and `GalleryScreen.kt` to use `NeonTagChip` and `NeonCategoryBadge`.
- **Verification:**
  - `./gradlew :core:ui:allTests :feature:detail:allTests :feature:gallery:allTests`

### Step 4.3: Replace Hand-Rolled A2UI with Hybrid `androidx.a2ui` & `a2ui-4k` Engines (`:core:a2ui`)
- **Goal:** Delete all hand-rolled A2UI parsing/routing code and implement both real A2UI libraries side-by-side using the Hybrid Catalog (Built-in Basic Catalog primitives + 4 custom `:core:ui` domain components).
- **Work Items:**
  1. Add `a2ui-4k = { module = "com.contextable:a2ui-4k", version = "0.10.0" }` to `gradle/libs.versions.toml` and `commonMain` of `:core:a2ui/build.gradle.kts` (alongside `androidx.a2ui.compose.runtime` in `androidMain`).
  2. **Delete** `A2UIPayloadParser.kt`, `A2UIComponents.kt`, and `NeonA2UICatalog.kt`.
  3. **Implement `JetpackHybridCatalog` & `JetpackA2UIEngine` (`androidMain`):**
     - Combine `materialA2uiBasicCatalogV1` with custom `A2uiComponent` wrappers (`SpotCard`, `TourCard`, `ArtistBioCard`, `MapPinOverlay`) and local `A2uiFunction`s (`focusMap`, `openSpotDetail`).
     - Wire `A2uiJsonMessageParser`, `A2uiMessageProcessor`, `collectMessages()`, `outboundEvents`, `A2uiUpdateDataModelMessage`, `A2uiSurface`, and `catalog.toJsonSchemaString()`.
  4. **Implement `KmpHybridCatalog` & `KmpA2UIEngine` (`commonMain`):**
     - Combine `CoreCatalog` from `com.contextable.a2ui4k` with custom widget renderers for the same 4 domain components and client functions.
     - Wire `SurfaceStateManager`, `processMessage(...)`, `updateDataModel`, and `A2UISurface`.
  5. Update mock fixtures in `:feature:tagalong` to valid A2UI v0.9 protocol envelopes (`createSurface`, `updateDataModel`, `updateComponents`), showcasing both custom cards (`SpotCard`, `TourCard`) and Basic Catalog compositions (Style Quiz & Community Voting built from `Card`, `Column`, `Text`, `ChoicePicker`, `Button`).
- **Verification:**
  - `./gradlew :core:a2ui:allTests`

### Step 4.4: Wire Agent Prompts to `toJsonSchemaString()`, Fix Photo Bytes & Connect Render Loop
- **Goal:** Feed the library-generated catalog JSON Schema directly into the Google ADK agent prompt, send real image bytes for attached photos, and render A2UI surfaces inline by `surfaceId`.
- **Work Items:**
  1. Replace hardcoded schemas in `TagAlongPrompts.kt` with `TagAlongPrompts.buildSystemInstruction(catalogSchemaJson = engine.getCatalogSchemaJson(), context = ...)`.
  2. In `TagAlongViewModel`, launch `engine.runRenderLoop()` and `engine.outboundEvents.collect { ... }`.
  3. When a photo is attached, use `PhotoProcessor` to load compressed image bytes and pass a binary image `Part` to `runner.runAsync(...)`.
  4. Support the 2-state engine toggle (`ANDROIDX_JETPACK_OFFICIAL` ↔ `A2UI_KMP_4K`) in the Android TagAlong header by replaying session envelopes into the newly selected engine.
- **Verification:**
  - `./gradlew :core:agent:allTests :feature:tagalong:allTests`

### Step 4.5: Theme Switching, New Conversation Controls & Desktop DB Persistence Fix
- **Goal:** Provide user control over Light/Dark modes, starting fresh conversation sessions, and persistent Desktop SQLite storage.
- **Work Items:**
  1. Update `JvmSpotDatabase.kt` to store `spot_database.db` and `tagalong_database.db` in `File(System.getProperty("user.home"), ".tagspotter/")` instead of `java.io.tmpdir`.
  2. Introduce `ThemeMode { SYSTEM, LIGHT, DARK }` in `:core:settings` and wire a theme toggle in the app bar / settings.
  3. Add `+` ("New Chat") button in `TagAlongScreen` top bar calling `TagAlongViewModel.startNewConversation()`.
- **Verification:**
  - `./gradlew :core:database:allTests :feature:tagalong:allTests`

### Step 4.6: 1-Tap Agentic Spot Saving, `Spot.CATEGORIES` Mapping & Two-Way DataModel Actions
- **Goal:** Allow users to save spots discovered in chat directly into the shared `SpotDatabase` when all required fields are gathered, using `Spot.CATEGORIES` for `category` and `spot.tags` for street-art styles.
- **Work Items:**
  1. Add `@Tool saveNewSpot` to `StreetArtTools.kt` validating `category in Spot.CATEGORIES` (mapping style subtypes like `"mural"` or `"stencil"` into `tags`).
  2. Wire `PhotoProcessor.extractMetadataFromUri` and `createThumbnailFromUri` into the photo-attachment flow so EXIF GPS, timestamp, and thumbnail path are ready for 1-tap saving.
  3. Handle outbound `save_spot` and `toggle_star` events from `engine.outboundEvents` in `TagAlongViewModel`, persisting to `SpotRepository` and calling `engine.updateDataModel(surfaceId, "/spot/isStarred", newValue)`.
  4. Forward conversational `A2uiClientEventMessage`s (`submit_quiz`, `cast_vote`, form submissions) back to the ADK `Runner` so the agent updates the surface via `updateDataModel`.
- **Verification:**
  - Verify saving or starring a spot in TagAlong immediately reflects in the `Gallery` and `Map` tabs.

### Step 4.7: Conversation History Drawer / Sheet Implementation
- **Goal:** Enable users to browse, switch between, and delete past chat sessions saved in `TagAlongDatabase`.
- **Work Items:**
  1. Expose `allSessions: StateFlow<List<ChatSession>>` in `TagAlongViewModel`.
  2. Add History Drawer / `ModalBottomSheet` in `TagAlongScreen`.
  3. Replay stored A2UI v0.9 messages into the active `A2UIRendererEngine` when switching sessions.
- **Verification:**
  - ViewModel unit tests for session switching, surface replay, and cascading session deletion.

### Step 4.8: Full Monorepo Regression Gate
- **Goal:** Pass all tests and assemble all targets across the unified monorepo.
- **Command:**
  ```bash
  ./gradlew allTests :androidApp:assembleDebug :desktopApp:assemble
  ```

---

## 9. Future Phases Horizon (Phase 5+)

| Phase | Title | Key Deliverables |
| :--- | :--- | :--- |
| **Phase 5** | **Offline Tour Caching & Geofenced Alerts** | Offline bundle download for walking routes, background GPS proximity triggers (25m alert when nearing a mural). |
| **Phase 6** | **Audio Tour Guide (TTS Narration)** | Real-time spoken tour narration using platform speech synthesis as user reaches tour waypoints. |
| **Phase 7** | **Curated Community Tour Packs** | Cloud pack publishing (`/shared_packs/{code}`) directly from agent-generated walking tours with QR code sharing. |
| **Phase 8** | **iOS & Web Targets for TagAlong Mode** | Enable the `tagAlongContent` slot on `iosApp` and `:webApp` using `a2ui-4k` once `google/adk-kotlin` publishes Kotlin/Native and Wasm artifacts. |

---

## 10. Comprehensive Test Plan

### Tier 1: Library Catalog Schema, Protocol & Data Model Unit Tests (`:core:a2ui`, `:core:agent`)
- **Scope:**
  - **Jetpack `androidx.a2ui` Tests (`androidHostTest`):**
    - Verify `createJetpackHybridCatalog(...).toJsonSchemaString()` generates valid Draft 2020-12 JSON Schema containing both the Basic Catalog primitives (`Card`, `Column`, `ChoicePicker`, `Button`, etc.), the 4 custom domain components (`SpotCard`, `TourCard`, `ArtistBioCard`, `MapPinOverlay`), and custom `A2uiFunction`s (`focusMap`, `openSpotDetail`).
    - Verify `A2uiMessageProcessor` processes `createSurface`, `updateDataModel`, and `updateComponents` messages and populates `activeSurfaces`.
    - Verify `A2uiUpdateDataModelMessage` updates JSON Pointer paths (e.g. `/spot/isStarred`, `/quiz/selectedAnswer`) and `event` actions emit `A2uiClientEventMessage` on `outboundEvents`.
  - **KMP `a2ui-4k` Tests (`commonTest`):**
    - Verify `SurfaceStateManager` processes identical `createSurface`, `updateDataModel`, and `updateComponents` envelopes for both Basic Catalog primitives and custom domain cards.
    - Verify JSON Pointer data model updates mutate bound component properties.
- **Command:**
  ```bash
  ./gradlew :core:a2ui:allTests :core:agent:allTests
  ```

### Tier 2: Shared Database, Category Mapping & Hybrid Repository Tests (`:core:database`, `:core:sync`)
- **Scope:**
  - Verify spots saved via `StreetArtTools.saveNewSpot` map `category` to `Spot.CATEGORIES` (`"graffiti"`, `"sculpture"`, etc.) and style subtypes (`"mural"`, `"stencil"`) to `spot.tags`, and are immediately returned by `SpotRepository.getAllSpots()`.
  - Verify `TagAlongDatabase` session creation, message persistence, and cascading deletes.
- **Command:**
  ```bash
  ./gradlew :core:database:allTests :core:sync:allTests
  ```

### Tier 3: Side-by-Side Dual-Engine A/B Verification (`:core:a2ui`, `:androidApp`)
- **Scope:**
  - Feed identical A2UI v0.9 JSON envelopes (`createSurface` + `updateDataModel` + `updateComponents`) into both `JetpackA2UIEngine` and `KmpA2UIEngine`.
  - Toggle the 2-state runtime engine switch in the TagAlong tab header on Android and verify that both engines render the exact same `:core:ui` Neon composables and Basic Catalog quizzes/voting cards, and react to `updateDataModel` mutations identically.

### Tier 4: In-App Mode Switching & UX Controls (`:feature:main`, `:feature:tagalong`)
- **Scope:**
  - Switch between `Gallery`, `Map`, and `TagAlong` tabs; verify state preservation.
  - Trigger `focusMap` on mobile -> verify switch to `Tab.Map` centered on target coordinates.
  - Trigger `openSpotDetail` -> verify navigation to `DetailScreen`.
  - Toggle Light/Dark mode, start a new conversation (`+`), and switch sessions in the History sheet.
- **Command:**
  ```bash
  ./gradlew :feature:main:allTests :feature:tagalong:allTests
  ```

### Tier 5: Live Agent, Multimodal Photo Bytes & App Check Smoke Tests (`:desktopApp`, `:androidApp`)
- Run Desktop (`./gradlew :desktopApp:run`) and Android (`./gradlew :androidApp:installDebug`), switch to the **TagAlong** tab, attach a mural photo, and verify live streaming A2UI v0.9 surface rendering, real vision analysis, and 1-tap spot saving.

### Tier 6: Monorepo Regression Gate
- **Command:**
  ```bash
  ./gradlew allTests :androidApp:assembleDebug :desktopApp:assemble
  ```

---

## 11. Clean Context Handoff & Execution Prompt

```markdown
Resume the Tag Spotter / TagAlong monorepo project at Phase 4.
Refer to docs/a2ui-phase-4-and-more.md.

Key Architectural Directives (Locked In via Design Review):
1. Single-App Consolidation: Retire `:tagalongApp:android` and `:tagalongApp:desktop`. Embed `:feature:tagalong` into `:androidApp` and `:desktopApp` via `Tab.TagAlong` and an optional `tagAlongContent` composable slot in `:feature:main` (keeping iOS and Wasm untouched). Remove the redundant mobile "Live Map" sub-tab in TagAlongScreen (route mobile map views to `Tab.Map`). Fix `JvmSpotDatabase.kt` to use `~/.tagspotter/`.
2. NO hand-rolled A2UI parser or custom component router: delete `A2UIPayloadParser.kt`, `A2UIComponents.kt`, and `NeonA2UICatalog.kt`.
3. Move the 4 custom domain composables (`NeonSpotCard`, `NeonTourCard`, `NeonArtistBioCard`, `NeonMapPinOverlayWidget`) + `ZoomableImageOverlay` + `NeonTagChip`/`NeonCategoryBadge` into `:core:ui` as shared, stateless Compose Multiplatform widgets.
4. Implement Hybrid Catalogs in both real A2UI engines side-by-side in `:core:a2ui`:
   - `androidMain`: Official Jetpack `androidx.a2ui` (`materialA2uiBasicCatalogV1` + 4 custom `A2uiComponent`s + local `A2uiFunction`s `focusMap` & `openSpotDetail` + `catalog.toJsonSchemaString()` + `A2uiMessageProcessor` + `A2uiSurface`).
   - `commonMain`: KMP `com.contextable:a2ui-4k:0.10.0` (`CoreCatalog` + 4 custom domain renderers + `SurfaceStateManager` + `A2UISurface`).
   - Interactive Style Quizzes, Community Voting, and Missing-Field Spot Forms use the built-in Basic Catalog primitives (`Card`, `Column`, `Text`, `ChoicePicker`, `TextField`, `Button`).
5. Render A2UI surfaces inline in the chat stream by `surfaceId` with a 2-state engine toggle (`ANDROIDX_JETPACK_OFFICIAL` ↔ `A2UI_KMP_4K`).
6. Pass real compressed photo bytes via `:core:photo` to the ADK Runner, keep `spot.category` within `Spot.CATEGORIES` (putting styles like `"mural"`/`"stencil"` in `spot.tags`), and support 1-tap spot saving when all required fields are present.
```
