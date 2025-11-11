package com.aktarjabed.feature.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.core.data.model.MediaItem
import com.aktarjabed.core.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DetailsViewModel(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _mediaItem = MutableStateFlow<MediaItem?>(null)
    val mediaItem: StateFlow<MediaItem?> = _mediaItem

    fun loadMediaItem(id: String) {
        viewModelScope.launch {
            // In a real app, you would fetch a single item by ID
            val photos = mediaRepository.getPhotos()
            _mediaItem.value = photos.find { it.id == id.toLong() }
        }
    }
}