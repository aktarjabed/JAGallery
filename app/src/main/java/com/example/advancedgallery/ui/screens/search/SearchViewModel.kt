package com.example.advancedgallery.ui.screens.search

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.advancedgallery.data.model.MediaItem
import com.example.advancedgallery.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: MediaRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    val searchQuery: StateFlow<String> = savedStateHandle.getStateFlow("searchQuery", "")

    val searchResults: StateFlow<List<MediaItem>> = combine(repository.mediaItems, searchQuery) { items, query ->
        if (query.isBlank()) {
            emptyList()
        } else {
            items.filter { it.name.contains(query, ignoreCase = true) }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        savedStateHandle["searchQuery"] = query
    }

    fun removeDeletedItems(deletedIds: List<String>) {
        viewModelScope.launch {
            repository.removeDeletedItems(deletedIds)
            repository.loadMedia()
        }
    }
}
