package com.aktarjabed.jagallery.ui.common.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DriveFileMove
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Lock
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
import com.aktarjabed.jagallery.R

import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

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
    onCopySelected: (() -> Unit)? = null,
    onMoveToVaultSelected: (() -> Unit)? = null
) {
    var showOverflowMenu by remember { mutableStateOf(false) }

    TopAppBar(
        title = { Text("$selectedCount ${stringResource(R.string.selected)}") },
        navigationIcon = {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.cancel))
            }
        },
        actions = {
            if (onShareSelected != null) {
                IconButton(onClick = onShareSelected) {
                    Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share))
                }
            }
            if (onDeleteSelected != null) {
                IconButton(onClick = onDeleteSelected) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                }
            }
            IconButton(onClick = onSelectAll) {
                Icon(Icons.Default.SelectAll, contentDescription = stringResource(R.string.select_all))
            }

            if (onRestoreSelected != null || onUnhideSelected != null || onHideSelected != null || onCopySelected != null || onMoveSelected != null || onMoveToVaultSelected != null) {
                IconButton(onClick = { showOverflowMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.cd_more_options))
                }
                DropdownMenu(
                    expanded = showOverflowMenu,
                    onDismissRequest = { showOverflowMenu = false }
                ) {
                    if (onRestoreSelected != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.restore)) },
                            leadingIcon = { Icon(Icons.Default.Restore, contentDescription = null) },
                            onClick = {
                                showOverflowMenu = false
                                onRestoreSelected()
                            }
                        )
                    }
                    if (onUnhideSelected != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.unhide)) },
                            leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null) },
                            onClick = {
                                showOverflowMenu = false
                                onUnhideSelected()
                            }
                        )
                    }
                    if (onHideSelected != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.hide)) },
                            leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null) },
                            onClick = {
                                showOverflowMenu = false
                                onHideSelected()
                            }
                        )
                    }
                    if (onCopySelected != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.copy_to_album)) },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                showOverflowMenu = false
                                onCopySelected()
                            }
                        )
                    }
                    if (onMoveSelected != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.move_to_album)) },
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.DriveFileMove, contentDescription = null) },
                            onClick = {
                                showOverflowMenu = false
                                onMoveSelected()
                            }
                        )
                    }
                    if (onMoveToVaultSelected != null) {
                        DropdownMenuItem(
                            text = { Text("Move to Vault") },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            onClick = {
                                showOverflowMenu = false
                                onMoveToVaultSelected()
                            }
                        )
                    }
                }
            }
        }
    )
}
