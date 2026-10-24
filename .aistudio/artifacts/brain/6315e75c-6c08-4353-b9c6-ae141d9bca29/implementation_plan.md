# JAGallery — Build Recovery, Toolchain Upgrade & Roadmap Completion Plan

This plan resolves the Gradle JDK toolchain build failure, upgrades and aligns the Android build stack with the JDK 21 runtime environment, refines the Jetpack Compose Material 3 user experience, and completes the remaining roadmap capabilities in native Android Kotlin.

## User Review & Critical Decisions

> [!IMPORTANT]
> Below is the summary of your confirmed preferences from our clarification step, which govern the scope and technical execution of this plan:

- **Confirmed Decision 1 — Comprehensive Scope ("Do All")**: Resolve all Gradle/toolchain build failures, polish the existing Jetpack Compose screens, and implement the remaining gallery roadmap features.
- **Confirmed Decision 2 — Full Toolchain & Build Stack Alignment**: Upgrade and align the Gradle daemon JVM toolchain (JDK 21), Android Gradle Plugin (AGP), Kotlin, KSP, and Hilt/Room configurations, and wire the container debug keystore signing configuration.
- **Confirmed Decision 3 — Strict Native Kotlin Preservation**: Retain 100% native Android Kotlin and Jetpack Compose architecture without introducing external languages or web wrappers.

---

## 1. Overview & Core Concept

- **What It Does**: JAGallery is a privacy-first, high-performance Android gallery and media organizer built with Kotlin, Jetpack Compose, Material 3, Room, Hilt, Media3 ExoPlayer, Coil, OsmDroid, and Google ML Kit.
- **Target Audience / Persona**: Android users seeking a fast, local-first photo and video gallery with smart organization (OCR text search, ML labeling, perceptual duplicate detection, EXIF editing, and an AES-256-GCM biometric vault) without cloud bloat.
- **Key Value**: Restores clean, reproducible APK compilation in the JDK 21 environment while completing missing roadmap capabilities—including Advanced EXIF editing, Video Mute & Frame Extraction, People/Portrait Smart Grouping, and high-speed Full-Text Search (FTS)—within a refined Material 3 interface.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. **Albums & Timeline Grid Flow**: Launch into responsive album folders or timeline-grouped media grids (`Today`, `Yesterday`, `Month`) with smooth multi-selection, batch operations, and quick filtering.
  2. **Fullscreen Media Viewer & Metadata Flow**: Swipe through photos and videos with pinch-to-zoom, slideshow playback, and a bottom metadata sheet featuring an interactive **EXIF Editor** (edit capture date/time, camera model, description, GPS coordinates, and strip sensitive EXIF data).
  3. **Video Playback & Studio Flow**: Play videos via Media3 ExoPlayer with one-tap audio mute/unmute, trim video ranges, **mute audio track on export**, and **extract high-resolution still frames** directly to the gallery.
  4. **Smart Discovery & People/OCR Search Flow**: Query photos using natural language or instant full-text search across filenames, ML labels, and OCR text, plus browse automatic **People & Portraits** smart albums powered by ML Kit face detection.
  5. **Privacy & Duplicate Cleanup Flow**: Lock sensitive media inside the AES-256-GCM biometric vault and review exact (SHA-256) or visually similar (64-bit dHash) duplicates for batch cleanup.

- **Visual Identity & Theme**:
  - *Aesthetic Direction*: Refined Editorial Dark & Adaptive Material You—clean spatial hierarchy that puts photography first with translucent glassmorphic overlays in fullscreen viewers and elevated tonal surfaces in organization views.
  - *Color Palette & Mood*: Dynamic Material 3 color scheme (`MaterialTheme.colorScheme`) supporting Android 12+ dynamic wallpaper extraction alongside a high-contrast custom fallback palette (`surfaceContainerLow`, `primaryContainer`, `tertiary` accents for smart AI badges).
  - *Typography & Hierarchy*: Crisp Material 3 typographic scale (`headlineMedium` for album headers, `titleMedium` for section dates, `labelMedium` with tabular spacing for EXIF badges and video timestamps).
  - *Component Styling & Layout*: Edge-to-edge layouts respecting `WindowInsets.safeDrawing` and `WindowInsets.navigationBars`, 48dp minimum interactive touch targets, expressive empty states, and spring-physics transitions.

- **Interactive Feedback & Motion**:
  - Smooth shared-feel transitions between grid thumbnails and fullscreen preview.
  - Real-time progress indicators during ML indexing, duplicate hashing, EXIF saving, and video frame/mute export with clear snackbar/toast confirmation.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: JDK 21 Toolchain & Modern AGP/Kotlin/KSP Alignment**
  - *Chosen Approach*: Update the Gradle daemon JVM configuration from JDK 17 to JDK 21, align Java/Kotlin compilation targets with the container's JDK 21 Temurin toolchain, update Gradle/AGP/Kotlin/KSP versions to compatible toolchain versions, and configure `debugConfig` signing with the root `debug.keystore`.
  - *Why*: Eliminates the `No defined toolchain download url for LINUX on x86_64 architecture` fatal Gradle daemon failure and ensures KSP annotation processors (Hilt and Room) compile cleanly without version skew.
  - *Alternatives Considered*: Keeping `toolchainVersion=17` fails immediately because external JDK provisioning is disabled in the build container.

