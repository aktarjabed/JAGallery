package com.example.advancedgallery.ui.screens.trash

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.advancedgallery.R
import com.example.advancedgallery.data.model.MediaSource
import com.example.advancedgallery.ui.common.components.MediaCollectionContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    onNavigateToViewer: (mediaId: String, source: MediaSource) -> Unit,
    onBack: () -> Unit,
    viewModel: TrashViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadTrashedMedia(context)
    }

    val loadResult by viewModel.trashedMediaLoadResult.collectAsState()

    MediaCollectionContent(
        loadResult = loadResult,
        onRemoveDeletedItems = { deletedIds ->
            viewModel.removeDeletedItems(deletedIds)
            viewModel.refreshAll(context)
        },
        emptyIcon = Icons.Default.Delete,
        emptyMessage = stringResource(R.string.no_trash_found),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_trash)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        onItemClick = { item ->
            onNavigateToViewer(item.id, MediaSource.Trash)
        }
    )
}
