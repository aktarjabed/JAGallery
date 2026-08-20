package com.example.advancedgallery.domain

import com.example.advancedgallery.data.model.MediaItem
import com.example.advancedgallery.data.repository.MediaRepository
import javax.inject.Inject
import javax.inject.Singleton

sealed interface OperationResult<out T> {
    data class Success<T>(val data: T) : OperationResult<T>
    data class Error(val exception: Throwable, val message: String? = null) : OperationResult<Nothing>
}

interface MediaOperations {
    suspend fun favoriteMedia(mediaItem: MediaItem): OperationResult<Unit>
    suspend fun unfavoriteMedia(mediaItem: MediaItem): OperationResult<Unit>
    suspend fun toggleFavorite(mediaItem: MediaItem): OperationResult<Unit>
    suspend fun removeDeletedItems(deletedIds: List<String>): OperationResult<Unit>
}

@Singleton
class MediaOperationsImpl @Inject constructor(
    private val repository: MediaRepository
) : MediaOperations {

    override suspend fun favoriteMedia(mediaItem: MediaItem): OperationResult<Unit> {
        return try {
            repository.favoriteMedia(mediaItem)
            OperationResult.Success(Unit)
        } catch (e: Exception) {
            OperationResult.Error(e, e.message)
        }
    }

    override suspend fun unfavoriteMedia(mediaItem: MediaItem): OperationResult<Unit> {
        return try {
            repository.unfavoriteMedia(mediaItem)
            OperationResult.Success(Unit)
        } catch (e: Exception) {
            OperationResult.Error(e, e.message)
        }
    }

    override suspend fun toggleFavorite(mediaItem: MediaItem): OperationResult<Unit> {
        return try {
            repository.toggleFavorite(mediaItem)
            OperationResult.Success(Unit)
        } catch (e: Exception) {
            OperationResult.Error(e, e.message)
        }
    }

    override suspend fun removeDeletedItems(deletedIds: List<String>): OperationResult<Unit> {
        if (deletedIds.isEmpty()) return OperationResult.Success(Unit)
        return try {
            repository.removeDeletedItems(deletedIds)
            OperationResult.Success(Unit)
        } catch (e: Exception) {
            OperationResult.Error(e, e.message)
        }
    }
}
