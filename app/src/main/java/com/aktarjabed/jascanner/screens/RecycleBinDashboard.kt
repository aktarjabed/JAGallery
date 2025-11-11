package com.aktarjabed.jascanner.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.aktarjabed.jascanner.datastore.SettingsDataStore
import com.aktarjabed.jascanner.workers.WorkManagerManager
import com.aktarjabed.jascanner.JAScannerApp
import com.aktarjabed.jascanner.utils.DeleteUtils
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecycleBinDashboard(
    onBack: () -> Unit,
    onViewRecycleBin: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val app = context.applicationContext as JAScannerApp
    val repository = remember { app.mediaRepository }
    val settingsDataStore = remember { SettingsDataStore(context) }
    val scope = rememberCoroutineScope()

    var recycleBinStats by remember { mutableStateOf(RecycleBinStats()) }

    // DataStore flows
    val autoCleanupEnabled by settingsDataStore.autoCleanupEnabled.collectAsState(initial = true)
    val secureDeletionEnabled by settingsDataStore.secureDeletionEnabled.collectAsState(initial = false)
    val recycleBinEnabled by settingsDataStore.recycleBinEnabled.collectAsState(initial = true)

    data class RecycleBinStats(
        val totalFiles: Int = 0,
        val totalSize: Long = 0,
        val expiredFiles: Int = 0,
        val filesExpiringSoon: Int = 0,
        val oldestFileAge: Long = 0
    )

    fun calculateStats(): RecycleBinStats {
        val files = repository.getRecycleBinContents()
        val currentTime = System.currentTimeMillis()
        val soonThreshold = 7 * DeleteUtils.MILLIS_PER_DAY // 7 days

        var totalSize = 0L
        var expiredCount = 0
        var expiringSoonCount = 0
        var oldestAge = 0L

        files.forEach { file ->
            totalSize += file.length()
            val fileAge = currentTime - file.lastModified()

            if (fileAge > DeleteUtils.CLEANUP_THRESHOLD_DAYS * DeleteUtils.MILLIS_PER_DAY) {
                expiredCount++
            } else if (fileAge > (DeleteUtils.CLEANUP_THRESHOLD_DAYS - 7) * DeleteUtils.MILLIS_PER_DAY) {
                expiringSoonCount++
            }

            if (fileAge > oldestAge) {
                oldestAge = fileAge
            }
        }

        return RecycleBinStats(
            totalFiles = files.size,
            totalSize = totalSize,
            expiredFiles = expiredCount,
            filesExpiringSoon = expiringSoonCount,
            oldestFileAge = oldestAge
        )
    }

    LaunchedEffect(Unit) {
        recycleBinStats = calculateStats()
    }

    // Update WorkManager when auto-cleanup setting changes
    LaunchedEffect(autoCleanupEnabled) {
        if (autoCleanupEnabled) {
            WorkManagerManager.scheduleAutoCleanup(context)
        } else {
            WorkManagerManager.cancelAutoCleanup(context)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Recycle Bin Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Header Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Recycling,
                        contentDescription = "Recycle Bin",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Recycle Bin Protection",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Text(
                        "Deleted photos are protected for ${DeleteUtils.CLEANUP_THRESHOLD_DAYS} days",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Statistics Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                StatCard(
                    title = "Files in Bin",
                    value = recycleBinStats.totalFiles.toString(),
                    subtitle = "Total items",
                    icon = Icons.Default.PhotoLibrary,
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.width(16.dp))

                StatCard(
                    title = "Storage Used",
                    value = formatFileSize(recycleBinStats.totalSize),
                    subtitle = "Space occupied",
                    icon = Icons.Default.Storage,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status Cards
            if (recycleBinStats.expiredFiles > 0) {
                StatusCard(
                    title = "Expired Files",
                    message = "${recycleBinStats.expiredFiles} files ready for cleanup",
                    icon = Icons.Default.Warning,
                    color = MaterialTheme.colorScheme.error,
                    actionText = "Cleanup Now",
                    onAction = {
                        scope.launch {
                            val (count, message) = DeleteUtils.cleanupExpiredFilesNow(context)
                            snackbarHostState.showSnackbar(message)
                            if (count > 0) {
                                recycleBinStats = calculateStats()
                            }
                        }
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (recycleBinStats.filesExpiringSoon > 0) {
                StatusCard(
                    title = "Expiring Soon",
                    message = "${recycleBinStats.filesExpiringSoon} files expiring in 7 days",
                    icon = Icons.Default.Schedule,
                    color = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Settings Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Cleanup Settings",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Auto Cleanup Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Automatic Cleanup")
                            Text(
                                "Delete expired files automatically",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = autoCleanupEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    settingsDataStore.setAutoCleanupEnabled(enabled)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Secure Deletion Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Secure Deletion")
                            Text(
                                "Overwrite files before deletion for privacy",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = secureDeletionEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    settingsDataStore.setSecureDeletionEnabled(enabled)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Recycle Bin Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Recycle Bin")
                            Text(
                                "Move files to Recycle Bin instead of permanent deletion",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = recycleBinEnabled,
                            onCheckedChange = { enabled ->
                                scope.launch {
                                    settingsDataStore.setRecycleBinEnabled(enabled)
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Retention Period Info
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Info",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Files are kept for ${DeleteUtils.CLEANUP_THRESHOLD_DAYS} days",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Action Buttons
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onViewRecycleBin,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = recycleBinStats.totalFiles > 0
                ) {
                    Icon(Icons.Default.Visibility, "View Recycle Bin")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View Recycle Bin (${recycleBinStats.totalFiles})")
                }

                OutlinedButton(
                    onClick = {
                        scope.launch {
                            val (success, message) = repository.emptyRecycleBin()
                            snackbarHostState.showSnackbar(message)
                            if (success) {
                                recycleBinStats = calculateStats()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = recycleBinStats.totalFiles > 0,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(Icons.Default.DeleteForever, "Empty Bin")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Empty Recycle Bin")
                }
            }

            // Footer Info
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = "Security",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        "Files in Recycle Bin are stored securely and don't appear in your gallery",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                value,
                style = MaterialTheme.typography.titleLarge
            )
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun StatusCard(
    title: String,
    message: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = title,
                tint = color
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = color
                )
                Text(
                    message,
                    style = MaterialTheme.typography.bodySmall,
                    color = color.copy(alpha = 0.8f)
                )
            }
            actionText?.let {
                TextButton(onClick = { onAction?.invoke() }) {
                    Text(it)
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
