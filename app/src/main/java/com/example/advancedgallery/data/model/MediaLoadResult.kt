package com.example.advancedgallery.data.model

sealed interface MediaLoadResult {
    data object Loading : MediaLoadResult
    data class Success(val items: List<MediaItem>) : MediaLoadResult
    data object Empty : MediaLoadResult
    data class Error(val cause: Throwable) : MediaLoadResult
}
