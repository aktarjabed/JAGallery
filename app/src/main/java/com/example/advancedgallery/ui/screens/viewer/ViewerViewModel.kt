package com.example.advancedgallery.ui.screens.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.advancedgallery.data.model.MediaItem
import com.example.advancedgallery.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ViewerViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _bucketId = MutableStateFlow<Long?>(null)

    val mediaItems: StateFlow<List<MediaItem>> = combine(repository.mediaItems, _bucketId) { items, bucket ->
        when (bucket) {
            -1L -> items.filter { it.isFavorite }
            -2L -> items // Searching context - will show all for now since passing the query via bundle is complex for this task
            null -> items
            else -> items.filter { it.bucketId == bucket }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setBucketId(bucketId: Long?) {
        _bucketId.value = bucketId
    }

    fun toggleFavorite(mediaItem: MediaItem) {
        viewModelScope.launch {
            repository.toggleFavorite(mediaItem)
        }
    }

    fun removeDeletedItem(deletedId: Long) {
        viewModelScope.launch {
            repository.removeDeletedItems(listOf(deletedId))
            repository.loadMedia()
        }
    }
}
