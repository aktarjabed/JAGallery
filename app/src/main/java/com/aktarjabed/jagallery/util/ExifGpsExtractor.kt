package com.aktarjabed.jagallery.util

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.exifinterface.media.ExifInterface

object ExifGpsExtractor {
    private const val TAG = "ExifGpsExtractor"

    fun extractLatLng(context: Context, uri: Uri): Pair<Double, Double>? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val latLong = exif.latLong
                if (latLong != null && latLong.size >= 2) {
                    latLong[0] to latLong[1]
                } else null
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException reading EXIF for $uri", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read EXIF GPS for $uri", e)
            null
        }
    }
}
