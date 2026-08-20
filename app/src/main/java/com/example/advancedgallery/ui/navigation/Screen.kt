package com.example.advancedgallery.ui.navigation

import android.net.Uri
import com.example.advancedgallery.data.model.MediaSource

sealed class Screen(val route: String) {
    object Albums : Screen("albums")
    object Grid : Screen("grid?source={source}&volumeName={volumeName}&bucketId={bucketId}") {
        fun createRoute(source: MediaSource = MediaSource.All): String {
            return when (source) {
                is MediaSource.Album -> "grid?source=ALBUM&volumeName=${Uri.encode(source.albumKey.volumeName)}&bucketId=${source.albumKey.bucketId}"
                is MediaSource.Search -> "grid?source=SEARCH"
                is MediaSource.Favorites -> "grid?source=FAVORITES"
                else -> "grid?source=ALL"
            }
        }
    }
    object Favorites : Screen("favorites")
    object Search : Screen("search")
    object Viewer : Screen("viewer?mediaId={mediaId}&source={source}&volumeName={volumeName}&bucketId={bucketId}&searchQuery={searchQuery}") {
        fun createRoute(
            mediaId: String,
            source: MediaSource
        ): String {
            val encodedMediaId = Uri.encode(mediaId)
            val sourceName = when (source) {
                is MediaSource.All -> "ALL"
                is MediaSource.Favorites -> "FAVORITES"
                is MediaSource.Album -> "ALBUM"
                is MediaSource.Search -> "SEARCH"
                is MediaSource.Trash -> "TRASH"
            }
            val builder = StringBuilder("viewer?mediaId=$encodedMediaId&source=$sourceName")
            when (source) {
                is MediaSource.Album -> {
                    builder.append("&volumeName=${Uri.encode(source.albumKey.volumeName)}")
                    builder.append("&bucketId=${source.albumKey.bucketId}")
                }
                is MediaSource.Search -> {
                    if (source.query.isNotBlank()) {
                        builder.append("&searchQuery=${Uri.encode(source.query)}")
                    }
                }
                else -> {}
            }
            return builder.toString()
        }
    }
    object Editor : Screen("editor?imageUri={imageUri}") {
        fun createRoute(imageUri: String): String {
            return "editor?imageUri=${Uri.encode(imageUri)}"
        }
    }
}
