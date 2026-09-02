package com.aktarjabed.jagallery.ui.screens.viewer

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.aktarjabed.jagallery.data.model.MediaItem
import com.aktarjabed.jagallery.data.model.MediaLoadResult
import com.aktarjabed.jagallery.data.model.MediaSource
import com.aktarjabed.jagallery.data.repository.MediaRepository
import com.aktarjabed.jagallery.domain.MediaOperations
import android.content.Context
import com.aktarjabed.jagallery.domain.OperationResult
import com.aktarjabed.jagallery.ui.common.BaseMediaViewModel
import com.aktarjabed.jagallery.ui.navigation.parseMediaSource
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    @ApplicationContext private val context: Context,
    repository: MediaRepository,
    mediaOperations: MediaOperations,
    private val savedStateHandle: SavedStateHandle
) : BaseMediaViewModel(mediaOperations, repository) {

    companion object {
        private const val TAG = "ViewerViewModel"
    }

    private val sourceStr: String? = savedStateHandle.get<String>("source")
    private val volumeName: String? = savedStateHandle.get<String>("volumeName")?.let { android.net.Uri.decode(it) }
    private val bucketId: Long? = savedStateHandle.get<String>("bucketId")?.toLongOrNull()
    private val relativePath: String? = savedStateHandle.get<String>("relativePath")?.let { android.net.Uri.decode(it) }
    private val searchQuery: String? = savedStateHandle.get<String>("searchQuery")?.let { android.net.Uri.decode(it) }

    val initialSource: MediaSource? = parseMediaSource(sourceStr, volumeName, bucketId, relativePath, searchQuery)

    private val _source = MutableStateFlow<MediaSource?>(initialSource)
    val source: StateFlow<MediaSource?> = _source

    private val _navigationEvent = MutableSharedFlow<ViewerNavigationEvent>(replay = 1, extraBufferCapacity = 1)
    val navigationEvent: SharedFlow<ViewerNavigationEvent> = _navigationEvent.asSharedFlow()

    private var lastValidIndex: Int = 0

    val currentMediaId: String?
        get() = savedStateHandle.get<String>("mediaId")?.let { android.net.Uri.decode(it) }

    fun setCurrentMediaId(mediaId: String) {
        savedStateHandle["mediaId"] = android.net.Uri.encode(mediaId)
    }

    val state: StateFlow<ViewerState> = combine(
        repository.mediaLoadResult,
        repository.hiddenMediaLoadResult,
        repository.trashedMediaLoadResult,
        _source
    ) { normalResult, hiddenResult, trashedResult, currentSource ->
        if (currentSource == null) {
            return@combine ViewerState.Empty
        }
        val targetResult = when (currentSource) {
            is MediaSource.Hidden -> hiddenResult
            is MediaSource.Trash -> trashedResult
            else -> normalResult
        }
        when (targetResult) {
            is MediaLoadResult.Loading -> ViewerState.Loading
            is MediaLoadResult.Error -> ViewerState.Error(targetResult.cause)
            is MediaLoadResult.Empty -> ViewerState.Empty
            is MediaLoadResult.Success -> {
                val items = targetResult.items
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
                    is MediaSource.Trash -> items
                    is MediaSource.Hidden -> items
                }

                if (filtered.isEmpty()) {
                    ViewerState.Empty
                } else {
                    ViewerState.Success(filtered)
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ViewerState.Loading)

    init {
        if (initialSource == null) {
            Log.w(TAG, "Invalid navigation route parameters: sourceStr=$sourceStr, volumeName=$volumeName, bucketId=$bucketId, searchQuery=$searchQuery")
            viewModelScope.launch {
                _navigationEvent.emit(ViewerNavigationEvent.PopBack)
            }
        }

        if (initialSource is MediaSource.Trash) {
            viewModelScope.launch {
                repository.loadTrashedMedia(context)
            }
        }

        viewModelScope.launch {
            state.collect { currentState ->
                if (currentState is ViewerState.Success) {
                    val filtered = currentState.items
                    val activeId = currentMediaId
                    val index = if (activeId != null) filtered.indexOfFirst { it.id == activeId } else -1
                    if (index != -1) {
                        lastValidIndex = index
                    } else {
                        val targetIndex = lastValidIndex.coerceIn(0, filtered.size - 1)
                        val nextItem = filtered[targetIndex]
                        setCurrentMediaId(nextItem.id)
                        lastValidIndex = targetIndex
                    }
                } else if (currentState is ViewerState.Empty) {
                    _navigationEvent.emit(ViewerNavigationEvent.PopBack)
                }
            }
        }
    }

    val mediaItems: StateFlow<List<MediaItem>> = state.map { currentState ->
        if (currentState is ViewerState.Success) currentState.items else emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateCurrentIndex(index: Int) {
        val items = mediaItems.value
        if (index in items.indices) {
            lastValidIndex = index
            setCurrentMediaId(items[index].id)
        }
    }

    fun removeDeletedItem(deletedId: String) {
        viewModelScope.launch {
            val currentItems = mediaItems.value
            val currentIndex = currentItems.indexOfFirst { it.id == deletedId }

            when (val result = mediaOperations.removeDeletedItems(listOf(deletedId))) {
                is OperationResult.Error -> {
                    removeDeletedItems(listOf(deletedId))
                }
                is OperationResult.Success -> {
                    val remainingItems = currentItems.filterNot { it.id == deletedId }
                    if (remainingItems.isEmpty()) {
                        _navigationEvent.emit(ViewerNavigationEvent.PopBack)
                    } else if (currentIndex != -1) {
                        val newIndex = currentIndex.coerceIn(0, remainingItems.size - 1)
                        val nextItem = remainingItems[newIndex]
                        lastValidIndex = newIndex
                        setCurrentMediaId(nextItem.id)
                    }
                }
            }
        }
    }
}
