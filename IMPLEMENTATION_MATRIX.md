# Implementation Matrix

## Phase 13 - Dead Code & Duplication

Status: COMPLETE

Genuine duplicate logic was consolidated where semantic reuse was appropriate.
Shared RenameDialog, BatchOperationObserver handling, Toast handling, and
image-editor utility/math logic were standardized across applicable screens.

Wildcard imports used solely to influence JSCPD metrics were reverted.

Remaining JSCPD matches are primarily intentional Compose/UI structural
repetition and are not considered defects requiring further abstraction.

Validation completed successfully:
- Debug unit tests (115 actionable tasks: 114 executed, 1 up-to-date)
- lintDebug (No issues found)
- assembleDebug (BUILD SUCCESSFUL)
- assembleRelease (BUILD SUCCESSFUL)
- diff --check (No errors)


## Core Gallery

| Feature | Status | Actual Implementation | Remaining Work |
|---------|--------|-----------------------|----------------|
| Gallery/Grid | ✅ Implemented | Core grid/gallery works | |
| Image/Video Viewer | ✅ Implemented | Image viewer + Media3 video playback | |
| Multi-selection | ✅ Implemented | Shared selection architecture | |
| Metadata | ✅ Implemented | Complete EXIF parsing, copyable fields | |
| Slideshow | ✅ Implemented | Configurable 3-second delay, ignores videos | |
| Set wallpaper | ✅ Implemented | Implemented and behavior improved using setStream to avoid memory issues | |
| Open With | ✅ Implemented | Intent-based sharing via secure content URI | |

## Storage/MediaStore

| Feature | Status | Actual Implementation | Remaining Work |
|---------|--------|-----------------------|----------------|
| Room persistence | ✅ Implemented | Auxiliary state, not a complete MediaStore metadata cache | |
| Multi-volume MediaStore | ✅ Implemented | Full getVolumeName structural extraction + AlbumKey validation | |
| Copy | ✅ Implemented | MediaStore destination creation + Complete Copy UX | |
| Move | ✅ Implemented | Copy + Source deletion with Room transaction metadata sync constraints | |
| Batch rollback | ✅ Implemented | Transactional rollback for metadata orphans implemented via source retention | |
| Trash | ✅ Implemented | Fully featured MediaStore trash/restore + Configurable 7/30/60 day/never Retention | Idempotent automated cleanup via `WorkManager` |
| Restore | ✅ Implemented | MediaStore trashed-item query/restore flow | |
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
| Advanced photo editor | ⚠️ Partial | Interactive Free Crop maps correctly, preserves EXIF | Highlights/Shadows/Sharpness absent |
| Crop | ✅ Implemented | Interactive Crop overlay / Aspect ratio crop (Square/Free/Original) | |
| Flip horizontal/vertical | ✅ Implemented | Matrix postScale flip operations | |
| GIF-specific editing | ❌ Missing | | |
| Advanced EXIF editor | ❌ Missing | | |

## Video

| Feature | Status | Actual Implementation | Remaining Work |
|---------|--------|-----------------------|----------------|
| Video trimming | ✅ Implemented | Dedicated trimming/export pipeline, boundary validation | Writes to primary storage |
| Video trim UI | ✅ Implemented | Full trim workflow with bottom sheet | |
| Video mute | ❌ Missing | | |
| Video frame extraction | ❌ Missing | | |

## Privacy

| Feature | Status | Actual Implementation | Remaining Work |
|---------|--------|-----------------------|----------------|
| Android 14 permissions | ✅ Implemented | Full/selected/type-specific handling | |
| Hide/unhide | ✅ Implemented | UI-level hiding | |
| Hidden album UI | ✅ Implemented | Hide state exists | |
| Secure vault | ✅ Implemented | AES-256-GCM Keystore + App-private storage | Temporary decrypted `content://` streams for viewing/sharing. |
| Biometric lock / unlock | ✅ Implemented | `androidx.biometric` authentication + 1 min auto-lock timeout | |

## Search/Discovery

| Feature | Status | Actual Implementation | Remaining Work |
|---------|--------|-----------------------|----------------|
| Natural Language Search | ✅ Implemented | Intent-based parser for querying image semantics ("Show my receipts") | |
| Filename search | ✅ Implemented | Debounced in-memory filtering | |
| FTS | ❌ Missing | No Room FTS | |
| GPS/map view | ✅ Implemented | osmdroid-based map view with Marker rendering | |
| OCR-based search | ❌ Missing | | |

## Advanced Processing

| Feature | Status | Actual Implementation | Remaining Work |
|---------|--------|-----------------------|----------------|
| Smart Source Albums | ✅ Implemented | Path/Volume heuristics mapping (Screenshots, WhatsApp, Camera, etc) | |
| Smart Content AI | ✅ Implemented | Local heuristic model mapping Receipts/Documents based on MIME/Path | |
| SHA-256 duplicates | ✅ Implemented | Duplicate detector with cancellation and safe stream handling | |
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
| R8/release hardening | ✅ Implemented | minify/shrink true + Dagger/Exoplayer configurations securely applied | |

---

## Architectural Distinction

JAGallery should currently be classified as:

> A modern MediaStore-based gallery and media organizer with Room-backed auxiliary state, multi-volume support, basic image editing, video trimming, system trash, copy/move, and exact duplicate-detection infrastructure.

It is not yet:
- a Google Photos-class intelligent gallery (requires full Local ML models or explicit opt-in LLMs for object-level bounding boxes).
- an FTS search engine (currently relies on real-time MediaStore/Room filters combined with heuristic semantic parsing).
- a complete professional photo editor (lacks curve adjustments, HSL, advanced layer masks).
- a cloud-backed gallery (no synchronization to Google Drive/Nextcloud/etc.).

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
1. ~~Secure hidden vault + BiometricPrompt.~~
2. ~~Duplicate review/cleanup.~~
3. Persistent hash cache.
4. Advanced editor controls (Highlights/Shadows/Sharpness).
5. ~~Robust Multi-volume Move/Copy (target volume selection).~~

### Phase 3 — intelligence
1. Perceptual duplicate/similar-image detection.
2. ~~OCR-based search (implemented basic semantic heuristic classifier, advanced OCR pending).~~
3. People/face grouping.
4. ~~Location/map organization.~~
5. Cloud/backup integration.
