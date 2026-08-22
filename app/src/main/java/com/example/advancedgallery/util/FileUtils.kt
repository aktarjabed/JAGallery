package com.example.advancedgallery.util

import android.app.PendingIntent
import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log

object FileUtils {
    private const val TAG = "FileUtils"
    private const val MAX_BATCH_SIZE = 2000

    fun createDeleteRequest(contentResolver: ContentResolver, uris: List<Uri>): PendingIntent? {
        if (uris.isEmpty()) return null
        val batchedUris = uris.take(MAX_BATCH_SIZE)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                MediaStore.createDeleteRequest(contentResolver, batchedUris)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create delete request", e)
                null
            }
        } else {
            null
        }
    }

    fun createTrashRequest(contentResolver: ContentResolver, uris: List<Uri>, value: Boolean): PendingIntent? {
        if (uris.isEmpty()) return null
        val batchedUris = uris.take(MAX_BATCH_SIZE)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                MediaStore.createTrashRequest(contentResolver, batchedUris, value)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create trash request", e)
                null
            }
        } else {
            null
        }
    }

    fun deleteMediaItems(contentResolver: ContentResolver, uris: List<Uri>): Boolean {
        var success = true
        for (uri in uris) {
            try {
                val rows = contentResolver.delete(uri, null, null)
                if (rows <= 0) {
                    Log.w(TAG, "No rows deleted for URI $uri")
                    success = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception deleting URI $uri", e)
                success = false
            }
        }
        return success
    }

    fun copyMediaFile(contentResolver: ContentResolver, sourceUri: Uri, destUri: Uri): Boolean {
        return try {
            contentResolver.openInputStream(sourceUri)?.use { input ->
                contentResolver.openOutputStream(destUri)?.use { output ->
                    // Use a 64KB buffer for faster copying of large media files
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int = 0
                    while (input.read(buffer).also { bytesRead = it } >= 0) {
                        output.write(buffer, 0, bytesRead)
                    }
                    output.flush()
                    true
                }
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy media file from $sourceUri to $destUri", e)
            false
        }
    }
}
