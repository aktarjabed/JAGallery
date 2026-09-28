package com.aktarjabed.jagallery.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class MediaEntity(
    @PrimaryKey val uri: String,
    val isFavorite: Boolean = true,
    val dateAdded: Long
)
