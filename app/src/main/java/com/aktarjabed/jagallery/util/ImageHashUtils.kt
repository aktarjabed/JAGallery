package com.aktarjabed.jagallery.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.security.MessageDigest

object ImageHashUtils {

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    suspend fun calculateSha256(uri: Uri, context: Context): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val digest = MessageDigest.getInstance("SHA-256")
            val inputStream: InputStream =
                context.contentResolver.openInputStream(uri) ?: return@withContext null
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
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
    }

    suspend fun calculatePHash(uri: Uri, context: Context): Long? = withContext(Dispatchers.IO) {
        var bitmap: Bitmap? = null
        var scaledBitmap: Bitmap? = null
        return@withContext try {
            // Pass 1: read dimensions only — no pixel memory allocation
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)
                ?.use { BitmapFactory.decodeStream(it, null, boundsOptions) }

            ensureActive()

            // Pass 2: decode at reduced size to avoid OOM on large camera photos (e.g. 40 MP)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(boundsOptions, reqWidth = 512, reqHeight = 512)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            bitmap = context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOptions)
            } ?: return@withContext null

            ensureActive()

            // Resize to 9×8 for difference hash
            scaledBitmap = Bitmap.createScaledBitmap(bitmap, 9, 8, true)
            if (scaledBitmap !== bitmap) {
                bitmap.recycle()
                bitmap = null
            }

            // Build grayscale pixel array and compute hash
            val width = scaledBitmap.width
            val height = scaledBitmap.height
            val pixels = IntArray(width * height)
            scaledBitmap.getPixels(pixels, 0, width, 0, 0, width, height)
            scaledBitmap.recycle()
            scaledBitmap = null

            var hash = 0L
            for (y in 0 until height) {
                for (x in 0 until width - 1) {
                    val pLeft = pixels[y * width + x]
                    val pRight = pixels[y * width + (x + 1)]
                    val grayLeft = (pLeft shr 16 and 0xFF) * 0.299 +
                        (pLeft shr 8 and 0xFF) * 0.587 +
                        (pLeft and 0xFF) * 0.114
                    val grayRight = (pRight shr 16 and 0xFF) * 0.299 +
                        (pRight shr 8 and 0xFF) * 0.587 +
                        (pRight and 0xFF) * 0.114
                    hash = hash shl 1
                    if (grayLeft < grayRight) hash = hash or 1L
                }
            }
            hash
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        } finally {
            if (scaledBitmap != null && !scaledBitmap.isRecycled) {
                scaledBitmap.recycle()
            }
            if (bitmap != null && !bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }
}
