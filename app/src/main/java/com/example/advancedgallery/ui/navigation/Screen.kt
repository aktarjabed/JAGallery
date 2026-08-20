package com.example.advancedgallery.ui.navigation

import android.net.Uri
import com.example.advancedgallery.data.model.MediaSource

sealed class Screen(val route: String) {
    object Albums : Screen("albums")
    object Grid : Screen("grid?bucketId={bucketId}") {
        fun createRoute(bucketId: Long?) = if (bucketId != null) "grid?bucketId=$bucketId" else "grid"
    }
    object Favorites : Screen("favorites")
    object Search : Screen("search")
    object Viewer : Screen("viewer?mediaId={mediaId}&source={source}&bucketId={bucketId}&searchQuery={searchQuery}") {
        fun createRoute(
            mediaId: String,
            source: MediaSource
        ): String {
            val encodedMediaId = Uri.encode(mediaId)
            val builder = StringBuilder("viewer?mediaId=$encodedMediaId&source=${source.name}")
            when (source) {
                is MediaSource.Album -> {
                    if (source.bucketId != null) builder.append("&bucketId=${source.bucketId}")
                }
                is MediaSource.Search -> {
                    if (source.query.isNotBlank()) builder.append("&searchQuery=${Uri.encode(source.query)}")
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
