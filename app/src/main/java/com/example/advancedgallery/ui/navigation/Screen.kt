package com.example.advancedgallery.ui.navigation

sealed class Screen(val route: String) {
    object Albums : Screen("albums")
    object Grid : Screen("grid?bucketId={bucketId}") {
        fun createRoute(bucketId: Long?) = if (bucketId != null) "grid?bucketId=$bucketId" else "grid"
    }
    object Favorites : Screen("favorites")
    object Search : Screen("search")
    object Viewer : Screen("viewer/{mediaId}?bucketId={bucketId}&searchQuery={searchQuery}") {
        fun createRoute(mediaId: Long, bucketId: Long?, searchQuery: String? = null): String {
            val builder = StringBuilder("viewer/$mediaId")
            val params = mutableListOf<String>()
            if (bucketId != null) params.add("bucketId=$bucketId")
            if (!searchQuery.isNullOrBlank()) params.add("searchQuery=${android.net.Uri.encode(searchQuery)}")
            if (params.isNotEmpty()) {
                builder.append("?").append(params.joinToString("&"))
            }
            return builder.toString()
        }
    }
}
