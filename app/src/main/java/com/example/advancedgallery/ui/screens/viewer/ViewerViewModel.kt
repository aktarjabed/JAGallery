package com.example.advancedgallery.ui.screens.viewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.advancedgallery.data.model.MediaItem
import com.example.advancedgallery.data.model.MediaLoadResult
import com.example.advancedgallery.data.model.MediaSource
import com.example.advancedgallery.data.repository.MediaRepository
import com.example.advancedgallery.domain.MediaOperations
import com.example.advancedgallery.domain.OperationResult
import com.example.advancedgallery.ui.common.BaseMediaViewModel
import com.example.advancedgallery.ui.common.OperationEvent
import com.example.advancedgallery.ui.navigation.parseMediaSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ViewerState {
    data object Loading : ViewerState
    data class Success(val items: List<MediaItem>) : ViewerState
    data object Empty : ViewerState
    data class Error(val cause: Throwable) : ViewerState
}

sealed interface ViewerNavigationEvent {
    data object PopBack : ViewerNavigationEvent
}

@HiltViewModel
class ViewerViewModel @Inject constructor(
    private val repository: MediaRepository,
    mediaOperations: MediaOperations,
    private val savedStateHandle: SavedStateHandle
) : BaseMediaViewModel(mediaOperations) {

    private val sourceStr: String? = savedStateHandle.get<String>("source")
    private val volumeName: String? = savedStateHandle.get<String>("volumeName")?.let { android.net.Uri.decode(it) }
    private val bucketId: Long? = savedStateHandle.get<String>("bucketId")?.toLongOrNull()
    private val searchQuery: String? = savedStateHandle.get<String>("searchQuery")?.let { android.net.Uri.decode(it) }

    val initialSource: MediaSource = parseMediaSource(sourceStr, volumeName, bucketId, searchQuery) ?: MediaSource.All

    private val _source = MutableStateFlow<MediaSource>(initialSource)
    val source: StateFlow<MediaSource> = _source

    private val _navigationEvent = MutableSharedFlow<ViewerNavigationEvent>()
    val navigationEvent: SharedFlow<ViewerNavigationEvent> = _navigationEvent.asSharedFlow()

    val currentMediaId: String?
        get() = savedStateHandle.get<String>("mediaId")?.let { android.net.Uri.decode(it) }

    fun setCurrentMediaId(mediaId: String) {
        savedStateHandle["mediaId"] = android.net.Uri.encode(mediaId)
    }

    val state: StateFlow<ViewerState> = combine(
        repository.mediaLoadResult,
        _source
    ) { result, currentSource ->
        when (result) {
            is MediaLoadResult.Loading -> ViewerState.Loading
            is MediaLoadResult.Error -> ViewerState.Error(result.cause)
            is MediaLoadResult.Empty -> ViewerState.Empty
            is MediaLoadResult.Success -> {
                val items = result.items
                val filtered = when (currentSource) {
                    is MediaSource.Favorites -> items.filter { it.isFavorite }
                    is MediaSource.Search -> {
                        if (currentSource.query.isNotBlank()) {
                            items.filter { it.name.contains(currentSource.query, ignoreCase = true) }
                        } else {
                            items
                        }
                    }
                    is MediaSource.Album -> {
                        items.filter { it.albumKey == currentSource.albumKey }
                    }
                    is MediaSource.All -> items
                }
                if (filtered.isEmpty()) ViewerState.Empty else ViewerState.Success(filtered)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ViewerState.Loading)

    val mediaItems: StateFlow<List<MediaItem>> = state.map { currentState ->
        if (currentState is ViewerState.Success) currentState.items else emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun removeDeletedItem(deletedId: String) {
        viewModelScope.launch {
            val currentItems = mediaItems.value
            val currentIndex = currentItems.indexOfFirst { it.id == deletedId }

            when (val result = mediaOperations.removeDeletedItems(listOf(deletedId))) {
                is OperationResult.Error -> {
                    // emit error through base viewmodel event
                    removeDeletedItems(listOf(deletedId))
                }
                is OperationResult.Success -> {
                    val remainingItems = currentItems.filterNot { it.id == deletedId }
                    if (remainingItems.isEmpty()) {
                        _navigationEvent.emit(ViewerNavigationEvent.PopBack)
                    } else if (currentIndex != -1) {
                        val newIndex = currentIndex.coerceIn(0, remainingItems.size - 1)
                        val nextItem = remainingItems[newIndex]
                        setCurrentMediaId(nextItem.id)
                    }
                }
            }
        }
    }
}
