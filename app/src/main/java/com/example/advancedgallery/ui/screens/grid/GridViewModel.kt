package com.example.advancedgallery.ui.screens.grid

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.example.advancedgallery.data.model.MediaItem
import com.example.advancedgallery.data.model.MediaSource
import com.example.advancedgallery.data.repository.MediaRepository
import com.example.advancedgallery.domain.MediaOperations
import com.example.advancedgallery.ui.common.BaseMediaViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class GridViewModel @Inject constructor(
    repository: MediaRepository,
    mediaOperations: MediaOperations,
    private val savedStateHandle: SavedStateHandle
) : BaseMediaViewModel(mediaOperations) {

    private val bucketId: Long? = savedStateHandle.get<String>("bucketId")?.toLongOrNull()
    private val sourceName: String? = savedStateHandle.get<String>("source")

    private val initialSource: MediaSource = if (sourceName == "ALBUM" && bucketId != null) {
        MediaSource.Album(bucketId)
    } else {
        MediaSource.All
    }

    private val _source = MutableStateFlow<MediaSource>(initialSource)
    val source: StateFlow<MediaSource> = _source

    val mediaItems: StateFlow<List<MediaItem>> = combine(repository.mediaItems, _source) { items, currentSource ->
        when (currentSource) {
            is MediaSource.Album -> items.filter { it.bucketId == currentSource.bucketId }
            else -> items
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSource(source: MediaSource) {
        _source.value = source
    }
}
