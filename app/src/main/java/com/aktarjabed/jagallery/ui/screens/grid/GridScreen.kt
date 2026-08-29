package com.aktarjabed.jagallery.ui.screens.grid

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
import com.aktarjabed.jagallery.R
import com.aktarjabed.jagallery.data.model.MediaItem
import com.aktarjabed.jagallery.data.model.MediaLoadResult
import com.aktarjabed.jagallery.data.model.MediaSource
import com.aktarjabed.jagallery.domain.MoveOperationResult
import com.aktarjabed.jagallery.ui.common.components.MediaCollectionContent
import androidx.compose.material.icons.automirrored.filled.Sort
import com.aktarjabed.jagallery.ui.common.components.SortFilterBottomSheet
import kotlinx.coroutines.launch

private data class MoveDeleteState(
    val pendingIntents: List<com.aktarjabed.jagallery.data.model.DeleteRequestChunk>,
    val currentIndex: Int,
    val processedIds: List<String>
)

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
                is com.aktarjabed.jagallery.ui.common.OperationEvent.Error -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is com.aktarjabed.jagallery.ui.common.OperationEvent.Success -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }


    val loadResult by viewModel.mediaLoadResult.collectAsStateWithLifecycle()
    val allAlbumNames by viewModel.allAlbumNames.collectAsStateWithLifecycle()
    val sortOption by viewModel.sortOption.collectAsStateWithLifecycle()
    val sortOrder by viewModel.sortOrder.collectAsStateWithLifecycle()
    val mediaFilter by viewModel.mediaFilter.collectAsStateWithLifecycle()

    var showSortFilterSheet by remember { mutableStateOf(false) }


    var moveDeleteState by remember { mutableStateOf<MoveDeleteState?>(null) }

    val moveDeleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val state = moveDeleteState ?: return@rememberLauncherForActivityResult
        if (result.resultCode != android.app.Activity.RESULT_OK) {
            // User cancelled — items from confirmed chunks are already deleted; remainder retained
            if (state.processedIds.isNotEmpty()) {
                viewModel.removeDeletedItems(state.processedIds)
            }
            Toast.makeText(context, context.getString(R.string.move_partial_copied), Toast.LENGTH_LONG).show()
            moveDeleteState = null
            return@rememberLauncherForActivityResult
        }
        val confirmedIds = state.pendingIntents[state.currentIndex].ids
        val nextProcessed = state.processedIds + confirmedIds

        val nextIndex = state.currentIndex + 1
        if (nextIndex < state.pendingIntents.size) {
            moveDeleteState = state.copy(currentIndex = nextIndex, processedIds = nextProcessed)
        } else {
            viewModel.removeDeletedItems(nextProcessed)
            Toast.makeText(context, context.getString(R.string.move_completed), Toast.LENGTH_SHORT).show()
            moveDeleteState = null
        }
    }

    LaunchedEffect(moveDeleteState) {
        val state = moveDeleteState ?: return@LaunchedEffect
        if (state.currentIndex < state.pendingIntents.size) {
            moveDeleteLauncher.launch(
                IntentSenderRequest.Builder(
                    state.pendingIntents[state.currentIndex].pendingIntent.intentSender
                ).build()
            )
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
        allAlbumNames = allAlbumNames,
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
                            moveDeleteState = MoveDeleteState(
                                pendingIntents = moveResult.pendingIntents,
                                currentIndex = 0,
                                processedIds = emptyList()
                            )
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
                val results = viewModel.copyMediaBatch(context, items, targetAlbum)
                val failed = results.count { it == null }
                if (failed == 0) {
                    Toast.makeText(context,
                        context.getString(R.string.copy_completed), Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context,
                        context.getString(R.string.copy_partial_n_failed, failed), Toast.LENGTH_LONG).show()
                }
                // Removed viewModel.loadMedia(context) - BaseMediaViewModel does not expose it natively, use repository.loadMedia if needed.
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
