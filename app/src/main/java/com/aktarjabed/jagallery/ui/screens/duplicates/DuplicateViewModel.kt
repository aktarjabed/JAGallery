package com.aktarjabed.jagallery.ui.screens.duplicates

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.jagallery.data.local.FileHashDao
import com.aktarjabed.jagallery.data.local.FileHashEntity
import com.aktarjabed.jagallery.data.model.MediaItem
import com.aktarjabed.jagallery.data.repository.MediaRepository
import com.aktarjabed.jagallery.ui.common.OperationEvent
import com.aktarjabed.jagallery.ui.common.selection.BatchOperationManager
import com.aktarjabed.jagallery.util.DuplicateGroup
import com.aktarjabed.jagallery.util.ImageHashUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
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
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import javax.inject.Inject

@HiltViewModel
class DuplicateViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    private val fileHashDao: FileHashDao,
    @ApplicationContext private val context: Context
) : ViewModel() {

    sealed interface UiState {
        data object Loading : UiState
        data object Empty : UiState
        data class Success(val groups: List<DuplicateGroup>) : UiState
        data class Error(val message: String) : UiState
    }

    val batchManager = BatchOperationManager()

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _operationEvent = MutableSharedFlow<OperationEvent>()
    val operationEvent: SharedFlow<OperationEvent> = _operationEvent.asSharedFlow()

    private val _scanProgress = MutableStateFlow(0f)
    val scanProgress: StateFlow<Float> = _scanProgress.asStateFlow()

    // Map of group index -> set of selected item IDs
    private val _selections = MutableStateFlow<Map<Int, Set<String>>>(emptyMap())
    val selections: StateFlow<Map<Int, Set<String>>> = _selections.asStateFlow()

    /**
     * Retrieves or computes the hash record for a media item.
     * Uses the persistent cache (FileHashDao) to avoid redundant computation.
     */
    private suspend fun getOrComputeHash(item: MediaItem): FileHashEntity? {
        val uriStr = item.uri.toString()
        val existing = fileHashDao.getHashForUri(uriStr)

        // Cache hit: return if the file hasn't been modified
        if (existing != null && existing.lastModifiedTime == item.dateAdded) {
            return existing
        }

        // Cache miss: compute fresh hashes
        val sha256 = ImageHashUtils.calculateSha256(item.uri, context) ?: return null
        val pHash = if (item.mimeType.startsWith("image/")) {
            ImageHashUtils.calculatePHash(item.uri, context)
        } else null

        val entity = FileHashEntity(
            uriStr = uriStr,
            sha256Hash = sha256,
            perceptualHash = pHash,
            lastModifiedTime = item.dateAdded
        )
        fileHashDao.insertHash(entity)
        return entity
    }

    fun loadDuplicates() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            _scanProgress.value = 0f
            try {
                mediaRepository.loadMedia(force = true, context = context)
                val result = withTimeoutOrNull(30_000L) {
                    mediaRepository.mediaLoadResult
                        .filterIsInstance<com.aktarjabed.jagallery.data.model.MediaLoadResult.Success>()
                        .first()
                }
                if (result == null) {
                    _uiState.value = UiState.Error("Failed to load media")
                    return@launch
                }
                val allItems = result.items

                val groups = withContext(Dispatchers.IO) {
                    val duplicateGroups = mutableListOf<DuplicateGroup>()
                    val allPHashEntries = mutableListOf<Pair<Long, MediaItem>>()
                    val exactDuplicateIds = mutableSetOf<String>()

                    // ── Phase 1: Exact Duplicates (Size → SHA-256) ──
                    val sizeGroups = allItems.groupBy { it.size }.filter { it.key > 0 && it.value.size > 1 }
                    var processed = 0
                    val totalCandidates = sizeGroups.values.sumOf { it.size }

                    for ((size, candidateItems) in sizeGroups) {
                        ensureActive()
                        val hashGroups = mutableMapOf<String, MutableList<MediaItem>>()

                        for (item in candidateItems) {
                            yield()
                            val hashRecord = getOrComputeHash(item)

                            if (hashRecord != null) {
                                hashGroups.getOrPut(hashRecord.sha256Hash!!) { mutableListOf() }.add(item)

                                // Collect pHash for Phase 2
                                if (hashRecord.perceptualHash != null) {
                                    allPHashEntries.add(Pair(hashRecord.perceptualHash, item))
                                }
                            }

                            processed++
                            _scanProgress.value = (processed.toFloat() / (totalCandidates + allItems.count { it.mimeType.startsWith("image/") })).coerceIn(0f, 1f)
                        }

                        for ((_, matchingItems) in hashGroups) {
                            if (matchingItems.size > 1) {
                                val sortedItems = matchingItems.sortedByDescending { it.dateAdded }
                                duplicateGroups.add(DuplicateGroup(size = size, items = sortedItems))
                                exactDuplicateIds.addAll(sortedItems.map { it.id })
                            }
                        }
                    }

                    // ── Phase 2: Similar Photos (Perceptual Hash / dHash) ──
                    // Also hash images that weren't in a same-size group
                    val imageItems = allItems.filter {
                        it.mimeType.startsWith("image/") && !exactDuplicateIds.contains(it.id)
                    }
                    for (item in imageItems) {
                        yield()
                        val hashRecord = getOrComputeHash(item)
                        if (hashRecord?.perceptualHash != null) {
                            // Only add if not already collected in Phase 1
                            if (allPHashEntries.none { it.second.id == item.id }) {
                                allPHashEntries.add(Pair(hashRecord.perceptualHash, item))
                            }
                        }
                        processed++
                        _scanProgress.value = (processed.toFloat() / (totalCandidates + imageItems.size)).coerceIn(0f, 1f)
                    }

                    // Cluster similar photos using Hamming distance on 64-bit dHash
                    val usedIds = mutableSetOf<String>()
                    for (i in allPHashEntries.indices) {
                        ensureActive()
                        val (hash1, item1) = allPHashEntries[i]
                        if (usedIds.contains(item1.id)) continue

                        val cluster = mutableListOf(item1)
                        usedIds.add(item1.id)

                        for (j in i + 1 until allPHashEntries.size) {
                            val (hash2, item2) = allPHashEntries[j]
                            if (usedIds.contains(item2.id)) continue

                            val hammingDistance = java.lang.Long.bitCount(hash1 xor hash2)
                            if (hammingDistance < 10) { // threshold: <10 bits different = visually similar
                                cluster.add(item2)
                                usedIds.add(item2.id)
                            }
                        }

                        if (cluster.size > 1) {
                            // Don't add if this exact set is already covered by an exact-duplicate group
                            val clusterIds = cluster.map { it.id }.toSet()
                            val alreadyCovered = duplicateGroups.any { group ->
                                group.items.map { it.id }.toSet() == clusterIds
                            }
                            if (!alreadyCovered) {
                                val avgSize = cluster.sumOf { it.size } / cluster.size
                                duplicateGroups.add(DuplicateGroup(size = avgSize, items = cluster.sortedByDescending { it.dateAdded }))
                            }
                        }
                    }

                    _scanProgress.value = 1f
                    duplicateGroups
                }

                _uiState.value = if (groups.isEmpty()) UiState.Empty else UiState.Success(groups)

                // Auto-select all but the first (newest) item in each group
                val autoSelect = groups.mapIndexed { index, group ->
                    index to group.items.drop(1).map { it.id }.toSet()
                }.toMap()
                _selections.value = autoSelect
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.value = UiState.Error(e.message ?: "Failed to scan duplicates")
            }
        }
    }

    fun toggleSelection(groupIndex: Int, itemId: String) {
        _selections.value = _selections.value.toMutableMap().apply {
            val groupSet = this[groupIndex]?.toMutableSet() ?: mutableSetOf()
            if (groupSet.contains(itemId)) {
                groupSet.remove(itemId)
            } else {
                groupSet.add(itemId)
            }
            put(groupIndex, groupSet)
        }
    }

    fun selectAllInGroup(groupIndex: Int, group: DuplicateGroup) {
        _selections.value = _selections.value.toMutableMap().apply {
            put(groupIndex, group.items.map { it.id }.toSet())
        }
    }

    fun deselectAllInGroup(groupIndex: Int) {
        _selections.value = _selections.value.toMutableMap().apply { put(groupIndex, emptySet()) }
    }

    fun keepOnlyFirst(groupIndex: Int, group: DuplicateGroup) {
        _selections.value = _selections.value.toMutableMap().apply {
            put(groupIndex, group.items.drop(1).map { it.id }.toSet())
        }
    }

    fun deleteSelected(
        groups: List<DuplicateGroup>,
        onRequestDeletePermission: (List<com.aktarjabed.jagallery.data.model.DeleteRequestChunk>, List<String>) -> Unit
    ) {
        viewModelScope.launch {
            try {
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

                val requestResult = withContext(Dispatchers.IO) {
                    com.aktarjabed.jagallery.util.FileUtils.createDeleteRequests(
                        context.contentResolver,
                        itemsToDelete.map { it.uri }
                    )
                }

                when (requestResult) {
                    is com.aktarjabed.jagallery.util.FileUtils.RequestCreationResult.Success -> {
                        if (requestResult.chunks.isNotEmpty()) {
                            onRequestDeletePermission(requestResult.chunks, allIds)
                        } else {
                            _operationEvent.emit(OperationEvent.Error("No deletion chunks created."))
                        }
                    }
                    is com.aktarjabed.jagallery.util.FileUtils.RequestCreationResult.Unsupported -> {
                        val result = withContext(Dispatchers.IO) {
                            com.aktarjabed.jagallery.util.FileUtils.deleteMediaItems(context.contentResolver, itemsToDelete.map { it.uri })
                        }
                        val succeededIds = itemsToDelete.filter { result.successfulUris.contains(it.uri) }.map { it.id }
                        if (succeededIds.isNotEmpty()) {
                            mediaRepository.removeDeletedItems(succeededIds)
                            loadDuplicates()
                        }
                        if (!result.isFullySuccessful) {
                            _operationEvent.emit(OperationEvent.Error("Partial failure: ${result.failedUris.size} failed to delete"))
                        }
                    }
                    is com.aktarjabed.jagallery.util.FileUtils.RequestCreationResult.Error -> {
                        _operationEvent.emit(OperationEvent.Error("Failed to create delete requests"))
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _operationEvent.emit(OperationEvent.Error(e.message ?: "Failed to delete selected items"))
            }
        }
    }

    fun onDeletePermissionResult(success: Boolean, deletedIds: List<String>) {
        viewModelScope.launch {
            try {
                if (success) {
                    _operationEvent.emit(OperationEvent.Success("Deleted ${deletedIds.size} duplicate(s)"))
                    _selections.value = _selections.value.mapValues { (_, set) ->
                        set - deletedIds.toSet()
                    }.toMutableMap()
                    loadDuplicates()
                } else {
                    _operationEvent.emit(OperationEvent.Error("Delete cancelled"))
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _operationEvent.emit(OperationEvent.Error(e.message ?: "Unknown error occurred"))
            }
        }
    }
}
