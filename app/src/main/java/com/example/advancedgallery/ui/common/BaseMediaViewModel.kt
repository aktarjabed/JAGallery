package com.example.advancedgallery.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.advancedgallery.data.model.MediaItem
import com.example.advancedgallery.domain.MediaOperations
import com.example.advancedgallery.domain.OperationResult
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import com.example.advancedgallery.data.repository.MediaRepository
import com.example.advancedgallery.data.model.MediaLoadResult
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted

sealed interface OperationEvent {
    data class Error(val message: String) : OperationEvent
    data class Success(val message: String) : OperationEvent
}

abstract class BaseMediaViewModel(
    protected val mediaOperations: MediaOperations,
    protected val repository: MediaRepository
) : ViewModel() {

    val allAlbumNames: StateFlow<List<String>> = repository.mediaLoadResult
        .map { result ->
            when (result) {
                is MediaLoadResult.Success -> result.items
                    .mapNotNull { it.bucketName.takeIf { name -> name.isNotBlank() } }
                    .distinct()
                    .sorted()
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

    suspend fun renameMedia(context: android.content.Context, item: com.example.advancedgallery.data.model.MediaItem, newName: String): com.example.advancedgallery.domain.RenameOperationResult {
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

    suspend fun copyMedia(
        context: android.content.Context,
        mediaItem: MediaItem,
        targetAlbumName: String
    ): OperationResult<android.net.Uri> {
        return mediaOperations.copyMedia(context, mediaItem, targetAlbumName)
    }

    suspend fun moveMediaBatch(
        context: android.content.Context,
        mediaItems: List<MediaItem>,
        targetAlbumName: String
    ): com.example.advancedgallery.domain.MoveOperationResult {
        return mediaOperations.moveMediaBatch(context, mediaItems, targetAlbumName)
    }
}
