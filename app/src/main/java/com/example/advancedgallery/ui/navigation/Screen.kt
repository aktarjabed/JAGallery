package com.example.advancedgallery.ui.navigation

sealed class Screen(val route: String) {
    object Albums : Screen("albums")
    object Grid : Screen("grid?bucketId={bucketId}") {
        fun createRoute(bucketId: Long?) = if (bucketId != null) "grid?bucketId=$bucketId" else "grid"
    }
    object Favorites : Screen("favorites")
    object Search : Screen("search")
    object Viewer : Screen("viewer/{mediaId}?bucketId={bucketId}") {
        fun createRoute(mediaId: Long, bucketId: Long?) =
            if (bucketId != null) "viewer/$mediaId?bucketId=$bucketId" else "viewer/$mediaId"
    }
}
