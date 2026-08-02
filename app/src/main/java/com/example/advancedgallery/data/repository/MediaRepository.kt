package com.example.advancedgallery.data.repository

import android.content.ContentResolver
import com.example.advancedgallery.data.local.MediaDao
import com.example.advancedgallery.data.local.MediaEntity
import com.example.advancedgallery.data.model.Album
import com.example.advancedgallery.data.model.MediaItem
import com.example.advancedgallery.util.MediaStoreHelper
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
    private val mediaDao: MediaDao
) {
    private val _mediaItems = MutableStateFlow<List<MediaItem>>(emptyList())

    val mediaItems: Flow<List<MediaItem>> = combine(_mediaItems, mediaDao.getFavorites()) { items, favorites ->
        val favoriteIds = favorites.map { it.id }.toSet()
        items.map { item ->
            item.copy(isFavorite = favoriteIds.contains(item.id))
        }
    }

    suspend fun loadMedia() {
        val items = MediaStoreHelper.getMediaItems(contentResolver)
        _mediaItems.update { items }
    }

    suspend fun toggleFavorite(mediaItem: MediaItem) {
        withContext(Dispatchers.IO) {
            val isFav = !mediaItem.isFavorite
            if (isFav) {
                mediaDao.insert(
                    MediaEntity(
                        id = mediaItem.id,
                        uri = mediaItem.uri.toString(),
                        isFavorite = true,
                        dateAdded = System.currentTimeMillis()
                    )
                )
            } else {
                mediaDao.removeFavorite(mediaItem.id)
            }
        }
    }

    suspend fun removeDeletedItems(deletedIds: List<Long>) {
        withContext(Dispatchers.IO) {
            mediaDao.removeFavorites(deletedIds)
        }
    }
}
