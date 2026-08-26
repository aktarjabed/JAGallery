# JAGallery - Advanced Android Gallery Application

JAGallery is a modern, high-performance Android gallery application built with Jetpack Compose, Kotlin Coroutines & Flow, Material 3, Room, and Hilt.

[Implementation Status → IMPLEMENTATION_MATRIX.md](IMPLEMENTATION_MATRIX.md)

## Key Features

- **Album View**: Displays media grouped by folders/albums with item counts and cover thumbnails.
- **Media Grid**: Responsive 3-column thumbnail grid with multi-selection mode (long press to select, select all, batch delete, batch share).
- **Fullscreen Media Viewer**:
  - **Image Viewing**: High-definition image viewer with pinch-to-zoom (1x - 5x) and pan gestures.
  - **Video Playback**: Embedded Media3 ExoPlayer for videos, automatically pausing when swiped away or off-screen.
  - **Immersive View**: Single tap to toggle top bar and controls overlay.
  - **Favorites Persistence**: One-tap toggling of favorite status backed by Room database.
  - **Metadata Info Sheet**: Displays file details including file name, MIME type, date added, album name, and Uri.
  - **Single & Batch Sharing**: Native Android sharing integration for single or multiple selected media files.
- **Search & Filtering**: Search media files by name in real-time.
- **Image Editing**: Basic image editor supporting cropping operations.
- **Trash Management**: View deleted media and empty trash to permanently delete items.
- **Hidden Media**: Hide sensitive media files and view or unhide them in a dedicated section.
  - *Note: Hidden media is only filtered from this app's view. Files remain in device storage and are visible to other apps and file managers.*
- **MediaStore & Deletion Handling**:
  - Uses `MediaStore.createDeleteRequest` for scoped storage on API 30+ / Android 11+.
  - Direct ContentResolver deletion fallback for API < 30.

## Architecture

- **MVVM Pattern**: ViewModels manage UI state using Kotlin StateFlow.
- **Dependency Injection**: Powered by Google Hilt (`@HiltViewModel`, `@AndroidEntryPoint`).
- **Database**: Room Database handles local persistence for favorites (`favorites` table).
- **Media Loading**: Coil image loading library with `VideoFrameDecoder` support.
- **Navigation**: Jetpack Compose Navigation with type-safe route parameter passing.

## Project Setup & Testing

### Building the Project
```bash
./gradlew assembleDebug
```

### Running Unit Tests
```bash
./gradlew test
```

The unit test suite covers `MediaRepository`, `AlbumsViewModel`, `GridViewModel`, `ViewerViewModel`, `SearchViewModel`, and `FavoritesViewModel`.
