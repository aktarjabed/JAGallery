package com.aktarjabed.jascanner.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Create DataStore instance
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * Manages app settings using DataStore for persistence
 */
class SettingsDataStore(private val context: Context) {

    companion object {
        private val AUTO_CLEANUP_ENABLED = booleanPreferencesKey("auto_cleanup_enabled")
        private val SECURE_DELETION_ENABLED = booleanPreferencesKey("secure_deletion_enabled")
        private val RECYCLE_BIN_ENABLED = booleanPreferencesKey("recycle_bin_enabled")
        private val RETENTION_DAYS = booleanPreferencesKey("retention_days")

        // Default values
        const val DEFAULT_AUTO_CLEANUP = true
        const val DEFAULT_SECURE_DELETION = false
        const val DEFAULT_RECYCLE_BIN = true
        const val DEFAULT_RETENTION_DAYS = 30
    }

    // Auto Cleanup
    val autoCleanupEnabled: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences ->
            preferences[AUTO_CLEANUP_ENABLED] ?: DEFAULT_AUTO_CLEANUP
        }

    suspend fun setAutoCleanupEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[AUTO_CLEANUP_ENABLED] = enabled
        }
    }

    // Secure Deletion
    val secureDeletionEnabled: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences ->
            preferences[SECURE_DELETION_ENABLED] ?: DEFAULT_SECURE_DELETION
        }

    suspend fun setSecureDeletionEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[SECURE_DELETION_ENABLED] = enabled
        }
    }

    // Recycle Bin
    val recycleBinEnabled: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences ->
            preferences[RECYCLE_BIN_ENABLED] ?: DEFAULT_RECYCLE_BIN
        }

    suspend fun setRecycleBinEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[RECYCLE_BIN_ENABLED] = enabled
        }
    }

    // Get all settings as a flow
    val allSettings: Flow<AppSettings> = context.settingsDataStore.data
        .map { preferences ->
            AppSettings(
                autoCleanupEnabled = preferences[AUTO_CLEANUP_ENABLED] ?: DEFAULT_AUTO_CLEANUP,
                secureDeletionEnabled = preferences[SECURE_DELETION_ENABLED] ?: DEFAULT_SECURE_DELETION,
                recycleBinEnabled = preferences[RECYCLE_BIN_ENABLED] ?: DEFAULT_RECYCLE_BIN
            )
        }
}

data class AppSettings(
    val autoCleanupEnabled: Boolean = true,
    val secureDeletionEnabled: Boolean = false,
    val recycleBinEnabled: Boolean = true
)
