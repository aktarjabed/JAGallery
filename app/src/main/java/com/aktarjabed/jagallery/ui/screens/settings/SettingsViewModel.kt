package com.aktarjabed.jagallery.ui.screens.settings

import androidx.lifecycle.ViewModel
import com.aktarjabed.jagallery.domain.SettingsRepository
import com.aktarjabed.jagallery.domain.TrashRetentionPolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _retentionPolicy = MutableStateFlow(settingsRepository.getTrashRetentionPolicy())
    val retentionPolicy: StateFlow<TrashRetentionPolicy> = _retentionPolicy.asStateFlow()

    fun setRetentionPolicy(policy: TrashRetentionPolicy) {
        settingsRepository.setTrashRetentionPolicy(policy)
        _retentionPolicy.value = policy
    }
}
