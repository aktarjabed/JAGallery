package com.aktarjabed.jagallery.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.jagallery.data.model.MediaItem
import com.aktarjabed.jagallery.domain.MediaOperations
import com.aktarjabed.jagallery.domain.OperationResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import com.aktarjabed.jagallery.data.repository.MediaRepository
import com.aktarjabed.jagallery.data.model.MediaLoadResult
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import com.aktarjabed.jagallery.ui.common.selection.BatchOperationManager

sealed interface OperationEvent {
    data class Error(val message: String) : OperationEvent
    data class Success(val message: String) : OperationEvent
}

abstract class BaseMediaViewModel(
    protected val mediaOperations: MediaOperations,
    protected val repository: MediaRepository
) : ViewModel() {

    val batchManager = BatchOperationManager()

    val allAlbums: StateFlow<List<com.aktarjabed.jagallery.data.model.Album>> = repository.mediaLoadResult
        .map { result ->
            when (result) {
                is MediaLoadResult.Success -> {
                    result.items
                        .groupBy { it.albumKey }
                        .mapNotNull { (key, itemsInAlbum) ->
                            val firstItem = itemsInAlbum.firstOrNull() ?: return@mapNotNull null
                            val name = firstItem.bucketName.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                            com.aktarjabed.jagallery.data.model.Album(key, name, itemsInAlbum.size, firstItem.uri)
                        }
                        .sortedBy { it.name }
                }
                else -> emptyList()
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    protected val _operationEvent = MutableSharedFlow<OperationEvent>()
    val operationEvent: SharedFlow<OperationEvent> = _operationEvent.asSharedFlow()

    open fun removeDeletedItems(deletedIds: List<String>) {
        viewModelScope.launch {
            when (val result = mediaOperations.removeDeletedItems(deletedIds)) {
                is OperationResult.Error -> {
                    _operationEvent.emit(OperationEvent.Error(result.message ?: "Failed to remove deleted items"))
                }
                else -> {}
            }
        }
    }

    suspend fun renameMedia(context: android.content.Context, item: com.aktarjabed.jagallery.data.model.MediaItem, newName: String): com.aktarjabed.jagallery.domain.RenameOperationResult {
        return mediaOperations.renameMedia(context, item, newName)
    }

    fun toggleFavorite(mediaItem: MediaItem) {
        viewModelScope.launch {
            when (val result = mediaOperations.toggleFavorite(mediaItem)) {
                is OperationResult.Error -> {
                    _operationEvent.emit(OperationEvent.Error(result.message ?: "Failed to update favorite"))
                }
                else -> {}
            }
        }
    }

    fun hideMedia(mediaItem: MediaItem) {
        viewModelScope.launch {
            when (val result = mediaOperations.hideMedia(mediaItem)) {
                is OperationResult.Error -> {
                    _operationEvent.emit(OperationEvent.Error(result.message ?: "Failed to hide media"))
                }
                else -> {}
            }
        }
    }

    fun hideMediaBatch(mediaItems: List<MediaItem>) {
        viewModelScope.launch {
            when (val result = mediaOperations.hideMediaBatch(mediaItems)) {
                is OperationResult.Error -> {
                    _operationEvent.emit(OperationEvent.Error(result.message ?: "Failed to hide media batch"))
                }
                else -> {}
            }
        }
    }

    fun unhideMedia(mediaItem: MediaItem) {
        viewModelScope.launch {
            when (val result = mediaOperations.unhideMedia(mediaItem)) {
                is OperationResult.Error -> {
                    _operationEvent.emit(OperationEvent.Error(result.message ?: "Failed to unhide media"))
                }
                else -> {}
            }
        }
    }

    fun unhideMediaBatch(mediaItems: List<MediaItem>) {
        viewModelScope.launch {
            when (val result = mediaOperations.unhideMediaBatch(mediaItems)) {
                is OperationResult.Error -> {
                    _operationEvent.emit(OperationEvent.Error(result.message ?: "Failed to unhide media batch"))
                }
                else -> {}
            }
        }
    }

    suspend fun copyMediaBatch(
        context: android.content.Context,
        mediaItems: List<MediaItem>,
        destination: com.aktarjabed.jagallery.data.model.AlbumDestination
    ): com.aktarjabed.jagallery.domain.MoveOperationResult {
        return mediaOperations.copyMediaBatch(context, mediaItems, destination)
    }

    suspend fun moveMediaBatch(
        context: android.content.Context,
        mediaItems: List<MediaItem>,
        destination: com.aktarjabed.jagallery.data.model.AlbumDestination
    ): com.aktarjabed.jagallery.domain.MoveOperationResult {
        return mediaOperations.moveMediaBatch(context, mediaItems, destination)
    }
}
