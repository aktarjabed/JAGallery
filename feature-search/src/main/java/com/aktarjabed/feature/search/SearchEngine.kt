package com.aktarjabed.feature.search

data class IndexedImage(
    val id: String,
    val path: String,
    val tags: List<String>,
    val extractedText: String,
    val timestamp: Long
)

class SearchEngine {
    private val imageIndex = mutableListOf<IndexedImage>()

    fun indexImage(image: IndexedImage) {
        imageIndex.removeAll { it.id == image.id }
        imageIndex.add(image)
    }

    fun search(query: String): List<IndexedImage> {
        val normalizedQuery = query.lowercase().trim()
        if (normalizedQuery.isEmpty()) return emptyList()
        val queryTerms = normalizedQuery.split("\\s+".toRegex()).map { it.trim() }.filter { it.isNotEmpty() }

        return imageIndex
            .map { image ->
                val score = queryTerms.sumOf { term ->
                    val tagMatches = image.tags.count { it.lowercase().contains(term) }
                    val textMatches = if (image.extractedText.lowercase().contains(term)) 1 else 0
                    tagMatches * 3 + textMatches
                }
                image to score
            }
            .filter { it.second > 0 }
            .sortedByDescending { it.second }
            .map { it.first }
    }

    fun clear() = imageIndex.clear()
}
