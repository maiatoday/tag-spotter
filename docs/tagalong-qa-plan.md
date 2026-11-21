# TagAlong Cross-Platform QA Plan

> **Document:** `docs/tagalong-qa-plan.md`  
> **Date:** 2026-11-21  
> **Phase 4 Architecture & Gaps:** [a2ui-phase-4-and-more.md](a2ui-phase-4-and-more.md)  
> **Task Tracker:** [tagalong-task-tracker.md](tagalong-task-tracker.md)  
> **ADK Runtime Guide:** [tagalong-adk-runtime-guide.md](tagalong-adk-runtime-guide.md)  
> **Workspace:** `/Users/maia/workspace/maiatoday/tag-spotter`

---

## 1. Platform Support & QA Scope Matrix

TagAlong is embedded into the unified Tag Spotter app via the KMP-safe `tagAlongContent` slot in `:feature:main`. QA verifies **full TagAlong functionality** on Android and Desktop JVM, and **graceful non-regression** on iOS, Web (WasmJs), and Wear OS.

| Platform / Target | TagAlong Support | A2UI Engine(s) | ADK / Gemini Backend | Layout Modes | QA Level |
| :--- | :---: | :--- | :--- | :--- | :--- |
| **Android (`:androidApp`)** | ✅ Full | **Dual Engine:** `androidx.a2ui` (`1.0.0-alpha02`) ↔ `a2ui-4k` (`0.10.0`) | `google-adk-kotlin-firebase-android` + `Firebase.ai` (`gemini-4-hydrogen`) + App Check | Compact (`< 760.dp` single-pane) & Tablet/Landscape (`>= 760.dp` 2-pane) | **Full Functional + Dual-Engine A/B** |
| **Desktop JVM (`:desktopApp` — macOS / Linux / Windows)** | ✅ Full | **KMP Engine:** `com.contextable:a2ui-4k` (`0.10.0`) | `google-adk-kotlin-core` + `com.google.genai:google-genai` (`gemini-4-hydrogen`) via API key | Wide (`>= 760.dp` 2-pane) & Narrow (`< 760.dp` single-pane) | **Full Functional + Desktop Persistence** |
| **iOS (`iosApp`)** | ⏳ Deferred (Phase 8) | N/A (`tagAlongContent == null`) | Tag Spotter single-shot `FirebaseAI` (`gemini-4-hydrogen`) | Gallery & Map only (`Tab.TagAlong` hidden) | **Non-Regression** |
| **Web (`:webApp` WasmJs)** | ⏳ Deferred (Phase 8) | N/A (`tagAlongContent == null`) | N/A | Gallery & Map only (`Tab.TagAlong` hidden) | **Non-Regression** |
| **Wear OS (`:wear`)** | ➖ Companion Only | N/A | N/A | Watch companion | **Build / Sync Non-Regression** |

---

## 2. Pre-Flight Setup & Manual Configuration Gates

