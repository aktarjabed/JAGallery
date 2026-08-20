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

sealed interface OperationEvent {
    data class Error(val message: String) : OperationEvent
}

abstract class BaseMediaViewModel(
    protected val mediaOperations: MediaOperations
) : ViewModel() {

    private val _operationEvent = MutableSharedFlow<OperationEvent>()
    val operationEvent: SharedFlow<OperationEvent> = _operationEvent.asSharedFlow()

    fun removeDeletedItems(deletedIds: List<String>) {
        viewModelScope.launch {
            when (val result = mediaOperations.removeDeletedItems(deletedIds)) {
                is OperationResult.Error -> {
                    _operationEvent.emit(OperationEvent.Error(result.message ?: "Failed to remove deleted items"))
                }
                else -> {}
            }
        }
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

    suspend fun moveMediaBatch(
        context: android.content.Context,
        mediaItems: List<MediaItem>,
        targetAlbumName: String
    ): com.example.advancedgallery.domain.MoveOperationResult {
        return mediaOperations.moveMediaBatch(context, mediaItems, targetAlbumName)
    }
}
