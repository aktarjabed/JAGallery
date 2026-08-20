package com.example.advancedgallery.ui.screens.search

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
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
fun SearchScreen(
    onNavigateToViewer: (mediaId: String, source: MediaSource) -> Unit,
    onBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val selectionState = rememberSelectionState()

    Scaffold(
        topBar = {
            MediaSelectionHandler(
                items = searchResults,
                selectionState = selectionState,
                onRemoveDeletedItems = { viewModel.removeDeletedItems(it) },
                topBarContent = {
                    TopAppBar(
                        title = {
                            TextField(
                                value = query,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                placeholder = { Text(stringResource(R.string.search_hint)) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
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
            items = searchResults,
            selectedIds = selectionState.selectedIds,
            selectionMode = selectionState.selectionMode,
            emptyIcon = Icons.Default.Search,
            emptyMessage = if (query.isBlank()) stringResource(R.string.search_hint) else stringResource(R.string.no_search_results),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            onItemClick = { item ->
                if (selectionState.selectionMode) {
                    selectionState.toggleSelection(item.id)
                } else {
                    onNavigateToViewer(item.id, MediaSource.Search(query))
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
