package com.aktarjabed.jagallery.data.repository

import android.content.ContentResolver
import android.content.Context
import com.aktarjabed.jagallery.data.local.HiddenMediaEntity
import com.aktarjabed.jagallery.data.local.MediaDao
import com.aktarjabed.jagallery.data.local.MediaEntity
import com.aktarjabed.jagallery.data.model.MediaItem
import com.aktarjabed.jagallery.data.model.MediaLoadResult
import com.aktarjabed.jagallery.util.MediaStoreHelper
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

    val trashedMediaLoadResult: Flow<MediaLoadResult> = combine(
        _trashedMediaLoadResult,
        mediaDao.getFavorites()
    ) { result, favorites ->
        val favoriteUris = favorites.map { it.uri }.toSet()
        when (result) {
            is MediaLoadResult.Success -> {
                val updated = result.items.map { item ->
                    item.copy(isFavorite = favoriteUris.contains(item.id))
                }
                if (updated.isEmpty()) MediaLoadResult.Empty else MediaLoadResult.Success(updated)
            }
            else -> result
        }
    }

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
                val liveUris = result.items.map { it.id }.toSet()
                val staleHiddenUris = hiddenMedia
                    .filter { !liveUris.contains(it.uri) }
                    .map { it.uri }
                if (staleHiddenUris.isNotEmpty()) {
                    mediaDao.unhideMediaBatch(staleHiddenUris)
                }

                val hiddenItems = result.items
                    .filter { hiddenUris.contains(it.id) && !staleHiddenUris.contains(it.id) }
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
                    pendingForcedScan = true
                    pendingContext = context
                }
                existingJob
            } else {
                if (force && (currentTime - lastRescanTimeMs < RESCAN_THROTTLE_MS)) {
                    // Throttle fast sequential jobs by firing a delayed job to catch up
                    isCurrentScanForced = force
                    lateinit var delayedJob: Deferred<Unit>
                    delayedJob = repositoryScope.async {
                        kotlinx.coroutines.delay(RESCAN_THROTTLE_MS - (currentTime - lastRescanTimeMs))
                        executeScanLoop(delayedJob, initialForce = force, initialContext = context)
                    }
                    activeScanJob = delayedJob
                    delayedJob
                } else {
                    isCurrentScanForced = force
                    lateinit var newJob: Deferred<Unit>
                    newJob = repositoryScope.async {
                        executeScanLoop(newJob, initialForce = force, initialContext = context)
                    }
                    activeScanJob = newJob
                    newJob
                }
            }
        }
        jobToAwait.await()
    }

    private suspend fun executeScanLoop(thisJob: Deferred<Unit>?, initialForce: Boolean, initialContext: Context?) {
        var forceForCurrentPass = initialForce
        var contextForCurrentPass = initialContext

        while (true) {
            if (forceForCurrentPass) {
                lastRescanTimeMs = System.currentTimeMillis()
            }
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
                    if (thisJob == null || activeScanJob === thisJob) {
                        activeScanJob = null
                        isCurrentScanForced = false
                    }
                    false
                }
            }

            if (!shouldContinue) {
                break
            }
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
            newUri = com.aktarjabed.jagallery.util.FileUtils.insertPendingMediaEntry(resolver, collection, contentValues) ?: return@withContext null

            val success = com.aktarjabed.jagallery.util.FileUtils.copyMediaFile(resolver, sourceItem.uri, newUri)

            if (success && !com.aktarjabed.jagallery.util.FileUtils.publishPendingEntry(resolver, newUri, contentValues)) {
                try { resolver.delete(newUri, null, null) } catch (e: Exception) {}
                return@withContext null
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
    ): Pair<List<Pair<MediaItem, android.net.Uri>>, List<MediaItem>> = withContext(ioDispatcher) {
        val successfulCopies = mutableListOf<Pair<MediaItem, android.net.Uri>>()
        val failedItems = mutableListOf<MediaItem>()
        for (item in sourceItems) {
            val newUri = copyMediaToAlbum(context, item, targetAlbumName, skipRescan = true)
            if (newUri != null) {
                successfulCopies.add(Pair(item, newUri))
            } else {
                failedItems.add(item)
            }
        }
        if (successfulCopies.isNotEmpty()) {
            loadMedia(force = true, context = context)
        }
        Pair(successfulCopies, failedItems)
    }
}
