# Tag Spotter - Project Brief & Specifications

An Android application for city walkers to capture, tag, and geolocate graffiti art.

```
+-----------------------------------------------------------------+
|                           Tag Spotter                           |
+-----------------------------------------------------------------+
|                                                                 |
|   [ Bottom Navigation Bar ]                                     |
|   ├── [ Gallery Tab ]     --> Grid of tagged photos            |
|   ├── [ Map Tab ]         --> OSM Map with pins of spots        |
|   └── [ Camera Tab ]      --> Capture photo & auto-locate       |
|                                                                 |
|   [ Capture Screen ] ---> [ Tagging / Detail Screen ]            |
|                           ├── Auto GPS Location (refreshable)   |
|                           ├── Date & Time                       |
|                           ├── Description Input                 |
|                           └── Quick Tags (#mural, #stencil)     |
|                               + Custom Tags                     |
|                                                                 |
+-----------------------------------------------------------------+
```

## 1. Project Goal
The goal of **Tag Spotter** is to create a lightweight, responsive, and visually striking local-first Android application that allows street art enthusiasts and city walkers to document graffiti locations offline.

---

## 2. Technical Specifications

### Architecture & UI Vibe
*   **Theme:** Custom Urban Dark Theme featuring a deep charcoal background and electric neon accents (lime green, cyan, hot pink).
*   **Navigation:** Modern Bottom Navigation Bar switching between:
    1.  **Gallery Screen:** A visual dashboard grid of saved spots.
    2.  **Map Screen:** Interactive map displaying all captured tags as interactive pins.
    3.  **Camera Screen:** Live camera preview for capturing new spots.

### 📸 Camera, Gallery Import & Location Capture
*   **Camera Integration:** High-performance preview and capture using **CameraX**.
*   **Gallery Import (Zero Permissions):** Integrate the modern Android **Photo Picker** (`PickVisualMedia`) to let users select existing photos from their device's gallery without requiring any storage permissions.
*   **Auto-Location (Camera):** Silently queries GPS coordinates on image capture using Google Play Services Location API.
*   **EXIF Location Extraction (Gallery):** For imported photos, the app reads the image's EXIF metadata to extract embedded GPS coordinates. 
*   **GPS Fallback & Warning:** If GPS coordinates are missing or slow to resolve, the app uses the best-guess last-known location and shows a "GPS Weak / Missing" warning on the tagging screen.
*   **Location Correction:** The tagging/details screen offers a "Select Location on Map" button that opens a temporary map where the user can tap/drag a pin to adjust the coordinates.

### 🏷️ Spot Tagging & Details Screen
*   **Auto-generated Metadata:** Date and Time of capture, GPS Coordinates.
*   **User Inputs:**
    *   **Description:** Free-text description field.
    *   **Quick Tags:** Predefined buttons to add popular tags (e.g., `#mural`, `#stencil`, `#throwup`, `#pasteup`, `#sticker`).
    *   **Custom Tags:** Free-form text input to append custom tags.
    *   **Recent Tag Suggestions:** A dynamic row of "Recent Custom Tags" (retrieved from previous database entries) for single-tap reuse.

### 💾 Local Database & Storage
*   **Database Schema (One-to-Many):** SQLite database managed by **Room** with three related tables to support historical tracking:
    *   `spots`: stores coordinates, initial description, tags list, category, date created, and status.
    *   `spot_images`: stores foreign key `spotId`, image file path, and capture timestamp.
    *   `spot_notes`: stores foreign key `spotId`, text note, and timestamp to document observations over time.
*   **Image Storage & Optimization:** Images are automatically scaled down to a maximum boundary of 1080p and compressed as JPEGs (80% quality) to save device space (reducing file sizes to ~300KB).
*   **Lifecycle:** Deleting a spot cascadingly removes all associated image files, notes, and database records. Mark as "Erased" updates the status flag without deleting any data.

### 🗺️ Map Integration
*   **Engine:** **OpenStreetMap (OSMDroid)** or **MapLibre** for a fully offline-capable, API-key-free developer experience.
*   **Interaction:** Tapping pins displays details of the spot (thumbnail, tags, description) and lets the user jump to the full Details Screen.
*   **Status Indicators:** Active spots display vibrant neon pins. Erased/gone spots display muted gray/faded pins to preserve their historical presence while indicating they are no longer visible on the street.

---

## 3. Technology Stack Summary
*   **Language:** Kotlin
*   **UI:** Jetpack Compose with Material 3 (customized styling)
*   **Database:** Room (SQLite)
*   **Image Loading:** Coil
*   **Camera:** Jetpack CameraX
*   **Map:** OSMDroid/MapLibre (Compose wrapper)

---

## 4. Future Features
*   **Artist & Style Discovery (Gemini Multimodal API):** Integrate Gemini 1.5 Flash using a Google AI Studio API Key to analyze captured photos, automatically identify potential artists, explain the graffiti style, and suggest tags.
*   **User Login & Cloud Sharing (Firebase):** 
    *   Integrate **Firebase Authentication** (Google / Email Login) for user profiles.
    *   Allow users to toggle between "Private" and "Shared" status for their captured spots.
    *   Use **Cloud Firestore** and **Firebase Storage** to share public tags and images.
    *   Display other users' photos of the same graffiti spot based on proximity queries (matching GPS coordinates).
    *   **Feedback & Moderation:** Integrate upvote/downvote mechanics for community ranking, and a "Flag as Offensive" feature to report inappropriate content (automatically hiding images once reported).
*   **Optimized Walking Routes:** Integrate OSRM or GraphHopper to generate and display the most efficient walking path on the map connecting selected graffiti spots.
*   **Multi-Category Expansion & Filtering:** 
    *   Support documenting other urban elements (sculptures, public places, trees, tree types, and architecture).
    *   Add a category selection filter to both the Gallery grid and Map screens to easily toggle or search specific types of spots.
*   **Historical Change Tracking / Spot Evolution:**
    *   Allow users to add new photos to an existing spot over time (e.g., to record updated graffiti layers, changing sculpture conditions, or trees across seasons).
    *   View all photos of a spot chronologically (carousel or time-lapse slider) to see visual changes over time.
*   **Cloud Synchronization:** Implement secure backup and cross-device sync of private spots and metadata.

---

## 5. Next Steps
1.  Initialize the Android application template.
2.  Add Room Database entity, DAO, and Repository classes.
3.  Implement CameraX preview and GPS capturing functionality.
4.  Design the customized Urban Dark Theme and layout navigation.
