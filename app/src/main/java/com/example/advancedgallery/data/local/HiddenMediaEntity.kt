package com.example.advancedgallery.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "hidden_media")
data class HiddenMediaEntity(
    @PrimaryKey val uri: String,
    val isHidden: Boolean = true,
    val dateHidden: Long
)
