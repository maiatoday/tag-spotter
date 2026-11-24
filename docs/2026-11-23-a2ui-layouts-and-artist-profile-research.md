# Research Report: A2UI Layouts Comparison (#80) & Artist Profile Architecture (#84)

**Date:** 2026-11-23  
**Scope:** GitHub Issues `#80` (*A2UI layouts comparison*) and `#84` (*How does the artist profile work*)  
**Repository:** `tag-spotter` (read-only research; no source files modified)

---

## Executive Summary

1. **Issue #80 (Why switching A2UI libraries changes layouts and buttons):**  
   The 4 custom domain cards (`SpotCard`, `TourCard`, `ArtistBioCard`, `MapPinOverlay`) wrap the exact same `:core:ui` composables (`NeonSpotCard`, `NeonTourCard`, `NeonArtistBioCard`, `NeonMapPinOverlayWidget`) in both engines, so they look nearly identical. The dramatic visual and behavioral differences occur in **Basic Catalog surfaces** (e.g., *Style Quiz*, *Community Vote*, and agent-generated forms) because **`androidx.compose.material3:material3-a2ui:1.0.0-alpha02`** (`ANDROIDX_JETPACK_OFFICIAL`) and **`com.contextable:a2ui-4k:0.10.0`** (`A2UI_KMP_4K`) implement the A2UI v0.9 Basic Catalog with different Compose widgets, default variants, and spacing:
   - **`Button`**: When `variant` is omitted (or `"default"`), Jetpack renders an **`OutlinedButton`** (`Variant.Secondary`), whereas `a2ui-4k` renders a **filled `Button`** (and has no `OutlinedButton` variant at all).
   - **`ChoicePicker`**: Jetpack defaults to **single-select** (`MutuallyExclusive`) rendered as a collapsed **`ExposedDropdownMenuBox` dropdown** (or `FilterChip`s for `displayStyle: "chips"`), whereas `a2ui-4k` defaults to **multi-select** (`multipleSelection = true`) rendered as an **expanded vertical list of `Checkbox`es** (ignoring `displayStyle` and `filterable`).
   - **`Card`, `Column`, `Row`, `Text`**: Jetpack uses outlined cards with `16.dp` padding, `8.dp` default `Column`/`Row` spacing, and heading padding (no Markdown yet); `a2ui-4k` uses borderless `surface` cards with `8.dp` padding, `0.dp` `Column`/`Row` spacing, and `0.dp` heading padding (with basic Markdown).

2. **Issue #84 (Do we need dedicated artist profiles, and how do they connect to A2UI?):**  
   TagSpotter currently has **no `Artist` model, Room table, or Firestore collection** — artists exist only as `Spot.artists: List<String>`. The TagAlong agent's `StreetArtTools.getArtistBio(artistName)` dynamically scans all local + public spots and returns a thin `ArtistBioDto`, while the A2UI `ArtistBioCard` (`NeonArtistBioCard`) expects richer fields (`avatarUrl`, `activeYears`, `artworks` with `id` + `imageUrl`).  
   **Recommendation:** You **do not** need a full Firestore `artists` collection right now. **Start with Option A (Dynamic Spot Aggregation + Rich `ArtistBioDto`)** to eliminate hallucinated artwork IDs/images and wire real spots directly into `ArtistBioCard`. Add **Option B (Local `artist_profiles` Room cache)** later only when you need offline/user-editable bios or a dedicated Artist screen.

---

## Part 1: Issue #80 — A2UI Layouts Comparison (`ANDROIDX_JETPACK_OFFICIAL` vs `A2UI_KMP_4K`)

### 1.1 Where the Dual-Engine Switch Lives