Before executing **live AI** QA scenarios, confirm the prerequisites below (see [Section 9 of a2ui-phase-4-and-more.md](a2ui-phase-4-and-more.md#9-manual-configuration--developer-action-prompts-prompt-user-gates) for `[PROMPT USER]` rules).

> [!CAUTION]
> **Secrets Policy:** Never commit, push, or paste secret values from `local.properties` into git, logs, or bug reports. Only verify key presence (`cut -d= -f1 local.properties`).

| Gate | Platform | Requirement | How to Verify |
| :---: | :--- | :--- | :--- |
| **M1** | Desktop JVM | `gemini.api.key` in root `local.properties`, `GEMINI_API_KEY` env var, or **Settings → Gemini API Key** | Launch `:desktopApp:run`, send a chat message, verify no Offline Fallback banner appears |
| **M2** | Android | **Firebase AI Logic** enabled in Firebase Console & model `gemini-4-hydrogen` accessible | Confirm in Firebase Console for the project in `androidApp/google-services.json` |
| **M3** | Android (Debug) | `firebase.appcheck.debug_token` in root `local.properties` **and** registered in Firebase Console → App Check → Apps → Manage debug tokens | Check Logcat for absence of `403 App Check token is invalid` |
| **M4** | Android & Desktop | Updated `firestore.rules` & `storage.rules` (`/public_spots/{uuid}`) deployed to Firebase | Run `firebase deploy --only firestore:rules,storage` (requires user approval) |
| **M5** | Android | Runtime **Location** & **Photos/Media Location** permissions granted; mock GPS set on Emulator | Emulator Extended Controls → Location (e.g. `52.5251, 13.4021` or near test spots) |

---

## 3. Automated QA Gate (Run Before Manual Testing)

All automated tests and builds must pass before manual QA sign-off:

```bash
# 1. Core A2UI dual-engine (Jetpack Robolectric + KMP a2ui-4k) & ADK agent/tool tests
./gradlew :core:a2ui:allTests :core:agent:allTests

# 2. Shared persistence, hybrid sync, UI & TagAlong feature tests
./gradlew :core:database:allTests :core:sync:allTests :core:ui:allTests :feature:main:allTests :feature:tagalong:allTests

# 3. Full monorepo regression gate & app assembly
./gradlew clean allTests :androidApp:assembleDebug :desktopApp:assemble
```

**Automated Coverage Checklist:**
- [ ] `JetpackA2UIEngineTest`: `toJsonSchemaString()` includes Basic Catalog primitives + 4 custom cards + `focusMap`/`openSpotDetail`; **all 6 valid `TagAlongMockFixtures`** hydrate `activeSurfaces`; `A2uiFunction` callbacks and `event` actions emit on `outboundEvents`.
- [ ] `KmpA2UIEngineTest` & `TagAlongMockFixturesTest`: **all 6 valid fixtures** + `MALFORMED_JSON` process cleanly; `updateDataModel` mutates JSON Pointer paths.
- [ ] `RunnerCallTest` & `StreetArtToolsTest`: `LlmAgent` with `staticInstruction` executes with a non-null JSON Schema (`{...}`) without `InstructionStateInjector` exceptions; `saveNewSpot` constrains `category` to `Spot.CATEGORIES` and routes styles to `tags`.
- [ ] `TagAlongViewModelTest`: Streaming/tool-event filtering, binary photo `Blob` attachment, `navigationEvents` (`uuid` → `Spot.id: Long`), `save_spot` / `toggle_star` / `answer_quiz` / `vote_option`, session switch/delete + `InMemoryRunner` continuity.

---

## 4. Shared Functional QA Suite (Run on BOTH Android & Desktop JVM)

Execute every scenario below on **both** `:androidApp` and `:desktopApp` and record pass/fail in the [Sign-Off Matrix](#7-qa-sign-off--defect-severity-matrix).

### QA-1: App Launch, Unified Tab Navigation & Adaptive Layout
1. Launch the app and verify the navigation bar/rail shows **Gallery**, **Map**, and **TagAlong** (`AutoAwesome` icon).
2. Switch **Gallery → TagAlong → Map → TagAlong**.
3. **Expected:**
   - TagAlong retains its active conversation, scroll position, and rendered A2UI surfaces across tab switches.
   - At width **`< 760.dp`** (Android phone portrait, or Desktop narrow window): renders a single-column Chat + inline A2UI stream with **no** redundant inner `"Live Map"` sub-tab.
   - At width **`>= 760.dp`** (Desktop default window, Android tablet/landscape): renders a 2-pane split (Chat + A2UI left ~56%, live `SpotMapView` right ~44%).

### QA-2: Offline Fallback Mode & Graceful Error Handling
1. Disable network (Airplane Mode on Android) **or** launch Desktop with no Gemini API key configured.
2. Send `"Find murals near me"`.
3. **Expected:**
   - App does **not** crash or hang on the typing indicator (`isGenerating` returns to `false`).
   - Yellow **OFFLINE FALLBACK MODE** banner appears inline with a clear reason.
   - User can still tap all **A2UI Mock Catalog Tester** chips and browse local spots/history.
4. Restore network / configure API key and send another prompt.
5. **Expected:** Banner clears and live agent replies succeed.

### QA-3: A2UI Mock Catalog Suite (All 7 Fixtures)
Tap each chip in the **A2UI Mock Catalog Tester** bar in sequence and verify both visual rendering and interactivity:

| Chip | Expected Component(s) | Interactive Verification |
| :--- | :--- | :--- |
| **`+ Spot Card`** | Custom `SpotCard` (`NeonSpotCard` from `:core:ui`) with image, artist, tags, distance badge | 1. Tap **Star** → toggles `☆ Star` ↔ `★ Starred` in place via `/spot/isStarred`.<br>2. Tap **Save Spot** → updates to `✓ Saved` via `/spot/isSaved` and persists to Gallery.<br>3. Tap **View on Map** → pans Desktop MapPane OR switches Mobile to `Tab.Map`. |
| **`+ Walking Tour`** | Custom `TourCard` (`NeonTourCard`) with duration, distance, and 4 numbered stops | Tap a stop → pans map to that waypoint's coordinates and updates focused title. |
| **`+ Artist Bio`** | Custom `ArtistBioCard` (`NeonArtistBioCard`) with avatar, bio, active years, style chips, artwork thumbnails | Tap a style chip or artwork thumbnail → triggers `filter_by_style` / `open_spot_detail` without crash. |
| **`+ Style Quiz`** | Built-in Basic Catalog (`Card` → `Column` → `Text`, `ChoicePicker`, `Button`) | Select an option in `ChoicePicker` and tap **Submit Answer** → dispatches `answer_quiz` event with selected option (no unknown `clientEvent` function error!) and shows agent/UI feedback. |
| **`+ Community Vote`** | Built-in Basic Catalog (`Card` → `Column` → `Text`, `ChoicePicker`, `Button`) | Select an option and tap **Vote Now** → dispatches `vote_option` event and updates feedback cleanly. |
| **`+ Map Overlay`** | Custom `MapPinOverlay` (`NeonMapPinOverlayWidget`) showing Kreuzberg pins | Card renders pins cleanly (not blank/unknown component!); tapping a pin focuses the map on that pin. |
| **`+ Malformed (Fallback)`** | Unknown component `AlienTeleportWidget` | Does **not** crash; gracefully renders fallback / logs `ClientError` while keeping the rest of the chat usable. |

### QA-4: Live Conversational Agent & All 5 `@Tool` Invocations
With live Gemini credentials active (Gates M1–M3), seed at least 2–3 local spots in Tag Spotter first, then test each prompt:
1. **`getNearbySpots`:** *"What street art spots are near me?"* (or *"Find murals near 52.52, 13.40"*).
   - **Expected:** Agent calls `getNearbySpots`, returns concise prose (with clean rendered **bold**/bullet Markdown, no raw `**`), and renders inline `SpotCard`(s) hydrated with real local/public spots. No `Context variable not found` crash!
2. **`generateWalkingTour`:** *"Plan a 30-minute walking tour around my spots."*
   - **Expected:** Calls `generateWalkingTour`, renders a `TourCard` with real waypoints, and drops numbered tour pins on the live map.
3. **`getArtistBio`:** *"Tell me about [Artist Name from your DB]."*
   - **Expected:** Calls `getArtistBio` and renders an `ArtistBioCard` listing cataloged works and styles.
4. **Interactive Quiz / Voting Turn:** *"Give me a quick graffiti style quiz."*
   - **Expected:** Agent emits a Basic Catalog `Card` + `ChoicePicker` + `Button` surface. Submitting an answer forwards the `ClientEvent` to the ADK `Runner` and either updates the surface in place (`updateDataModel`) or replies contextually.
5. **Streaming / Multi-Turn Integrity:**
   - **Expected:** Intermediate tool-call turns do **not** show raw JSON tool arguments in the chat bubble, and final response text is **not** duplicated twice in the same bubble.

### QA-5: Multimodal Photo Attachment, Category Mapping & 1-Tap Spot Creation
1. Tap the camera/photo attachment button (`📷`) in TagAlong and pick a street art photo **with EXIF GPS metadata**.
   - **Expected:** Attachment pill appears; map centers on the photo's EXIF coordinates.
2. Send *"Identify this mural and help me save it"*.
   - **Expected:**
     - User bubble displays a visual **`AsyncImage` thumbnail** (NOT raw `content://` or `file://` text); tapping it opens `ZoomableImageOverlay` with pinch-to-zoom.
     - Agent receives real compressed JPEG bytes and describes what is **actually visible in the photo** (not a hallucinated refusal claiming it can only see a URI).
     - Agent invokes `identifyMuralByPhoto` / `saveNewSpot` or renders a `SpotCard` with a **Save Spot** button.
3. Trigger 1-tap save (either via `@Tool saveNewSpot` or the card's **Save Spot** button) and switch to **Gallery** and **Map**.
   - **Expected:**
     - New spot appears immediately in **Gallery** and **Map** with its thumbnail and coordinates.
     - Inspect spot in **DetailScreen**: `category` is strictly one of `Spot.CATEGORIES` (`graffiti`, `sculpture`, `nature`, `architecture`, `public_place`, `food`), while style subtypes (`mural`, `stencil`, `sticker`, `paste-up`) appear in **Tags**.
4. Repeat with a photo **without EXIF GPS** (and with location unavailable / mock off):
   - **Expected:** Agent renders an interactive Basic Catalog **missing-field form** (`TextField` / `ChoicePicker` + Save `Button`) instead of silently saving `(0.0, 0.0)` garbage coordinates.

### QA-6: Bidirectional Navigation (`uuid` → `Spot.id: Long`) & Shared State Sync
1. From a live `SpotCard` returned by `getNearbySpots` (backed by a real local spot with a UUID string):
   - Tap the **card body** (`openSpotDetail`).
   - **Expected:** Navigates directly into Tag Spotter's **`DetailScreen`** for that exact spot (verifying UUID → Room `Long` ID lookup works!). Tap Back → returns cleanly to `Tab.TagAlong`.
2. Tap **Star** on a real local spot's `SpotCard` inside TagAlong, then switch to **Gallery** / **DetailScreen**.
   - **Expected:** The spot's starred state matches immediately in Gallery & DetailScreen.
3. Tap **View on Map** on a `SpotCard`:
   - **Desktop (`>= 760.dp`):** Right-hand `MapPane` pans/centers on the spot and highlights its marker.
   - **Mobile (`< 760.dp`):** Switches `MainContainer` to **`Tab.Map`** centered on the spot's latitude/longitude.

### QA-7: Light / Dark Theme Fidelity & Input Ergonomics
1. Open the navigation drawer → switch **Theme** between **Light**, **Dark**, and **Auto**.
2. Inspect TagAlong header, engine badge, fixture chips, user & agent chat bubbles, `OutlinedTextField`, `HistoryBottomSheet`, all 4 custom Neon cards (`SpotCard`, `TourCard`, `ArtistBioCard`, `MapPinOverlay`), Basic Catalog Quiz/Vote cards, and `MapPane`.
3. **Expected:**
   - No hardcoded dark backgrounds or unreadable neon-on-white / dark-on-dark text in **Light Mode**.
   - `MapPane` switches map tile polarity appropriately with theme.
4. Type a message and press **Enter** (Desktop) or **Send** on soft keyboard (Android).
   - **Expected:** Submits the message immediately and clears the input field.

### QA-8: Session Lifecycle, History Sheet & Multi-Turn Context Continuity
1. Have a 2-turn conversation in Session A (e.g., *"I'm looking for stencils by Banksy"* → agent replies with a card).
2. Tap **`+` (New Chat)** in the TagAlong header.
   - **Expected:** Clears active surfaces, creates a fresh session with welcome message, and preserves Session A in `TagAlongDatabase`.
3. Send a message in Session B, then tap the **History** icon (`Icons.Default.History`).
   - **Expected:** `HistoryBottomSheet` lists both Session A and Session B with active session highlighted.
4. Switch back to **Session A**.
   - **Expected:** Messages and inline A2UI surfaces from Session A replay cleanly.
5. Ask a follow-up referencing earlier context (*"Which artist were we just talking about?"*).
   - **Expected:** Agent remembers Session A's prior turns (verifying `InMemoryRunner` session re-seeding!).
6. Close and relaunch the app → open **TagAlong** → ask a follow-up in the restored session.
   - **Expected:** Past messages, A2UI surfaces, and conversational memory survive app restart.
7. Delete an inactive session, then delete the **active** session from `HistoryBottomSheet`.
   - **Expected:** Deleting the active session cleanly switches to the next remaining session (or creates a new chat if none remain) and cleans up both Room DB and `runner.sessionService`.

### QA-9: Hybrid Community `/public_spots` Discovery & Privacy Boundary
*(Requires Gate M4 — deployed `firestore.rules` & `storage.rules`)*
1. Signed in as **User A**, create two spots:
   - Spot 1: toggle **"Share as Public Spot"** (`isPublic = true`) and sync.
   - Spot 2: leave private (`isPublic = false`) and sync.
2. Signed in as **User B** (or a second device/platform), ask TagAlong *"Find street art near [coordinates]"*.
3. **Expected:**
   - Spot 1 (public) is discovered by `HybridSpotRepository` and shown in TagAlong with its public thumbnail.
   - Spot 2 (private) is **never** returned to User B.
4. As User A, toggle Spot 1's **"Share as Public Spot"** back OFF (`isPublic = false`) and sync → verify Spot 1 disappears from User B's TagAlong queries.

---

## 5. Platform-Specific QA Suites

### 5.1 Android-Specific QA (`:androidApp`)

- [ ] **QA-AND-1: Dual-Engine Runtime A/B Switcher (`ANDROIDX_JETPACK_OFFICIAL` ↔ `A2UI_KMP_4K`)**
  1. Load Spot Card, Walking Tour, Artist Bio, Style Quiz, Community Vote, and Map Overlay fixtures into one chat session.
  2. Tap the engine badge (`ANDROIDX_JETPACK_OFFICIAL ⇄`) in the top bar to switch to `A2UI_KMP_4K`, then toggle back to `ANDROIDX_JETPACK_OFFICIAL`.
  3. **Expected:** All 6 surfaces replay cleanly on **both** engines with matching `:core:ui` Neon card appearance and working interactions (`focusMap`, `openSpotDetail`, `toggle_star`, `save_spot`, `answer_quiz`, `vote_option`).

- [ ] **QA-AND-2: Static App Shortcut & Deep Link (`tagspotter://tagalong`)**
  1. Long-press the Tag Spotter launcher icon → tap **"TagAlong AI Guide"** (cold start).
  2. Switch to Gallery, background the app, and re-trigger the shortcut (warm `onNewIntent`), or run:
     ```bash
     adb shell am start -a android.intent.action.VIEW -d "tagspotter://tagalong"
     ```
  3. **Expected:** Both cold start and warm start open directly onto `Tab.TagAlong`.

- [ ] **QA-AND-3: Edge-to-Edge IME (`imePadding()`) & System Insets**
  1. Tap the chat text field on an Android phone (portrait & landscape) with gesture navigation enabled.
  2. **Expected:** The input bar lifts cleanly above the soft keyboard (not obscured by IME or bottom nav bar), status bar icons remain legible in both Light and Dark themes, and the message list scrolls smoothly.

- [ ] **QA-AND-4: Android Media & Location Permissions**
  1. On a fresh install, tap `📷` in TagAlong → verify `ACCESS_MEDIA_LOCATION` permission is requested before picking a geotagged photo.
  2. Grant permission and pick a geotagged photo → verify EXIF GPS is extracted (non-zero coordinates).
  3. Repeat with Location permission denied → verify graceful fallback (no crash, prompts/asks user for area).

- [ ] **QA-AND-5: Firebase AI Logic & App Check**
  1. With `firebase.appcheck.debug_token` set and registered (Gate M3), send a live message and check Logcat for zero App Check `403` errors.
  2. Build release/staging or test with an unregistered token → verify failure surfaces as a readable offline/error banner rather than a crash.

- [ ] **QA-AND-6: Single `SpotDatabase` Invalidation with `ImportPackWorker`**
  1. Import a `.ts_pack` file while Tag Spotter is running.
  2. Without restarting the app, switch to **TagAlong** and ask about the newly imported mural(s).
  3. **Expected:** TagAlong immediately finds the imported spots (confirming `TagSpotterApplication` and Koin share one Room `SpotDatabase` instance).

---

### 5.2 Desktop JVM-Specific QA (`:desktopApp`)

- [ ] **QA-DESK-1: API Key Priority & Live Settings Reload**
  1. Launch `./gradlew :desktopApp:run` with `gemini.api.key` in root `local.properties` (note: Gradle runs with `workingDir = desktopApp/`, verifying `../local.properties` lookup!).
  2. Verify live chat works immediately.
  3. Temporarily launch without `GEMINI_API_KEY` and without `gemini.api.key` in `local.properties` → verify Offline Mode → navigate to **Settings**, paste a Gemini API key, return to **TagAlong**, and send a prompt.
  4. **Expected:** TagAlong transitions online immediately without restarting DesktopApp.

- [ ] **QA-DESK-2: Persistent `~/.tagspotter/` Database Storage**
  1. Create a chat session and save a spot from TagAlong on Desktop.
  2. Inspect `~/.tagspotter/` on disk: confirm both `spot_database.db` and `tagalong_database.db` exist there (and **not** in `/tmp`).
  3. Quit and relaunch `:desktopApp:run` → verify both the saved spot and chat session/surfaces persist.

- [ ] **QA-DESK-3: Desktop File Picker Paths With Spaces & Non-JPEG Image Scaling**
  1. Attach a `.png` (and a large `> 4MB` `.jpg`) located in a folder containing spaces (e.g., `~/Desktop/Street Art Photos/mural test 1.png`).
  2. **Expected:** URL-decoded path resolves cleanly (`%20` handled), `RealPhotoProcessor.decodeScaledBitmap` downscales to `<= 1024px` and encodes valid JPEG bytes (`image/jpeg`), thumbnail renders in chat, and Gemini Vision accurately identifies the image.

- [ ] **QA-DESK-4: Window Resize Breakpoint (`760.dp`) & Keyboard Submit**
  1. Drag window width back and forth across `760.dp`.
  2. **Expected:** Smoothly switches between 2-pane (`ChatPane` + `MapPane`) and single-pane (`ChatPane` routing *"View on Map"* to `Tab.Map`) without losing chat text or active surfaces. Pressing **Enter** submits messages.

- [ ] **QA-DESK-5: Remote HTTPS Image Rendering in A2UI Cards**
  1. Trigger `+ Spot Card` and `+ Artist Bio` fixtures (using `https://picsum.photos/...` URLs) and a public Firestore spot.
  2. **Expected:** Coil + Ktor OkHttp fetches and renders all remote HTTPS images cleanly on Desktop JVM.

---

### 5.3 Non-TagAlong Platform Regression Checks (`iosApp`, `:webApp`, `:wear`)

- [ ] **QA-REG-1 (`iosApp`):** Build/run on iOS Simulator — verify `Tab.TagAlong` is **not** shown (`tagAlongContent == null`), Gallery/Map/Detail/Settings function normally, and single-shot AI identification (`SwiftFirebaseAiService.swift` using `FirebaseAI` + `gemini-4-hydrogen`) builds and runs cleanly.
- [ ] **QA-REG-2 (`:webApp` WasmJs):** Run `./gradlew :webApp:wasmJsBrowserDevelopmentWebpack` (or `compileKotlinWasmJs`) — verify `Tab.TagAlong` is hidden and no Android/JVM-only imports leaked into `:feature:main` or `:core:ui` `commonMain`.
- [ ] **QA-REG-3 (`:wear`):** Run `./gradlew :wear:assembleDebug` — verify Wear OS companion builds cleanly with the consolidated `:androidApp`.

---

## 6. Cross-Platform Parity & Edge-Case Checklist

- [ ] **Long / Rapid Prompts:** Sending a 500-char prompt or tapping Send rapidly while `isGenerating == true` does not corrupt session order or spawn duplicate agent bubbles.
- [ ] **Empty / Whitespace Input:** Tapping Send with empty text and no attached photo is a clean no-op.
- [ ] **Photo-Only Send:** Attaching a photo and tapping Send with blank text still invokes multimodal analysis cleanly.
- [ ] **Special Characters & Emoji:** Artists/titles with quotes, accents, or emoji (`Kira 'Volt' Jensen`, `RAW-Gelände`, `🎨`) serialize cleanly in A2UI JSON and SQLite without parse errors.
- [ ] **Stale Build Cleanliness:** Verified after `./gradlew clean` so no leftover deleted Phase 3 classes (`A2UIPayloadParser`) linger in build outputs.

---

## 7. QA Sign-Off & Defect Severity Matrix

### Severity Definitions
- **P0 — Blocker (Must fix before release/demo):** Crash, ADK `InstructionStateInjector` exception, broken A2UI rendering on a core fixture, secret leak, data loss, or broken core Tag Spotter regression.
- **P1 — Major (Fix before sign-off):** Navigation/permission failure (`uuid` → `Long`, `ACCESS_MEDIA_LOCATION`, IME obscuring input), broken Light Mode contrast, Desktop path/scaling failure, or lost session memory on switch.
- **P2 — Minor / Polish:** Subtle spacing/animation glitches, Markdown edge cases, minor map zoom offsets.

### Sign-Off Table

| Platform | Build / Commit | Automated Gate | Shared QA (QA-1..9) | Platform QA | Open P0/P1 Bugs | Status |
| :--- | :--- | :---: | :---: | :---: | :---: | :---: |
| **Android (Phone `< 760.dp`)** | `TBD` | ⏳ | ⏳ | ⏳ | — | ⏳ Pending |
| **Android (Tablet/Landscape `>= 760.dp`)** | `TBD` | ⏳ | ⏳ | ⏳ | — | ⏳ Pending |
| **Desktop JVM (macOS / Linux / Win)** | `TBD` | ⏳ | ⏳ | ⏳ | — | ⏳ Pending |
| **iOS (`iosApp` Non-Regression)** | `TBD` | ⏳ | N/A | ⏳ (`QA-REG-1`) | — | ⏳ Pending |
| **Web (`:webApp` Non-Regression)** | `TBD` | ⏳ | N/A | ⏳ (`QA-REG-2`) | — | ⏳ Pending |
| **Wear (`:wear` Non-Regression)** | `TBD` | ⏳ | N/A | ⏳ (`QA-REG-3`) | — | ⏳ Pending |
