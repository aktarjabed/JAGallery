package com.example.advancedgallery.util

import android.content.Context
import android.util.Log
import com.example.advancedgallery.data.model.MediaItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.security.MessageDigest

data class DuplicateGroup(
    val size: Long,
    val items: List<MediaItem>
)

object DuplicateDetector {
    private const val TAG = "DuplicateDetector"

    suspend fun findDuplicates(context: Context, items: List<MediaItem>): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        val sizeGroups = items.groupBy { it.size }.filter { it.key > 0 && it.value.size > 1 }
        val duplicateGroups = mutableListOf<DuplicateGroup>()

        for ((size, candidateItems) in sizeGroups) {
            val hashGroups = mutableMapOf<String, MutableList<MediaItem>>()
            for (item in candidateItems) {
                val hash = computeSha256(context, item)
                if (hash != null) {
                    hashGroups.getOrPut(hash) { mutableListOf() }.add(item)
                }
            }

            for ((_, matchingItems) in hashGroups) {
                if (matchingItems.size > 1) {
                    duplicateGroups.add(DuplicateGroup(size = size, items = matchingItems))
                }
            }
        }

        duplicateGroups
    }

    private fun computeSha256(context: Context, item: MediaItem): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            context.contentResolver.openInputStream(item.uri)?.use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val hashBytes = digest.digest()
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compute SHA-256 for ${item.uri}", e)
            null
        }
    }
}
