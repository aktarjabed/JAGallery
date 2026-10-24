package com.aktarjabed.jagallery.ui.screens.search

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.aktarjabed.jagallery.data.local.SmartTagDao
import com.aktarjabed.jagallery.data.local.SmartTagEntity
import com.aktarjabed.jagallery.data.model.MediaLoadResult
import com.aktarjabed.jagallery.data.repository.MediaRepository
import com.aktarjabed.jagallery.domain.MediaOperations
import com.aktarjabed.jagallery.domain.NaturalLanguageSearchParser
import com.aktarjabed.jagallery.domain.SmartAlbumClassifier
import com.aktarjabed.jagallery.ui.common.BaseMediaViewModel
import com.aktarjabed.jagallery.util.SmartAnalyzer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SmartIndexingState(
    val isIndexing: Boolean = false,
    val processedCount: Int = 0,
    val totalCount: Int = 0
)

@OptIn(FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    repository: MediaRepository,
    mediaOperations: MediaOperations,
    private val savedStateHandle: SavedStateHandle,
    private val smartTagDao: SmartTagDao?
) : BaseMediaViewModel(mediaOperations, repository) {

    constructor(
        repository: MediaRepository,
        mediaOperations: MediaOperations,
        savedStateHandle: SavedStateHandle
    ) : this(repository, mediaOperations, savedStateHandle, null)

    val searchQuery: StateFlow<String> = savedStateHandle.getStateFlow("searchQuery", "")

    private val _indexingState = MutableStateFlow(SmartIndexingState())
    val indexingState: StateFlow<SmartIndexingState> = _indexingState.asStateFlow()

    private val smartTagsFlow: Flow<List<SmartTagEntity>> =
        smartTagDao?.getAllTagsFlow() ?: flowOf(emptyList())

    val indexedTagCount: StateFlow<Int> = combine(smartTagsFlow) { (tags) ->
        tags.size
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val searchResult: StateFlow<MediaLoadResult> = combine(
        repository.mediaLoadResult,
        searchQuery.debounce(250L),
        smartTagsFlow
    ) { result, query, smartTags ->
        when (result) {
            is MediaLoadResult.Success -> {
                if (query.isBlank()) {
                    MediaLoadResult.Empty
                } else {
                    val tagByUri = smartTags.associateBy { it.uriStr }
                    val intent = NaturalLanguageSearchParser.parse(query)
                    val filtered = result.items.filter { item ->
                        val tag = tagByUri[item.uri.toString()]
                        val matchesName = item.name.contains(query, ignoreCase = true) ||
                            item.bucketName.contains(query, ignoreCase = true)

                        val matchesOcrOrLabel = tag != null && (
                            tag.primaryLabel?.contains(query, ignoreCase = true) == true ||
                                tag.extractedText?.contains(query, ignoreCase = true) == true ||
                                tag.locationName?.contains(query, ignoreCase = true) == true
                            )

                        val categoryMatches = intent.category != null &&
                            SmartAlbumClassifier.classifyCategory(item) == intent.category
                        val docMatches = intent.isDocument &&
                            SmartAlbumClassifier.isReceiptOrDocument(item, tag)
                        val receiptMatches = intent.isReceipt &&
                            SmartAlbumClassifier.isReceiptOrDocument(item, tag)
                        val peopleMatches = intent.isPeople &&
                            SmartAlbumClassifier.isPeopleOrPortrait(item, tag)
                        val favoriteMatches = intent.isFavorite && item.isFavorite
                        val videoMatches = intent.isVideo && item.isVideo

                        matchesName || matchesOcrOrLabel || categoryMatches ||
                            docMatches || receiptMatches || peopleMatches ||
                            favoriteMatches || videoMatches
                    }
                    if (filtered.isEmpty()) MediaLoadResult.Empty else MediaLoadResult.Success(filtered)
                }
            }
            else -> result
        }
    }.flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MediaLoadResult.Loading)

    fun updateSearchQuery(query: String) {
        savedStateHandle["searchQuery"] = query
    }

    fun runSmartIndexing(context: Context, onComplete: (Int) -> Unit = {}) {
        val dao = smartTagDao ?: return
        if (_indexingState.value.isIndexing) return

        viewModelScope.launch(Dispatchers.IO) {
            val currentLoad = repository.mediaLoadResult.firstOrNull()
            val imageItems = (currentLoad as? MediaLoadResult.Success)
                ?.items
                ?.filter { !it.isVideo }
                ?.take(60)
                ?: emptyList()

            if (imageItems.isEmpty()) {
                onComplete(0)
                return@launch
            }

            _indexingState.value = SmartIndexingState(
                isIndexing = true,
                processedCount = 0,
                totalCount = imageItems.size
            )

            val analyzer = SmartAnalyzer(context.applicationContext, dao)
            var newlyIndexed = 0
            imageItems.forEachIndexed { index, item ->
                val existing = dao.getTagForUri(item.uri.toString())
                if (existing == null) {
                    analyzer.analyzeAndSave(item.uri)
                    newlyIndexed++
                }
                _indexingState.value = SmartIndexingState(
                    isIndexing = true,
                    processedCount = index + 1,
                    totalCount = imageItems.size
                )
            }

            _indexingState.value = SmartIndexingState(isIndexing = false)
            onComplete(newlyIndexed)
        }
    }
}
