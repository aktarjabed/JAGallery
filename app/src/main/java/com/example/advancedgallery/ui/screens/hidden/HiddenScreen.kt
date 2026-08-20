package com.example.advancedgallery.ui.screens.hidden

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.advancedgallery.R
import com.example.advancedgallery.data.model.MediaSource
import com.example.advancedgallery.ui.common.components.MediaCollectionContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenScreen(
    onNavigateToViewer: (mediaId: String, source: MediaSource) -> Unit,
    onBack: () -> Unit,
    viewModel: HiddenViewModel = hiltViewModel()
) {
    val loadResult by viewModel.mediaLoadResult.collectAsState()

    MediaCollectionContent(
        loadResult = loadResult,
        onRemoveDeletedItems = { viewModel.removeDeletedItems(it) },
        onUnhideSelected = { selected ->
            viewModel.unhideMediaBatch(selected)
        },
        emptyIcon = Icons.Default.VisibilityOff,
        emptyMessage = stringResource(R.string.no_hidden_found),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.tab_hidden_media)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        onItemClick = { item ->
            onNavigateToViewer(item.id, MediaSource.Hidden)
        }
    )
}
