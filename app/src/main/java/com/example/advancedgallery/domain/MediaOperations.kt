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

sealed interface MoveOperationResult {
    data class RequestSourceDelete(
        val successfulCopies: List<Pair<MediaItem, android.net.Uri>>,
        val pendingIntent: android.app.PendingIntent?
    ) : MoveOperationResult

    data class Success(val movedItems: List<Pair<MediaItem, android.net.Uri>>) : MoveOperationResult
    data class CopiedSourceRetained(val copiedItems: List<Pair<MediaItem, android.net.Uri>>) : MoveOperationResult
    data class Error(val message: String, val cause: Throwable? = null) : MoveOperationResult
}

interface MediaOperations {
    suspend fun favoriteMedia(mediaItem: MediaItem): OperationResult<Unit>
    suspend fun unfavoriteMedia(mediaItem: MediaItem): OperationResult<Unit>
    suspend fun toggleFavorite(mediaItem: MediaItem): OperationResult<Unit>
    suspend fun hideMedia(mediaItem: MediaItem): OperationResult<Unit>
    suspend fun hideMediaBatch(mediaItems: List<MediaItem>): OperationResult<Unit>
    suspend fun unhideMedia(mediaItem: MediaItem): OperationResult<Unit>
    suspend fun unhideMediaBatch(mediaItems: List<MediaItem>): OperationResult<Unit>
    suspend fun removeDeletedItems(deletedIds: List<String>): OperationResult<Unit>
    suspend fun copyMedia(context: android.content.Context, sourceItem: MediaItem, targetAlbumName: String): OperationResult<android.net.Uri>
    suspend fun moveMedia(context: android.content.Context, sourceItem: MediaItem, targetAlbumName: String): OperationResult<android.net.Uri>
    suspend fun moveMediaBatch(context: android.content.Context, sourceItems: List<MediaItem>, targetAlbumName: String): MoveOperationResult
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

    override suspend fun hideMedia(mediaItem: MediaItem): OperationResult<Unit> {
        return try {
            repository.hideMedia(mediaItem)
            OperationResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            OperationResult.Error(e, e.message)
        }
    }

    override suspend fun hideMediaBatch(mediaItems: List<MediaItem>): OperationResult<Unit> {
        return try {
            repository.hideMediaBatch(mediaItems)
            OperationResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            OperationResult.Error(e, e.message)
        }
    }

    override suspend fun unhideMedia(mediaItem: MediaItem): OperationResult<Unit> {
        return try {
            repository.unhideMedia(mediaItem)
            OperationResult.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            OperationResult.Error(e, e.message)
        }
    }

    override suspend fun unhideMediaBatch(mediaItems: List<MediaItem>): OperationResult<Unit> {
        return try {
            repository.unhideMediaBatch(mediaItems)
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
            val result = moveMediaBatch(context, listOf(sourceItem), targetAlbumName)
            when (result) {
                is MoveOperationResult.Success -> OperationResult.Success(result.movedItems.first().second)
                is MoveOperationResult.RequestSourceDelete -> OperationResult.Error(
                    Exception("Source deletion confirmation required"),
                    "Source deletion confirmation required"
                )
                is MoveOperationResult.CopiedSourceRetained -> OperationResult.Error(
                    Exception("Copied to album, but source retained"),
                    "Copied to album, but source retained"
                )
                is MoveOperationResult.Error -> OperationResult.Error(Exception(result.message), result.message)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            OperationResult.Error(e, e.message)
        }
    }

    override suspend fun moveMediaBatch(
        context: android.content.Context,
        sourceItems: List<MediaItem>,
        targetAlbumName: String
    ): MoveOperationResult {
        return try {
            val successfulCopies = repository.copyMediaBatchToAlbum(context, sourceItems, targetAlbumName)
            if (successfulCopies.isEmpty()) {
                return MoveOperationResult.Error("Failed to copy media items to target album")
            }

            val sourceUris = successfulCopies.map { it.first.uri }
            val pendingIntent = com.example.advancedgallery.util.FileUtils.createDeleteRequest(context.contentResolver, sourceUris)

            if (pendingIntent != null) {
                MoveOperationResult.RequestSourceDelete(successfulCopies, pendingIntent)
            } else {
                val success = com.example.advancedgallery.util.FileUtils.deleteMediaItems(context.contentResolver, sourceUris)
                if (success) {
                    repository.removeDeletedItems(successfulCopies.map { it.first.id })
                    MoveOperationResult.Success(successfulCopies)
                } else {
                    MoveOperationResult.CopiedSourceRetained(successfulCopies)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            MoveOperationResult.Error(e.message ?: "Move operation failed", e)
        }
    }
}
