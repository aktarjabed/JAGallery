package com.example.advancedgallery.ui.screens.trash

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import android.widget.Toast
import com.example.advancedgallery.util.FileUtils
import com.example.advancedgallery.data.model.MediaLoadResult
import com.example.advancedgallery.ui.common.selection.DeleteOperationState
import com.example.advancedgallery.ui.common.components.DeleteConfirmationDialog
import com.example.advancedgallery.data.model.PendingDeleteBatch
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

    var showEmptyTrashDialog by remember { mutableStateOf(false) }
    var emptyTrashState by remember { mutableStateOf<DeleteOperationState>(DeleteOperationState.Idle) }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val state = emptyTrashState
        if (state is DeleteOperationState.SystemConfirmation) {
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val chunkSize = com.example.advancedgallery.util.Constants.MAX_BATCH_SIZE
                val processedBatchIds = state.batch.ids.drop(state.currentIndex * chunkSize).take(chunkSize)
                val updatedProcessedIds = state.processedIds + processedBatchIds

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
            val intentSender = state.pendingIntents[state.currentIndex].intentSender
            deleteLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
        }
    }

    if (showEmptyTrashDialog) {
        val items = (loadResult as? MediaLoadResult.Success)?.items ?: emptyList()
        val count = items.size

        AlertDialog(
            onDismissRequest = { showEmptyTrashDialog = false },
            title = { Text(stringResource(R.string.empty_trash)) },
            text = { Text(stringResource(R.string.delete_confirm_message, count)) },
            confirmButton = {
                TextButton(onClick = {
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
                                // Only update local state. MediaStore observer will rescan and clean up.
                                // refreshAll is not needed here and causes re-flash.
                            } else {
                                Toast.makeText(context, context.getString(R.string.failed_to_delete_media), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }) {
                    Text(stringResource(R.string.empty_trash))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEmptyTrashDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    MediaCollectionContent(
        loadResult = loadResult,
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
