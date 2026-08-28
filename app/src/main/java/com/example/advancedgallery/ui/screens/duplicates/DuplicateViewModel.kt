package com.example.advancedgallery.ui.screens.duplicates

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.advancedgallery.data.model.MediaItem
import com.example.advancedgallery.data.repository.MediaRepository
import com.example.advancedgallery.ui.common.OperationEvent
import com.example.advancedgallery.util.DuplicateDetector
import com.example.advancedgallery.util.DuplicateGroup
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class DuplicateViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState
        data object Empty : UiState
        data class Success(val groups: List<DuplicateGroup>) : UiState
        data class Error(val message: String) : UiState
    }

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _operationEvent = MutableSharedFlow<OperationEvent>()
    val operationEvent: SharedFlow<OperationEvent> = _operationEvent.asSharedFlow()

    // Map of group index -> set of selected item IDs
    private val _selections = MutableStateFlow<Map<Int, Set<String>>>(emptyMap())
    val selections: StateFlow<Map<Int, Set<String>>> = _selections.asStateFlow()

    fun loadDuplicates() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            try {
                val result = mediaRepository.mediaLoadResult
                    .filterIsInstance<com.example.advancedgallery.data.model.MediaLoadResult.Success>()
                    .first()

                val groups = withContext(Dispatchers.IO) {
                    DuplicateDetector.findDuplicates(context, result.items)
                }

                _uiState.value = if (groups.isEmpty()) UiState.Empty else UiState.Success(groups)

                // Auto-select all but the first item in each group (smart default)
                val autoSelect = groups.mapIndexed { index, group ->
                    index to group.items.drop(1).map { it.id }.toSet()
                }.toMap()
                _selections.value = autoSelect
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to scan duplicates")
            }
        }
    }

    fun toggleSelection(groupIndex: Int, itemId: String) {
        _selections.update { current ->
            val groupSet = current[groupIndex]?.toMutableSet() ?: mutableSetOf()
            if (groupSet.contains(itemId)) {
                groupSet.remove(itemId)
            } else {
                groupSet.add(itemId)
            }
            current.toMutableMap().apply { put(groupIndex, groupSet) }
        }
    }

    fun selectAllInGroup(groupIndex: Int, group: DuplicateGroup) {
        _selections.update { current ->
            current.toMutableMap().apply {
                put(groupIndex, group.items.map { it.id }.toSet())
            }
        }
    }

    fun deselectAllInGroup(groupIndex: Int) {
        _selections.update { current ->
            current.toMutableMap().apply { put(groupIndex, emptySet()) }
        }
    }

    fun keepOnlyFirst(groupIndex: Int, group: DuplicateGroup) {
        _selections.update { current ->
            current.toMutableMap().apply {
                put(groupIndex, group.items.drop(1).map { it.id }.toSet())
            }
        }
    }

    fun deleteSelected(
        groups: List<DuplicateGroup>,
        onRequestDeletePermission: (List<android.app.PendingIntent>, List<String>) -> Unit
    ) {
        viewModelScope.launch {
            val currentSelections = _selections.value
            val itemsToDelete = mutableListOf<MediaItem>()
            val allIds = mutableListOf<String>()

            groups.forEachIndexed { index, group ->
                val selectedIds = currentSelections[index] ?: emptySet()
                group.items.filter { selectedIds.contains(it.id) }.forEach { item ->
                    itemsToDelete.add(item)
                    allIds.add(item.id)
                }
            }

            if (itemsToDelete.isEmpty()) {
                _operationEvent.emit(OperationEvent.Error("No items selected"))
                return@launch
            }

            // Use FileUtils for batching delete requests
            val pendingIntents = withContext(Dispatchers.IO) {
                com.example.advancedgallery.util.FileUtils.createDeleteRequests(
                    context.contentResolver,
                    itemsToDelete.map { it.uri }
                )
            }

            if (pendingIntents.isNotEmpty()) {
                onRequestDeletePermission(pendingIntents, allIds)
            } else {
                // Fallback direct delete for older API
                withContext(Dispatchers.IO) {
                    itemsToDelete.forEach { item ->
                        try {
                            context.contentResolver.delete(item.uri, null, null)
                        } catch (_: Exception) { }
                    }
                }
                _operationEvent.emit(OperationEvent.Success("Deleted ${itemsToDelete.size} duplicate(s)"))
                loadDuplicates() // Refresh
            }
        }
    }

    fun onDeletePermissionResult(success: Boolean, deletedIds: List<String>) {
        viewModelScope.launch {
            if (success) {
                _operationEvent.emit(OperationEvent.Success("Deleted ${deletedIds.size} duplicate(s)"))
                // Remove from selection
                _selections.update { current ->
                    current.mapValues { (_, set) ->
                        set - deletedIds.toSet()
                    }.toMutableMap()
                }
                loadDuplicates()
            } else {
                _operationEvent.emit(OperationEvent.Error("Delete cancelled"))
            }
        }
    }

    private fun <K, V> MutableStateFlow<Map<K, V>>.update(block: (Map<K, V>) -> MutableMap<K, V>) {
        value = block(value)
    }
}
