# AI Augmentation Security & Firebase Integration Plan

This implementation plan covers two major phases for securing AI integration in **DetailScreen** aligned during our `/grill-me` interactive session:
1. **Option 3 (Graceful UI Fallback)**: Automatically detect API key availability and hide the sparkle and Wikipedia search icons from the UI when keys are not configured or supported.
2. **Option 1 (Firebase AI Logic)**: Securely manage the Gemini API key on Google Cloud using Firebase Vertex AI for **Android** and **iOS** client SDKs, completely omitting Web/Wasm.

---

## Shared Design Decisions (Aligned in Grill-Me)

1. **Dependency Injection (Koin)**: We will use the established `expect val platformAiModule: Module` inside the `core:ai` module, ensuring consistent DI architecture with the database, photo, and settings modules in the codebase.
2. **Settings Screen Behavior**: On Android and iOS, we will hide the "Gemini API Key" input field since Firebase manages the key. On Desktop/JVM, the key field remains visible to allow a manual Ktor-based key fallback.
3. **Firebase App Check**: We will integrate Firebase App Check using **Debug Providers** for local development and testing, and provide explicit setup documentation for migrating to production providers (Play Integrity for Android, App Attest/DeviceCheck for iOS).

---

## Technical Feasibility & KMP Architecture

To support Android and iOS cleanly, we will leverage **Dependency Injection (Koin)**:
- We will define the platform-specific AI service implementations using native languages (Kotlin on Android, Swift on iOS).
- On iOS, we will implement the service directly in Swift using the official Swift `FirebaseVertexAI` library and inject it into Koin during the iOS app startup. This avoids having to write complex Swift package bindings inside Kotlin/Native.
- We will exclude Web/Wasm from AI access, gracefully hiding the features using the UI fallback (Option 3).
- Desktop/JVM will continue to run with the Ktor fallback using the manually configured key in settings, hiding the features if no key is supplied.

---

## Proposed Changes

### Component 1: Core Settings & UI Fallback (Option 3)

We will update the KMP core/shared layer to monitor API key availability in real-time. If no API key is injected and the user hasn't supplied their own key in Settings, we'll mark AI as unavailable and hide all UI entry points.

#### [MODIFY] [DetailViewModel.kt](file:///Users/maia/workspace/maiatoday/tag-spotter/feature/detail/src/commonMain/kotlin/net/maiatoday/tagspotter/feature/detail/DetailViewModel.kt)
- Define a new `isAiAugmentationAvailable: StateFlow<Boolean>` computed flow:
  ```kotlin
  val isAiAugmentationAvailable: StateFlow<Boolean> = combine(
      settingsRepository.artistRecognitionEnabled,
      settingsRepository.geminiApiKey
  ) { enabled, userKey ->
      enabled && (secretsProvider.getGeminiApiKey().isNotEmpty() || userKey.isNotEmpty())
  }.stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = false
  )
  ```

#### [MODIFY] [DetailScreen.kt](file:///Users/maia/workspace/maiatoday/tag-spotter/feature/detail/src/commonMain/kotlin/net/maiatoday/tagspotter/feature/detail/DetailScreen.kt)
- Collect `isAiAugmentationAvailable` from the ViewModel:
  ```kotlin
  val isAiAugmentationAvailable by viewModel.isAiAugmentationAvailable.collectAsStateWithLifecycle()
  ```
- Pass `isAiAugmentationAvailable` down to `DetailMetadataCard` and `DetailNotesSection` instead of using raw `isArtistRecognitionEnabled` directly.

#### [MODIFY] [DetailMetadataCard.kt](file:///Users/maia/workspace/maiatoday/tag-spotter/feature/detail/src/commonMain/kotlin/net/maiatoday/tagspotter/feature/detail/DetailMetadataCard.kt)
- Use `isAiAugmentationAvailable` to show or hide the sparkle/AutoAwesome (`onIdentifyArtist`) and SearchLens (`onSearchImage`) icon buttons.

#### [MODIFY] [SettingsScreen.kt](file:///Users/maia/workspace/maiatoday/tag-spotter/feature/settings/src/commonMain/kotlin/net/maiatoday/tagspotter/feature/settings/SettingsScreen.kt)
- Dynamically hide the "Gemini API Key" input text field on Android and iOS targets using platform-specific conditions:
  ```kotlin
  val isMobile = platformHelper.isAndroid || platformHelper.isIos
  if (!isMobile) {
      // Show Gemini API Key input field
  }
  ```

---

### Component 2: Android Firebase AI Logic (Option 1)

