package com.example.advancedgallery.ui.screens.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.advancedgallery.data.model.MediaItem
import com.example.advancedgallery.data.model.MediaSource
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

    private val _source = MutableStateFlow<MediaSource>(MediaSource.All)

    val mediaItems: StateFlow<List<MediaItem>> = combine(
        repository.mediaItems,
        _source
    ) { items, source ->
        when (source) {
            is MediaSource.Favorites -> items.filter { it.isFavorite }
            is MediaSource.Search -> {
                if (source.query.isNotBlank()) {
                    items.filter { it.name.contains(source.query, ignoreCase = true) }
                } else {
                    items
                }
            }
            is MediaSource.Album -> {
                if (source.bucketId != null) {
                    items.filter { it.bucketId == source.bucketId }
                } else {
                    items
                }
            }
            is MediaSource.All -> items
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSource(source: MediaSource) {
        _source.value = source
    }

    fun toggleFavorite(mediaItem: MediaItem) {
        viewModelScope.launch {
            repository.toggleFavorite(mediaItem)
        }
    }

    fun removeDeletedItem(deletedId: String) {
        viewModelScope.launch {
            repository.removeDeletedItems(listOf(deletedId))
            repository.loadMedia()
        }
    }
}