| Layer | File | Symbol / Behavior |
|---|---|---|
| Engine contract | `core/a2ui/src/commonMain/kotlin/net/maiatoday/tagspotter/core/a2ui/A2UIRendererEngine.kt` | `A2UIEngineType` (`A2UI_KMP_4K`, `ANDROIDX_JETPACK_OFFICIAL`), `A2UIRendererEngine` |
| Jetpack engine (Android) | `core/a2ui/src/androidMain/kotlin/net/maiatoday/tagspotter/core/a2ui/engine/JetpackA2UIEngine.kt` | `JetpackA2UIEngine` backed by `androidx.a2ui:*:1.0.0-alpha02` and `androidx.compose.material3:material3-a2ui:1.0.0-alpha02` |
| Jetpack catalog (Android) | `core/a2ui/src/androidMain/kotlin/net/maiatoday/tagspotter/core/a2ui/engine/JetpackHybridCatalog.kt` | `createJetpackHybridCatalog(...)` combining `materialA2uiBasicCatalogV1` + 4 custom `A2uiComponent`s + 2 `A2uiFunction`s (`focusMap`, `openSpotDetail`) |
| KMP engine (Common) | `core/a2ui/src/commonMain/kotlin/net/maiatoday/tagspotter/core/a2ui/engine/KmpA2UIEngine.kt` | `KmpA2UIEngine` backed by `com.contextable:a2ui-4k:0.10.0` |
| KMP catalog (Common) | `core/a2ui/src/commonMain/kotlin/net/maiatoday/tagspotter/core/a2ui/engine/KmpHybridCatalog.kt` | `createKmpHybridCatalog()` combining `CoreCatalogItems.asCatalog()` + 4 custom `CatalogItem`s |
| Platform engine list | `feature/tagalong/src/androidMain/.../A2UIEngineHelper.android.kt` & `jvmMain/.../A2UIEngineHelper.jvm.kt` | Android exposes `[JetpackA2UIEngine, KmpA2UIEngine]` (Jetpack default); Desktop JVM exposes `[KmpA2UIEngine]` only |
| UI toggle & replay | `feature/tagalong/src/commonMain/.../TagAlongScreen.kt` & `TagAlongViewModel.kt` | Header badge pill toggles `currentEngineIndex` → `TagAlongViewModel.switchEngine(nextEngine)` clears active surfaces and replays every message's stored `a2uiPayload` into the new engine |

---

### 1.2 Root-Cause Comparison: Why Layouts & Buttons Differ

Inspecting the actual widget implementations inside `material3-a2ui-1.0.0-alpha02-sources.jar`, `compose-ui-1.0.0-alpha02-sources.jar` (`A2uiBasicCatalogV1.kt`), and `a2ui-4k-0.10.0-sources.jar` explains every visual difference observed when toggling the engine badge:

