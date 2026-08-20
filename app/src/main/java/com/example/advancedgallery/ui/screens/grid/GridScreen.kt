package com.example.advancedgallery.ui.screens.grid

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.advancedgallery.R
import com.example.advancedgallery.data.model.MediaSource
import com.example.advancedgallery.ui.common.components.MediaCollectionContent

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

    val albumTitle = stringResource(R.string.album_default_title)
    val titleText = remember(source, mediaItems, albumTitle) {
        when (source) {
            is MediaSource.Album -> mediaItems.firstOrNull()?.bucketName?.ifBlank { albumTitle } ?: albumTitle
            else -> null
        }
    }

    MediaCollectionContent(
        items = mediaItems,
        onRemoveDeletedItems = { viewModel.removeDeletedItems(it) },
        emptyIcon = Icons.Default.PhotoLibrary,
        emptyMessage = stringResource(R.string.no_media_found),
        topBar = {
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
        onItemClick = { item ->
            onNavigateToViewer(item.id, source)
        }
    )
}
