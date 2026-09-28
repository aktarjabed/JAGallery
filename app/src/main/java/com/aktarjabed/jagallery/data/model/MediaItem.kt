package com.aktarjabed.jagallery.data.model

import android.net.Uri

data class MediaItem(
    val uri: Uri,
    val mediaStoreId: Long,
    val name: String,
    val dateAdded: Long,
    val mimeType: String,
    val bucketId: Long,
    val bucketName: String,
    val relativePath: String = "",
    val isVideo: Boolean,
    val isFavorite: Boolean = false,
    val volumeName: String = "",
    val size: Long = 0L,
    val isTrashed: Boolean = false,
    val dateTrashed: Long = 0L
) {
    val id: String
        get() = uri.toString()

    val albumKey: AlbumKey
        get() = AlbumKey(volumeName = volumeName, bucketId = bucketId, relativePath = relativePath)
}
