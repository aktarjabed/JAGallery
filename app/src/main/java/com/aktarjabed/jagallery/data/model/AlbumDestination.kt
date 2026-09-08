package com.aktarjabed.jagallery.data.model

sealed interface AlbumDestination {
    data class ExistingAlbum(val album: Album) : AlbumDestination

    data class NewAlbum(
        val name: String,
        val volumeName: String,
        val relativePath: String
    ) : AlbumDestination
}
