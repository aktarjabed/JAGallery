package com.example.advancedgallery.util

import android.app.PendingIntent
import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log

object FileUtils {
    private const val TAG = "FileUtils"

    fun createDeleteRequest(contentResolver: ContentResolver, uris: List<Uri>): PendingIntent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                MediaStore.createDeleteRequest(contentResolver, uris)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create delete request", e)
                null
            }
        } else {
            null
        }
    }

    fun createTrashRequest(contentResolver: ContentResolver, uris: List<Uri>, value: Boolean): PendingIntent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                MediaStore.createTrashRequest(contentResolver, uris, value)
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
}
