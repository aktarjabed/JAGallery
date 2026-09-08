package com.aktarjabed.jagallery.ui.common.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.aktarjabed.jagallery.data.model.MediaItem
import com.aktarjabed.jagallery.data.model.MediaLoadResult
import com.aktarjabed.jagallery.ui.common.selection.MediaSelectionHandler
import com.aktarjabed.jagallery.ui.common.selection.rememberSelectionState
import com.aktarjabed.jagallery.ui.common.selection.BatchOperationManager

@Composable
fun MediaCollectionContent(
    loadResult: MediaLoadResult,
    allAlbums: List<com.aktarjabed.jagallery.data.model.Album>,
    batchManager: BatchOperationManager,
    onRemoveDeletedItems: (List<String>) -> Unit,
    emptyIcon: ImageVector,
    emptyMessage: String,
    topBar: @Composable () -> Unit,
    onItemClick: (MediaItem) -> Unit,
    onHideSelected: ((List<MediaItem>) -> Unit)? = null,
    onUnhideSelected: ((List<MediaItem>) -> Unit)? = null,
    onRestoreSelected: ((List<MediaItem>) -> Unit)? = null,
    onMoveSelected: ((List<MediaItem>, com.aktarjabed.jagallery.data.model.AlbumDestination) -> Unit)? = null,
    onCopySelected: ((List<MediaItem>, com.aktarjabed.jagallery.data.model.AlbumDestination) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val selectionState = rememberSelectionState()
    val items = (loadResult as? MediaLoadResult.Success)?.items ?: emptyList()

    Scaffold(
        topBar = {
            MediaSelectionHandler(
                items = items,
                albums = allAlbums,
                selectionState = selectionState,
                batchManager = batchManager,
                onRemoveDeletedItems = onRemoveDeletedItems,
                onHideSelected = onHideSelected,
                onUnhideSelected = onUnhideSelected,
                onRestoreSelected = onRestoreSelected,
                onMoveSelected = onMoveSelected,
                onCopySelected = onCopySelected,
                topBarContent = topBar
            )
        },
        modifier = modifier
    ) { padding ->
        when (loadResult) {
            is MediaLoadResult.Loading -> {
                FullScreenLoading(
                    modifier = Modifier.padding(padding)
                )
            }
            is MediaLoadResult.Error -> {
                FullScreenError(
                    message = loadResult.cause.localizedMessage ?: "Error loading media",
                    modifier = Modifier.padding(padding)
                )
            }
            is MediaLoadResult.Empty -> {
                MediaGrid(
                    items = emptyList(),
                    selectedIds = emptySet(),
                    selectionMode = false,
                    emptyIcon = emptyIcon,
                    emptyMessage = emptyMessage,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    onItemClick = {},
                    onItemLongClick = {}
                )
            }
            is MediaLoadResult.Success -> {
                MediaGrid(
                    items = loadResult.items,
                    selectedIds = selectionState.selectedIds,
                    selectionMode = selectionState.selectionMode,
                    emptyIcon = emptyIcon,
                    emptyMessage = emptyMessage,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    onItemClick = { item ->
                        if (selectionState.selectionMode) {
                            selectionState.toggleSelection(item.id)
                        } else {
                            onItemClick(item)
                        }
                    },
                    onItemLongClick = { item ->
                        if (!selectionState.selectionMode) {
                            selectionState.startSelection(item.id)
                        }
                    }
                )
            }
        }
    }
}
