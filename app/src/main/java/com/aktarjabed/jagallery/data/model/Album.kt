package com.aktarjabed.jagallery.data.model

import android.net.Uri

data class Album(
    val key: AlbumKey,
    val name: String,
    val mediaCount: Int,
    val coverUri: Uri
) {
    val bucketId: Long
        get() = key.bucketId

    val volumeName: String
        get() = key.volumeName

    val id: String
        get() = "${key.volumeName}:${key.bucketId}"
}
