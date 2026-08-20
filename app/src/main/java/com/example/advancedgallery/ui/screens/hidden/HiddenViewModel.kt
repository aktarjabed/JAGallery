package com.example.advancedgallery.ui.screens.hidden

import androidx.lifecycle.viewModelScope
import com.example.advancedgallery.data.model.MediaLoadResult
import com.example.advancedgallery.data.repository.MediaRepository
import com.example.advancedgallery.domain.MediaOperations
import com.example.advancedgallery.ui.common.BaseMediaViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HiddenViewModel @Inject constructor(
    repository: MediaRepository,
    mediaOperations: MediaOperations
) : BaseMediaViewModel(mediaOperations) {

    val mediaLoadResult: StateFlow<MediaLoadResult> = repository.hiddenMediaLoadResult.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MediaLoadResult.Loading
    )
}
