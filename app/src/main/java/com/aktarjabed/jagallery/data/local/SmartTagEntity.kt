package com.aktarjabed.jagallery.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "smart_tags")
data class SmartTagEntity(
    @PrimaryKey val uriStr: String,
    val primaryLabel: String?,
    val confidence: Float?,
    val locationName: String?,
    val extractedText: String?
)
