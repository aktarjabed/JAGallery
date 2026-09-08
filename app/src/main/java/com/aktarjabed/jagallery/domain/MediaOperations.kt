package com.aktarjabed.jagallery.domain

import com.aktarjabed.jagallery.data.model.MediaItem
import com.aktarjabed.jagallery.data.repository.MediaRepository
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
        val pendingIntents: List<com.aktarjabed.jagallery.data.model.DeleteRequestChunk>
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
    suspend fun toggleFavorite(mediaItem: MediaItem): OperationResult<Unit>
    suspend fun hideMedia(mediaItem: MediaItem): OperationResult<Unit>
    suspend fun hideMediaBatch(mediaItems: List<MediaItem>): OperationResult<Unit>
    suspend fun unhideMedia(mediaItem: MediaItem): OperationResult<Unit>
    suspend fun unhideMediaBatch(mediaItems: List<MediaItem>): OperationResult<Unit>
    suspend fun removeDeletedItems(deletedIds: List<String>): OperationResult<Unit>
    suspend fun copyMediaBatch(context: android.content.Context, sourceItems: List<MediaItem>, destination: com.aktarjabed.jagallery.data.model.AlbumDestination): MoveOperationResult
    suspend fun moveMediaBatch(context: android.content.Context, sourceItems: List<MediaItem>, destination: com.aktarjabed.jagallery.data.model.AlbumDestination): MoveOperationResult
    suspend fun renameMedia(context: android.content.Context, item: MediaItem, newName: String): RenameOperationResult
    suspend fun renameAlbum(context: android.content.Context, sourceAlbum: com.aktarjabed.jagallery.data.model.Album, newName: String, items: List<MediaItem>): MoveOperationResult
}

@Singleton
class MediaOperationsImpl @Inject constructor(
    private val repository: MediaRepository
) : MediaOperations {

    private suspend fun <T> runOperation(block: suspend () -> T): OperationResult<T> {
        return try {
            OperationResult.Success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            OperationResult.Error(e, e.message)
        }
    }

    override suspend fun toggleFavorite(mediaItem: MediaItem): OperationResult<Unit> = runOperation {
        repository.toggleFavorite(mediaItem)
    }

    override suspend fun hideMedia(mediaItem: MediaItem): OperationResult<Unit> = runOperation {
        repository.hideMedia(mediaItem)
    }

    override suspend fun hideMediaBatch(mediaItems: List<MediaItem>): OperationResult<Unit> = runOperation {
        repository.hideMediaBatch(mediaItems)
    }

    override suspend fun unhideMedia(mediaItem: MediaItem): OperationResult<Unit> = runOperation {
        repository.unhideMedia(mediaItem)
    }

    override suspend fun unhideMediaBatch(mediaItems: List<MediaItem>): OperationResult<Unit> = runOperation {
        repository.unhideMediaBatch(mediaItems)
    }

    override suspend fun removeDeletedItems(deletedIds: List<String>): OperationResult<Unit> = runOperation {
        if (deletedIds.isNotEmpty()) {
            repository.removeDeletedItems(deletedIds)
        }
    }

    override suspend fun copyMediaBatch(
        context: android.content.Context,
        sourceItems: List<MediaItem>,
        destination: com.aktarjabed.jagallery.data.model.AlbumDestination
    ): MoveOperationResult {
        return try {
            val (successfulCopies, failedItems) = repository.copyMediaBatchToAlbum(context, sourceItems, destination)
            if (failedItems.isNotEmpty()) {
                MoveOperationResult.Error("Copy partially failed (${failedItems.size} items)")
            } else {
                MoveOperationResult.CopiedSourceRetained(successfulCopies)
            }
        } catch (e: Exception) {
            MoveOperationResult.Error(e.message ?: "Copy operation failed", e)
        }
    }

    override suspend fun moveMediaBatch(
        context: android.content.Context,
        sourceItems: List<MediaItem>,
        destination: com.aktarjabed.jagallery.data.model.AlbumDestination
    ): MoveOperationResult {
        return try {
            val (successfulCopies, failedItems) = repository.copyMediaBatchToAlbum(context, sourceItems, destination)
            if (successfulCopies.isEmpty()) {
                return MoveOperationResult.Error("All items failed to copy")
            }

            val sourceUris = successfulCopies.map { it.first.uri }
            when (val creationResult = com.aktarjabed.jagallery.util.FileUtils.createDeleteRequests(context.contentResolver, sourceUris)) {
                is com.aktarjabed.jagallery.util.FileUtils.RequestCreationResult.Success -> {
                    if (creationResult.chunks.isNotEmpty()) {
                        MoveOperationResult.RequestSourceDelete(successfulCopies, failedItems, creationResult.chunks)
                    } else {
                        MoveOperationResult.CopiedSourceRetained(successfulCopies)
                    }
                }
                is com.aktarjabed.jagallery.util.FileUtils.RequestCreationResult.Unsupported -> {
                    // Try to direct delete each file, keep track of which succeed so we only pass those back to the UI
                    val effectivelyDeletedUris = mutableListOf<android.net.Uri>()
                    val successfullyMovedItems = mutableListOf<Pair<MediaItem, android.net.Uri>>()
                    var deletionFailed = false

                    for ((item, newUri) in successfulCopies) {
                        val success = com.aktarjabed.jagallery.util.FileUtils.deleteMediaItems(context.contentResolver, listOf(item.uri))
                        if (success) {
                            effectivelyDeletedUris.add(item.uri)
                            successfullyMovedItems.add(Pair(item, newUri))
                        } else {
                            deletionFailed = true
                        }
                    }

                    if (successfullyMovedItems.isNotEmpty()) {
                        MoveOperationResult.RequestSourceDelete(successfullyMovedItems, failedItems, emptyList())
                    } else {
                        MoveOperationResult.CopiedSourceRetained(successfulCopies)
                    }
                }
                else -> {
                    MoveOperationResult.CopiedSourceRetained(successfulCopies)
                }
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

    override suspend fun renameAlbum(context: android.content.Context, sourceAlbum: com.aktarjabed.jagallery.data.model.Album, newName: String, items: List<MediaItem>): MoveOperationResult {
        if (sourceAlbum.name == newName) {
            return MoveOperationResult.Error("New name must be different from current name")
        }

        // The actual renaming of the directory is not fully supported by standard MediaStore
        // APIs without SAF (Storage Access Framework) DocumentFile operations.
        // We will maintain the Move operation (Copy + Delete), but ensure the paths are respected in repository.

        // Derive the new path from the source album's relative path
        val sourcePath = sourceAlbum.key.relativePath
        val parentPath = if (sourcePath.endsWith("/")) {
            val parts = sourcePath.trimEnd('/').split("/")
            if (parts.size > 1) {
                parts.dropLast(1).joinToString("/") + "/"
            } else {
                "" // root level
            }
        } else {
            val parts = sourcePath.split("/")
            if (parts.size > 1) {
                parts.dropLast(1).joinToString("/") + "/"
            } else {
                "" // root level
            }
        }
        val newRelativePath = "$parentPath$newName/"

        val destination = com.aktarjabed.jagallery.data.model.AlbumDestination.NewAlbum(newName, sourceAlbum.volumeName, newRelativePath)
        return moveMediaBatch(context, items, destination)
    }
}
