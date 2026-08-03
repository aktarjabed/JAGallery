package com.example.advancedgallery.ui.common.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectionToolbar(
    selectedCount: Int,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onDeleteSelected: () -> Unit
) {
    TopAppBar(
        title = { Text("$selectedCount Selected") },
        navigationIcon = {
            IconButton(onClick = onClearSelection) {
                Icon(Icons.Default.Close, contentDescription = "Clear Selection")
            }
        },
        actions = {
            IconButton(onClick = onSelectAll) {
                Icon(Icons.Default.SelectAll, contentDescription = "Select All")
            }
            IconButton(onClick = onDeleteSelected) {
                Icon(Icons.Default.Delete, contentDescription = "Delete Selected")
            }
        }
    )
}
