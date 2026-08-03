package com.example.advancedgallery.ui.screens.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.advancedgallery.data.model.Album
import com.example.advancedgallery.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    val albums: StateFlow<List<Album>> = repository.mediaItems.map { items ->
        items.groupBy { it.bucketId }
            .map { (bucketId, bucketItems) ->
                val firstItem = bucketItems.first()
                Album(
                    bucketId = bucketId,
                    name = firstItem.bucketName,
                    mediaCount = bucketItems.size,
                    coverUri = firstItem.uri
                )
            }.sortedByDescending { it.mediaCount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.loadMedia()
        }
    }
}
