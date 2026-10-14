package com.aktarjabed.jagallery.ui.screens.favorites

import androidx.compose.material.icons.Icons
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.aktarjabed.jagallery.R
import com.aktarjabed.jagallery.data.model.MediaSource
import com.aktarjabed.jagallery.ui.common.components.MediaCollectionContent

import androidx.compose.material.icons.filled.Favorite
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.aktarjabed.jagallery.ui.theme.PrimaryPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onNavigateToViewer: (mediaId: String, source: MediaSource) -> Unit,
    onBack: () -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val loadResult by viewModel.mediaLoadResult.collectAsStateWithLifecycle()
    val allAlbums by viewModel.allAlbums.collectAsStateWithLifecycle()

    com.aktarjabed.jagallery.ui.common.components.OperationToastEffect(viewModel.operationEvent)

    MediaCollectionContent(
        loadResult = loadResult,
        allAlbums = allAlbums,
        batchManager = viewModel.batchManager,
        onRemoveDeletedItems = { viewModel.removeDeletedItems(it) },
        emptyIcon = Icons.Filled.Favorite,
        emptyMessage = stringResource(R.string.no_favorites_found),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.tab_favorites),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        },
        onItemClick = { item ->
            onNavigateToViewer(item.id, MediaSource.Favorites)
        }
    )
}
