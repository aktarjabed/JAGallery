package com.example.advancedgallery.ui.screens.grid

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.advancedgallery.R
import com.example.advancedgallery.data.model.MediaSource
import com.example.advancedgallery.ui.common.components.MediaGrid
import com.example.advancedgallery.ui.common.selection.MediaSelectionHandler
import com.example.advancedgallery.ui.common.selection.rememberSelectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GridScreen(
    source: MediaSource,
    onNavigateToViewer: (mediaId: String, source: MediaSource) -> Unit,
    onBack: () -> Unit,
    viewModel: GridViewModel = hiltViewModel()
) {
    LaunchedEffect(source) {
        viewModel.setSource(source)
    }

    val mediaItems by viewModel.mediaItems.collectAsState()
    val selectionState = rememberSelectionState()

    val albumTitle = stringResource(R.string.album_default_title)
    val titleText = remember(source, mediaItems, albumTitle) {
        when (source) {
            is MediaSource.Album -> mediaItems.firstOrNull()?.bucketName?.ifBlank { albumTitle } ?: albumTitle
            else -> null
        }
    }

    Scaffold(
        topBar = {
            MediaSelectionHandler(
                items = mediaItems,
                selectionState = selectionState,
                onRemoveDeletedItems = { viewModel.removeDeletedItems(it) },
                topBarContent = {
                    TopAppBar(
                        title = {
                            Text(
                                titleText ?: stringResource(R.string.tab_all_media)
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                            }
                        }
                    )
                },
                content = {}
            )
        }
    ) { padding ->
        MediaGrid(
            items = mediaItems,
            selectedIds = selectionState.selectedIds,
            selectionMode = selectionState.selectionMode,
            emptyIcon = Icons.Default.PhotoLibrary,
            emptyMessage = stringResource(R.string.no_media_found),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            onItemClick = { item ->
                if (selectionState.selectionMode) {
                    selectionState.toggleSelection(item.id)
                } else {
                    onNavigateToViewer(item.id, source)
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
