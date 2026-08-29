package com.aktarjabed.jagallery.ui.common.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.aktarjabed.jagallery.R

@Composable
fun DeleteConfirmationDialog(
    count: Int,
    isPermanent: Boolean = false,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_media_title)) },
        text = {
            Text(
                if (count > 1) stringResource(R.string.delete_confirm_message, count)
                else stringResource(R.string.delete_single_confirm_message)
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(if (isPermanent) R.string.delete_permanently else R.string.delete))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
