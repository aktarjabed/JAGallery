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
    private val volumeName: String? = savedStateHandle.get<String>("volumeName")
    private val bucketId: Long? = savedStateHandle.get<String>("bucketId")?.toLongOrNull()
    private val relativePath: String? = savedStateHandle.get<String>("relativePath")
    private val searchQuery: String? = savedStateHandle.get<String>("searchQuery")

    val initialSource: MediaSource? = parseMediaSource(sourceStr, volumeName, bucketId, relativePath, searchQuery)

    private val _source = MutableStateFlow<MediaSource?>(initialSource)
    val source: StateFlow<MediaSource?> = _source

    private val _navigationEvent = MutableSharedFlow<ViewerNavigationEvent>(replay = 1, extraBufferCapacity = 1)
    val navigationEvent: SharedFlow<ViewerNavigationEvent> = _navigationEvent.asSharedFlow()

    private var lastValidIndex: Int = 0

    private var currentVaultDestinationMapping: Map<String, String>? = null
    private var requestedVaultSourceIds: List<String> = emptyList()

    val currentMediaId: String?
        get() = savedStateHandle.get<String>("mediaId")?.let { com.aktarjabed.jagallery.util.NavCodec.decode(it) }

    fun setCurrentMediaId(mediaId: String) {
        savedStateHandle["mediaId"] = mediaId
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
                        val q = currentSource.query
                        if (q.isNotBlank()) {
                            val intent = com.aktarjabed.jagallery.domain.NaturalLanguageSearchParser.parse(q)
                            val matched = items.filter { item ->
                                val matchesName = item.name.contains(q, ignoreCase = true) ||
                                    item.bucketName.contains(q, ignoreCase = true)
                                val categoryMatches = intent.category != null &&
                                    com.aktarjabed.jagallery.domain.SmartAlbumClassifier.classifyCategory(item) == intent.category
                                val docMatches = intent.isDocument &&
                                    com.aktarjabed.jagallery.domain.SmartAlbumClassifier.isReceiptOrDocument(item)
                                val receiptMatches = intent.isReceipt &&
                                    com.aktarjabed.jagallery.domain.SmartAlbumClassifier.isReceiptOrDocument(item)
                                val peopleMatches = intent.isPeople &&
                                    com.aktarjabed.jagallery.domain.SmartAlbumClassifier.isPeopleOrPortrait(item)
                                val favoriteMatches = intent.isFavorite && item.isFavorite
                                val videoMatches = intent.isVideo && item.isVideo
                                matchesName || categoryMatches || docMatches || receiptMatches ||
                                    peopleMatches || favoriteMatches || videoMatches
                            }
                            matched.ifEmpty { items }
                        } else {
                            items
                        }
                    }
                    is MediaSource.Album -> {
                        items.filter { it.albumKey == currentSource.albumKey }
                    }
                    is MediaSource.TypeFiltered -> items.filter { it.isVideo == currentSource.isVideo }
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
                try {
                    _navigationEvent.emit(ViewerNavigationEvent.PopBack)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }

        if (initialSource is MediaSource.Trash) {
            viewModelScope.launch {
                try {
                    repository.loadTrashedMedia(context)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }

        viewModelScope.launch {
            try {
                state.collect { currentState ->
                    if (currentState is ViewerState.Success) {
                        val filtered = currentState.items
                        val activeId = currentMediaId
                        val index = if (activeId != null) filtered.indexOfFirst { it.id == activeId } else -1
                        if (index != -1) {
                            lastValidIndex = index
                        } else if (filtered.isNotEmpty()) {
                            val targetIndex = lastValidIndex.coerceIn(0, filtered.size - 1)
                            val nextItem = filtered[targetIndex]
                            setCurrentMediaId(nextItem.id)
                            lastValidIndex = targetIndex
                        }
                    } else if (currentState is ViewerState.Empty) {
                        _navigationEvent.emit(ViewerNavigationEvent.PopBack)
                    }
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                // Ignore
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

    
    /**
     * Handles the MediaStore delete request result for the currently viewed item.
     * Only removes the item from the UI/repository after Android confirms deletion.
     */
    fun storeVaultContext(sourceIds: List<String>, mapping: Map<String, String>) {
        requestedVaultSourceIds = sourceIds
        currentVaultDestinationMapping = mapping
    }

    fun rollbackVault(deletedSourceIds: Set<String>) {
        val mapping = currentVaultDestinationMapping ?: return
        val unconfirmedSourceIds = requestedVaultSourceIds.filterNot { it in deletedSourceIds }
        if (unconfirmedSourceIds.isNotEmpty()) {
            viewModelScope.launch {
                try {
                    mediaOperations.rollbackVaultDestinationEntries(unconfirmedSourceIds, mapping)
                } catch (e: kotlinx.coroutines.CancellationException) {
                    throw e
                } catch (e: Exception) {
                    _operationEvent.emit(
                        com.aktarjabed.jagallery.ui.common.OperationEvent.Error("Vault rollback failed: ${e.message}")
                    )
                }
            }
        }
    }



    fun onVaultSourceDeleteResult(item: MediaItem, deleteConfirmed: Boolean) {
        viewModelScope.launch {
            if (deleteConfirmed) {
                when (mediaOperations.removeDeletedItems(listOf(item.id))) {
                    is OperationResult.Success -> {
                        val remaining = mediaItems.value.filterNot { it.id == item.id }
                        if (remaining.isEmpty()) {
                            _navigationEvent.emit(ViewerNavigationEvent.PopBack)
                        } else {
                            val targetIndex = lastValidIndex.coerceIn(0, remaining.lastIndex)
                            setCurrentMediaId(remaining[targetIndex].id)
                        }
                    }
                    is OperationResult.Error -> _operationEvent.emit(
                        com.aktarjabed.jagallery.ui.common.OperationEvent.Error(
                            "The original item was deleted, but the gallery could not refresh."
                        )
                    )
                }
            } else {
                _operationEvent.emit(
                    com.aktarjabed.jagallery.ui.common.OperationEvent.Error(
                        "Vault copy retained; original media was not deleted."
                    )
                )
            }
        }
    }

    fun removeDeletedItem(deletedId: String) {
        viewModelScope.launch {
            try {
                val currentItems = mediaItems.value
                val currentIndex = currentItems.indexOfFirst { it.id == deletedId }

                when (mediaOperations.removeDeletedItems(listOf(deletedId))) {
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
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
