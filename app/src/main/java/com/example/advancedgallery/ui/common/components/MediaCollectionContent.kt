package com.example.advancedgallery.ui.common.components

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
import com.example.advancedgallery.data.model.MediaItem
import com.example.advancedgallery.data.model.MediaLoadResult
import com.example.advancedgallery.ui.common.selection.MediaSelectionHandler
import com.example.advancedgallery.ui.common.selection.rememberSelectionState

@Composable
fun MediaCollectionContent(
    loadResult: MediaLoadResult,
    onRemoveDeletedItems: (List<String>) -> Unit,
    emptyIcon: ImageVector,
    emptyMessage: String,
    topBar: @Composable () -> Unit,
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val selectionState = rememberSelectionState()
    val items = (loadResult as? MediaLoadResult.Success)?.items ?: emptyList()

    Scaffold(
        topBar = {
            MediaSelectionHandler(
                items = items,
                selectionState = selectionState,
                onRemoveDeletedItems = onRemoveDeletedItems,
                topBarContent = topBar
            )
        },
        modifier = modifier
    ) { padding ->
        when (loadResult) {
            is MediaLoadResult.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is MediaLoadResult.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = loadResult.cause.localizedMessage ?: "Error loading media",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
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
