package com.example.advancedgallery.ui.screens.grid

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.advancedgallery.data.model.MediaItem
import com.example.advancedgallery.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GridViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val bucketIdFlow: StateFlow<Long?> = savedStateHandle.getStateFlow("bucketId", null)

    val mediaItems: StateFlow<List<MediaItem>> = combine(repository.mediaItems, bucketIdFlow) { items, bucket ->
        if (bucket != null) {
            items.filter { it.bucketId == bucket }
        } else {
            items
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setBucketId(bucketId: Long?) {
        savedStateHandle["bucketId"] = bucketId
    }

    fun removeDeletedItems(deletedIds: List<String>) {
        viewModelScope.launch {
            repository.removeDeletedItems(deletedIds)
            repository.loadMedia()
        }
    }
}
