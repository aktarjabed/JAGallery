package com.aktarjabed.jascanner.model

import android.net.Uri

data class Photo(
    val id: String,
    val uri: Uri,
    val displayName: String?,
    val dateTaken: Long?,
    val width: Int?,
    val height: Int?,
    val mimeType: String?,
    val bucket: String?,
    val bucketId: String?,
    val relativePath: String?
) {
    // Lazy evaluation for category detection (computed only when needed)
    fun isScreenshot(): Boolean {
        return displayName?.contains("screenshot", ignoreCase = true) == true ||
                bucket?.contains("screenshot", ignoreCase = true) == true ||
                relativePath?.contains("screenshot", ignoreCase = true) == true ||
                displayName?.contains("screen_shot", ignoreCase = true) == true
    }

    fun isCamera(): Boolean {
        return bucket?.contains("camera", ignoreCase = true) == true ||
                relativePath?.contains("dcim", ignoreCase = true) == true ||
                relativePath?.contains("camera", ignoreCase = true) == true
    }

    fun isWhatsApp(): Boolean {
        return bucket?.contains("whatsapp", ignoreCase = true) == true ||
                relativePath?.contains("whatsapp", ignoreCase = true) == true
    }

    fun isDownloads(): Boolean {
        return bucket?.contains("download", ignoreCase = true) == true ||
                relativePath?.contains("download", ignoreCase = true) == true
    }

    fun isSocialMedia(): Boolean {
        return bucket?.let {
            it.contains("instagram", ignoreCase = true) ||
            it.contains("facebook", ignoreCase = true) ||
            it.contains("messenger", ignoreCase = true) ||
            it.contains("telegram", ignoreCase = true) ||
            it.contains("snapchat", ignoreCase = true)
        } == true || relativePath?.let { path ->
            path.contains("instagram", ignoreCase = true) ||
            path.contains("facebook", ignoreCase = true) ||
            path.contains("messenger", ignoreCase = true) ||
            path.contains("telegram", ignoreCase = true) ||
            path.contains("snapchat", ignoreCase = true)
        } == true
    }

    fun matchesCategory(category: String): Boolean {
        return when (category) {
            "camera" -> isCamera()
            "screenshots" -> isScreenshot()
            "whatsapp" -> isWhatsApp()
            "downloads" -> isDownloads()
            "social" -> isSocialMedia()
            else -> bucket?.contains(category, ignoreCase = true) == true
        }
    }
}