package com.example.advancedgallery.ui.screens.favorites

import androidx.lifecycle.viewModelScope
import com.example.advancedgallery.data.model.MediaLoadResult
import com.example.advancedgallery.data.repository.MediaRepository
import com.example.advancedgallery.domain.MediaOperations
import com.example.advancedgallery.ui.common.BaseMediaViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    repository: MediaRepository,
    mediaOperations: MediaOperations
) : BaseMediaViewModel(mediaOperations) {

    val mediaLoadResult: StateFlow<MediaLoadResult> = repository.mediaLoadResult.map { result ->
        when (result) {
            is MediaLoadResult.Success -> {
                val favorites = result.items.filter { it.isFavorite }
                if (favorites.isEmpty()) MediaLoadResult.Empty else MediaLoadResult.Success(favorites)
            }
            else -> result
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MediaLoadResult.Loading)
}
