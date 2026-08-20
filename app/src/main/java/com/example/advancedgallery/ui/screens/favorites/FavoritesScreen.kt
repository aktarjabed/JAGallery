package com.example.advancedgallery.ui.screens.favorites

import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.advancedgallery.R
import com.example.advancedgallery.data.model.MediaSource
import com.example.advancedgallery.data.model.PendingDeleteBatch
import com.example.advancedgallery.ui.common.components.DeleteConfirmationDialog
import com.example.advancedgallery.ui.common.components.MediaGrid
import com.example.advancedgallery.ui.common.components.SelectionToolbar
import com.example.advancedgallery.ui.common.selection.rememberSelectionState
import com.example.advancedgallery.util.FileUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onNavigateToViewer: (mediaId: String, source: MediaSource) -> Unit,
    onBack: () -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val favoriteItems by viewModel.favoriteItems.collectAsState()
    val selectionState = rememberSelectionState()
    var pendingDeleteBatch by rememberSaveable { mutableStateOf<PendingDeleteBatch?>(null) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            pendingDeleteBatch?.let { batch ->
                viewModel.removeDeletedItems(batch.ids)
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
                    viewModel.removeDeletedItems(batch.ids)
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

    Scaffold(
        topBar = {
            if (selectionState.selectionMode) {
                SelectionToolbar(
                    selectedCount = selectionState.selectedCount,
                    onClearSelection = { selectionState.clearSelection() },
                    onSelectAll = { selectionState.selectAll(favoriteItems) },
                    onShareSelected = {
                        val selected = selectionState.getSelectedItems(favoriteItems)
                        if (selected.isNotEmpty()) {
                            val selectedUris = ArrayList(selected.map { it.uri })
                            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                type = "*/*"
                                putParcelableArrayListExtra(Intent.EXTRA_STREAM, selectedUris)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                if (selectedUris.isNotEmpty()) {
                                    clipData = ClipData.newRawUri("Media", selectedUris.first()).apply {
                                        for (i in 1 until selectedUris.size) {
                                            addItem(ClipData.Item(selectedUris[i]))
                                        }
                                    }
                                }
                            }
                            try {
                                context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share)))
                            } catch (e: Exception) {
                                Toast.makeText(context, context.getString(R.string.no_app_to_handle_share), Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    onDeleteSelected = {
                        val selected = selectionState.getSelectedItems(favoriteItems)
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
                TopAppBar(
                    title = { Text(stringResource(R.string.tab_favorites)) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    }
                )
            }
        }
    ) { padding ->
        MediaGrid(
            items = favoriteItems,
            selectedIds = selectionState.selectedIds,
            selectionMode = selectionState.selectionMode,
            emptyIcon = Icons.Default.FavoriteBorder,
            emptyMessage = stringResource(R.string.no_favorites_found),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            onItemClick = { item ->
                if (selectionState.selectionMode) {
                    selectionState.toggleSelection(item.id)
                } else {
                    onNavigateToViewer(item.id, MediaSource.FAVORITES)
                }
            },
            onItemLongClick = { item ->
                if (!selectionState.selectionMode) {
                    selectionState.startSelection(item.id)
                }
            }
        )
    }
}
