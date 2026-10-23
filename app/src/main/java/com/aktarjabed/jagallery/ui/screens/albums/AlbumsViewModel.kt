package com.aktarjabed.jagallery.ui.screens.albums

import android.content.Context
import android.net.Uri
import androidx.lifecycle.viewModelScope
import com.aktarjabed.jagallery.R
import com.aktarjabed.jagallery.data.model.Album
import com.aktarjabed.jagallery.data.model.DeleteRequestChunk
import com.aktarjabed.jagallery.data.model.MediaItem
import com.aktarjabed.jagallery.data.model.MediaLoadResult
import com.aktarjabed.jagallery.data.repository.MediaRepository
import com.aktarjabed.jagallery.domain.MediaOperations
import com.aktarjabed.jagallery.domain.MoveOperationResult
import com.aktarjabed.jagallery.ui.common.BaseMediaViewModel
import com.aktarjabed.jagallery.ui.common.OperationEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface AlbumsUiState {
    data object Loading : AlbumsUiState
    data class Success(
        val albums: List<Album>,
        val totalCount: Int,
        val coverUri: Uri?,
        val rawItems: List<MediaItem>,
        val trashCount: Int = 0,
        val vaultCount: Int = 0
    ) : AlbumsUiState
    data object Empty : AlbumsUiState
    data class Error(val cause: Throwable) : AlbumsUiState
}

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    repository: MediaRepository,
    mediaOperations: MediaOperations,
    @ApplicationContext private val context: Context
) : BaseMediaViewModel(mediaOperations, repository) {

    val uiState: StateFlow<AlbumsUiState> = combine(
        repository.mediaLoadResult,
        repository.mediaDao.getVaultMedia(),
        repository.mediaDao.getTrashMedia(),
        repository.trashedMediaLoadResult
    ) { result, vaultItems, trashItems, trashLoadResult ->
        val visibleTrashCount = when (trashLoadResult) {
            is MediaLoadResult.Success -> trashLoadResult.items.size
            is MediaLoadResult.Empty -> 0
            else -> trashItems.size
        }
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
                            val covers = bucketItems.take(3).map { it.uri }
                            val isVideo = bucketItems.all { it.isVideo } || firstItem.bucketName.contains("Video", ignoreCase = true)
                            Album(
                                key = albumKey,
                                name = firstItem.bucketName.ifBlank { context.getString(R.string.internal_storage) },
                                mediaCount = bucketItems.size,
                                coverUri = firstItem.uri,
                                coverUris = covers,
                                isVideo = isVideo
                            )
                        }.sortedByDescending { it.mediaCount }

                    AlbumsUiState.Success(
                        albums = albumsList,
                        totalCount = items.size,
                        coverUri = items.firstOrNull()?.uri,
                        rawItems = items,
                        trashCount = visibleTrashCount,
                        vaultCount = vaultItems.size
                    )
                }
            }
        }
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AlbumsUiState.Loading)

    fun refreshTrashCount() {
        viewModelScope.launch {
            try {
                repository.loadTrashedMedia(context)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _operationEvent.emit(
                    OperationEvent.Error("Failed to refresh Trash count: ${e.message}")
                )
            }
        }
    }

    private val _albumRenameDeleteEvent = MutableSharedFlow<Pair<List<String>, List<DeleteRequestChunk>>>()
    val albumRenameDeleteEvent = _albumRenameDeleteEvent.asSharedFlow()

    fun renameAlbum(context: Context, sourceAlbum: Album, newAlbumName: String, items: List<MediaItem>) {
        viewModelScope.launch {
            try {
                when (val result = mediaOperations.renameAlbum(context, sourceAlbum, newAlbumName, items)) {
                    is MoveOperationResult.RequestSourceDelete -> {
                        if (result.pendingIntents.isNotEmpty()) {
                            _albumRenameDeleteEvent.emit(
                                Pair(result.successfulCopies.map { it.first.id }, result.pendingIntents)
                            )
                        } else {
                            super.removeDeletedItems(result.successfulCopies.map { it.first.id })
                            _operationEvent.emit(OperationEvent.Success("Album renamed successfully."))
                        }
                    }
                    is MoveOperationResult.Success -> {
                        _operationEvent.emit(OperationEvent.Success("Album renamed successfully."))
                    }
                    is MoveOperationResult.CopiedSourceRetained -> {
                         _operationEvent.emit(OperationEvent.Success("Album renamed successfully. Old album items retained."))
                    }
                    is MoveOperationResult.Error -> {
                        _operationEvent.emit(OperationEvent.Error(result.message))
                    }
                    else -> {}
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _operationEvent.emit(OperationEvent.Error(e.message ?: "Unknown error occurred"))
            }
        }
    }

    init {
        viewModelScope.launch {
            try {
                repository.loadMedia(context = context)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _operationEvent.emit(OperationEvent.Error(e.message ?: "Failed to load media"))
            }
        }
    }
}
