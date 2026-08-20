package com.example.advancedgallery.domain

import com.example.advancedgallery.data.model.MediaItem
import com.example.advancedgallery.data.repository.MediaRepository
import kotlinx.coroutines.CancellationException
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
    suspend fun copyMedia(context: android.content.Context, sourceItem: MediaItem, targetAlbumName: String): OperationResult<android.net.Uri>
    suspend fun moveMedia(context: android.content.Context, sourceItem: MediaItem, targetAlbumName: String): OperationResult<android.net.Uri>
}

@Singleton
class MediaOperationsImpl @Inject constructor(
    private val repository: MediaRepository
) : MediaOperations {

    override suspend fun favoriteMedia(mediaItem: MediaItem): OperationResult<Unit> {
        return try {
            repository.favoriteMedia(mediaItem)
            OperationResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            OperationResult.Error(e, e.message)
        }
    }

    override suspend fun unfavoriteMedia(mediaItem: MediaItem): OperationResult<Unit> {
        return try {
            repository.unfavoriteMedia(mediaItem)
            OperationResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            OperationResult.Error(e, e.message)
        }
    }

    override suspend fun toggleFavorite(mediaItem: MediaItem): OperationResult<Unit> {
        return try {
            repository.toggleFavorite(mediaItem)
            OperationResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            OperationResult.Error(e, e.message)
        }
    }

    override suspend fun removeDeletedItems(deletedIds: List<String>): OperationResult<Unit> {
        if (deletedIds.isEmpty()) return OperationResult.Success(Unit)
        return try {
            repository.removeDeletedItems(deletedIds)
            OperationResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            OperationResult.Error(e, e.message)
        }
    }

    override suspend fun copyMedia(
        context: android.content.Context,
        sourceItem: MediaItem,
        targetAlbumName: String
    ): OperationResult<android.net.Uri> {
        return try {
            val uri = repository.copyMediaToAlbum(context, sourceItem, targetAlbumName)
            if (uri != null) {
                OperationResult.Success(uri)
            } else {
                OperationResult.Error(Exception("Failed to copy media"), "Copy operation failed")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            OperationResult.Error(e, e.message)
        }
    }

    override suspend fun moveMedia(
        context: android.content.Context,
        sourceItem: MediaItem,
        targetAlbumName: String
    ): OperationResult<android.net.Uri> {
        return try {
            val uri = repository.copyMediaToAlbum(context, sourceItem, targetAlbumName)
            if (uri != null) {
                OperationResult.Success(uri)
            } else {
                OperationResult.Error(Exception("Failed to move media"), "Move operation failed during copy step")
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            OperationResult.Error(e, e.message)
        }
    }
}