- **Decision 2: On-Device ML Kit Face Detection & Smart Album Clustering**
  - *Chosen Approach*: Integrate on-device face/portrait detection alongside the existing ML Kit Image Labeling and Text Recognition pipeline, persisting semantic tags in Room for instant filtering and a dedicated People & Portraits smart category.
  - *Why*: Preserves JAGallery's 100% offline, privacy-first guarantee while completing Phase 3 of the roadmap.
  - *Alternatives Considered*: Cloud-based facial recognition APIs were rejected to respect user privacy and offline operation.

- **Decision 3: Non-Destructive Video Mute Export & Frame Extraction via MediaMuxer / MediaMetadataRetriever**
  - *Chosen Approach*: Extend the native Android `MediaExtractor` / `MediaMuxer` video trimming pipeline with an optional audio-track stripping toggle (Video Mute) and `MediaMetadataRetriever.getFrameAtTime()` high-resolution JPEG export (Video Frame Extraction).
  - *Why*: Uses zero-bloat native Android multimedia APIs in Kotlin for fast, lossless stream muxing without re-encoding video frames.
  - *Alternatives Considered*: Bundling FFmpeg native binaries would bloat APK size and violate no-NDK container build constraints.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

- **Architecture & Component Diagram**:

```
┌─────────────────────────────────────────────────────────────────────────┐
│                   Jetpack Compose UI Layer (Material 3)                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  ┌─────────────┐  │
│  │ Albums &     │  │ Fullscreen   │  │ Photo/Video  │  │ Smart Search│  │
│  │ Media Grid   │  │ Viewer & EXIF│  │ Editor Studio│  │ & Duplicates│  │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘  └──────┬──────┘  │
└─────────┼─────────────────┼─────────────────┼─────────────────┼─────────┘
          │                 │                 │                 │
          ▼                 ▼                 ▼                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                Hilt ViewModels & Reactive StateFlow Layer               │
│  ┌──────────────────┐  ┌──────────────────┐  ┌───────────────────────┐  │
│  │ Albums / Grid /  │  │ Viewer / EXIF /  │  │ Search / Smart Albums │  │
│  │ Vault ViewModels │  │ Editor ViewModels│  │ / Duplicate ViewModels│  │
│  └────────┬─────────┘  └────────┬─────────┘  └───────────┬───────────┘  │
└───────────┼─────────────────────┼────────────────────────┼──────────────┘
            │                     │                        │
            ▼                     ▼                        ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                     Domain & Repository Orchestration                   │
│  ┌────────────────────┐ ┌────────────────────┐ ┌─────────────────────┐  │
│  │ MediaRepository &  │ │ ExifEditor & Video │ │ SmartIntelligence & │  │
│  │ VaultRepository    │ │ Trimmer/Extractor  │ │ DuplicateDetector   │  │
│  └─────────┬──────────┘ └─────────┬──────────┘ └──────────┬──────────┘  │
└────────────┼──────────────────────┼───────────────────────┼─────────────┘
             │                      │                       │
             ▼                      ▼                       ▼
┌────────────────────────┐ ┌────────────────────┐ ┌───────────────────────┐
│ Android MediaStore &   │ │ Room Database      │ │ Google ML Kit &       │
│ ExifInterface / Muxer  │ │ (Entities + FTS)   │ │ Keystore AES-256-GCM  │
└────────────────────────┘ └────────────────────┘ └───────────────────────┘
```

- **Data Model & State**:
  - **Media & Auxiliary Entities**: `MediaEntity` (favorites, timestamps), `HiddenMediaEntity`, `TrashMediaEntity`, `VaultMediaEntity` (encrypted file metadata), `FileHashEntity` (SHA-256 and 64-bit perceptual dHash), and `SmartTagEntity` (OCR text, ML labels, face/portrait tags, and FTS search tokens).
  - **EXIF Metadata Model**: Structured read/write model supporting `TAG_DATETIME`, `TAG_MAKE`, `TAG_MODEL`, `TAG_IMAGE_DESCRIPTION`, `TAG_ARTIST`, `TAG_COPYRIGHT`, GPS latitude/longitude, and a one-tap privacy sanitizer that strips location and device serial tags.

- **Interactive Component & State Mapping**:
  - **Build & Signing Pipeline**: Align Gradle daemon JVM to 21, configure `debugConfig` signing against `debug.keystore`, and verify all Hilt/Room KSP code generation and unit tests pass.
  - **Fullscreen Viewer & EXIF Editor Sheet**: Viewing an image opens the Metadata bottom sheet with an "Edit EXIF" mode and "Strip Private EXIF" action that writes changes via `ExifInterface` (with Scoped Storage `RecoverableSecurityException` handling) and refreshes the UI state.
  - **Video Studio (Trim, Mute, Extract Frame)**: In the video viewer and trim sheet, users can toggle "Mute Audio" when exporting a trimmed or full clip via `MediaMuxer` (skipping the audio track) or tap "Capture Frame" to extract the current playback timestamp via `MediaMetadataRetriever` into `MediaStore.Images`.
  - **Smart Search, FTS & People Grouping**: The Search screen indexes and queries OCR text, ML image labels, and face/portrait detections with instant filter chips (`People & Selfies`, `Documents & Text`, `Receipts`, `Screenshots`, `Favorites`, `Videos`).
