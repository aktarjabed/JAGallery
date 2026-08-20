package com.example.advancedgallery.data.model

sealed interface MediaSource {
    object All : MediaSource
    object Favorites : MediaSource
    data class Album(val bucketId: Long?) : MediaSource
    data class Search(val query: String) : MediaSource

    val name: String
        get() = when (this) {
            is All -> "ALL"
            is Favorites -> "FAVORITES"
            is Album -> "ALBUM"
            is Search -> "SEARCH"
        }

    companion object {
        fun from(sourceName: String?, bucketId: Long? = null, query: String? = null): MediaSource {
            return when (sourceName?.uppercase()) {
                "FAVORITES" -> Favorites
                "ALBUM" -> Album(bucketId)
                "SEARCH" -> Search(query ?: "")
                else -> All
            }
        }
    }
}
