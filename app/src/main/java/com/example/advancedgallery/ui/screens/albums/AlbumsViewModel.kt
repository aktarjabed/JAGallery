package com.example.advancedgallery.ui.screens.albums

import android.content.Context
import android.net.Uri
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
import com.example.advancedgallery.domain.MediaOperations
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.example.advancedgallery.ui.common.BaseMediaViewModel

sealed interface AlbumsUiState {
    data object Loading : AlbumsUiState
    data class Success(val albums: List<Album>, val totalCount: Int, val coverUri: Uri?, val rawItems: List<com.example.advancedgallery.data.model.MediaItem>) : AlbumsUiState
    data object Empty : AlbumsUiState
    data class Error(val cause: Throwable) : AlbumsUiState
}

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    private val repository: MediaRepository,
    mediaOperations: MediaOperations,
    @ApplicationContext private val context: Context
) : BaseMediaViewModel(mediaOperations) {

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
                    val albumsList = items.groupBy { it.albumKey }
                        .map { (albumKey, bucketItems) ->
                            val firstItem = bucketItems.first()
                            Album(
                                key = albumKey,
                                name = firstItem.bucketName.ifBlank { context.getString(com.example.advancedgallery.R.string.internal_storage) },
                                mediaCount = bucketItems.size,
                                coverUri = firstItem.uri
                            )
                        }.sortedByDescending { it.mediaCount }
                    AlbumsUiState.Success(
                        albums = albumsList,
                        totalCount = items.size,
                        coverUri = items.firstOrNull()?.uri,
                        rawItems = items
                    )
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AlbumsUiState.Loading)

    private val _pendingAlbumRenameDelete = MutableSharedFlow<android.app.PendingIntent?>()
    val pendingAlbumRenameDelete = _pendingAlbumRenameDelete.asSharedFlow()

    private val _pendingDeletedUris = MutableSharedFlow<List<String>>()
    val pendingDeletedUris = _pendingDeletedUris.asSharedFlow()

    override fun removeDeletedItems(deletedIds: List<String>) {
        viewModelScope.launch {
            repository.removeDeletedItems(deletedIds)
        }
    }

    fun renameAlbum(context: android.content.Context, items: List<com.example.advancedgallery.data.model.MediaItem>, newAlbumName: String) {
        viewModelScope.launch {
            when (val result = mediaOperations.renameAlbum(context, items, newAlbumName)) {
                is com.example.advancedgallery.domain.MoveOperationResult.RequestSourceDelete -> {
                    if (result.pendingIntent != null) {
                        _pendingDeletedUris.emit(result.successfulCopies.map { it.first.id })
                        _pendingAlbumRenameDelete.emit(result.pendingIntent)
                    } else {
                        repository.removeDeletedItems(result.successfulCopies.map { it.first.id })
                        _operationEvent.emit(com.example.advancedgallery.ui.common.OperationEvent.Success("Album renamed successfully."))
                    }
                }
                is com.example.advancedgallery.domain.MoveOperationResult.Success -> {
                    _operationEvent.emit(com.example.advancedgallery.ui.common.OperationEvent.Success("Album renamed successfully."))
                }
                is com.example.advancedgallery.domain.MoveOperationResult.CopiedSourceRetained -> {
                     _operationEvent.emit(com.example.advancedgallery.ui.common.OperationEvent.Success("Album renamed successfully. Old album items retained."))
                }
                is com.example.advancedgallery.domain.MoveOperationResult.Error -> {
                    _operationEvent.emit(com.example.advancedgallery.ui.common.OperationEvent.Error(result.message))
                }
            }
        }
    }

    init {
        viewModelScope.launch {
            repository.loadMedia(context = context)
        }
    }
}
