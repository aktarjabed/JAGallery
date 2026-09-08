package com.aktarjabed.jagallery.ui.common.selection

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.platform.LocalContext
import com.aktarjabed.jagallery.R
import com.aktarjabed.jagallery.data.model.Album
import com.aktarjabed.jagallery.data.model.AlbumDestination
import com.aktarjabed.jagallery.data.model.MediaItem
import com.aktarjabed.jagallery.data.model.PendingDeleteBatch
import com.aktarjabed.jagallery.ui.common.components.AlbumSelectionDialog
import com.aktarjabed.jagallery.ui.common.components.DeleteConfirmationDialog
import com.aktarjabed.jagallery.ui.common.components.SelectionToolbar
import com.aktarjabed.jagallery.util.FileUtils

private const val TAG = "MediaSelectionHandler"

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
    } catch (e: SecurityException) {
        Log.e(TAG, "SecurityException during share", e)
        Toast.makeText(context, context.getString(R.string.share_failed), Toast.LENGTH_SHORT).show()
    } catch (e: IllegalArgumentException) {
        Log.e(TAG, "IllegalArgumentException during share", e)
        Toast.makeText(context, context.getString(R.string.share_failed), Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun MediaSelectionHandler(
    items: List<MediaItem>,
    albums: List<Album>,
    selectionState: SelectionState,
    batchManager: BatchOperationManager,
    onRemoveDeletedItems: (List<String>) -> Unit,
    onHideSelected: ((List<MediaItem>) -> Unit)? = null,
    onUnhideSelected: ((List<MediaItem>) -> Unit)? = null,
    onRestoreSelected: ((List<MediaItem>) -> Unit)? = null,
    onMoveSelected: ((List<MediaItem>, AlbumDestination) -> Unit)? = null,
    onCopySelected: ((List<MediaItem>, AlbumDestination) -> Unit)? = null,
    topBarContent: @Composable () -> Unit
) {
    val context = LocalContext.current
    var deleteState by remember { mutableStateOf<DeleteOperationState>(DeleteOperationState.Idle) }

    LaunchedEffect(items) {
        selectionState.pruneSelection(items)
    }

    var restoreState by remember { mutableStateOf<DeleteOperationState>(DeleteOperationState.Idle) }
    var showMoveAlbumDialog by remember { mutableStateOf(false) }
    var showCopyAlbumDialog by remember { mutableStateOf(false) }

    var pendingDeleteBatchForMessage by remember { mutableStateOf<PendingDeleteBatch?>(null) }
    var pendingRestoreBatchForMessage by remember { mutableStateOf<PendingDeleteBatch?>(null) }

    val batchState by batchManager.batchState.collectAsStateWithLifecycle()

    BatchOperationObserver(
        batchState = batchState,
        onChunkResult = { resultCode -> batchManager.onBatchChunkResult(resultCode) },
        onComplete = { result ->
            when (result.tag) {
                "SELECTION_DELETE" -> {
                    if (result.succeededIds.isNotEmpty()) {
                        onRemoveDeletedItems(result.succeededIds)
                        selectionState.clearSelection()
                    }
                    if (result.cancelled && result.succeededIds.isNotEmpty()) {
                        android.widget.Toast.makeText(
                            context,
                            context.getString(
                                com.aktarjabed.jagallery.R.string.batch_partially_processed,
                                result.succeededIds.size,
                                pendingDeleteBatchForMessage?.count ?: 0
                            ),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                    deleteState = DeleteOperationState.Idle
                    pendingDeleteBatchForMessage = null
                }
                "SELECTION_RESTORE" -> {
                    if (result.succeededIds.isNotEmpty()) {
                        val selected = items.filter { result.succeededIds.contains(it.id) }
                        if (selected.isNotEmpty()) {
                            onRestoreSelected?.invoke(selected)
                        }
                        selectionState.clearSelection()
                    }
                    if (result.cancelled && result.succeededIds.isNotEmpty()) {
                        android.widget.Toast.makeText(
                            context,
                            context.getString(
                                com.aktarjabed.jagallery.R.string.batch_partially_processed,
                                result.succeededIds.size,
                                pendingRestoreBatchForMessage?.count ?: 0
                            ),
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                    restoreState = DeleteOperationState.Idle
                    pendingRestoreBatchForMessage = null
                }
            }
            batchManager.clearState()
        }
    )

    when (val currentState = deleteState) {
        is DeleteOperationState.Confirming -> {
            val isTrashedBatch = items.any { currentState.batch.ids.contains(it.id) && it.isTrashed }
            DeleteConfirmationDialog(
                count = currentState.batch.count,
                isPermanent = isTrashedBatch,
                onConfirm = {
                    val isTrashedBatch = items.any { currentState.batch.ids.contains(it.id) && it.isTrashed }

                    val requestResult = if (isTrashedBatch) {
                        FileUtils.createDeleteRequests(context.contentResolver, currentState.batch.uris)
                    } else {
                        FileUtils.createTrashRequests(context.contentResolver, currentState.batch.uris, true)
                    }

                    when (requestResult) {
                        is FileUtils.RequestCreationResult.Success -> {
                            if (requestResult.chunks.isNotEmpty()) {
                                pendingDeleteBatchForMessage = currentState.batch
                                batchManager.startBatch(requestResult.chunks, "SELECTION_DELETE")
                            } else {
                                deleteState = DeleteOperationState.Idle
                            }
                        }
                        is FileUtils.RequestCreationResult.Unsupported -> {
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
                        is FileUtils.RequestCreationResult.Error -> {
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

    if ((showMoveAlbumDialog || showCopyAlbumDialog) && (onMoveSelected != null || onCopySelected != null)) {
        AlbumSelectionDialog(
            albums = albums,
            onAlbumSelected = { destination ->
                val selected = selectionState.getSelectedItems(items)
                if (selected.isNotEmpty()) {
                    if (showMoveAlbumDialog && onMoveSelected != null) {
                        onMoveSelected(selected, destination)
                    } else if (showCopyAlbumDialog && onCopySelected != null) {
                        onCopySelected(selected, destination)
                    }
                }
                showMoveAlbumDialog = false
                showCopyAlbumDialog = false
                selectionState.clearSelection()
            },
            onDismiss = {
                showMoveAlbumDialog = false
                showCopyAlbumDialog = false
            }
        )
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
            },
            onHideSelected = onHideSelected?.let { callback ->
                {
                    val selected = selectionState.getSelectedItems(items)
                    callback(selected)
                    selectionState.clearSelection()
                }
            },
            onUnhideSelected = onUnhideSelected?.let { callback ->
                {
                    val selected = selectionState.getSelectedItems(items)
                    callback(selected)
                    selectionState.clearSelection()
                }
            },
            onRestoreSelected = onRestoreSelected?.let { callback ->
                {
                    val selected = selectionState.getSelectedItems(items)
                    if (selected.isNotEmpty()) {
                        val batch = PendingDeleteBatch(
                            ids = selected.map { it.id },
                            uris = selected.map { it.uri }
                        )
                        when (val requestResult = FileUtils.createTrashRequests(context.contentResolver, batch.uris, false)) {
                            is FileUtils.RequestCreationResult.Success -> {
                                if (requestResult.chunks.isNotEmpty()) {
                                    pendingRestoreBatchForMessage = batch
                                    batchManager.startBatch(requestResult.chunks, "SELECTION_RESTORE")
                                }
                            }
                            is FileUtils.RequestCreationResult.Unsupported -> {
                                val success = FileUtils.untrashMediaItems(context.contentResolver, batch.uris)
                                if (success) {
                                    callback(selected)
                                    selectionState.clearSelection()
                                } else {
                                    Toast.makeText(context, context.getString(R.string.failed_to_delete_media), Toast.LENGTH_SHORT).show()
                                }
                            }
                            is FileUtils.RequestCreationResult.Error -> {
                                Toast.makeText(context, context.getString(R.string.failed_to_delete_media), Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            },
            onMoveSelected = if (onMoveSelected != null) {
                {
                    showMoveAlbumDialog = true
                }
            } else null,
            onCopySelected = if (onCopySelected != null) {
                {
                    showCopyAlbumDialog = true
                }
            } else null
        )
    } else {
        topBarContent()
    }
}
