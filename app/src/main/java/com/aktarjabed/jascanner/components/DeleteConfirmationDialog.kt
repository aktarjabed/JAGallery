package com.aktarjabed.jascanner.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.aktarjabed.jascanner.datastore.AppSettings

@Composable
fun DeleteConfirmationDialog(
    photoName: String? = null,
    multipleCount: Int = 0,
    settings: AppSettings, // Add settings parameter
    onDismiss: () -> Unit,
    onDelete: (useSecureDelete: Boolean, useRecycleBin: Boolean) -> Unit
) {
    var useSecureDelete by remember { mutableStateOf(settings.secureDeletionEnabled) }
    var useRecycleBin by remember { mutableStateOf(settings.recycleBinEnabled) }

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true),
        title = {
            Text(
                text = if (multipleCount > 0) {
                    "Delete $multipleCount photos?"
                } else {
                    "Delete ${photoName ?: "photo"}?"
                },
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                // Warning message
                Text(
                    text = if (multipleCount > 0) {
                        "This will permanently delete $multipleCount photos from your device storage."
                    } else {
                        "This will permanently delete the photo from your device storage."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Recycle bin option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Move to Recycle Bin",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                "Photos can be restored within 30 days",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useRecycleBin,
                            onCheckedChange = { useRecycleBin = it }
                        )
                    }
                }

                // Secure deletion option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Secure Deletion",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                "Overwrites file before deletion for enhanced privacy",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = useSecureDelete,
                            onCheckedChange = { useSecureDelete = it }
                        )
                    }
                }

                // Final warning
                if (!useRecycleBin) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "⚠️ This action cannot be undone!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onDelete(useSecureDelete, useRecycleBin)
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text(if (useRecycleBin) "Move to Bin" else "Delete Permanently")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
