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
                viewModel.removeDeletedItems(state.batch.ids)
                viewModel.refreshAll(context)
            }
        }
        emptyTrashState = DeleteOperationState.Idle
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
                        val pendingIntent = FileUtils.createDeleteRequest(context.contentResolver, batch.uris)
                        if (pendingIntent != null) {
                            emptyTrashState = DeleteOperationState.SystemConfirmation(batch)
                            deleteLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                        } else {
                            val success = FileUtils.deleteMediaItems(context.contentResolver, batch.uris)
                            if (success) {
                                viewModel.removeDeletedItems(batch.ids)
                                viewModel.refreshAll(context)
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
            viewModel.refreshAll(context)
        },
        onRestoreSelected = { restoredItems ->
            viewModel.removeDeletedItems(restoredItems.map { it.id })
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
