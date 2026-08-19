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
import com.example.advancedgallery.util.Constants

@HiltViewModel
class ViewerViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    private val _bucketId = MutableStateFlow<Long?>(null)
    private val _searchQuery = MutableStateFlow<String?>(null)

    val mediaItems: StateFlow<List<MediaItem>> = combine(repository.mediaItems, _bucketId, _searchQuery) { items, bucket, query ->
        when (bucket) {
            Constants.BUCKET_ID_FAVORITES -> items.filter { it.isFavorite }
            Constants.BUCKET_ID_SEARCH -> {
                if (!query.isNullOrBlank()) {
                    items.filter { it.name.contains(query, ignoreCase = true) }
                } else {
                    items
                }
            }
            null -> items
            else -> items.filter { it.bucketId == bucket }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setParams(bucketId: Long?, searchQuery: String? = null) {
        _bucketId.value = bucketId
        _searchQuery.value = searchQuery
    }

    fun setBucketId(bucketId: Long?) {
        setParams(bucketId, null)
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
