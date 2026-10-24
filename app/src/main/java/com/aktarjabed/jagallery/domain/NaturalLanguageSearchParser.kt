package com.aktarjabed.jagallery.domain

import java.util.Locale

data class SearchIntent(
    val category: String? = null,
    val isDocument: Boolean = false,
    val isReceipt: Boolean = false,
    val isPeople: Boolean = false,
    val isFavorite: Boolean = false,
    val isVideo: Boolean = false,
    val rawQuery: String
)

object NaturalLanguageSearchParser {
    fun parse(query: String): SearchIntent {
        val lower = query.lowercase(Locale.ROOT)

        var category: String? = null
        if (lower.contains("whatsapp")) category = "whatsapp"
        if (lower.contains("screenshot")) category = "screenshots"
        if (lower.contains("camera")) category = "camera"

        val isDocument = lower.contains("document") || lower.contains("id") || lower.contains("card") || lower.contains("text") || lower.contains("ocr") || lower.contains("note")
        val isReceipt = lower.contains("receipt") || lower.contains("bill") || lower.contains("invoice")
        val isPeople = lower.contains("people") || lower.contains("person") || lower.contains("selfie") || lower.contains("portrait") || lower.contains("face") || lower.contains("family") || lower.contains("smile")
        val isFavorite = lower.contains("favorite") || lower.contains("starred") || lower.contains("liked")
        val isVideo = lower.contains("video") || lower.contains("clip") || lower.contains("movie")

        return SearchIntent(
            category = category,
            isDocument = isDocument,
            isReceipt = isReceipt,
            isPeople = isPeople,
            isFavorite = isFavorite,
            isVideo = isVideo,
            rawQuery = query
        )
    }
}
