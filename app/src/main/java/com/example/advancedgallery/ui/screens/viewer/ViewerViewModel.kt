package com.example.advancedgallery.ui.screens.viewer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.advancedgallery.data.model.MediaItem
import com.example.advancedgallery.data.model.MediaLoadResult
import com.example.advancedgallery.data.model.MediaSource
import com.example.advancedgallery.data.repository.MediaRepository
import com.example.advancedgallery.domain.MediaOperations
import com.example.advancedgallery.ui.navigation.parseMediaSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
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

@HiltViewModel
class ViewerViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val mediaOperations: MediaOperations,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val sourceStr: String? = savedStateHandle.get<String>("source")
    private val bucketId: Long? = savedStateHandle.get<String>("bucketId")?.toLongOrNull()
    private val searchQuery: String? = savedStateHandle.get<String>("searchQuery")?.let { android.net.Uri.decode(it) }

    private val initialSource: MediaSource = parseMediaSource(sourceStr, bucketId, searchQuery)

    private val _source = MutableStateFlow<MediaSource>(initialSource)

    val currentMediaId: String?
        get() = savedStateHandle.get<String>("mediaId")?.let { android.net.Uri.decode(it) }

    fun setCurrentMediaId(mediaId: String) {
        savedStateHandle["mediaId"] = android.net.Uri.encode(mediaId)
    }

    val state: StateFlow<ViewerState> = combine(
        repository.mediaLoadResult,
        _source
    ) { result, source ->
        when (result) {
            is MediaLoadResult.Loading -> ViewerState.Loading
            is MediaLoadResult.Error -> ViewerState.Error(result.cause)
            is MediaLoadResult.Empty -> ViewerState.Empty
            is MediaLoadResult.Success -> {
                val items = result.items
                val filtered = when (source) {
                    is MediaSource.Favorites -> items.filter { it.isFavorite }
                    is MediaSource.Search -> {
                        if (source.query.isNotBlank()) {
                            items.filter { it.name.contains(source.query, ignoreCase = true) }
                        } else {
                            items
                        }
                    }
                    is MediaSource.Album -> {
                        items.filter { it.bucketId == source.bucketId }
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

    fun setSource(source: MediaSource) {
        _source.value = source
    }

    fun toggleFavorite(mediaItem: MediaItem) {
        viewModelScope.launch {
            mediaOperations.toggleFavorite(mediaItem)
        }
    }

    fun removeDeletedItem(deletedId: String) {
        val currentItems = mediaItems.value
        val currentIndex = currentItems.indexOfFirst { it.id == deletedId }

        if (currentIndex != -1 && currentItems.size > 1) {
            val nextItem = if (currentIndex < currentItems.size - 1) {
                currentItems[currentIndex + 1]
            } else {
                currentItems[currentIndex - 1]
            }
            setCurrentMediaId(nextItem.id)
        }

        viewModelScope.launch {
            mediaOperations.removeDeletedItems(listOf(deletedId))
        }
    }
}
