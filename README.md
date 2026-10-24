# JAGallery - Advanced Android Gallery Application

<p align="center">
  <img src="logo.png" alt="JAGallery Logo" width="120"/>
</p>

<p align="center">
  <a href="https://github.com/aktarjabed/JAGallery/releases/latest"><img src="https://img.shields.io/github/v/release/aktarjabed/JAGallery?label=Download%20APK&logo=android&style=for-the-badge" alt="Download APK"/></a>
  <img src="https://img.shields.io/badge/Min%20SDK-31%20(Android%2012)-brightgreen?style=for-the-badge&logo=android" alt="Min SDK 31"/>
  <img src="https://img.shields.io/badge/Target%20SDK-36-blue?style=for-the-badge&logo=android" alt="Target SDK 36"/>
  <img src="https://img.shields.io/badge/Language-Kotlin-purple?style=for-the-badge&logo=kotlin" alt="Kotlin"/>
</p>

JAGallery is a modern, high-performance Android gallery application built with Jetpack Compose, Kotlin Coroutines & Flow, Material 3, Room, and Hilt.

> **📦 [Download the latest APK →](https://github.com/aktarjabed/JAGallery/releases/latest)**

[Implementation Status → IMPLEMENTATION_MATRIX.md](IMPLEMENTATION_MATRIX.md)

## Key Features

- **Album View**: Displays media grouped by folders/albums with item counts and cover thumbnails.
- **Media Grid**: Responsive 3-column thumbnail grid with multi-selection mode (long press to select, select all, batch delete, batch share).
- **System-Wide Explicit Theme Toggle**:
  - Switch seamlessly between **System Default**, **Light**, and **Dark** modes via a Material 3 segmented selector or a quick **Dark Mode** toggle switch in Settings, plus optional **Dynamic Color (Material You)** support on Android 12+.
  - Synchronizes status bar and navigation bar icon contrast (`WindowInsetsControllerCompat`) and `AppCompatDelegate` night mode across the entire application.
- **Fullscreen Media Viewer**:
  - **Image Viewing**: High-definition image viewer with pinch-to-zoom (1x - 5x) and pan gestures.
  - **Slideshow**: Auto-advance through images every 3 seconds.
  - **Video Playback**: Embedded Media3 ExoPlayer for videos with live **Mute / Unmute** audio toggle, single-tap **Capture Video Frame** (exports a high-resolution JPEG snapshot to `Pictures/JAGallery`), and automatic pause when swiped away or off-screen.
  - **Immersive View**: Single tap to toggle top bar and controls overlay.
  - **Favorites Persistence**: One-tap toggling of favorite status backed by Room database.
  - **Interactive Metadata Info Sheet & EXIF Editor**: Displays file details and EXIF metadata with an inline **EXIF Editor** (edit Capture Date/Time, Camera Make/Model, Description, Artist, and GPS coordinates) and a 1-tap **Strip GPS & Private EXIF** sanitizer before sharing.
  - **Single & Batch Sharing**: Native Android sharing integration for single or multiple selected media files.
  - **Open With**: Launch media in external applications using secure Content URIs.
  - **Set Wallpaper**: Quickly set images as the device wallpaper.
- **Smart Search & Full-Text Filtering**:
  - Search media files by filename, album, **OCR-extracted text**, **ML semantic labels**, and **location tags** in real-time via Room reactive queries.
  - One-tap quick filter chips (`People & Selfies`, `Documents & OCR`, `Receipts`, `Screenshots`, `Favorites`, `Videos`) and on-demand ML Kit indexing with live progress.
- **Image Editing**: Interactive Image Editor supporting freeform crop, rotation, flip horizontal/vertical, brightness, contrast, and saturation.
- **Video Editing & Audio Stripping**: Lossless `MediaMuxer` video trimming pipeline with bottom sheet UI and an optional **Mute Audio** toggle to strip audio tracks on export.
- **Trash Management**: View deleted media and empty trash to permanently delete items. (MediaStore trash/restore integration with robust batch deletion tracking)
- **Hidden Media**: Hide sensitive media files and view or unhide them in a dedicated section.
  - *Note: Hidden media is only filtered from this app's view. Files remain in device storage and are visible to other apps and file managers.*
- **Map View**: Lifecycle-aware OsmDroid map view with marker rendering using EXIF GPS metadata.
- **Duplicate & Similar Detection**: Find exact duplicate media files via batched SHA-256 caching (safe for large libraries with >1,000 items), and detect visually similar photos (bursts/screenshots) using 64-bit Perceptual Hashing (dHash).
- **MediaStore & Handling**:
  - Full support for Scoped Storage (API 31+) using `MediaStore.createDeleteRequest`, `createTrashRequest`, and `createWriteRequest`.
  - Batch operation handling with `PendingIntent` chunks and strict success/failure tracking.
  - Copy and Move operations preserving Room metadata and avoiding complete data loss on partial failures.
- **Secure Hidden Vault**: Biometric authentication (`androidx.biometric`), AES-256-GCM Keystore encryption, auto-lock mechanism, zero-byte/partial decryption fault cleanup, and App-private encrypted storage.
- **Smart Intelligence**:
  - **OCR Text Extraction**: Google ML Kit `TextRecognition` scans documents, receipts, and screenshots for text, enabling full-text search.
  - **Image Labeling & People/Portrait Detection**: Google ML Kit `ImageLabeling` automatically categorizes photos (e.g., People & Portraits, Pets, Food, Landscapes, Documents).
  - **Smart Albums**: Automatically groups photos by semantic labels or detected text.
- **Natural Language Search**: Intent-based search to quickly query images by semantics, people/portraits, OCR text, dates, or source.
- **Configurable Trash Retention**: Policy-driven cleanup (7, 30, 60 days, or Never) executed via resilient idempotent `WorkManager` tasks.

## Tech Stack

| Category | Technology |
|---|---|
| **Language** | Kotlin 2.2 (JVM Target 21) |
| **UI** | Jetpack Compose + Material 3 (Dynamic Color + Explicit Light/Dark Mode) |
| **Architecture** | MVVM (ViewModel + StateFlow) |
| **DI** | Hilt / Dagger |
| **Database** | Room (Version 6) |
| **Image Loading** | Coil (with `VideoFrameDecoder`) |
| **Video Player & Editing** | Media3 ExoPlayer, `MediaExtractor` + `MediaMuxer`, `MediaMetadataRetriever` |
| **Maps** | OsmDroid |
| **ML / AI** | Google ML Kit (Image Labeling, Text Recognition) |
| **Security** | Android Keystore + AES-256-GCM, BiometricPrompt |
| **Background** | WorkManager |
| **Build** | Gradle 9.3.1 (Kotlin DSL), KSP, JDK 21 Toolchain |

## Threat Model (Secure Vault)

The Vault feature protects files using Android Keystore and AES-256-GCM.
- Media is encrypted using randomly generated streaming IVs and cannot be read without the specific device Keystore credentials.
- Vault items are strictly hidden from MediaStore.
- Upon viewing or sharing, temporary decrypted `content://` streams or cache files are generated inside the app's cache directory, verified for integrity (partial/failed decryptions are immediately purged), and proactively cleared on session timeout or lock events.

## Architecture

- **MVVM Pattern**: ViewModels manage UI state using Kotlin StateFlow.
- **Dependency Injection**: Powered by Google Hilt (`@HiltViewModel`, `@AndroidEntryPoint`).
- **Database**: Room Database handles local persistence for favorites, smart tags (OCR text, ML labels, location tags), file/perceptual hashes, and hidden states.
- **Media Loading**: Coil image loading library with `VideoFrameDecoder` support.
- **Navigation**: Jetpack Compose Navigation with URL-encoded string route parameter passing.

## Roadmap & Upcoming Features

- **Advanced Editor Controls**: Highlights, shadows, and sharpness.
- **Cloud-aware Albums**: Syncing logic and custom-album organization.

## Project Setup & Testing

### Prerequisites
- **JDK 21** — Required for compilation (`toolchainVersion=21` in `gradle/gradle-daemon-jvm.properties`). Install [Adoptium Temurin 21](https://adoptium.net/) or any OpenJDK 21 distribution.
- **Android SDK** — Install via [Android Studio](https://developer.android.com/studio) or the [command-line tools](https://developer.android.com/studio#command-line-tools-only).
- **`JAVA_HOME`** — Must point to your JDK 21 installation:
  ```powershell
  # Windows (PowerShell)
  [System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Eclipse Adoptium\jdk-21", "User")
  ```
  ```bash
  # macOS / Linux
  export JAVA_HOME=$(/usr/libexec/java_home -v 21)  # macOS
  export JAVA_HOME=/usr/lib/jvm/java-21-openjdk      # Linux (path may vary)
  ```

### Building the Project (Local APK)
You can generate a debug APK for testing directly on your device by running:
```bash
gradle assembleDebug
```
Once the build completes, the APK will be available at:
`app/build/outputs/apk/debug/app-debug.apk`

Transfer this file to your Android device to install and test the app.

### Building a Release APK
```bash
gradle assembleRelease
```
The signed release APK (with ProGuard/R8 minification) will be at:
`app/build/outputs/apk/release/app-release.apk`

### Running Unit Tests
```bash
gradle :app:testDebugUnitTest
```

## Download

JAGallery is distributed exclusively via GitHub Releases. No Google Play Services, no analytics, no bloat.

**📦 [Download the latest APK from Releases →](https://github.com/aktarjabed/JAGallery/releases/latest)**

You can also browse [all releases](https://github.com/aktarjabed/JAGallery/releases).

## License

This project is proprietary software. All rights reserved.
