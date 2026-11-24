# Research Report: Artist Profile Recommendations (#84)

**Date:** 2026-11-23  
**Scope:** GitHub Issue `#84` (*How does the artist profile work*)  
**Repository:** `tag-spotter` (read-only research; no source files modified)

---

## Executive Summary

TagSpotter currently has **no `Artist` model, Room table, or Firestore collection** — artists exist only as `Spot.artists: List<String>`. The TagAlong agent's `StreetArtTools.getArtistBio(artistName)` dynamically scans all local + public spots and returns a thin `ArtistBioDto`, while the A2UI `ArtistBioCard` (`NeonArtistBioCard`) expects richer fields (`avatarUrl`, `activeYears`, `artworks` with `id` + `imageUrl`).

**Recommendation:** You **do not** need a full Firestore `artists` collection right now. **Start with Option A (Dynamic Spot Aggregation + Rich `ArtistBioDto`)** to eliminate hallucinated artwork IDs/images and wire real spots directly into `ArtistBioCard`. Add **Option B (Local `artist_profiles` Room cache)** later only when you need offline/user-editable bios or a dedicated Artist screen.

---

## 1. Current State of Artist Data in TagSpotter

There is **no dedicated `Artist` domain model, Room entity/table, DAO query, or Firestore collection** anywhere in the repository:

| Layer | File & Symbol | How Artists Are Represented Today |
|---|---|---|
| Domain model | `core/model/.../Spot.kt` (`Spot`) | Flat `val artists: List<String> = emptyList()` on each `Spot` |
| Local Room DB | `core/database/.../SpotEntity.kt` (`SpotEntity`), `SpotDao.kt` | Stored as a serialized string list column on `spots`. `SpotDao` only has `updateSpotArtists(spotId, artists, lastEditedAt)` — no `getSpotsByArtist` or `getAllArtists` query. |
| Firestore | `firestore.rules` | Only 3 collections: `users/{userId}/{document=**}`, `shared_packs/{code}`, and `public_spots/{uuid}` (validating `spot.artists` as a list of ≤100 strings). No `artists` collection. |
| App UI (Gallery, Map, Detail) | `GalleryScreen.kt`, `GalleryViewModel.kt`, `MapViewModel.kt`, `DetailMetadataCard.kt`, `DetailViewModel.kt` | Plain string display (`"by " + artists.joinToString(", ")`), substring search, and Wikipedia lookup via `aiRecognitionService.searchWikipediaForSpot(title, category, artists)`. `NeonArtistBioCard` is **not** yet used in `DetailScreen` (despite being listed as reusable in `docs/a2ui-phase-4-and-more.md`). |

---

## 2. How TagAlong & the A2UI `ArtistBioCard` Currently Connect

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

### Concrete Disconnects Found in the Current Pipeline

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

## 3. Answering Issue #84: Do We Need to Build Artist Profiles?

**Short answer: No — you do NOT need to build a dedicated artist profile database/collection right now to make the A2UI `ArtistBioCard` accurate and useful.**

`ArtistBioCard` is a **view/presentation contract** (a stateless `:core:ui` composable wrapped as an A2UI component). It does not require a persisted `Artist` table — it only needs structured data fed to it by the agent tool (or a ViewModel if reused in `DetailScreen`).

You only need dedicated persisted artist profiles **if and when** you want:
- User-curated/editable artist bios, avatars, social links, or crew memberships;
- Offline artist bios without calling Gemini;
- A browsable "Artists" directory tab or canonical alias deduplication (`"Kira 'Volt' Jensen"` = `"Volt"`).

---

## 4. Architectural Options & Trade-Offs

| Option | Description | Pros | Cons |
|---|---|---|---|
| **Option A: Dynamic Aggregation + Enriched Tool Contract** *(Recommended Now)* | Keep `Spot.artists` as the sole source of truth. Upgrade `ArtistBioDto` to return structured `artworks: List<ArtistArtworkDto(spotId, title, imageUrl, latitude, longitude)>` and derived `activeYears` (from `artworkDate` / `createdAt` min–max). Let Gemini write the narrative `bio` from tool data + world knowledge, with prompt rules forbidding fabricated `spotId`/`imageUrl`. | • Zero DB/Room migration<br>• Zero Firestore rules/sync changes<br>• Fixes broken artwork thumbnails & `open_spot_detail` clicks immediately<br>• Always reflects latest local + public spots | • Bio text is regenerated each chat (requires LLM/online)<br>• No user-editable bio/avatar/socials<br>• Free-text name typos can still split an artist |
| **Option B: Hybrid — Local `artist_profiles` Room Cache** *(Recommended Next Step)* | Add a lightweight `ArtistProfileEntity` in Room (`normalizedName` PK, `displayName`, `bio`, `avatarUrl`, `activeYears`, `aliases`, `wikipediaUrl`, `updatedAt`). Dynamically join `artworks` and `styles` from `spots` at query time. Cache AI-generated or user-edited bios locally. | • Works offline & avoids redundant LLM calls<br>• Enables reusing `NeonArtistBioCard` directly in `DetailScreen` / Artist sheet<br>• Lets users edit/pin bios and aliases locally<br>• No Firestore schema/moderation burden | • Requires Room schema migration<br>• Local-only unless synced under `users/{uid}/artist_profiles` |
| **Option C: Dedicated Firestore `artists/{artistId}` Collection** *(Future / Community Scale)* | First-class shared `Artist` collection in Firestore + `artistIds: List<String>` foreign keys on `Spot`. | • Shared community-curated artist wiki<br>• Canonical identity across all users and packs | • Significant migration of `Spot`, `BackupWrapper`, `shared_packs`, `public_spots`, and `firestore.rules`<br>• Requires moderation/ownership rules (who can edit a public artist profile?)<br>• Premature for current product stage |

---

## 5. Recommended Path Forward for Issue #84

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
