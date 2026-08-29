package com.aktarjabed.jagallery.data.model

sealed interface MediaSource {
    object All : MediaSource
    object Favorites : MediaSource
    data class Album(val albumKey: AlbumKey) : MediaSource {
        constructor(volumeName: String, bucketId: Long) : this(AlbumKey(volumeName, bucketId))
        val bucketId: Long get() = albumKey.bucketId
        val volumeName: String get() = albumKey.volumeName
    }
    data class Search(val query: String) : MediaSource
    object Trash : MediaSource
    object Hidden : MediaSource
}
