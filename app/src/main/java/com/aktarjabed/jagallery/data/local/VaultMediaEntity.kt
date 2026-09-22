package com.aktarjabed.jagallery.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault_media")
data class VaultMediaEntity(
    @PrimaryKey val id: String, // Random UUID
    val originalUriStr: String,
    val mimeType: String,
    val encryptedFilePath: String,
    val dateAdded: Long,
    val originalName: String
)
