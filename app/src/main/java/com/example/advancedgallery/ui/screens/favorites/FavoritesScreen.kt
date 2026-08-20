package com.example.advancedgallery.ui.screens.favorites

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
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
fun FavoritesScreen(
    onNavigateToViewer: (mediaId: String, source: MediaSource) -> Unit,
    onBack: () -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val favoriteItems by viewModel.favoriteItems.collectAsState()
    val selectionState = rememberSelectionState()

    Scaffold(
        topBar = {
            MediaSelectionHandler(
                items = favoriteItems,
                selectionState = selectionState,
                onRemoveDeletedItems = { viewModel.removeDeletedItems(it) },
                topBarContent = {
                    TopAppBar(
                        title = { Text(stringResource(R.string.tab_favorites)) },
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
            items = favoriteItems,
            selectedIds = selectionState.selectedIds,
            selectionMode = selectionState.selectionMode,
            emptyIcon = Icons.Default.FavoriteBorder,
            emptyMessage = stringResource(R.string.no_favorites_found),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            onItemClick = { item ->
                if (selectionState.selectionMode) {
                    selectionState.toggleSelection(item.id)
                } else {
                    onNavigateToViewer(item.id, MediaSource.Favorites)
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
