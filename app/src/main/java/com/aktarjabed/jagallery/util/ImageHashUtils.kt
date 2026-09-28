package com.aktarjabed.jagallery.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.security.MessageDigest

object ImageHashUtils {

    suspend fun calculateSha256(uri: Uri, context: Context): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val digest = MessageDigest.getInstance("SHA-256")
            val inputStream: InputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            inputStream.use { input ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (input.read(buffer).also { bytesRead = it } != -1) {
                    ensureActive()
                    digest.update(buffer, 0, bytesRead)
                }
            }
            val hashBytes = digest.digest()
            hashBytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun calculatePHash(uri: Uri, context: Context): Long? = withContext(Dispatchers.IO) {
        return@withContext try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = false
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val inputStream: InputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val bitmap = inputStream.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            } ?: return@withContext null

            // Resize to 9x8 for difference hash
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 9, 8, true)
            if (scaledBitmap != bitmap) {
                bitmap.recycle()
            }

            // Convert to grayscale and get pixels
            val width = scaledBitmap.width
            val height = scaledBitmap.height
            val pixels = IntArray(width * height)
            scaledBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            scaledBitmap.recycle()

            // Calculate pHash (difference hash)
            var hash = 0L
            for (y in 0 until height) {
                for (x in 0 until width - 1) {
                    val indexLeft = y * width + x
                    val indexRight = y * width + (x + 1)
                    
                    val pLeft = pixels[indexLeft]
                    val pRight = pixels[indexRight]
                    
                    val grayLeft = (pLeft shr 16 and 0xFF) * 0.299 + (pLeft shr 8 and 0xFF) * 0.587 + (pLeft and 0xFF) * 0.114
                    val grayRight = (pRight shr 16 and 0xFF) * 0.299 + (pRight shr 8 and 0xFF) * 0.587 + (pRight and 0xFF) * 0.114
                    
                    hash = hash shl 1
                    if (grayLeft < grayRight) {
                        hash = hash or 1L
                    }
                }
            }
            hash
        } catch (e: Exception) {
            null
        }
    }
}
