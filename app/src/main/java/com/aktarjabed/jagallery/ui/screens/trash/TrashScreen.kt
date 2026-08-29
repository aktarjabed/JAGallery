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
    val allAlbumNames by viewModel.allAlbumNames.collectAsStateWithLifecycle()

    var showEmptyTrashDialog by remember { mutableStateOf(false) }
    var emptyTrashState by remember { mutableStateOf<DeleteOperationState>(DeleteOperationState.Idle) }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val state = emptyTrashState
        if (state is DeleteOperationState.SystemConfirmation) {
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val currentChunkIds = state.pendingIntents[state.currentIndex].ids
                val updatedProcessedIds = state.processedIds + currentChunkIds

                if (state.currentIndex + 1 < state.pendingIntents.size) {
                    val nextIndex = state.currentIndex + 1
                    emptyTrashState = state.copy(currentIndex = nextIndex, processedIds = updatedProcessedIds)
                } else {
                    viewModel.removeDeletedItems(updatedProcessedIds)
                    viewModel.refreshAll(context)
                    emptyTrashState = DeleteOperationState.Idle
                }
            } else {
                if (state.processedIds.isNotEmpty()) {
                    viewModel.removeDeletedItems(state.processedIds)
                    viewModel.refreshAll(context)
                }
                emptyTrashState = DeleteOperationState.Idle
            }
        } else {
            emptyTrashState = DeleteOperationState.Idle
        }
    }

    LaunchedEffect(emptyTrashState) {
        val state = emptyTrashState
        if (state is DeleteOperationState.SystemConfirmation && state.pendingIntents.isNotEmpty()) {
            val intentSender = state.pendingIntents[state.currentIndex].pendingIntent.intentSender
            deleteLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
        }
    }

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
                    val pendingIntents = FileUtils.createDeleteRequests(context.contentResolver, batch.uris)
                    if (pendingIntents.isNotEmpty()) {
                        emptyTrashState = DeleteOperationState.SystemConfirmation(
                            batch = batch,
                            pendingIntents = pendingIntents,
                            currentIndex = 0
                        )
                    } else {
                        val success = FileUtils.deleteMediaItems(context.contentResolver, batch.uris)
                        if (success) {
                            viewModel.removeDeletedItems(batch.ids)
                        } else {
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
        allAlbumNames = allAlbumNames,
        onRemoveDeletedItems = { deletedIds ->
            viewModel.removeDeletedItems(deletedIds)
            // No refreshAll here -- observer handles post-delete rescan
        },
        onRestoreSelected = { restoredItems ->
            viewModel.removeDeletedItems(restoredItems.map { it.id })
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
