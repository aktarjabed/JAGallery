package com.example.advancedgallery.data.model

sealed interface MediaSource {
    object All : MediaSource
    object Favorites : MediaSource
    data class Album(val bucketId: Long) : MediaSource
    data class Search(val query: String) : MediaSource
}
