package com.example.advancedgallery.data.model

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val dateAdded: Long,
    val mimeType: String,
    val bucketId: Long,
    val bucketName: String,
    val isVideo: Boolean,
    val isFavorite: Boolean = false
)
