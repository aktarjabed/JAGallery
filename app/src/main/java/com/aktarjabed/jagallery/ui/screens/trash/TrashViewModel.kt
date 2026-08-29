package com.aktarjabed.jagallery.ui.screens.trash

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.aktarjabed.jagallery.data.model.MediaLoadResult
import com.aktarjabed.jagallery.data.repository.MediaRepository
import com.aktarjabed.jagallery.domain.MediaOperations
import com.aktarjabed.jagallery.ui.common.BaseMediaViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TrashViewModel @Inject constructor(
    repository: MediaRepository,
    mediaOperations: MediaOperations
) : BaseMediaViewModel(mediaOperations, repository) {

    val trashedMediaLoadResult: StateFlow<MediaLoadResult> = repository.trashedMediaLoadResult.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MediaLoadResult.Loading
    )

    fun loadTrashedMedia(context: Context? = null) {
        viewModelScope.launch {
            repository.loadTrashedMedia(context)
        }
    }

    fun refreshAll(context: Context? = null) {
        viewModelScope.launch {
            repository.loadTrashedMedia(context)
            repository.loadMedia(force = true, context = context)
        }
    }
}
