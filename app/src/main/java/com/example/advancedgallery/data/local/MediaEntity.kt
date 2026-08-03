package com.example.advancedgallery.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "favorites")
data class MediaEntity(
    @PrimaryKey val id: Long,
    val uri: String,
    val isFavorite: Boolean,
    val dateAdded: Long
)
