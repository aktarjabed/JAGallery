package com.example.advancedgallery.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.advancedgallery.domain.MediaOperations
import kotlinx.coroutines.launch

abstract class BaseMediaViewModel(
    protected val mediaOperations: MediaOperations
) : ViewModel() {

    fun removeDeletedItems(deletedIds: List<String>) {
        viewModelScope.launch {
            mediaOperations.removeDeletedItems(deletedIds)
        }
    }
}
