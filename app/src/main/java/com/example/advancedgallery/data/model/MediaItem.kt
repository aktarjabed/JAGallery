package com.example.advancedgallery.data.model

import android.net.Uri

data class MediaItem(
    val uri: Uri,
    val mediaStoreId: Long,
    val name: String,
    val dateAdded: Long,
    val mimeType: String,
    val bucketId: Long,
    val bucketName: String,
    val isVideo: Boolean,
    val isFavorite: Boolean = false,
    val volumeName: String = ""
) {
    val id: String
        get() = uri.toString()

    val albumKey: AlbumKey
        get() = AlbumKey(volumeName = volumeName, bucketId = bucketId)
}
