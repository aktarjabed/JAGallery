package com.example.advancedgallery.data.repository

import android.content.ContentResolver
import android.content.Context
import com.example.advancedgallery.data.local.MediaDao
import com.example.advancedgallery.data.local.MediaEntity
import com.example.advancedgallery.data.model.MediaItem
import com.example.advancedgallery.data.model.MediaLoadResult
import com.example.advancedgallery.util.MediaStoreHelper
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaRepository @Inject constructor(
    private val contentResolver: ContentResolver,
    private val mediaDao: MediaDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val _mediaLoadResult = MutableStateFlow<MediaLoadResult>(MediaLoadResult.Loading)

    val mediaLoadResult: Flow<MediaLoadResult> = combine(_mediaLoadResult, mediaDao.getFavorites()) { result, favorites ->
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

    suspend fun loadMedia(force: Boolean = false, context: Context? = null) {
        val current = _mediaLoadResult.value
        if (!force && context != null && current is MediaLoadResult.Success && current.items.isNotEmpty() && MediaStoreHelper.isMediaStoreVersionCurrent(context)) {
            return
        }
        val result = MediaStoreHelper.getMediaItemsResult(contentResolver, ioDispatcher, context)
        _mediaLoadResult.value = result
    }

    suspend fun favoriteMedia(mediaItem: MediaItem) {
        withContext(ioDispatcher) {
            mediaDao.insert(
                MediaEntity(
                    uri = mediaItem.id,
                    isFavorite = true,
                    dateAdded = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun unfavoriteMedia(mediaItem: MediaItem) {
        withContext(ioDispatcher) {
            mediaDao.removeFavorite(mediaItem.id)
        }
    }

    suspend fun toggleFavorite(mediaItem: MediaItem) {
        if (mediaItem.isFavorite) {
            unfavoriteMedia(mediaItem)
        } else {
            favoriteMedia(mediaItem)
        }
    }

    suspend fun removeDeletedItems(deletedIds: List<String>) {
        withContext(ioDispatcher) {
            mediaDao.removeFavorites(deletedIds)
            _mediaLoadResult.update { current ->
                if (current is MediaLoadResult.Success) {
                    val filtered = current.items.filterNot { deletedIds.contains(it.id) }
                    if (filtered.isEmpty()) MediaLoadResult.Empty else MediaLoadResult.Success(filtered)
                } else {
                    current
                }
            }
        }
    }
}
