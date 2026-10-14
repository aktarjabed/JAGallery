package com.aktarjabed.jagallery.domain

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class TrashRetentionPolicy(val days: Int) {
    SEVEN(7), THIRTY(30), SIXTY(60), NEVER(-1)
}

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("jagallery_settings", Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(readThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _dynamicColor = MutableStateFlow(readDynamicColor())
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    private val _gridColumns = MutableStateFlow(readGridColumns())
    val gridColumns: StateFlow<Int> = _gridColumns.asStateFlow()

    private val _vaultTimeoutMinutes = MutableStateFlow(readVaultTimeoutMinutes())
    val vaultTimeoutMinutes: StateFlow<Int> = _vaultTimeoutMinutes.asStateFlow()

    private val _trashRetentionPolicy = MutableStateFlow(readTrashRetentionPolicy())
    val trashRetentionPolicy: StateFlow<TrashRetentionPolicy> = _trashRetentionPolicy.asStateFlow()

    private fun readThemeMode(): ThemeMode {
        val name = prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        return runCatching { ThemeMode.valueOf(name) }.getOrDefault(ThemeMode.SYSTEM)
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit { putString("theme_mode", mode.name) }
        _themeMode.value = mode
    }

    private fun readDynamicColor(): Boolean {
        return prefs.getBoolean("dynamic_color", true)
    }

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit { putBoolean("dynamic_color", enabled) }
        _dynamicColor.value = enabled
    }

    private fun readGridColumns(): Int {
        return prefs.getInt("grid_columns", 0) // 0 = Adaptive
    }

    fun setGridColumns(columns: Int) {
        prefs.edit { putInt("grid_columns", columns) }
        _gridColumns.value = columns
    }

    private fun readVaultTimeoutMinutes(): Int {
        return prefs.getInt("vault_timeout_minutes", 1) // default 1 minute
    }

    fun setVaultTimeoutMinutes(minutes: Int) {
        prefs.edit { putInt("vault_timeout_minutes", minutes) }
        _vaultTimeoutMinutes.value = minutes
    }

    private fun readTrashRetentionPolicy(): TrashRetentionPolicy {
        val days = prefs.getInt("trash_retention_days", 30)
        return TrashRetentionPolicy.entries.find { it.days == days } ?: TrashRetentionPolicy.THIRTY
    }

    fun getTrashRetentionPolicy(): TrashRetentionPolicy {
        return readTrashRetentionPolicy()
    }

    fun setTrashRetentionPolicy(policy: TrashRetentionPolicy) {
        prefs.edit { putInt("trash_retention_days", policy.days) }
        _trashRetentionPolicy.value = policy
    }
}
