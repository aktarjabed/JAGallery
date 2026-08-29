package com.aktarjabed.jagallery.ui.screens.albums

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.aktarjabed.jagallery.data.model.Album
import com.aktarjabed.jagallery.data.model.MediaLoadResult
import com.aktarjabed.jagallery.data.repository.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.aktarjabed.jagallery.domain.MediaOperations
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.aktarjabed.jagallery.ui.common.BaseMediaViewModel

sealed interface AlbumsUiState {
    data object Loading : AlbumsUiState
    data class Success(val albums: List<Album>, val totalCount: Int, val coverUri: Uri?, val rawItems: List<com.aktarjabed.jagallery.data.model.MediaItem>) : AlbumsUiState
    data object Empty : AlbumsUiState
    data class Error(val cause: Throwable) : AlbumsUiState
}

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    repository: MediaRepository,
    mediaOperations: MediaOperations,
    @ApplicationContext private val context: Context
) : BaseMediaViewModel(mediaOperations, repository) {

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
                                name = firstItem.bucketName.ifBlank { context.getString(com.aktarjabed.jagallery.R.string.internal_storage) },
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

    private val _albumRenameDeleteEvent = MutableSharedFlow<Pair<List<String>, List<com.aktarjabed.jagallery.data.model.DeleteRequestChunk>>>()
    val albumRenameDeleteEvent = _albumRenameDeleteEvent.asSharedFlow()

    override fun removeDeletedItems(deletedIds: List<String>) {
        viewModelScope.launch {
            repository.removeDeletedItems(deletedIds)
        }
    }

    fun renameAlbum(context: android.content.Context, items: List<com.aktarjabed.jagallery.data.model.MediaItem>, newAlbumName: String) {
        viewModelScope.launch {
            when (val result = mediaOperations.renameAlbum(context, items, newAlbumName)) {
                is com.aktarjabed.jagallery.domain.MoveOperationResult.RequestSourceDelete -> {
                    if (result.pendingIntents.isNotEmpty()) {
                        _albumRenameDeleteEvent.emit(
                            Pair(result.successfulCopies.map { it.first.id }, result.pendingIntents)
                        )
                    } else {
                        repository.removeDeletedItems(result.successfulCopies.map { it.first.id })
                        _operationEvent.emit(com.aktarjabed.jagallery.ui.common.OperationEvent.Success("Album renamed successfully."))
                    }
                }
                is com.aktarjabed.jagallery.domain.MoveOperationResult.Success -> {
                    _operationEvent.emit(com.aktarjabed.jagallery.ui.common.OperationEvent.Success("Album renamed successfully."))
                }
                is com.aktarjabed.jagallery.domain.MoveOperationResult.CopiedSourceRetained -> {
                     _operationEvent.emit(com.aktarjabed.jagallery.ui.common.OperationEvent.Success("Album renamed successfully. Old album items retained."))
                }
                is com.aktarjabed.jagallery.domain.MoveOperationResult.Error -> {
                    _operationEvent.emit(com.aktarjabed.jagallery.ui.common.OperationEvent.Error(result.message))
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
