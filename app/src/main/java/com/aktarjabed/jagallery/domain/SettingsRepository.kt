package com.aktarjabed.jagallery.domain

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class TrashRetentionPolicy(val days: Int) {
    SEVEN(7), THIRTY(30), SIXTY(60), NEVER(-1)
}

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs = context.getSharedPreferences("jagallery_settings", Context.MODE_PRIVATE)

    fun getTrashRetentionPolicy(): TrashRetentionPolicy {
        val days = prefs.getInt("trash_retention_days", 30)
        return TrashRetentionPolicy.values().find { it.days == days } ?: TrashRetentionPolicy.THIRTY
    }

    fun setTrashRetentionPolicy(policy: TrashRetentionPolicy) {
        prefs.edit { putInt("trash_retention_days", policy.days) }
    }
}
