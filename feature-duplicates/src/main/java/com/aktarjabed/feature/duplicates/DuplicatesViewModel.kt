package com.aktarjabed.feature.duplicates

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.core.data.model.MediaItem
import com.aktarjabed.core.data.repository.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DuplicatesViewModel(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _duplicates = MutableStateFlow<List<List<MediaItem>>>(emptyList())
    val duplicates: StateFlow<List<List<MediaItem>>> = _duplicates

    init {
        findDuplicates()
    }

    private fun findDuplicates() {
        viewModelScope.launch {
            val photos = mediaRepository.getPhotos()
            // This is a placeholder for a real duplicate detection algorithm.
            // A real implementation would use image hashing (pHash, dHash)
            // or AI-based similarity checks.
            val duplicates = photos.groupBy { it.displayName }
                .filter { it.value.size > 1 }
                .values.toList()
            _duplicates.value = duplicates
        }
    }
}