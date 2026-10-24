package com.aktarjabed.jagallery.ui.screens.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.Coil
import coil.annotation.ExperimentalCoilApi
import com.aktarjabed.jagallery.domain.SettingsRepository
import com.aktarjabed.jagallery.domain.ThemeMode
import com.aktarjabed.jagallery.domain.TrashRetentionPolicy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = settingsRepository.themeMode
    val dynamicColor: StateFlow<Boolean> = settingsRepository.dynamicColor
    val gridColumns: StateFlow<Int> = settingsRepository.gridColumns
    val vaultTimeoutMinutes: StateFlow<Int> = settingsRepository.vaultTimeoutMinutes
    val retentionPolicy: StateFlow<TrashRetentionPolicy> = settingsRepository.trashRetentionPolicy

    fun setThemeMode(mode: ThemeMode) {
        settingsRepository.setThemeMode(mode)
    }

    fun toggleExplicitDarkMode(enableDark: Boolean) {
        settingsRepository.toggleExplicitDarkMode(enableDark)
    }

    fun setDynamicColor(enabled: Boolean) {
        settingsRepository.setDynamicColor(enabled)
    }

    fun setGridColumns(columns: Int) {
        settingsRepository.setGridColumns(columns)
    }

    fun setVaultTimeoutMinutes(minutes: Int) {
        settingsRepository.setVaultTimeoutMinutes(minutes)
    }

    fun setRetentionPolicy(policy: TrashRetentionPolicy) {
        settingsRepository.setTrashRetentionPolicy(policy)
    }

    @OptIn(ExperimentalCoilApi::class)
    fun clearCache(context: Context, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val imageLoader = Coil.imageLoader(context)
                imageLoader.diskCache?.clear()
                imageLoader.memoryCache?.clear()
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Failed to clear Coil cache", e)
            } finally {
                withContext(Dispatchers.Main) {
                    onComplete()
                }
            }
        }
    }
}
