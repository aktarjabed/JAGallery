# JAGallery

JAGallery is a full-featured, production-ready gallery application for Android, built with Kotlin and Jetpack Compose.

## Features

### Gallery
- **Real-time Updates:** The gallery automatically updates when new photos are taken or screenshots are saved.
- **Paging:** Photos are loaded in chunks for a smooth and performant user experience, even with large photo libraries.
- **Thumbnail Caching:** Smooth scrolling with cached thumbnails.
- **Smart Albums:** Automatically categorizes photos into albums like "Camera", "Screenshots", "WhatsApp", and "Downloads".
- **AI Image Tagging:** Uses a TensorFlow Lite model to automatically tag images.

### Image Viewer
- **Enhanced Zoom:** Pinch-to-zoom with gesture controls and reset functionality.
- **Image Info:** A bottom sheet displays detailed image metadata, including dimensions, date taken, and file size.
- **Share:** Share images directly from the viewer.

### Image Editor
- **Advanced Adjustments:** Sliders for brightness, contrast, and saturation.
- **Professional Filters:** A collection of filters including B&W, Sepia, Vivid, Warm, and Cool.
- **Transform Tools:** Rotate (left/right) and Flip (horizontal/vertical).
- **Smart Cropping:** Multiple aspect ratios (square, landscape, portrait, etc.).

### Deletion System
- **Recycle Bin:** Deleted photos are moved to a recycle bin for a 30-day recovery period.
- **Secure Deletion:** Option to overwrite files before deletion for enhanced privacy.
- **Automated Cleanup:** A `WorkManager` automatically cleans up the recycle bin daily.
- **Recycle Bin Dashboard:** A dedicated screen to view and manage the contents of the recycle bin.

### Security
- **Secure Vault:** A secure, encrypted vault for storing private photos, protected with biometric authentication.

### Settings
- **Persistent Settings:** User preferences for features like auto-cleanup and secure deletion are saved using `DataStore`.
