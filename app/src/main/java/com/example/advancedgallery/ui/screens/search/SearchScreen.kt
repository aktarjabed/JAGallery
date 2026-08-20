package com.example.advancedgallery.ui.screens.search

import android.content.ClipData
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
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
fun SearchScreen(
    onNavigateToViewer: (mediaId: String, source: MediaSource, searchQuery: String) -> Unit,
    onBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
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
                    onSelectAll = { selectionState.selectAll(searchResults) },
                    onShareSelected = {
                        val selected = selectionState.getSelectedItems(searchResults)
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
                        val selected = selectionState.getSelectedItems(searchResults)
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
                    title = {
                        TextField(
                            value = query,
                            onValueChange = { viewModel.updateSearchQuery(it) },
                            placeholder = { Text(stringResource(R.string.search_hint)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
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
            items = searchResults,
            selectedIds = selectionState.selectedIds,
            selectionMode = selectionState.selectionMode,
            emptyIcon = Icons.Default.Search,
            emptyMessage = if (query.isBlank()) stringResource(R.string.search_hint) else stringResource(R.string.no_search_results),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            onItemClick = { item ->
                if (selectionState.selectionMode) {
                    selectionState.toggleSelection(item.id)
                } else {
                    onNavigateToViewer(item.id, MediaSource.SEARCH, query)
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
