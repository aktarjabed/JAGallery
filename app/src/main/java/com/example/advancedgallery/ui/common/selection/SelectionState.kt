package com.example.advancedgallery.ui.common.selection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.advancedgallery.data.model.MediaItem

class SelectionState {
    var selectionMode by mutableStateOf(false)
        private set

    var selectedIds by mutableStateOf(setOf<String>())
        private set

    val selectedCount: Int get() = selectedIds.size

    fun toggleSelection(id: String) {
        val newSelection = selectedIds.toMutableSet()
        if (newSelection.contains(id)) {
            newSelection.remove(id)
            if (newSelection.isEmpty()) {
                selectionMode = false
            }
        } else {
            newSelection.add(id)
            selectionMode = true
        }
        selectedIds = newSelection
    }

    fun startSelection(id: String) {
        selectionMode = true
        selectedIds = setOf(id)
    }

    fun selectAll(items: List<MediaItem>) {
        selectedIds = items.map { it.id }.toSet()
        selectionMode = true
    }

    fun clearSelection() {
        selectionMode = false
        selectedIds = emptySet()
    }

    fun getSelectedItems(items: List<MediaItem>): List<MediaItem> {
        return items.filter { selectedIds.contains(it.id) }
    }
}

@Composable
fun rememberSelectionState(): SelectionState {
    return remember { SelectionState() }
}
