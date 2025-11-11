package com.aktarjabed.feature.album

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.core.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AlbumViewModel(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _albums = MutableStateFlow<Map<String, List<com.aktarjabed.core.data.model.MediaItem>>>(emptyMap())
    val albums: StateFlow<Map<String, List<com.aktarjabed.core.data.model.MediaItem>>> = _albums

    init {
        loadAlbums()
    }

    private fun loadAlbums() {
        viewModelScope.launch {
            val photos = mediaRepository.getPhotos()
            _albums.value = photos.groupBy { it.bucket }
        }
    }
}