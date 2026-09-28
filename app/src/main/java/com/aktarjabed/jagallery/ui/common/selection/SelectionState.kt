package com.aktarjabed.jagallery.ui.common.selection

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.aktarjabed.jagallery.data.model.MediaItem

class SelectionState(initialSelectionMode: Boolean = false, initialSelectedIds: Set<String> = emptySet()) {
    var selectionMode by mutableStateOf(initialSelectionMode)
        private set

    var selectedIds by mutableStateOf(initialSelectedIds)
        private set

    companion object {
        val Saver: Saver<SelectionState, *> = Saver(
            save = { state ->
                listOf(state.selectionMode, state.selectedIds.toList())
            },
            restore = { value ->
                val list = value as? List<*>
                val mode = list?.getOrNull(0) as? Boolean ?: false
                val idsList = list?.getOrNull(1) as? List<*>
                val ids = idsList?.filterIsInstance<String>() ?: emptyList()
                SelectionState(mode, ids.toSet())
            }
        )
    }

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

    fun pruneSelection(items: List<MediaItem>) {
        val validIds = items.map { it.id }.toSet()
        val newSelection = selectedIds.intersect(validIds)
        if (newSelection != selectedIds) {
            selectedIds = newSelection
            if (selectedIds.isEmpty()) {
                selectionMode = false
            }
        }
    }

    fun getSelectedItems(items: List<MediaItem>): List<MediaItem> {
        pruneSelection(items)
        return items.filter { selectedIds.contains(it.id) }
    }
}

@Composable
fun rememberSelectionState(): SelectionState {
    return rememberSaveable(saver = SelectionState.Saver) { SelectionState() }
}
