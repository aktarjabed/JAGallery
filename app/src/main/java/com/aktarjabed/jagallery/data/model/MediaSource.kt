package com.aktarjabed.jagallery.data.model

sealed interface MediaSource {
    object All : MediaSource
    object Favorites : MediaSource
    data class Album(val albumKey: AlbumKey) : MediaSource {
        constructor(volumeName: String, bucketId: Long, relativePath: String) : this(AlbumKey(volumeName, bucketId, relativePath))
        val bucketId: Long get() = albumKey.bucketId
        val volumeName: String get() = albumKey.volumeName
        val relativePath: String get() = albumKey.relativePath
    }
    data class Search(val query: String) : MediaSource
    object Trash : MediaSource
    object Hidden : MediaSource
}
