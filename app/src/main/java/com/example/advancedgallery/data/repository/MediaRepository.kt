package com.example.advancedgallery.data.repository

import android.content.ContentResolver
import android.content.Context
import com.example.advancedgallery.data.local.HiddenMediaEntity
import com.example.advancedgallery.data.local.MediaDao
import com.example.advancedgallery.data.local.MediaEntity
import com.example.advancedgallery.data.model.MediaItem
import com.example.advancedgallery.data.model.MediaLoadResult
import com.example.advancedgallery.util.MediaStoreHelper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    private val contentResolver: ContentResolver,
    private val mediaDao: MediaDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val repositoryScope = CoroutineScope(ioDispatcher + SupervisorJob())
    private val _mediaLoadResult = MutableStateFlow<MediaLoadResult>(MediaLoadResult.Loading)
    private val _trashedMediaLoadResult = MutableStateFlow<MediaLoadResult>(MediaLoadResult.Loading)
    private val loadMutex = Mutex()
    private val favoriteMutex = Mutex()

    private var activeScanJob: Deferred<Unit>? = null
    private var isCurrentScanForced = false
    private var pendingForcedScan = false
    private var pendingContext: Context? = null

    // Throttling for bulk rescans
    private var lastRescanTimeMs: Long = 0
    private val RESCAN_THROTTLE_MS = 2000L

    val mediaLoadResult: Flow<MediaLoadResult> = combine(
        _mediaLoadResult,
        mediaDao.getFavorites(),
        mediaDao.getHiddenMedia()
    ) { result, favorites, hiddenMedia ->
        val favoriteUris = favorites.map { it.uri }.toSet()
        val hiddenUris = hiddenMedia.map { it.uri }.toSet()
        when (result) {
            is MediaLoadResult.Success -> {
                val updated = result.items
                    .filterNot { hiddenUris.contains(it.id) }
                    .map { item -> item.copy(isFavorite = favoriteUris.contains(item.id)) }
                if (updated.isEmpty()) MediaLoadResult.Empty else MediaLoadResult.Success(updated)
            }
            else -> result
        }
    }

    val trashedMediaLoadResult: Flow<MediaLoadResult> = _trashedMediaLoadResult

    suspend fun loadTrashedMedia(context: Context? = null) = withContext(ioDispatcher) {
        val result = MediaStoreHelper.getMediaItemsResult(contentResolver, ioDispatcher, context, includeTrashed = true)
        _trashedMediaLoadResult.value = result
    }

    val hiddenMediaLoadResult: Flow<MediaLoadResult> = combine(
        _mediaLoadResult,
        mediaDao.getHiddenMedia(),
        mediaDao.getFavorites()
    ) { result, hiddenMedia, favorites ->
        val favoriteUris = favorites.map { it.uri }.toSet()
        val hiddenUris = hiddenMedia.map { it.uri }.toSet()
        when (result) {
            is MediaLoadResult.Success -> {
                val hiddenItems = result.items
                    .filter { hiddenUris.contains(it.id) }
                    .map { item -> item.copy(isFavorite = favoriteUris.contains(item.id)) }
                if (hiddenItems.isEmpty()) MediaLoadResult.Empty else MediaLoadResult.Success(hiddenItems)
            }
            else -> result
        }
    }

    suspend fun loadMedia(force: Boolean = false, context: Context? = null) = withContext(ioDispatcher) {
        val jobToAwait = loadMutex.withLock {
            val currentTime = System.currentTimeMillis()
            val existingJob = activeScanJob
            if (existingJob != null) {
                if (force) {
                    if (!isCurrentScanForced) {
                        pendingForcedScan = true
                        pendingContext = context
                    }
                }
                existingJob
            } else {
                if (force && (currentTime - lastRescanTimeMs < RESCAN_THROTTLE_MS)) {
                    // Throttle fast sequential jobs by firing a delayed job to catch up
                    isCurrentScanForced = force
                    val delayedJob = repositoryScope.async {
                        kotlinx.coroutines.delay(RESCAN_THROTTLE_MS - (currentTime - lastRescanTimeMs))
                        lastRescanTimeMs = System.currentTimeMillis()
                        executeScanLoop(initialForce = force, initialContext = context)
                    }
                    activeScanJob = delayedJob
                    delayedJob
                } else {
                    if (force) {
                        lastRescanTimeMs = currentTime
                    }
                    isCurrentScanForced = force
                    val newJob = repositoryScope.async {
                        executeScanLoop(initialForce = force, initialContext = context)
                    }
                    activeScanJob = newJob
                    newJob
                }
            }
        }
        jobToAwait.await()
    }

    private suspend fun executeScanLoop(initialForce: Boolean, initialContext: Context?) {
        var forceForCurrentPass = initialForce
        var contextForCurrentPass = initialContext

        while (true) {
            val current = _mediaLoadResult.value
            val skip = !forceForCurrentPass && contextForCurrentPass != null && current is MediaLoadResult.Success && current.items.isNotEmpty() && MediaStoreHelper.isMediaStoreVersionCurrent(contextForCurrentPass)
            if (!skip) {
                val result = MediaStoreHelper.getMediaItemsResult(contentResolver, ioDispatcher, contextForCurrentPass)
                _mediaLoadResult.value = result
            }

            val shouldContinue = loadMutex.withLock {
                if (pendingForcedScan) {
                    forceForCurrentPass = true
                    isCurrentScanForced = true
                    contextForCurrentPass = pendingContext
                    pendingForcedScan = false
                    pendingContext = null
                    true
                } else {
                    activeScanJob = null
                    isCurrentScanForced = false
                    false
                }
            }

            if (!shouldContinue) {
                break
            }
        }
    }

    suspend fun favoriteMedia(mediaItem: MediaItem) = withContext(ioDispatcher) {
        favoriteMutex.withLock {
            mediaDao.insert(
                MediaEntity(
                    uri = mediaItem.id,
                    isFavorite = true,
                    dateAdded = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun unfavoriteMedia(mediaItem: MediaItem) = withContext(ioDispatcher) {
        favoriteMutex.withLock {
            mediaDao.removeFavorite(mediaItem.id)
        }
    }

    suspend fun toggleFavorite(mediaItem: MediaItem) = withContext(ioDispatcher) {
        favoriteMutex.withLock {
            val existing = mediaDao.getFavoriteById(mediaItem.id)
            if (existing != null) {
                mediaDao.removeFavorite(mediaItem.id)
            } else {
                mediaDao.insert(
                    MediaEntity(
                        uri = mediaItem.id,
                        isFavorite = true,
                        dateAdded = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    suspend fun hideMedia(mediaItem: MediaItem) = withContext(ioDispatcher) {
        mediaDao.hideMedia(HiddenMediaEntity(uri = mediaItem.id, dateHidden = System.currentTimeMillis()))
    }

    suspend fun hideMediaBatch(mediaItems: List<MediaItem>) = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        val entities = mediaItems.map { HiddenMediaEntity(uri = it.id, dateHidden = now) }
        mediaDao.hideMediaBatch(entities)
    }

    suspend fun unhideMedia(mediaItem: MediaItem) = withContext(ioDispatcher) {
        mediaDao.unhideMedia(mediaItem.id)
    }

    suspend fun unhideMediaBatch(mediaItems: List<MediaItem>) = withContext(ioDispatcher) {
        mediaDao.unhideMediaBatch(mediaItems.map { it.id })
    }

    suspend fun removeDeletedItems(deletedIds: List<String>) = withContext(ioDispatcher) {
        mediaDao.removeFavorites(deletedIds)
        mediaDao.unhideMediaBatch(deletedIds)
        _mediaLoadResult.update { current ->
            if (current is MediaLoadResult.Success) {
                val filtered = current.items.filterNot { deletedIds.contains(it.id) }
                if (filtered.isEmpty()) MediaLoadResult.Empty else MediaLoadResult.Success(filtered)
            } else {
                current
            }
        }
        _trashedMediaLoadResult.update { current ->
            if (current is MediaLoadResult.Success) {
                val filtered = current.items.filterNot { deletedIds.contains(it.id) }
                if (filtered.isEmpty()) MediaLoadResult.Empty else MediaLoadResult.Success(filtered)
            } else {
                current
            }
        }
    }

    suspend fun copyMediaToAlbum(
        context: Context,
        sourceItem: MediaItem,
        targetAlbumName: String,
        skipRescan: Boolean = false
    ): android.net.Uri? = withContext(ioDispatcher) {
        val resolver = context.contentResolver
        val relativePath = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            if (sourceItem.isVideo) "Movies/$targetAlbumName/" else "Pictures/$targetAlbumName/"
        } else {
            ""
        }

        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, sourceItem.name)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, sourceItem.mimeType)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
                put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val collection = if (sourceItem.isVideo) {
            android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        var newUri: android.net.Uri? = null
        try {
            newUri = resolver.insert(collection, contentValues) ?: return@withContext null
            var success = false
            resolver.openInputStream(sourceItem.uri)?.use { input ->
                resolver.openOutputStream(newUri)?.use { output ->
                    input.copyTo(output)
                    success = true
                }
            }

            if (success && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
                val updated = resolver.update(newUri, contentValues, null, null)
                if (updated != 1) {
                    try { resolver.delete(newUri, null, null) } catch (e: Exception) {}
                    return@withContext null
                }
            }

            if (!success) {
                try { resolver.delete(newUri, null, null) } catch (e: Exception) {}
                null
            } else {
                if (!skipRescan) {
                    loadMedia(force = true, context = context)
                }
                newUri
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            if (newUri != null) {
                try { resolver.delete(newUri, null, null) } catch (delEx: Exception) {}
            }
            throw e
        } catch (e: Exception) {
            if (newUri != null) {
                try { resolver.delete(newUri, null, null) } catch (delEx: Exception) {}
            }
            null
        }
    }

    suspend fun copyMediaBatchToAlbum(
        context: Context,
        sourceItems: List<MediaItem>,
        targetAlbumName: String
    ): List<Pair<MediaItem, android.net.Uri>> = withContext(ioDispatcher) {
        val successfulCopies = mutableListOf<Pair<MediaItem, android.net.Uri>>()
        for (item in sourceItems) {
            val newUri = copyMediaToAlbum(context, item, targetAlbumName, skipRescan = true)
            if (newUri != null) {
                successfulCopies.add(Pair(item, newUri))
            }
        }
        if (successfulCopies.isNotEmpty()) {
            loadMedia(force = true, context = context)
        }
        successfulCopies
    }
}
