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

    private val _source = MutableStateFlow(MediaSource.ALL)
    private val _bucketId = MutableStateFlow<Long?>(null)
    private val _searchQuery = MutableStateFlow<String?>(null)

    val mediaItems: StateFlow<List<MediaItem>> = combine(
        repository.mediaItems,
        _source,
        _bucketId,
        _searchQuery
    ) { items, source, bucket, query ->
        when (source) {
            MediaSource.FAVORITES -> items.filter { it.isFavorite }
            MediaSource.SEARCH -> {
                if (!query.isNullOrBlank()) {
                    items.filter { it.name.contains(query, ignoreCase = true) }
                } else {
                    items
                }
            }
            MediaSource.ALBUM -> {
                if (bucket != null) {
                    items.filter { it.bucketId == bucket }
                } else {
                    items
                }
            }
            MediaSource.ALL -> items
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setParams(source: MediaSource, bucketId: Long? = null, searchQuery: String? = null) {
        _source.value = source
        _bucketId.value = bucketId
        _searchQuery.value = searchQuery
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
