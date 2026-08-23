package com.example.advancedgallery.ui.screens.grid

import androidx.compose.material.icons.Icons
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    LaunchedEffect(viewModel.operationEvent) {
        viewModel.operationEvent.collect { event ->
            when (event) {
                is com.example.advancedgallery.ui.common.OperationEvent.Error -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is com.example.advancedgallery.ui.common.OperationEvent.Success -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }


    val loadResult by viewModel.mediaLoadResult.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val mediaFilter by viewModel.mediaFilter.collectAsStateWithLifecycle()

    var showSortFilterSheet by remember { mutableStateOf(false) }


    data class MoveDeleteState(
        val copies: List<Pair<MediaItem, Uri>>,
        val pendingIntents: List<android.app.PendingIntent>,
        val currentIndex: Int = 0,
        val processedIds: List<String> = emptyList()
    )

    var moveDeleteState by remember { mutableStateOf<MoveDeleteState?>(null) }

    val moveDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val state = moveDeleteState
        if (state != null) {
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val chunkSize = com.example.advancedgallery.util.Constants.MAX_BATCH_SIZE
                val processedBatchIds = state.copies.map { it.first.id }.drop(state.currentIndex * chunkSize).take(chunkSize)
                val updatedProcessedIds = state.processedIds + processedBatchIds

                if (state.currentIndex + 1 < state.pendingIntents.size) {
                    val nextIndex = state.currentIndex + 1
                    moveDeleteState = state.copy(currentIndex = nextIndex, processedIds = updatedProcessedIds)
                } else {
                    viewModel.removeDeletedItems(updatedProcessedIds)
                    Toast.makeText(context, context.getString(R.string.move_completed), Toast.LENGTH_SHORT).show()
                    moveDeleteState = null
                }
            } else {
                if (state.processedIds.isNotEmpty()) {
                    viewModel.removeDeletedItems(state.processedIds)
                }
                Toast.makeText(context, context.getString(R.string.move_partial_copied), Toast.LENGTH_LONG).show()
                moveDeleteState = null
            }
        }
    }

    LaunchedEffect(moveDeleteState) {
        val state = moveDeleteState
        if (state != null && state.pendingIntents.isNotEmpty()) {
            val intentSender = state.pendingIntents[state.currentIndex].intentSender
            moveDeleteLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
        }
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
                        if (moveResult.failedItems.isNotEmpty()) {
                            Toast.makeText(context, context.getString(R.string.move_partial_n_failed, moveResult.failedItems.size), Toast.LENGTH_LONG).show()
                        }
                        if (moveResult.pendingIntents.isNotEmpty()) {
                            moveDeleteState = MoveDeleteState(moveResult.successfulCopies, moveResult.pendingIntents)
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
        onCopySelected = { items, targetAlbum ->
            coroutineScope.launch {
                var successCount = 0
                for (item in items) {
                    when (viewModel.copyMedia(context, item, targetAlbum)) {
                        is com.example.advancedgallery.domain.OperationResult.Success -> successCount++
                        else -> {}
                    }
                }
                if (successCount == items.size) {
                    Toast.makeText(context, "Copy completed", Toast.LENGTH_SHORT).show()
                } else if (successCount > 0) {
                    Toast.makeText(context, "Partial copy completed", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Copy failed", Toast.LENGTH_SHORT).show()
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
