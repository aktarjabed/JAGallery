package com.example.advancedgallery.ui.screens.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.advancedgallery.data.model.MediaItem
import com.example.advancedgallery.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: MediaRepository
) : ViewModel() {

    val favoriteItems: StateFlow<List<MediaItem>> = repository.mediaItems.map { items ->
        items.filter { it.isFavorite }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun removeDeletedItems(deletedIds: List<Long>) {
        viewModelScope.launch {
            repository.removeDeletedItems(deletedIds)
            repository.loadMedia()
        }
    }
}
