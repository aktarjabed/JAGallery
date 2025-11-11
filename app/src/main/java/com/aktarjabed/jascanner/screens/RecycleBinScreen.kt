package com.aktarjabed.jascanner.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.aktarjabed.jascanner.JAScannerApp
import com.aktarjabed.jascanner.utils.DeleteUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinScreen(
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val app = context.applicationContext as JAScannerApp
    val repository = remember { app.mediaRepository }
    val scope = rememberCoroutineScope()

    var recycleBinContents by remember { mutableStateOf(emptyList<File>()) }
    var showEmptyBinDialog by remember { mutableStateOf(false) }
    var expiredFilesCount by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        recycleBinContents = repository.getRecycleBinContents()
        expiredFilesCount = DeleteUtils.getExpiredRecycleBinFiles(context).size
    }

    fun refreshContents() {
        recycleBinContents = repository.getRecycleBinContents()
        expiredFilesCount = DeleteUtils.getExpiredRecycleBinFiles(context).size
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Recycle Bin") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Cleanup expired files button
                    if (expiredFilesCount > 0) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    val (count, message) = DeleteUtils.cleanupExpiredFilesNow(context)
                                    snackbarHostState.showSnackbar(message)
                                    if (count > 0) {
                                        refreshContents()
                                    }
                                }
                            }
                        ) {
                            BadgedBox(
                                badge = {
                                    if (expiredFilesCount > 0) {
                                        Badge {
                                            Text(expiredFilesCount.toString())
                                        }
                                    }
                                }
                            ) {
                                Icon(Icons.Default.CleaningServices, "Cleanup Expired Files")
                            }
                        }
                    }

                    // Empty bin button
                    if (recycleBinContents.isNotEmpty()) {
                        IconButton(
                            onClick = { showEmptyBinDialog = true },
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.DeleteForever, "Empty Recycle Bin")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (recycleBinContents.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { showEmptyBinDialog = true },
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                    icon = { Icon(Icons.Default.DeleteForever, "Empty Bin") },
                    text = { Text("Empty Bin") }
                )
            }
        }
    ) { padding ->
        if (recycleBinContents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.DeleteOutline,
                        contentDescription = "Empty",
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Recycle Bin is Empty",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "Deleted photos will appear here for 30 days",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                item {
                    Text(
                        "Photos in Recycle Bin (${recycleBinContents.size})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )

                    if (expiredFilesCount > 0) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = "Warning",
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    "$expiredFilesCount files will be automatically deleted soon",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }

                items(recycleBinContents) { file ->
                    RecycleBinItem(
                        file = file,
                        onRestore = {
                            scope.launch {
                                val (success, message) = repository.restoreFromRecycleBin(file)
                                snackbarHostState.showSnackbar(message)
                                if (success) {
                                    refreshContents()
                                }
                            }
                        },
                        onDeletePermanently = {
                            scope.launch {
                                val deleted = file.delete()
                                if (deleted) {
                                    snackbarHostState.showSnackbar("Photo permanently deleted")
                                    refreshContents()
                                } else {
                                    snackbarHostState.showSnackbar("Failed to delete photo")
                                }
                            }
                        }
                    )
                }
            }
        }

        // Empty bin confirmation dialog
        if (showEmptyBinDialog) {
            AlertDialog(
                onDismissRequest = { showEmptyBinDialog = false },
                title = { Text("Empty Recycle Bin?") },
                text = {
                    Text("This will permanently delete all ${recycleBinContents.size} photos in the recycle bin. This action cannot be undone.")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val (success, message) = repository.emptyRecycleBin()
                                snackbarHostState.showSnackbar(message)
                                if (success) {
                                    refreshContents()
                                }
                                showEmptyBinDialog = false
                            }
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Empty Bin")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEmptyBinDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun RecycleBinItem(
    file: File,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    val isExpired = System.currentTimeMillis() - file.lastModified() >
        DeleteUtils.CLEANUP_THRESHOLD_DAYS *
        DeleteUtils.MILLIS_PER_DAY

    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isExpired) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // File info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        file.name.substringAfter('_'), // Remove timestamp prefix
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        color = if (isExpired) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    Text(
                        buildString {
                            append("Size: ${formatFileSize(file.length())} • ${getFileAge(file)}")
                            if (isExpired) {
                                append(" • EXPIRED")
                            }
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isExpired) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }

                // Actions
                Row {
                    IconButton(onClick = onRestore) {
                        Icon(
                            Icons.Default.Restore,
                            "Restore",
                            tint = if (isExpired) {
                                MaterialTheme.colorScheme.onErrorContainer
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                    IconButton(
                        onClick = onDeletePermanently,
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.DeleteForever, "Delete Permanently")
                    }
                }
            }
        }
    }
}

private fun formatFileSize(size: Long): String {
    return when {
        size > 1024 * 1024 -> "%.1f MB".format(size / (1024.0 * 1024.0))
        size > 1024 -> "%.1f KB".format(size / 1024.0)
        else -> "$size B"
    }
}

private fun getFileAge(file: File): String {
    val days = (System.currentTimeMillis() - file.lastModified()) / (1000 * 60 * 60 * 24)
    return when {
        days == 0L -> "Today"
        days == 1L -> "Yesterday"
        days < 30 -> "$days days ago"
        else -> {
            val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            formatter.format(Date(file.lastModified()))
        }
    }
}
