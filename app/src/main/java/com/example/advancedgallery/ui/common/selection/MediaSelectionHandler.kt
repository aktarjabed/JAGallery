package com.example.advancedgallery.ui.common.selection

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.advancedgallery.R
import com.example.advancedgallery.data.model.MediaItem
import com.example.advancedgallery.data.model.PendingDeleteBatch
import com.example.advancedgallery.ui.common.components.DeleteConfirmationDialog
import com.example.advancedgallery.ui.common.components.SelectionToolbar
import com.example.advancedgallery.util.FileUtils

fun shareMediaItems(context: Context, items: List<MediaItem>) {
    if (items.isEmpty()) return
    val uris = ArrayList(items.map { it.uri })
    val allImages = items.all { !it.isVideo }
    val allVideos = items.all { it.isVideo }
    val mimeType = when {
        allImages -> "image/*"
        allVideos -> "video/*"
        else -> "*/*"
    }

    val shareIntent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = items.first().mimeType.ifEmpty { mimeType }
            putExtra(Intent.EXTRA_STREAM, uris.first())
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri("Media", uris.first())
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = mimeType
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            clipData = ClipData.newRawUri("Media", uris.first()).apply {
                for (i in 1 until uris.size) {
                    addItem(ClipData.Item(uris[i]))
                }
            }
        }
    }

    try {
        context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share)))
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, context.getString(R.string.no_app_to_handle_share), Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.share_failed), Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun MediaSelectionHandler(
    items: List<MediaItem>,
    selectionState: SelectionState,
    onRemoveDeletedItems: (List<String>) -> Unit,
    topBarContent: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var deleteState by remember { mutableStateOf<DeleteOperationState>(DeleteOperationState.Idle) }

    LaunchedEffect(items) {
        selectionState.pruneSelection(items)
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val state = deleteState
        if (state is DeleteOperationState.SystemConfirmation) {
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                onRemoveDeletedItems(state.batch.ids)
                selectionState.clearSelection()
            }
        }
        deleteState = DeleteOperationState.Idle
    }

    when (val currentState = deleteState) {
        is DeleteOperationState.Confirming -> {
            DeleteConfirmationDialog(
                count = currentState.batch.count,
                onConfirm = {
                    val pendingIntent = FileUtils.createDeleteRequest(context.contentResolver, currentState.batch.uris)
                    if (pendingIntent != null) {
                        deleteState = DeleteOperationState.SystemConfirmation(currentState.batch)
                        deleteLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                    } else {
                        val success = FileUtils.deleteMediaItems(context.contentResolver, currentState.batch.uris)
                        if (success) {
                            onRemoveDeletedItems(currentState.batch.ids)
                            selectionState.clearSelection()
                            deleteState = DeleteOperationState.Idle
                        } else {
                            Toast.makeText(context, context.getString(R.string.failed_to_delete_media), Toast.LENGTH_SHORT).show()
                            deleteState = DeleteOperationState.Failed(currentState.batch)
                        }
                    }
                },
                onDismiss = {
                    deleteState = DeleteOperationState.Idle
                }
            )
        }
        is DeleteOperationState.Failed -> {
            LaunchedEffect(currentState) {
                deleteState = DeleteOperationState.Idle
            }
        }
        else -> {}
    }

    BackHandler(enabled = selectionState.selectionMode) {
        selectionState.clearSelection()
    }

    if (selectionState.selectionMode) {
        SelectionToolbar(
            selectedCount = selectionState.selectedCount,
            onClearSelection = { selectionState.clearSelection() },
            onSelectAll = { selectionState.selectAll(items) },
            onShareSelected = {
                val selected = selectionState.getSelectedItems(items)
                shareMediaItems(context, selected)
            },
            onDeleteSelected = {
                val selected = selectionState.getSelectedItems(items)
                if (selected.isNotEmpty()) {
                    val batch = PendingDeleteBatch(
                        ids = selected.map { it.id },
                        uris = selected.map { it.uri }
                    )
                    deleteState = DeleteOperationState.Confirming(batch)
                }
            }
        )
    } else {
        topBarContent()
    }

    content()
}
