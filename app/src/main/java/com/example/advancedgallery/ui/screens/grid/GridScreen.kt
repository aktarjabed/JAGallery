package com.example.advancedgallery.ui.screens.grid

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoLibrary
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.advancedgallery.R
import com.example.advancedgallery.data.model.MediaItem
import com.example.advancedgallery.data.model.MediaLoadResult
import com.example.advancedgallery.data.model.MediaSource
import com.example.advancedgallery.domain.MoveOperationResult
import com.example.advancedgallery.ui.common.components.MediaCollectionContent
import androidx.compose.material.icons.automirrored.filled.Sort
import com.example.advancedgallery.ui.common.components.SortFilterBottomSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GridScreen(
    source: MediaSource,
    onNavigateToViewer: (mediaId: String, source: MediaSource) -> Unit,
    onBack: () -> Unit,
    viewModel: GridViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(source) {
        viewModel.setSource(source)
    }


    val loadResult by viewModel.mediaLoadResult.collectAsState()
    val sortOption by viewModel.sortOption.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    val mediaFilter by viewModel.mediaFilter.collectAsState()

    var showSortFilterSheet by remember { mutableStateOf(false) }


    var pendingMoveDeleteCopies by remember { mutableStateOf<List<Pair<MediaItem, Uri>>?>(null) }

    val moveDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val copies = pendingMoveDeleteCopies
        if (copies != null) {
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                viewModel.removeDeletedItems(copies.map { it.first.id })
                Toast.makeText(context, context.getString(R.string.move_completed), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, context.getString(R.string.move_partial_copied), Toast.LENGTH_LONG).show()
            }
        }
        pendingMoveDeleteCopies = null
    }

    val albumTitle = stringResource(R.string.album_default_title)
    val titleText = remember(source, loadResult, albumTitle) {
        when (source) {
            is MediaSource.Album -> {
                val items = (loadResult as? MediaLoadResult.Success)?.items
                items?.firstOrNull()?.bucketName?.ifBlank { albumTitle } ?: albumTitle
            }
            else -> null
        }
    }

    MediaCollectionContent(
        loadResult = loadResult,
        onRemoveDeletedItems = { viewModel.removeDeletedItems(it) },
        onHideSelected = { items -> viewModel.hideMediaBatch(items) },
        onMoveSelected = { items, targetAlbum ->
            coroutineScope.launch {
                val moveResult = viewModel.moveMediaBatch(context, items, targetAlbum)
                when (moveResult) {
                    is MoveOperationResult.RequestSourceDelete -> {
                        pendingMoveDeleteCopies = moveResult.successfulCopies
                        if (moveResult.failedItems.isNotEmpty()) {
                            Toast.makeText(context, context.getString(R.string.move_partial_n_failed, moveResult.failedItems.size), Toast.LENGTH_LONG).show()
                        }
                        if (moveResult.pendingIntent != null) {
                            moveDeleteLauncher.launch(IntentSenderRequest.Builder(moveResult.pendingIntent.intentSender).build())
                        } else {
                            viewModel.removeDeletedItems(moveResult.successfulCopies.map { it.first.id })
                            Toast.makeText(context, context.getString(R.string.move_completed), Toast.LENGTH_SHORT).show()
                        }
                    }
                    is MoveOperationResult.Success -> {
                        Toast.makeText(context, context.getString(R.string.move_completed), Toast.LENGTH_SHORT).show()
                    }
                    is MoveOperationResult.CopiedSourceRetained -> {
                        Toast.makeText(context, context.getString(R.string.move_partial_copied), Toast.LENGTH_LONG).show()
                    }
                    is MoveOperationResult.Error -> {
                        Toast.makeText(context, moveResult.message, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        },
        emptyIcon = Icons.Default.PhotoLibrary,
        emptyMessage = stringResource(R.string.no_media_found),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        titleText ?: stringResource(R.string.tab_all_media)
                    )
                },
                actions = {
                    IconButton(onClick = { showSortFilterSheet = true }) {
                        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort and Filter")
                    }
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
    if (showSortFilterSheet) {
        val sheetState = rememberModalBottomSheetState()
        SortFilterBottomSheet(
            sheetState = sheetState,
            currentSortOption = sortOption,
            currentSortOrder = sortOrder,
            currentFilter = mediaFilter,
            onApply = { opt, ord, filt ->
                viewModel.setSortAndFilter(opt, ord, filt)
                showSortFilterSheet = false
            },
            onDismiss = { showSortFilterSheet = false }
        )
    }
}
