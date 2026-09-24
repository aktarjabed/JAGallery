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

    private data class ScanRequest(val force: Boolean, val context: Context?)
    private var currentScanJob: Deferred<Unit>? = null
    private var pendingRequest: ScanRequest? = null

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
                // WARNING: Do NOT automatically purge hidden records here.
                // Under Android 14+ Selected Photos Access, or if items are Trashed,
                // the MediaStore snapshot might be incomplete.
                // Purging here would permanently lose the hidden state for items that still exist
                // but are simply invisible to the current permission scope or are in the Trash.

                val hiddenItems = result.items
                    .filter { hiddenUris.contains(it.id) }
                    .map { item -> item.copy(isFavorite = favoriteUris.contains(item.id)) }
                if (hiddenItems.isEmpty()) MediaLoadResult.Empty else MediaLoadResult.Success(hiddenItems)
            }
            else -> result
        }
    }

    suspend fun loadMedia(force: Boolean = false, context: Context? = null) {
        val jobToAwait = loadMutex.withLock {
            if (currentScanJob != null) {
                if (force) {
                    pendingRequest = ScanRequest(force = true, context = context)
                } else if (pendingRequest == null) {
                    pendingRequest = ScanRequest(force = false, context = context)
                }
                currentScanJob!!
            } else {
                val newJob = repositoryScope.async {
                    executeScanLoop(ScanRequest(force, context))
                }
                currentScanJob = newJob
                newJob
            }
        }
        jobToAwait.await()
    }

    private suspend fun executeScanLoop(initialRequest: ScanRequest) {
        var currentRequest = initialRequest

        try {
            while (true) {
                val current = _mediaLoadResult.value
                val skip = !currentRequest.force && currentRequest.context != null && current is MediaLoadResult.Success && current.items.isNotEmpty() && MediaStoreHelper.isMediaStoreVersionCurrent(currentRequest.context)

                if (!skip) {
                    val result = MediaStoreHelper.getMediaItemsResult(contentResolver, ioDispatcher, currentRequest.context)
                    _mediaLoadResult.value = result
                }

                val nextRequest = loadMutex.withLock {
                    val next = pendingRequest
                    pendingRequest = null
                    next
                }

                if (nextRequest == null) {
                    break
                }
                currentRequest = nextRequest
            }
        } finally {
            loadMutex.withLock {
                currentScanJob = null
                val nextRequest = pendingRequest
                if (nextRequest != null) {
                    pendingRequest = null
                    val newJob = repositoryScope.async {
                        executeScanLoop(nextRequest)
                    }
                    currentScanJob = newJob
                }
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
        val updateResult: (MediaLoadResult) -> MediaLoadResult = { current ->
            if (current is MediaLoadResult.Success) {
                val filtered = current.items.filterNot { deletedIds.contains(it.id) }
                if (filtered.isEmpty()) MediaLoadResult.Empty else MediaLoadResult.Success(filtered)
            } else {
                current
            }
        }
        _mediaLoadResult.update(updateResult)
        _trashedMediaLoadResult.update(updateResult)
    }

    suspend fun copyMediaToAlbum(
        context: Context,
        sourceItem: MediaItem,
        destination: com.aktarjabed.jagallery.data.model.AlbumDestination,
        skipRescan: Boolean = false
    ): android.net.Uri? = withContext(ioDispatcher) {
        val resolver = context.contentResolver
        var newUri: android.net.Uri? = null
        try {
            newUri = com.aktarjabed.jagallery.util.FileUtils.copyFileToMediaStore(
                context,
                sourceItem.uri,
                destination,
                sourceItem.name,
                sourceItem.mimeType
            )

            if (newUri == null) {
                return@withContext null
            } else {
                // Preserve Room metadata for Hidden / Favorite
                val newUriStr = newUri.toString()
                var metadataSuccess = true
                try {
                    if (sourceItem.isFavorite) {
                        val oldEntity = mediaDao.getFavoriteById(sourceItem.uri.toString())
                        if (oldEntity != null) {
                            mediaDao.insert(com.aktarjabed.jagallery.data.local.MediaEntity(newUriStr, true, oldEntity.dateAdded))
                        } else {
                            mediaDao.insert(com.aktarjabed.jagallery.data.local.MediaEntity(newUriStr, true, System.currentTimeMillis()))
                        }
                    }
                    val hiddenRecord = mediaDao.getHiddenMediaById(sourceItem.uri.toString())
                    if (hiddenRecord != null) {
                        mediaDao.hideMedia(com.aktarjabed.jagallery.data.local.HiddenMediaEntity(newUriStr, true, hiddenRecord.dateHidden))
                    }
                } catch (e: Exception) {
                    metadataSuccess = false
                }

                if (!metadataSuccess) {
                    // Rollback dependent metadata and MediaStore object to prevent orphans
                    try { mediaDao.removeFavorite(newUriStr) } catch (e: Exception) {}
                    try { mediaDao.unhideMedia(newUriStr) } catch (e: Exception) {}
                    try { resolver.delete(newUri, null, null) } catch (e: Exception) {}
                    null
                } else {
                    if (!skipRescan) {
                        loadMedia(force = true, context = context)
                    }
                    newUri
                }
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
        destination: com.aktarjabed.jagallery.data.model.AlbumDestination
    ): Pair<List<Pair<MediaItem, android.net.Uri>>, List<MediaItem>> = withContext(ioDispatcher) {
        val successfulCopies = mutableListOf<Pair<MediaItem, android.net.Uri>>()
        val failedItems = mutableListOf<MediaItem>()
        for (item in sourceItems) {
            val newUri = copyMediaToAlbum(context, item, destination, skipRescan = true)
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
