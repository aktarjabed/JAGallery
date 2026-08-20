package com.example.advancedgallery.ui.screens.favorites

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.advancedgallery.R
import com.example.advancedgallery.data.model.MediaSource
import com.example.advancedgallery.ui.common.components.MediaCollectionContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onNavigateToViewer: (mediaId: String, source: MediaSource) -> Unit,
    onBack: () -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val favoriteItems by viewModel.favoriteItems.collectAsState()

    MediaCollectionContent(
        items = favoriteItems,
        onRemoveDeletedItems = { viewModel.removeDeletedItems(it) },
        emptyIcon = Icons.Default.FavoriteBorder,
        emptyMessage = stringResource(R.string.no_favorites_found),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_favorites)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        onItemClick = { item ->
            onNavigateToViewer(item.id, MediaSource.Favorites)
        }
    )
}
