package com.example.advancedgallery.ui.screens.favorites

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.advancedgallery.ui.common.components.MediaThumbnail
import com.example.advancedgallery.ui.common.components.SelectionToolbar
import com.example.advancedgallery.util.FileUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onNavigateToViewer: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: FavoritesViewModel = hiltViewModel()
) {
    val favoriteItems by viewModel.favoriteItems.collectAsState()
    var selectionMode by remember { mutableStateOf(false) }
    var selectedIds by remember { mutableStateOf(setOf<Long>()) }

    val context = LocalContext.current

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.removeDeletedItems(selectedIds.toList())
            selectionMode = false
            selectedIds = emptySet()
        }
    }

    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Media") },
            text = { Text("Are you sure you want to delete ${selectedIds.size} items?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    val urisToDelete = favoriteItems.filter { selectedIds.contains(it.id) }.map { it.uri }
                    val pendingIntent = FileUtils.createDeleteRequest(context.contentResolver, urisToDelete)
                    if (pendingIntent != null) {
                        deleteLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                    } else {
                        viewModel.removeDeletedItems(selectedIds.toList())
                        selectionMode = false
                        selectedIds = emptySet()
                    }
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
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
                        selectedIds = favoriteItems.map { it.id }.toSet()
                    },
                    onDeleteSelected = {
                        if (selectedIds.isNotEmpty()) {
                            showDeleteConfirmation = true
                        }
                    }
                )
            } else {
                TopAppBar(
                    title = { Text("Favorites") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(2.dp)
        ) {
            items(favoriteItems, key = { it.id }) { item ->
                MediaThumbnail(
                    mediaItem = item,
                    isSelected = selectedIds.contains(item.id),
                    selectionMode = selectionMode,
                    onClick = {
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
                    onLongClick = {
                        if (!selectionMode) {
                            selectionMode = true
                            selectedIds = setOf(item.id)
                        }
                    }
                )
            }
        }
    }
}
