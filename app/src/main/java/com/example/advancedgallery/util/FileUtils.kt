package com.example.advancedgallery.util

import android.app.PendingIntent
import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log

object FileUtils {
    private const val TAG = "FileUtils"
    private const val MAX_BATCH_SIZE = com.example.advancedgallery.util.Constants.MAX_BATCH_SIZE

    fun createTrashRequests(contentResolver: ContentResolver, uris: List<Uri>, value: Boolean): List<PendingIntent> {
        if (uris.isEmpty()) return emptyList()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return emptyList()

        return uris.chunked(MAX_BATCH_SIZE).mapNotNull { chunk ->
            try {
                MediaStore.createTrashRequest(contentResolver, chunk, value)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create trash request for chunk", e)
                null
            }
        }
    }

    fun createDeleteRequests(contentResolver: ContentResolver, uris: List<Uri>): List<PendingIntent> {
        if (uris.isEmpty()) return emptyList()
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return emptyList()

        return uris.chunked(MAX_BATCH_SIZE).mapNotNull { chunk ->
            try {
                MediaStore.createDeleteRequest(contentResolver, chunk)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create delete request for chunk", e)
                null
            }
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

    fun untrashMediaItems(contentResolver: ContentResolver, uris: List<Uri>): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return true
        var success = true
        for (uri in uris) {
            try {
                val contentValues = android.content.ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_TRASHED, 0)
                }
                val rows = contentResolver.update(uri, contentValues, null, null)
                if (rows <= 0) {
                    Log.w(TAG, "No rows updated for untrashing URI $uri")
                    success = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception untrashing URI $uri", e)
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

    fun insertPendingMediaEntry(
        contentResolver: ContentResolver,
        collection: Uri,
        contentValues: android.content.ContentValues
    ): Uri? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        return try {
            contentResolver.insert(collection, contentValues)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert pending media entry", e)
            null
        }
    }

    fun publishPendingEntry(
        contentResolver: ContentResolver,
        uri: Uri,
        contentValues: android.content.ContentValues
    ): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            return try {
                val updated = contentResolver.update(uri, contentValues, null, null)
                updated == 1
            } catch (e: Exception) {
                Log.e(TAG, "Failed to publish pending entry", e)
                false
            }
        }
        return true // No IS_PENDING before API 29
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val clamped = digitGroups.coerceIn(0, units.size - 1)
        return String.format("%.1f %s", bytes / Math.pow(1024.0, clamped.toDouble()), units[clamped])
    }
}
