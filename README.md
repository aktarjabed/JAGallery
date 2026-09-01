# JAGallery - Advanced Android Gallery Application

JAGallery is a modern, high-performance Android gallery application built with Jetpack Compose, Kotlin Coroutines & Flow, Material 3, Room, and Hilt.

[Implementation Status → IMPLEMENTATION_MATRIX.md](IMPLEMENTATION_MATRIX.md)

## Key Features

- **Album View**: Displays media grouped by folders/albums with item counts and cover thumbnails.
- **Media Grid**: Responsive 3-column thumbnail grid with multi-selection mode (long press to select, select all, batch delete, batch share).
- **Fullscreen Media Viewer**:
  - **Image Viewing**: High-definition image viewer with pinch-to-zoom (1x - 5x) and pan gestures.
  - **Slideshow**: Auto-advance through images every 3 seconds.
  - **Video Playback**: Embedded Media3 ExoPlayer for videos, automatically pausing when swiped away or off-screen.
  - **Immersive View**: Single tap to toggle top bar and controls overlay.
  - **Favorites Persistence**: One-tap toggling of favorite status backed by Room database.
  - **Metadata Info Sheet**: Displays file details including file name, MIME type, date added, album name, and EXIF metadata.
  - **Single & Batch Sharing**: Native Android sharing integration for single or multiple selected media files.
  - **Open With**: Launch media in external applications using secure Content URIs.
  - **Set Wallpaper**: Quickly set images as the device wallpaper.
- **Search & Filtering**: Search media files by name in real-time.
- **Image Editing**: Interactive Image Editor supporting freeform crop, rotation, flip horizontal/vertical, brightness, contrast, and saturation.
- **Video Editing**: Video trimming pipeline with bottom sheet UI.
- **Trash Management**: View deleted media and empty trash to permanently delete items. (MediaStore trash/restore integration)
- **Hidden Media**: Hide sensitive media files and view or unhide them in a dedicated section.
  - *Note: Hidden media is only filtered from this app's view. Files remain in device storage and are visible to other apps and file managers.*
- **Map View**: OsmDroid-based map view with marker rendering using EXIF GPS metadata.
- **Duplicate Detection**: Find exact duplicate media files via SHA-256 hashing.
- **MediaStore & Handling**:
  - Full support for Scoped Storage (API 30+) using `MediaStore.createDeleteRequest` and `createTrashRequest`.
  - Batch operation handling with `PendingIntent` chunks.
  - Copy and Move operations preserving Room metadata.

## Architecture

- **MVVM Pattern**: ViewModels manage UI state using Kotlin StateFlow.
- **Dependency Injection**: Powered by Google Hilt (`@HiltViewModel`, `@AndroidEntryPoint`).
- **Database**: Room Database handles local persistence for favorites and hidden states.
- **Media Loading**: Coil image loading library with `VideoFrameDecoder` support.
- **Navigation**: Jetpack Compose Navigation with type-safe route parameter passing.

## Roadmap & Upcoming Features

- **Secure Hidden Vault**: Biometric authentication and private storage encryption for hidden files.
- **Duplicate Cleanup**: Streamlined UI for deleting duplicates and managing storage.
- **Persistent Hash Cache**: Speed up duplicate scans with a persistent hash store.
- **Advanced Editor Controls**: Highlights, shadows, and sharpness.
- **Robust Multi-volume Management**: Advanced destination picking for copy/move operations, and proper metadata handling.

## Project Setup & Testing

### Building the Project
```bash
./gradlew assembleDebug
```

### Running Unit Tests
```bash
./gradlew testDebugUnitTest
```

## Releases

JAGallery is distributed exclusively via GitHub Releases. The application is designed to be a personal, high-performance gallery app without Google Play Services bloat or analytics.

You can download the latest APK from the [Releases page](https://github.com/aktarjabed/jagallery/releases).
