package com.aktarjabed.jagallery.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "file_hashes")
data class FileHashEntity(
    @PrimaryKey val uriStr: String,
    val sha256Hash: String?,
    val perceptualHash: Long?,
    val lastModifiedTime: Long
)
