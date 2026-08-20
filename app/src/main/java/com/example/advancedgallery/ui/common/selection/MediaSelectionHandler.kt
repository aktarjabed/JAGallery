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
import androidx.compose.runtime.saveable.rememberSaveable
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
    var pendingDeleteBatch by rememberSaveable { mutableStateOf<PendingDeleteBatch?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    LaunchedEffect(items) {
        selectionState.pruneSelection(items)
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            pendingDeleteBatch?.let { batch ->
                onRemoveDeletedItems(batch.ids)
            }
            selectionState.clearSelection()
            pendingDeleteBatch = null
        }
    }

    if (showDeleteConfirmation && pendingDeleteBatch != null) {
        DeleteConfirmationDialog(
            count = pendingDeleteBatch?.count ?: 0,
            onConfirm = {
                showDeleteConfirmation = false
                val batch = pendingDeleteBatch ?: return@DeleteConfirmationDialog
                val pendingIntent = FileUtils.createDeleteRequest(context.contentResolver, batch.uris)
                if (pendingIntent != null) {
                    deleteLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                } else {
                    FileUtils.deleteMediaItems(context.contentResolver, batch.uris)
                    onRemoveDeletedItems(batch.ids)
                    selectionState.clearSelection()
                    pendingDeleteBatch = null
                }
            },
            onDismiss = {
                showDeleteConfirmation = false
                pendingDeleteBatch = null
            }
        )
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
                    pendingDeleteBatch = PendingDeleteBatch(
                        ids = selected.map { it.id },
                        uris = selected.map { it.uri }
                    )
                    showDeleteConfirmation = true
                }
            }
        )
    } else {
        topBarContent()
    }

    content()
}
