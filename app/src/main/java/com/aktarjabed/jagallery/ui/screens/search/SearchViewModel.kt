package com.aktarjabed.jagallery.ui.screens.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.aktarjabed.jagallery.data.model.MediaLoadResult
import com.aktarjabed.jagallery.data.repository.MediaRepository
import com.aktarjabed.jagallery.domain.MediaOperations
import com.aktarjabed.jagallery.ui.common.BaseMediaViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    repository: MediaRepository,
    mediaOperations: MediaOperations,
    private val savedStateHandle: SavedStateHandle
) : BaseMediaViewModel(mediaOperations, repository) {

    val searchQuery: StateFlow<String> = savedStateHandle.getStateFlow("searchQuery", "")

    val searchResult: StateFlow<MediaLoadResult> = combine(
        repository.mediaLoadResult,
        searchQuery.debounce(300L)
    ) { result, query ->
        when (result) {
            is MediaLoadResult.Success -> {
                if (query.isBlank()) {
                    MediaLoadResult.Empty
                } else {
                    val filtered = result.items.filter { it.name.contains(query, ignoreCase = true) }
                    if (filtered.isEmpty()) MediaLoadResult.Empty else MediaLoadResult.Success(filtered)
                }
            }
            else -> result
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MediaLoadResult.Loading)

    fun updateSearchQuery(query: String) {
        savedStateHandle["searchQuery"] = query
    }
}
