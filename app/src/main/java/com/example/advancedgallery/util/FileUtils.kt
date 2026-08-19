package com.example.advancedgallery.util

import android.app.PendingIntent
import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

object FileUtils {
    fun createDeleteRequest(contentResolver: ContentResolver, uris: List<Uri>): PendingIntent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            MediaStore.createDeleteRequest(contentResolver, uris)
        } else {
            null
        }
    }

    fun deleteMediaItems(contentResolver: ContentResolver, uris: List<Uri>): Boolean {
        var success = true
        for (uri in uris) {
            try {
                val rows = contentResolver.delete(uri, null, null)
                if (rows <= 0) success = false
            } catch (e: Exception) {
                e.printStackTrace()
                success = false
            }
        }
        return success
    }
}
