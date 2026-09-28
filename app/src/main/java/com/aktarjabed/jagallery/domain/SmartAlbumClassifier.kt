package com.aktarjabed.jagallery.domain

import com.aktarjabed.jagallery.data.model.MediaItem
import java.util.Locale

object SmartAlbumClassifier {
    fun classifyCategory(item: MediaItem): String? {
        val lowerPath = item.relativePath.lowercase(Locale.ROOT)
        val lowerBucket = item.bucketName.lowercase(Locale.ROOT)

        return when {
            lowerBucket.contains("whatsapp") || lowerPath.contains("whatsapp") -> "whatsapp"
            lowerBucket.contains("screenshot") || lowerPath.contains("screenshot") -> "screenshots"
            lowerBucket.contains("camera") || lowerPath.contains("dcim/camera") -> "camera"
            else -> null
        }
    }

    fun isReceiptOrDocument(item: MediaItem): Boolean {
        val lowerName = item.name.lowercase(Locale.ROOT)
        // Basic heuristic for local classification without relying on an external AI OCR
        if (lowerName.contains("receipt") || lowerName.contains("invoice") || lowerName.contains("scan") || lowerName.contains("doc")) return true
        if (item.mimeType.contains("pdf")) return true
        return false
    }
}
