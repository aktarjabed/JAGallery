package com.aktarjabed.jagallery.domain

import java.util.Locale

data class SearchIntent(
    val category: String? = null,
    val isDocument: Boolean = false,
    val isReceipt: Boolean = false,
    val rawQuery: String
)

object NaturalLanguageSearchParser {
    fun parse(query: String): SearchIntent {
        val lower = query.lowercase(Locale.ROOT)

        var category: String? = null
        if (lower.contains("whatsapp")) category = "whatsapp"
        if (lower.contains("screenshot")) category = "screenshots"
        if (lower.contains("camera")) category = "camera"

        val isDocument = lower.contains("document") || lower.contains("id") || lower.contains("card")
        val isReceipt = lower.contains("receipt") || lower.contains("bill") || lower.contains("invoice")

        return SearchIntent(
            category = category,
            isDocument = isDocument,
            isReceipt = isReceipt,
            rawQuery = query
        )
    }
}
