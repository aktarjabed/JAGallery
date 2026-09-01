# Implementation Matrix

## Core Gallery

| Feature | Status | Actual Implementation | Remaining Work |
|---------|--------|-----------------------|----------------|
| Gallery/Grid | ✅ Implemented | Core grid/gallery works | |
| Image/Video Viewer | ✅ Implemented | Image viewer + Media3 video playback | |
| Multi-selection | ✅ Implemented | Shared selection architecture | |
| Metadata | ✅ Implemented | Complete EXIF parsing, copyable fields | |
| Slideshow | ✅ Implemented | Configurable 3-second delay, ignores videos | |
| Set wallpaper | 🐛 Implemented but broken | Implemented and permission present, but behavior may be inconsistent across OEMs | |
| Open With | ✅ Implemented | Intent-based sharing via secure content URI | |

## Storage/MediaStore

| Feature | Status | Actual Implementation | Remaining Work |
|---------|--------|-----------------------|----------------|
| Room persistence | ✅ Implemented | Auxiliary state, not a complete MediaStore metadata cache | |
| Multi-volume MediaStore | ⚠️ Partial | getVolumeName extracted for copy/move | Missing physical album identification via AlbumKey |
| Copy | ✅ Implemented | MediaStore destination creation + Complete Copy UX | |
| Move | ⚠️ Partial | Copy + source deletion | Transactional rollback missing for metadata orphans |
| Batch rollback | ❌ Missing | No transactional rollback or partial-failure state tracking | |
| Trash | ⚠️ Partial | MediaStore trash/restore integration | Handling of exact expiration tracking (DATE_EXPIRES) implemented but batch failures escalate |
| Restore | ⚠️ Partial | MediaStore trashed-item query/restore flow | |
| Rename individual media | ✅ Implemented | MediaStore rename via ContentResolver update | |
| External MediaStore synchronization | ✅ Implemented | MediaStore observing | |

## Organization

| Feature | Status | Actual Implementation | Remaining Work |
|---------|--------|-----------------------|----------------|
| Albums | ✅ Implemented | MediaStore/folder-based albums | |
| Sort/Filter | ✅ Implemented | Bottom-sheet based | |
| Create album | ✅ Implemented | Available through MediaStore folder creation | |
| Rename album | ⚠️ Partial | Batch move + delete intent flow (copy/delete rather than atomic rename) | |
| Timeline/date grouping | ✅ Implemented | Today/Yesterday/Month grid separators | |
| Favorites persistence | ✅ Implemented | | |

## Editing

| Feature | Status | Actual Implementation | Remaining Work |
|---------|--------|-----------------------|----------------|
| Basic image editing | ✅ Implemented | Rotate/brightness/contrast/saturation/flip | |
| Advanced photo editor | ⚠️ Partial | Interactive Free Crop works, preserves EXIF | Highlights/Shadows/Sharpness absent |
| Crop | ✅ Implemented | Interactive Crop overlay / Aspect ratio crop (Square/Free/Original) | |
| Flip horizontal/vertical | ✅ Implemented | Matrix postScale flip operations | |
| GIF-specific editing | ❌ Missing | | |
| Advanced EXIF editor | ❌ Missing | | |

## Video

| Feature | Status | Actual Implementation | Remaining Work |
|---------|--------|-----------------------|----------------|
| Video trimming | ✅ Implemented | Dedicated trimming/export pipeline | Validation of boundaries missing, writes to primary storage |
| Video trim UI | ✅ Implemented | Full trim workflow with bottom sheet | |
| Video mute | ❌ Missing | | |
| Video frame extraction | ❌ Missing | | |

## Privacy

| Feature | Status | Actual Implementation | Remaining Work |
|---------|--------|-----------------------|----------------|
| Android 14 permissions | ✅ Implemented | Full/selected/type-specific handling | |
| Hide/unhide | ⚠️ Partial | UI-level hiding only | |
| Hidden album UI | ⚠️ Partial | Hide state exists | Secure locking missing |
| Secure vault | ❌ Missing | Media remains in public MediaStore | |
| Biometric lock / unlock | ❌ Missing | No authentication layer | |

## Search/Discovery

| Feature | Status | Actual Implementation | Remaining Work |
|---------|--------|-----------------------|----------------|
| Filename search | ✅ Implemented | Debounced in-memory filtering | |
| FTS | ❌ Missing | No Room FTS | |
| GPS/map view | ✅ Implemented | osmdroid-based map view with Marker rendering | |
| OCR-based search | ❌ Missing | | |

## Advanced Processing

| Feature | Status | Actual Implementation | Remaining Work |
|---------|--------|-----------------------|----------------|
| SHA-256 duplicates | ✅ Implemented | Duplicate detector implemented | Lacks proper cancellation and error handling on null streams |
| Duplicate review UI | ✅ Implemented | Dedicated Duplicates screen and viewmodel | |
| Persistent hash cache | ❌ Missing | Hashes aren't persisted | |
| Perceptual similarity | ❌ Missing | No pHash/visual similarity | |
| Cloud-aware albums | ❌ Missing | No cloud/custom-album synchronization | |
| Duplicate cleaner | ❌ Missing | | |
| Motion/Live Photo | ❌ Missing | | |
| People/face grouping | ❌ Missing | | |

## Build/Testing

| Feature | Status | Actual Implementation | Remaining Work |
|---------|--------|-----------------------|----------------|
| R8/release hardening | ⚠️ Partial | minify/shrink false in build.gradle | Configure proguard rules and enable |

---

## Architectural Distinction

JAGallery should currently be classified as:

> A modern MediaStore-based gallery and media organizer with Room-backed auxiliary state, multi-volume support, basic image editing, video trimming, system trash, copy/move, and exact duplicate-detection infrastructure.

It is not yet:
- a Google Photos-class intelligent gallery;
- an FTS search engine;
- a complete professional photo editor;
- a cloud-backed gallery.

## Recommended Roadmap

### Phase 1 — essential gallery completion
1. ~~Rename media.~~
2. ~~Rename/create/manage albums.~~
3. ~~Crop + flip.~~
4. ~~Timeline/date grouping.~~
5. ~~Complete Move/Copy UX.~~
6. ~~Complete metadata/EXIF presentation.~~
7. ~~Robust Trash/Restore UX.~~
8. ~~Images/Videos filtering and sorting.~~
9. ~~Open With & Set Wallpaper.~~
10. ~~Interactive crop.~~

### Phase 2 — advanced gallery
1. Secure hidden vault + BiometricPrompt.
2. Duplicate review/cleanup.
3. Persistent hash cache.
4. Advanced editor controls (Highlights/Shadows/Sharpness).
5. Robust Multi-volume Move/Copy (target volume selection).

### Phase 3 — intelligence
1. Perceptual duplicate/similar-image detection.
2. OCR-based search.
3. People/face grouping.
4. Location/map organization.
5. Cloud/backup integration.
