package com.example.advancedgallery.ui.screens.grid

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.advancedgallery.ui.common.components.DeleteConfirmationDialog
import com.example.advancedgallery.ui.common.components.MediaGrid
import com.example.advancedgallery.ui.common.components.SelectionToolbar
import com.example.advancedgallery.util.FileUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GridScreen(
    bucketId: Long?,
    onNavigateToViewer: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: GridViewModel = hiltViewModel()
) {
    LaunchedEffect(bucketId) {
        viewModel.setBucketId(bucketId)
    }

    val mediaItems by viewModel.mediaItems.collectAsState()
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
                val urisToDelete = mediaItems.filter { selectedIds.contains(it.id) }.map { it.uri }
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
                        selectedIds = mediaItems.map { it.id }.toSet()
                    },
                    onShareSelected = {
                        val selectedUris = ArrayList(mediaItems.filter { selectedIds.contains(it.id) }.map { it.uri })
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
                    title = { Text(if (bucketId == null) "All Media" else mediaItems.firstOrNull()?.bucketName?.ifBlank { "Album" } ?: "Album") },
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
            items = mediaItems,
            selectedIds = selectedIds,
            selectionMode = selectionMode,
            emptyIcon = Icons.Default.PhotoLibrary,
            emptyMessage = "No media in this album",
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
                    onNavigateToViewer(item.id)
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
