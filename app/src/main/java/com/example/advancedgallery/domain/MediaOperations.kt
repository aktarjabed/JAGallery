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
        val failedItems: List<MediaItem>,
        val pendingIntents: List<android.app.PendingIntent> = emptyList()
    ) : MoveOperationResult

    data class Success(val movedItems: List<Pair<MediaItem, android.net.Uri>>) : MoveOperationResult
    data class CopiedSourceRetained(val copiedItems: List<Pair<MediaItem, android.net.Uri>>) : MoveOperationResult
    data class Error(val message: String, val cause: Throwable? = null) : MoveOperationResult
}

sealed interface RenameOperationResult {
    data class Success(val uri: android.net.Uri) : RenameOperationResult
    data class NeedsPermission(
        val pendingIntent: android.app.PendingIntent,
        val item: MediaItem,
        val newName: String
    ) : RenameOperationResult
    data class Error(val message: String, val cause: Throwable? = null) : RenameOperationResult
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
    suspend fun moveMedia(context: android.content.Context, sourceItem: MediaItem, targetAlbumName: String): MoveOperationResult
    suspend fun moveMediaBatch(context: android.content.Context, sourceItems: List<MediaItem>, targetAlbumName: String): MoveOperationResult
    suspend fun renameMedia(context: android.content.Context, item: MediaItem, newName: String): RenameOperationResult
    suspend fun renameAlbum(context: android.content.Context, items: List<MediaItem>, newAlbumName: String): MoveOperationResult
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
    ): MoveOperationResult {
        return moveMediaBatch(context, listOf(sourceItem), targetAlbumName)
    }

    override suspend fun moveMediaBatch(
        context: android.content.Context,
        sourceItems: List<MediaItem>,
        targetAlbumName: String
    ): MoveOperationResult {
        return try {
            val (successfulCopies, failedItems) = repository.copyMediaBatchToAlbum(context, sourceItems, targetAlbumName)
            if (successfulCopies.isEmpty()) {
                return MoveOperationResult.Error("All items failed to copy")
            }

            val sourceUris = successfulCopies.map { it.first.uri }
            val pendingIntents = com.example.advancedgallery.util.FileUtils.createDeleteRequests(context.contentResolver, sourceUris)

            if (pendingIntents.isNotEmpty()) {
                MoveOperationResult.RequestSourceDelete(successfulCopies, failedItems, pendingIntents)
            } else {
                MoveOperationResult.CopiedSourceRetained(successfulCopies)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            MoveOperationResult.Error(e.message ?: "Move operation failed", e)
        }
    }

    override suspend fun renameMedia(
        context: android.content.Context,
        item: MediaItem,
        newName: String
    ): RenameOperationResult {
        return try {
            val resolver = context.contentResolver
            val values = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, newName)
            }

            val updated = resolver.update(item.uri, values, null, null)
            if (updated > 0) {
                repository.loadMedia(force = true, context = context)
                RenameOperationResult.Success(item.uri)
            } else {
                RenameOperationResult.Error("Failed to update media store")
            }
        } catch (e: android.app.RecoverableSecurityException) {
            RenameOperationResult.NeedsPermission(e.userAction.actionIntent, item, newName)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            RenameOperationResult.Error(e.message ?: "Rename operation failed", e)
        }
    }

    override suspend fun renameAlbum(context: android.content.Context, items: List<MediaItem>, newAlbumName: String): MoveOperationResult {
        return moveMediaBatch(context, items, newAlbumName)
    }
}
