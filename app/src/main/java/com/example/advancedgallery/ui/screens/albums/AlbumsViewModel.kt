package com.example.advancedgallery.ui.screens.albums

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.advancedgallery.data.model.Album
import com.example.advancedgallery.data.model.MediaLoadResult
import com.example.advancedgallery.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AlbumsUiState {
    data object Loading : AlbumsUiState
    data class Success(val albums: List<Album>, val totalCount: Int, val coverUri: Uri?) : AlbumsUiState
    data object Empty : AlbumsUiState
    data class Error(val cause: Throwable) : AlbumsUiState
}

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val repository: MediaRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val uiState: StateFlow<AlbumsUiState> = repository.mediaLoadResult.map { result ->
        when (result) {
            is MediaLoadResult.Loading -> AlbumsUiState.Loading
            is MediaLoadResult.Error -> AlbumsUiState.Error(result.cause)
            is MediaLoadResult.Empty -> AlbumsUiState.Empty
            is MediaLoadResult.Success -> {
                val items = result.items
                if (items.isEmpty()) {
                    AlbumsUiState.Empty
                } else {
                    val albumsList = items.groupBy { it.bucketId }
                        .map { (bucketId, bucketItems) ->
                            val firstItem = bucketItems.first()
                            Album(
                                bucketId = bucketId,
                                name = firstItem.bucketName.ifBlank { context.getString(com.example.advancedgallery.R.string.internal_storage) },
                                mediaCount = bucketItems.size,
                                coverUri = firstItem.uri
                            )
                        }.sortedByDescending { it.mediaCount }
                    AlbumsUiState.Success(
                        albums = albumsList,
                        totalCount = items.size,
                        coverUri = items.firstOrNull()?.uri
                    )
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AlbumsUiState.Loading)

    init {
        viewModelScope.launch {
            repository.loadMedia(context = context)
        }
    }
}
