package com.example.advancedgallery.ui.screens.search

import android.content.Intent
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.advancedgallery.R
import com.example.advancedgallery.ui.common.components.DeleteConfirmationDialog
import com.example.advancedgallery.ui.common.components.MediaGrid
import com.example.advancedgallery.ui.common.components.SelectionToolbar
import com.example.advancedgallery.util.FileUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateToViewer: (Long, String) -> Unit,
    onBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val query by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }

    val context = LocalContext.current
    val currentSelectedIds = rememberUpdatedState(selectedIds)

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.removeDeletedItems(currentSelectedIds.value.toList())
            selectionMode = false
            selectedIds = emptySet()
        }
    }

    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        DeleteConfirmationDialog(
            count = selectedIds.size,
            onConfirm = {
                showDeleteConfirmation = false
                val urisToDelete = searchResults.filter { selectedIds.contains(it.id) }.map { it.uri }
                val pendingIntent = FileUtils.createDeleteRequest(context.contentResolver, urisToDelete)
                if (pendingIntent != null) {
                    deleteLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                } else {
                    FileUtils.deleteMediaItems(context.contentResolver, urisToDelete)
                    viewModel.removeDeletedItems(selectedIds.toList())
                    selectionMode = false
                    selectedIds = emptySet()
                }
            },
            onDismiss = { showDeleteConfirmation = false }
        )
    }

    BackHandler(enabled = selectionMode) {
        selectionMode = false
        selectedIds = emptySet()
    }

    Scaffold(
        topBar = {
            if (selectionMode) {
                SelectionToolbar(
                    selectedCount = selectedIds.size,
                    onClearSelection = {
                        selectionMode = false
                        selectedIds = emptySet()
                    },
                    onSelectAll = {
                        selectedIds = searchResults.map { it.id }.toSet()
                    },
                    onShareSelected = {
                        val selectedUris = ArrayList(searchResults.filter { selectedIds.contains(it.id) }.map { it.uri })
                        if (selectedUris.isNotEmpty()) {
                            val shareIntent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                                type = "*/*"
                                putParcelableArrayListExtra(Intent.EXTRA_STREAM, selectedUris)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
                        }
                    },
                    onDeleteSelected = {
                        if (selectedIds.isNotEmpty()) {
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        }
    ) { padding ->
        MediaGrid(
            items = searchResults,
            selectedIds = selectedIds,
            selectionMode = selectionMode,
            emptyIcon = Icons.Default.Search,
            emptyMessage = if (query.isBlank()) stringResource(R.string.search_hint) else stringResource(R.string.no_search_results),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            onItemClick = { item ->
                if (selectionMode) {
                    val newSelection = selectedIds.toMutableSet()
                    if (newSelection.contains(item.id)) {
                        newSelection.remove(item.id)
                        if (newSelection.isEmpty()) selectionMode = false
                    } else {
                        newSelection.add(item.id)
                    }
                    selectedIds = newSelection
                } else {
                    onNavigateToViewer(item.id, query)
                }
            },
            onItemLongClick = { item ->
                if (!selectionMode) {
                    selectionMode = true
                    selectedIds = setOf(item.id)
                }
            }
        )
    }
}
