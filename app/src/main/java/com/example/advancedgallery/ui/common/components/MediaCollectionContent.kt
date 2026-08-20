package com.example.advancedgallery.ui.common.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.advancedgallery.data.model.MediaItem
import com.example.advancedgallery.ui.common.selection.MediaSelectionHandler
import com.example.advancedgallery.ui.common.selection.rememberSelectionState

@Composable
fun MediaCollectionContent(
    items: List<MediaItem>,
    onRemoveDeletedItems: (List<String>) -> Unit,
    emptyIcon: ImageVector,
    emptyMessage: String,
    topBar: @Composable () -> Unit,
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectionState = rememberSelectionState()

    Scaffold(
        topBar = {
            MediaSelectionHandler(
                items = items,
                selectionState = selectionState,
                onRemoveDeletedItems = onRemoveDeletedItems,
                topBarContent = topBar,
                content = {}
            )
        },
        modifier = modifier
    ) { padding ->
        MediaGrid(
            items = items,
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
