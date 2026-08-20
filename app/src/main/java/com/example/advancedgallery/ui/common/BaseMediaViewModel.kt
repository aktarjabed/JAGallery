package com.example.advancedgallery.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.advancedgallery.data.repository.MediaRepository
import kotlinx.coroutines.launch

abstract class BaseMediaViewModel(
    protected val repository: MediaRepository
) : ViewModel() {

    fun removeDeletedItems(deletedIds: List<String>) {
        viewModelScope.launch {
            repository.removeDeletedItems(deletedIds)
        }
    }
}
