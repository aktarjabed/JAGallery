package com.example.advancedgallery.domain

import com.example.advancedgallery.data.model.MediaItem
import com.example.advancedgallery.data.repository.MediaRepository
import javax.inject.Inject
import javax.inject.Singleton

interface MediaOperations {
    suspend fun toggleFavorite(mediaItem: MediaItem)
    suspend fun removeDeletedItems(deletedIds: List<String>)
}

@Singleton
class MediaOperationsImpl @Inject constructor(
    private val repository: MediaRepository
) : MediaOperations {

    override suspend fun toggleFavorite(mediaItem: MediaItem) {
        repository.toggleFavorite(mediaItem)
    }

    override suspend fun removeDeletedItems(deletedIds: List<String>) {
        if (deletedIds.isNotEmpty()) {
            repository.removeDeletedItems(deletedIds)
        }
    }
}
