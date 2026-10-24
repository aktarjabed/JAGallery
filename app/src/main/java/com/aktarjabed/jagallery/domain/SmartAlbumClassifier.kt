package com.aktarjabed.jagallery.domain

import com.aktarjabed.jagallery.data.local.SmartTagEntity
import com.aktarjabed.jagallery.data.model.MediaItem
import java.util.Locale

object SmartAlbumClassifier {
    private val PEOPLE_KEYWORDS = listOf(
        "person", "people", "selfie", "portrait", "face", "smile", "eyelash", "skin", "hair", "human", "crowd", "bride", "groom", "baby", "child"
    )

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

    fun isReceiptOrDocument(item: MediaItem, tag: SmartTagEntity? = null): Boolean {
        val lowerName = item.name.lowercase(Locale.ROOT)
        if (lowerName.contains("receipt") || lowerName.contains("invoice") || lowerName.contains("scan") || lowerName.contains("doc")) return true
        if (item.mimeType.contains("pdf")) return true
        if (tag != null) {
            val labelLower = tag.primaryLabel?.lowercase(Locale.ROOT) ?: ""
            if (labelLower.contains("paper") || labelLower.contains("document") || labelLower.contains("receipt") || labelLower.contains("text") || labelLower.contains("poster")) {
                return true
            }
            if (!tag.extractedText.isNullOrBlank() && tag.extractedText.length >= 12) {
                return true
            }
        }
        return false
    }

    fun isPeopleOrPortrait(item: MediaItem, tag: SmartTagEntity? = null): Boolean {
        val lowerName = item.name.lowercase(Locale.ROOT)
        val lowerBucket = item.bucketName.lowercase(Locale.ROOT)
        if (lowerName.contains("selfie") || lowerName.contains("portrait") || lowerName.contains("img_front") ||
            lowerBucket.contains("selfie") || lowerBucket.contains("portrait") || lowerBucket.contains("people")
        ) {
            return true
        }
        val labelLower = tag?.primaryLabel?.lowercase(Locale.ROOT) ?: return false
        return PEOPLE_KEYWORDS.any { keyword -> labelLower.contains(keyword) }
    }
}