| Component / Area | `ANDROIDX_JETPACK_OFFICIAL` (`material3-a2ui:1.0.0-alpha02`) | `A2UI_KMP_4K` (`a2ui-4k:0.10.0`) | Visible Effect When Switching |
|---|---|---|---|
| **`Button`** | `MaterialA2uiBasicCatalogV1Button`:<br>• `Primary` (`"primary"`) → filled `Button`<br>• `Secondary` (`"default"`, **also the default when omitted**) → **`OutlinedButton`**<br>• `Borderless` (`"borderless"`) → `TextButton`<br>• Adds `Modifier.padding(vertical = 4.dp)` and renders inline `checks` error text below button | `ButtonWidget`:<br>• `"primary"` → filled `Button`<br>• `"borderless"` → `TextButton`<br>• **`else` (including `"default"` or omitted) → filled `Button`** (no `OutlinedButton` exists)<br>• `0.dp` vertical padding; `checks` only disable the button | **Direct cause of "different buttons":** `QUIZ_SAMPLE_JSON` (`submit_btn`) and `VOTING_SAMPLE_JSON` (`v_btn`) omit `variant`, so Jetpack shows an **outlined button** while `a2ui-4k` shows a **solid filled primary button**. |
| **`ChoicePicker`** | `MaterialA2uiBasicCatalogV1ChoicePicker`:<br>• Default `variant`: **`MutuallyExclusive`** (single-select)<br>• Default `displayStyle`: `Checkbox` → renders a **collapsed `ExposedDropdownMenuBox` + `OutlinedTextField` dropdown**<br>• `displayStyle: "chips"` → `FlowRow` of `FilterChip`s<br>• Supports `filterable` search input | `ChoicePickerWidget`:<br>• Default `variant`: **`multipleSelection = true`** (`variant != "mutuallyExclusive"`)<br>• **Ignores `displayStyle` and `filterable`**<br>• Always renders an **expanded vertical `Column` of `Checkbox` rows** (or `RadioButton` rows if `variant == "mutuallyExclusive"`) | **Biggest layout & UX shift:** *Style Quiz* and *Community Vote* omit `variant` & `displayStyle`, so Jetpack renders a **compact single-select dropdown** while `a2ui-4k` renders a **tall multi-select checkbox list**. |
| **`Card`** | `MaterialA2uiBasicCatalogV1Card`:<br>• `Surface` with `CardDefaults.outlinedCardBorder()` and `CardDefaults.outlinedShape`<br>• **`16.dp` internal padding**<br>• Wraps content width (respects incoming `modifier`) | `CardWidget`:<br>• `Card` with `MaterialTheme.colorScheme.surface` and **no border**<br>• **`8.dp` internal padding**<br>• Forces `Modifier.fillMaxWidth()` | Cards look outlined and spacious in Jetpack vs. borderless, tighter (`8.dp`), and full-width in `a2ui-4k`. |
| **`Column` & `Row`** | `MaterialA2uiBasicCatalogV1Column` / `Row`:<br>• Applies **`Arrangement.spacedBy(8.dp)`** (`ItemSpacing = 8.dp`) for `Start`/`Center`/`End`/`Stretch`<br>• `align: "stretch"` applies `fillMaxWidth()`/`fillMaxHeight()` to children; `justify: "stretch"` applies `weight(1f)` | `ColumnWidget` / `RowWidget`:<br>• Uses `Arrangement.Top` / `Arrangement.Start` with **`0.dp` spacing**<br>• `align: "stretch"` falls back to `Alignment.CenterHorizontally` (does not stretch children) | Children in Jetpack have natural `8.dp` vertical/horizontal breathing room; in `a2ui-4k` they sit flush against each other. |
| **`Text`** | `MaterialA2uiBasicCatalogV1Text`:<br>• Built-in vertical padding on headings (`H1`: 16/8dp, `H2`: 12/6dp, `H3`: 8/4dp, `H4`/`H5`: 4/2dp)<br>• Adds `semantics { heading() }`<br>• `Caption` → `labelMedium`<br>• **No Markdown support yet** (`TODO(b/544548540)`) | `TextWidget`:<br>• **`0.dp` padding** on all variants<br>• `caption` → `bodySmall`<br>• **Parses basic Markdown** via `parseBasicMarkdown(text)` | Headings have tighter spacing in `a2ui-4k`, but Markdown bold/italic renders in `a2ui-4k` and appears as raw syntax in Jetpack. |
| **Custom Neon Cards** (`SpotCard`, `TourCard`, `ArtistBioCard`, `MapPinOverlay`) | Wraps `:core:ui` `Neon*` composables and forwards `modifier = modifier` from `A2uiComponentScope` | Wraps the same `:core:ui` `Neon*` composables, **without** a `modifier` parameter (`a2ui-4k`'s `CatalogItem` lambda has no `Modifier` argument) | Visually identical when rendered as root, but nested layout modifiers (like `weight`) only propagate in Jetpack. |
| **Loading / Error / Unknown Widgets** | `AnimatedContent` transitions, shimmer `LoadingIndicator`, and styled `MaterialA2uiDefaults.ErrorFallback` | Plain `Text("Unknown widget '$widgetType' for component: $componentId")` in `A2UiSurface.kt` | `MALFORMED_JSON` (`AlienTeleportWidget`) shows a Material error fallback in Jetpack vs. raw text in `a2ui-4k`. |
| **Local `functionCall` & Schema Generation** | Registers `focusMap` and `openSpotDetail` as `A2uiFunction`s; `getJetpackHybridCatalogSchemaJson()` emits full Draft 2020-12 JSON Schema (used on Android in `TagSpotterApplication.kt`) | Does **not** register `focusMap` or `openSpotDetail` in `FunctionEvaluator`; `KmpA2UIEngine.getCatalogSchemaJson()` uses `A2UiRenderTool` (used on Desktop in `desktopApp/.../Main.kt`), which omits custom component property schemas & custom functions | If the LLM emits `{"functionCall": {"call": "focusMap", ...}}` per `TagAlongPrompts.kt`, it works on Jetpack but **silently no-ops on `a2ui-4k`**. |

---

### 1.3 Pros, Cons & Gaps

#### `ANDROIDX_JETPACK_OFFICIAL` (`androidx.a2ui` + `material3-a2ui:1.0.0-alpha02`)
- **Pros:** Faithful Material 3 spacing/borders/variants (`OutlinedButton`, `ExposedDropdownMenuBox`, `FilterChip`), proper `ChoicePicker` defaults (`MutuallyExclusive`), built-in `A2uiFunction` execution (`focusMap`, `openSpotDetail`), Draft 2020-12 JSON Schema export (`toJsonSchemaString()`), shimmer loading & error fallbacks, accessibility semantics.
- **Cons:** **Android-only** (cannot run on Desktop JVM, iOS, or Wasm); early alpha (`1.0.0-alpha02`); `Text` lacks Markdown rendering (`TODO(b/544548540)`); requires an active coroutine render loop (`processor.collectMessages()`).

#### `A2UI_KMP_4K` (`com.contextable:a2ui-4k:0.10.0`)
- **Pros:** **True KMP** (runs on Android, Desktop JVM, iOS, Wasm); simpler synchronous `SurfaceStateManager`; `TextWidget` supports basic Markdown; easy to override any built-in widget via `coreCatalog + Catalog.of(...)`.
- **Cons:** Missing `OutlinedButton` (`"default"` renders as filled `Button`); `ChoicePicker` ignores `displayStyle`/`filterable` and defaults to multi-select; `0.dp` spacing in `Column`/`Row` and tight `8.dp` borderless `Card`; `CatalogItem` has no `Modifier` parameter; `focusMap`/`openSpotDetail` `functionCall` actions are not wired; `getCatalogSchemaJson()` does not describe custom component schemas.

---

### 1.4 Concrete Recommendations for Issue #80

1. **Fix fixture & prompt ambiguity immediately (quick win, zero risk):**
   - In `feature/tagalong/src/commonMain/.../fixtures/TagAlongMockFixtures.kt` (`QUIZ_SAMPLE_JSON`, `VOTING_SAMPLE_JSON`) and `core/agent/.../TagAlongPrompts.kt`:
     - Explicitly set `"variant": "primary"` on primary action `Button`s (so both engines render a filled `Button`).
     - Explicitly set `"variant": "mutuallyExclusive"` on single-choice `ChoicePicker`s (so `a2ui-4k` stops treating quizzes/polls as multi-select checkboxes).
2. **Close the KMP parity gaps in `KmpHybridCatalog.kt` and `KmpA2UIEngine.kt`:**
   - Because `coreCatalog + Catalog.of(...)` overrides entries by name in `a2ui-4k`, override **`Button`** (map `"default"` to `OutlinedButton` with `4.dp` vertical padding), **`Card`** (`OutlinedCard` / `16.dp` padding), **`Column` / `Row`** (`Arrangement.spacedBy(8.dp)`), and **`ChoicePicker`** (honor `displayStyle` / default to `mutuallyExclusive`) in `KmpHybridCatalog.kt` — or theme both engines' Basic Catalogs consistently with TagSpotter's Neon palette.
   - In `KmpA2UIEngine.normalizeAction(...)`, translate `functionCall` for `focusMap` and `openSpotDetail` into `event` (`view_on_map` / `open_spot_detail`) so `Button` clicks using `functionCall` work identically on Desktop/KMP as they do on Android Jetpack.
   - Align the catalog schema passed to the agent on Desktop (`desktopApp/.../Main.kt`) with Android (`TagSpotterApplication.kt`) so the LLM receives the same custom component property schemas on every platform.
3. **Long-term engine strategy:**
   - Keep `ANDROIDX_JETPACK_OFFICIAL` as the Android reference / compliance benchmark while `androidx.a2ui` matures (it's still `alpha02`), and use `A2UI_KMP_4K` with the targeted overrides above as the cross-platform production renderer so Android and Desktop users see consistent layouts and buttons.

---

## Part 2: Issue #84 — How Does the Artist Profile Work?

### 2.1 Current State of Artist Data in TagSpotter

There is **no dedicated `Artist` domain model, Room entity/table, DAO query, or Firestore collection** anywhere in the repository:

| Layer | File & Symbol | How Artists Are Represented Today |
|---|---|---|
| Domain model | `core/model/.../Spot.kt` (`Spot`) | Flat `val artists: List<String> = emptyList()` on each `Spot` |
| Local Room DB | `core/database/.../SpotEntity.kt` (`SpotEntity`), `SpotDao.kt` | Stored as a serialized string list column on `spots`. `SpotDao` only has `updateSpotArtists(spotId, artists, lastEditedAt)` — no `getSpotsByArtist` or `getAllArtists` query. |
| Firestore | `firestore.rules` | Only 3 collections: `users/{userId}/{document=**}`, `shared_packs/{code}`, and `public_spots/{uuid}` (validating `spot.artists` as a list of ≤100 strings). No `artists` collection. |
| App UI (Gallery, Map, Detail) | `GalleryScreen.kt`, `GalleryViewModel.kt`, `MapViewModel.kt`, `DetailMetadataCard.kt`, `DetailViewModel.kt` | Plain string display (`"by " + artists.joinToString(", ")`), substring search, and Wikipedia lookup via `aiRecognitionService.searchWikipediaForSpot(title, category, artists)`. `NeonArtistBioCard` is **not** yet used in `DetailScreen` (despite being listed as reusable in `docs/a2ui-phase-4-and-more.md`). |

---

### 2.2 How TagAlong & the A2UI `ArtistBioCard` Currently Connect

```
User asks: "Tell me about <Artist>"
  │
  ▼
ADK Agent calls StreetArtTools.getArtistBio(artistName)
  │  └─ Calls hybridSpotRepository.getAllSpots()
  │  └─ Filters in memory: spot.artists.any { it.contains(artistName, ignoreCase = true) }
  │  └─ Returns ArtistBioDto(artistName, knownStyles, muralCount, notableMurals: List<String>, bioSummary)
  ▼
LLM synthesizes A2UI JSON with component type "ArtistBioCard"
  │  └─ Properties: artistName, bio, avatarUrl?, activeYears?, styles[], artworks[{id, title, imageUrl}]
  ▼
KmpHybridCatalog / JetpackHybridCatalog → NeonArtistBioCard (:core:ui)
  │  ├─ Tap artwork thumbnail → event "open_spot_detail" {spotId, title}
  │  │    └─ TagAlongViewModel.resolveAndEmitOpenSpotDetail() → looks up UUID in HybridSpotRepository → navigates to DetailScreen
  │  └─ Tap style chip → event "filter_by_style" {style}
  │       └─ NOT handled locally in TagAlongViewModel; forwarded to ADK agent as [A2UI ClientEvent] (no-op offline)
```

#### Concrete Disconnects Found in the Current Pipeline

1. **Schema mismatch between `ArtistBioDto` and `ArtistBioCard`:**
   - `StreetArtTools.getArtistBio` (`core/agent/.../StreetArtTools.kt`, lines 49–55 & 173–201) returns `notableMurals: List<String>` (plain 40-char title snippets) — it **omits** each spot's `uuid`, `imageUrl`/`thumbnailPath`, and date range.
   - `ArtistBioCard` (`KmpHybridCatalog.kt` lines 201–260, `JetpackHybridCatalog.kt` lines 319–387) expects `artworks: List<{id, title, imageUrl}>`, `avatarUrl`, and `activeYears`.
   - **Result:** The LLM either leaves thumbnails empty or **hallucinates** fake `id`s and `imageUrl`s. Tapping a hallucinated artwork thumbnail calls `hybridSpotRepository.getSpotByUuid(fakeId)` which returns `null`, so navigation silently fails.
2. **Generic fallback when no spots match:**
   - If the artist has 0 spots in local/public DB, `getArtistBio` returns `knownStyles = ["Contemporary Muralism", "Graffiti Art"]` and a boilerplate `bioSummary`, relying entirely on the LLM's parametric knowledge for real-world artists.
3. **Free-text artist name fragmentation:**
   - Because `Spot.artists` is unnormalized free text, `"Banksy"` vs `"banksy"` vs `"Mr. Brainwash"` vs `"Mr Brainwash"` fragment results, and `contains(artistName, ignoreCase = true)` can false-match short names (e.g., `"Blu"` matching `"Blue..."`).
4. **`filter_by_style` event is unhandled locally:**
   - Tapping a style chip in `NeonArtistBioCard` dispatches `filter_by_style`, which falls through to `TagAlongViewModel`'s `else` branch and round-trips to the LLM (or does nothing in offline mode) instead of filtering spots or triggering a local search.

---

### 2.3 Answering Issue #84: Do We Need to Build Artist Profiles?

**Short answer: No — you do NOT need to build a dedicated artist profile database/collection right now to make the A2UI `ArtistBioCard` accurate and useful.**

`ArtistBioCard` is a **view/presentation contract** (a stateless `:core:ui` composable wrapped as an A2UI component). It does not require a persisted `Artist` table — it only needs structured data fed to it by the agent tool (or a ViewModel if reused in `DetailScreen`).

You only need dedicated persisted artist profiles **if and when** you want:
- User-curated/editable artist bios, avatars, social links, or crew memberships;
- Offline artist bios without calling Gemini;
- A browsable "Artists" directory tab or canonical alias deduplication (`"Kira 'Volt' Jensen"` = `"Volt"`).

---

### 2.4 Architectural Options & Trade-Offs

| Option | Description | Pros | Cons |
|---|---|---|---|
| **Option A: Dynamic Aggregation + Enriched Tool Contract** *(Recommended Now)* | Keep `Spot.artists` as the sole source of truth. Upgrade `ArtistBioDto` to return structured `artworks: List<ArtistArtworkDto(spotId, title, imageUrl, latitude, longitude)>` and derived `activeYears` (from `artworkDate` / `createdAt` min–max). Let Gemini write the narrative `bio` from tool data + world knowledge, with prompt rules forbidding fabricated `spotId`/`imageUrl`. | • Zero DB/Room migration<br>• Zero Firestore rules/sync changes<br>• Fixes broken artwork thumbnails & `open_spot_detail` clicks immediately<br>• Always reflects latest local + public spots | • Bio text is regenerated each chat (requires LLM/online)<br>• No user-editable bio/avatar/socials<br>• Free-text name typos can still split an artist |
| **Option B: Hybrid — Local `artist_profiles` Room Cache** *(Recommended Next Step)* | Add a lightweight `ArtistProfileEntity` in Room (`normalizedName` PK, `displayName`, `bio`, `avatarUrl`, `activeYears`, `aliases`, `wikipediaUrl`, `updatedAt`). Dynamically join `artworks` and `styles` from `spots` at query time. Cache AI-generated or user-edited bios locally. | • Works offline & avoids redundant LLM calls<br>• Enables reusing `NeonArtistBioCard` directly in `DetailScreen` / Artist sheet<br>• Lets users edit/pin bios and aliases locally<br>• No Firestore schema/moderation burden | • Requires Room schema migration<br>• Local-only unless synced under `users/{uid}/artist_profiles` |
| **Option C: Dedicated Firestore `artists/{artistId}` Collection** *(Future / Community Scale)* | First-class shared `Artist` collection in Firestore + `artistIds: List<String>` foreign keys on `Spot`. | • Shared community-curated artist wiki<br>• Canonical identity across all users and packs | • Significant migration of `Spot`, `BackupWrapper`, `shared_packs`, `public_spots`, and `firestore.rules`<br>• Requires moderation/ownership rules (who can edit a public artist profile?)<br>• Premature for current product stage |

---

### 2.5 Recommended Path Forward for Issue #84

1. **Immediate (Option A — High ROI, Low Effort):**
   - **Enrich `ArtistBioDto` in `core/agent/.../StreetArtTools.kt`:**
     - Replace `notableMurals: List<String>` with `artworks: List<ArtistArtworkDto>` carrying real `spotId` (`spot.uuid`), `title`, `imageUrl` (`images.firstOrNull()?.thumbnailPath ?: images.firstOrNull()?.imagePath ?: ""`), `latitude`, and `longitude`.
     - Derive `activeYears` from the min/max year of matching spots' `artworkDate` (or `createdAt`) when available.
     - Tighten matching in `getArtistBio` (exact case-insensitive match first, falling back to word-boundary/contains match) so short artist names don't false-match.
   - **Update `TagAlongPrompts.kt`:**
     - Instruct the agent to populate `ArtistBioCard.artworks` **only** from `getArtistBio`'s returned `artworks` (using real `spotId` and `imageUrl`), omit `avatarUrl` when unknown (so `NeonArtistBioCard` renders its clean neon initial badge instead of a broken/fake URL), and synthesize `bio` from catalog data + general street-art knowledge.
   - **Handle `filter_by_style` in `TagAlongViewModel.kt`:**
     - Provide a clean local/offline fallback (e.g., querying `hybridSpotRepository` for spots matching that style tag and rendering matching `SpotCard`s or a helpful message) rather than silently dropping it when offline.
2. **Follow-up (Option B — When ready for in-app Artist views):**
   - Add a `SpotDao` query for spots by artist and an optional local `ArtistProfileEntity` cache so tapping an artist name on `DetailMetadataCard` can open a sheet displaying `NeonArtistBioCard` using the exact same component as TagAlong.