#### [MODIFY] [libs.versions.toml](file:///Users/maia/workspace/maiatoday/tag-spotter/gradle/libs.versions.toml)
- Define Firebase BOM and Firebase AI SDK dependencies:
  ```toml
  [versions]
  firebaseBom = "33.1.2"
  googleServicesPlugin = "4.4.2"
  
  [libraries]
  firebase-bom = { group = "com.google.firebase", name = "firebase-bom", version.ref = "firebaseBom" }
  firebase-ai = { group = "com.google.firebase", name = "firebase-ai" }
  firebase-appcheck-debug = { group = "com.google.firebase", name = "firebase-appcheck-debug" }
  ```

#### [MODIFY] [build.gradle.kts (project root)](file:///Users/maia/workspace/maiatoday/tag-spotter/build.gradle.kts)
- Apply Google Services Gradle plugin.

#### [MODIFY] [build.gradle.kts (androidApp)](file:///Users/maia/workspace/maiatoday/tag-spotter/androidApp/build.gradle.kts)
- Apply `com.google.gms.google-services` plugin.
- Add dependencies for Firebase App Check Debug.

#### [NEW] [google-services.json](file:///Users/maia/workspace/maiatoday/tag-spotter/androidApp/google-services.json)
- Add Firebase Android configuration file.

#### [NEW] [AndroidFirebaseAiService.kt](file:///Users/maia/workspace/maiatoday/tag-spotter/core/ai/src/androidMain/kotlin/net/maiatoday/tagspotter/core/ai/AndroidFirebaseAiService.kt)
- Implement `AiRecognitionService` interface using the native Android Firebase AI Logic SDK:
  ```kotlin
  import com.google.firebase.vertexai.vertexAI
  import com.google.firebase.vertexai.type.content
  
  class AndroidFirebaseAiService(
      private val photoProcessor: PhotoProcessor
  ) : AiRecognitionService {
      private val model = Firebase.vertexAI.generativeModel("gemini-2.5-flash")
      // Implement identifyArtist and searchWikipediaForSpot using the SDK
  }
  ```

---

### Component 3: iOS Firebase AI Logic (Option 1)

#### [MODIFY] [iosApp Xcode project settings](file:///Users/maia/workspace/maiatoday/tag-spotter/iosApp/iosApp.xcodeproj)
- Add `FirebaseVertexAI` and `FirebaseAppCheck` Swift Packages via Xcode / SPM.

#### [NEW] [GoogleService-Info.plist](file:///Users/maia/workspace/maiatoday/tag-spotter/iosApp/iosApp/GoogleService-Info.plist)
- Add Firebase iOS configuration file to the Xcode project.

#### [NEW] [SwiftFirebaseAiService.swift](file:///Users/maia/workspace/maiatoday/tag-spotter/iosApp/iosApp/SwiftFirebaseAiService.swift)
- Implement the `AiRecognitionService` interface in Swift using `FirebaseVertexAI`:
  ```swift
  import FirebaseVertexAI
  import Shared
  
  class SwiftFirebaseAiService: NSObject, AiRecognitionService {
      private let model = VertexAI.vertexAI().generativeModel(modelName: "gemini-2.5-flash")
      
      func identifyArtist(...) async throws -> AiSuggestion? {
          // Implement using Swift Firebase SDK
      }
      
      func searchWikipediaForSpot(...) async throws -> String? {
          // Implement using Swift Firebase SDK
      }
  }
  ```

#### [MODIFY] [iOSApp.swift](file:///Users/maia/workspace/maiatoday/tag-spotter/iosApp/iosApp/iOSApp.swift)
- Initialize Firebase and App Check during startup (using AppCheckDebugProviderFactory).
- Register `SwiftFirebaseAiService` into Koin:
  ```swift
  KoinKt.doInitKoin { builder in
      builder.modules([
          module {
              single(factory: { SwiftFirebaseAiService() })
          }
      ])
  }
  ```

---

## Backend Provisioning (Required)

To enable Firebase AI Logic in your Google Cloud backend, we must provision it:
1. Run the initialization tool:
   ```bash
   npx -y firebase-tools@latest init ailogic
   ```
2. Enable **Firebase App Check** in the Firebase Console:
   - Configure **Play Integrity** for Android.
   - Configure **App Attest / DeviceCheck** for iOS.

---

## Verification Plan

### Automated Tests
- Modify `DetailViewModelTest.kt` to verify that `isAiAugmentationAvailable` is correctly calculated:
  - Verify that when `geminiApiKey` is missing AND no local key is available, `isAiAugmentationAvailable` is false.
  - Verify that when `geminiApiKey` or local key is available, and `artistRecognitionEnabled` is true, `isAiAugmentationAvailable` is true.

### Manual Verification
1. Run the Android app with empty local key and verified settings, verify that the sparkle/AutoAwesome icon is hidden.
2. Enter a manual key in Settings, verify that the sparkle icon appears immediately.
3. Verify compilation on iOS and Android with the Firebase configuration injected.
