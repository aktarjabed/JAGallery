package com.example.advancedgallery.data.model

data class Album(
    val bucketId: Long,
    val name: String,
    val mediaCount: Int,
    val coverUri: android.net.Uri
)
