package com.aktarjabed.jagallery.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trash_media")
data class TrashMediaEntity(
    @PrimaryKey val uri: String,
    val dateTrashed: Long
)
