package com.example.advancedgallery.ui.screens.albums

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.advancedgallery.data.model.Album
import com.example.advancedgallery.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val repository: MediaRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val totalMediaCount: StateFlow<Int> = repository.mediaItems.map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val coverUri: StateFlow<android.net.Uri?> = repository.mediaItems.map { it.firstOrNull()?.uri }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val albums: StateFlow<List<Album>> = repository.mediaItems.map { items ->
        items.groupBy { it.bucketId }
            .map { (bucketId, bucketItems) ->
                val firstItem = bucketItems.first()
                Album(
                    bucketId = bucketId,
                    name = firstItem.bucketName.ifBlank { context.getString(com.example.advancedgallery.R.string.internal_storage) },
                    mediaCount = bucketItems.size,
                    coverUri = firstItem.uri
                )
            }.sortedByDescending { it.mediaCount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.loadMedia(context = context)
        }
    }
}
