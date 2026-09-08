package com.aktarjabed.jagallery.ui.screens.trash

import androidx.compose.material.icons.Icons
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import android.widget.Toast
import com.aktarjabed.jagallery.util.FileUtils
import com.aktarjabed.jagallery.data.model.MediaLoadResult
import com.aktarjabed.jagallery.ui.common.selection.DeleteOperationState
import com.aktarjabed.jagallery.ui.common.components.DeleteConfirmationDialog
import com.aktarjabed.jagallery.data.model.PendingDeleteBatch
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.aktarjabed.jagallery.R
import com.aktarjabed.jagallery.data.model.MediaSource
import com.aktarjabed.jagallery.ui.common.components.MediaCollectionContent

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

    LaunchedEffect(viewModel.operationEvent) {
        viewModel.operationEvent.collect { event ->
            when (event) {
                is com.aktarjabed.jagallery.ui.common.OperationEvent.Error -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                is com.aktarjabed.jagallery.ui.common.OperationEvent.Success -> Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    val loadResult by viewModel.trashedMediaLoadResult.collectAsStateWithLifecycle()
    val allAlbums by viewModel.allAlbums.collectAsStateWithLifecycle()

    var showEmptyTrashDialog by remember { mutableStateOf(false) }

    var pendingEmptyTrashBatch by remember { mutableStateOf<PendingDeleteBatch?>(null) }

    val batchState by viewModel.batchManager.batchState.collectAsStateWithLifecycle()

    com.aktarjabed.jagallery.ui.common.selection.BatchOperationObserver(
        batchState = batchState,
        onChunkResult = { resultCode -> viewModel.batchManager.onBatchChunkResult(resultCode) },
        onComplete = { result ->
            if (result.tag == "EMPTY_TRASH") {
                if (result.succeededIds.isNotEmpty()) {
                    viewModel.removeDeletedItems(result.succeededIds)
                    viewModel.refreshAll(context)
                }
                if (result.cancelled && result.succeededIds.isNotEmpty()) {
                    Toast.makeText(
                        context,
                        context.getString(
                            R.string.batch_partially_processed,
                            result.succeededIds.size,
                            pendingEmptyTrashBatch?.count ?: 0
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                pendingEmptyTrashBatch = null
            }
            viewModel.batchManager.clearState()
        }
    )

    if (showEmptyTrashDialog) {
        val items = (loadResult as? MediaLoadResult.Success)?.items ?: emptyList()
        val count = items.size

        DeleteConfirmationDialog(
            count = count,
            isPermanent = true,
            onConfirm = {
                showEmptyTrashDialog = false
                if (items.isNotEmpty()) {
                    val batch = PendingDeleteBatch(
                        ids = items.map { it.id },
                        uris = items.map { it.uri }
                    )
                    when (val requestResult = FileUtils.createDeleteRequests(context.contentResolver, batch.uris)) {
                        is FileUtils.RequestCreationResult.Success -> {
                            if (requestResult.chunks.isNotEmpty()) {
                                pendingEmptyTrashBatch = batch
                                viewModel.batchManager.startBatch(requestResult.chunks, "EMPTY_TRASH")
                            }
                        }
                        is FileUtils.RequestCreationResult.Unsupported -> {
                            val success = FileUtils.deleteMediaItems(context.contentResolver, batch.uris)
                            if (success) {
                                viewModel.removeDeletedItems(batch.ids)
                            } else {
                                Toast.makeText(context, context.getString(R.string.failed_to_delete_media), Toast.LENGTH_SHORT).show()
                            }
                        }
                        is FileUtils.RequestCreationResult.Error -> {
                            Toast.makeText(context, context.getString(R.string.failed_to_delete_media), Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            },
            onDismiss = {
                showEmptyTrashDialog = false
            }
        )
    }

    MediaCollectionContent(
        loadResult = loadResult,
        allAlbums = allAlbums,
        batchManager = viewModel.batchManager,
        onRemoveDeletedItems = { deletedIds ->
            viewModel.removeDeletedItems(deletedIds)
            // No refreshAll here -- observer handles post-delete rescan
        },
        onRestoreSelected = { restoredItems ->
            viewModel.refreshAll(context) // refresh needed on restore path
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
                },
                actions = {
                    val items = (loadResult as? MediaLoadResult.Success)?.items ?: emptyList()
                    if (items.isNotEmpty()) {
                        IconButton(onClick = { showEmptyTrashDialog = true }) {
                            Icon(Icons.Default.DeleteForever, contentDescription = stringResource(R.string.empty_trash))
                        }
                    }
                }
            )
        },
        onItemClick = { item ->
            onNavigateToViewer(item.id, MediaSource.Trash)
        }
    )
}
