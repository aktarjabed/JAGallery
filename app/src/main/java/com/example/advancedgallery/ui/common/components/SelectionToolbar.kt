package com.example.advancedgallery.ui.common.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.advancedgallery.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionToolbar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onShareSelected: (() -> Unit)? = null,
    onDeleteSelected: (() -> Unit)? = null,
    onHideSelected: (() -> Unit)? = null,
    onUnhideSelected: (() -> Unit)? = null,
    onRestoreSelected: (() -> Unit)? = null,
    onMoveSelected: (() -> Unit)? = null,
    onCopySelected: (() -> Unit)? = null
) {
    TopAppBar(
        title = { Text("$selectedCount ${stringResource(R.string.selected)}") },
        navigationIcon = {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
            }
        },
        actions = {
            if (onRestoreSelected != null) {
                IconButton(onClick = onRestoreSelected) {
                    Icon(Icons.Default.Restore, contentDescription = stringResource(R.string.restore))
                }
            }
            if (onUnhideSelected != null) {
                IconButton(onClick = onUnhideSelected) {
                    Icon(Icons.Default.Visibility, contentDescription = stringResource(R.string.unhide))
                }
            }
            if (onHideSelected != null) {
                IconButton(onClick = onHideSelected) {
                    Icon(Icons.Default.VisibilityOff, contentDescription = stringResource(R.string.hide))
                }
            }
            if (onCopySelected != null) {
                IconButton(onClick = onCopySelected) {
                    Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.copy_to_album))
                }
            }
            if (onMoveSelected != null) {
                IconButton(onClick = onMoveSelected) {
                    Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = stringResource(R.string.move_to_album))
                }
            }
            if (onShareSelected != null) {
                IconButton(onClick = onShareSelected) {
                    Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share))
                }
            }
            IconButton(onClick = onSelectAll) {
                Icon(Icons.Default.SelectAll, contentDescription = stringResource(R.string.select_all))
            }
            if (onDeleteSelected != null) {
                IconButton(onClick = onDeleteSelected) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                }
            }
        }
    )
}
