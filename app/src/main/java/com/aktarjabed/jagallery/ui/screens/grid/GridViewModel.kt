package com.aktarjabed.jagallery.ui.screens.grid

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.aktarjabed.jagallery.data.model.MediaLoadResult
import com.aktarjabed.jagallery.data.model.MediaSource
import com.aktarjabed.jagallery.data.repository.MediaRepository
import com.aktarjabed.jagallery.domain.MediaOperations
import com.aktarjabed.jagallery.ui.common.BaseMediaViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import com.aktarjabed.jagallery.ui.common.components.MediaTypeFilter
import com.aktarjabed.jagallery.ui.common.components.SortOption
import com.aktarjabed.jagallery.ui.common.components.SortOrder

import javax.inject.Inject

@HiltViewModel
class GridViewModel @Inject constructor(
    repository: MediaRepository,
    mediaOperations: MediaOperations,
    private val savedStateHandle: SavedStateHandle
) : BaseMediaViewModel(mediaOperations, repository) {

    private val bucketId: Long? = savedStateHandle.get<String>("bucketId")?.toLongOrNull()
    private val volumeName: String? = savedStateHandle.get<String>("volumeName")
    private val sourceName: String? = savedStateHandle.get<String>("source")
    private val relativePath: String = savedStateHandle.get<String>("relativePath") ?: ""

    private val initialSource: MediaSource = if (sourceName == "ALBUM" && !volumeName.isNullOrBlank() && bucketId != null) {
        MediaSource.Album(volumeName, bucketId, relativePath)
    } else {
        MediaSource.All
    }


    private val _sortOption = MutableStateFlow(SortOption.DATE)
    val sortOption: StateFlow<SortOption> = _sortOption

    private val _sortOrder = MutableStateFlow(SortOrder.DESCENDING)
    val sortOrder: StateFlow<SortOrder> = _sortOrder

    private val _mediaFilter = MutableStateFlow(MediaTypeFilter.ALL)
    val mediaFilter: StateFlow<MediaTypeFilter> = _mediaFilter

    fun setSortAndFilter(option: SortOption, order: SortOrder, filter: MediaTypeFilter) {
        _sortOption.value = option
        _sortOrder.value = order
        _mediaFilter.value = filter
    }
    private val _source = MutableStateFlow<MediaSource>(initialSource)
    val source: StateFlow<MediaSource> = _source

    val mediaLoadResult: StateFlow<MediaLoadResult> = combine(
        repository.mediaLoadResult,
        _source,
        _sortOption,
        _sortOrder,
        _mediaFilter
    ) { result, currentSource, sortOpt, sortOrd, mediaFilt ->
        when (result) {
            is MediaLoadResult.Success -> {
                var filtered = when (currentSource) {
                    is MediaSource.Album -> result.items.filter { it.albumKey == currentSource.albumKey }
                    else -> result.items
                }

                filtered = when (mediaFilt) {
                    MediaTypeFilter.ALL -> filtered
                    MediaTypeFilter.IMAGES_ONLY -> filtered.filter { !it.isVideo }
                    MediaTypeFilter.VIDEOS_ONLY -> filtered.filter { it.isVideo }
                }

                filtered = when (sortOpt) {
                    SortOption.DATE -> {
                        if (sortOrd == SortOrder.ASCENDING) filtered.sortedWith(compareBy({ it.dateAdded }, { it.name }))
                        else filtered.sortedWith(compareByDescending<com.aktarjabed.jagallery.data.model.MediaItem> { it.dateAdded }.thenByDescending { it.name })
                    }
                    SortOption.NAME -> {
                        if (sortOrd == SortOrder.ASCENDING) filtered.sortedWith(compareBy({ it.name.lowercase() }, { it.dateAdded }))
                        else filtered.sortedWith(compareByDescending<com.aktarjabed.jagallery.data.model.MediaItem> { it.name.lowercase() }.thenByDescending { it.dateAdded })
                    }
                    SortOption.SIZE -> {
                        if (sortOrd == SortOrder.ASCENDING) filtered.sortedWith(compareBy({ it.size }, { it.dateAdded }))
                        else filtered.sortedWith(compareByDescending<com.aktarjabed.jagallery.data.model.MediaItem> { it.size }.thenByDescending { it.dateAdded })
                    }
                }

                if (filtered.isEmpty()) MediaLoadResult.Empty else MediaLoadResult.Success(filtered)
            }
            else -> result
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MediaLoadResult.Loading)

    fun setSource(source: MediaSource) {
        _source.value = source
    }

}
