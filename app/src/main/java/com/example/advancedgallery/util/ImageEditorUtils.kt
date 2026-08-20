package com.example.advancedgallery.util

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ImageEditorUtils {

    suspend fun decodeSampledBitmapFromUri(
        context: Context,
        uri: Uri,
        reqWidth: Int = 2048,
        reqHeight: Int = 2048
    ): Bitmap? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return@withContext null

            options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
            options.inJustDecodeBounds = false

            val bitmap = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            } ?: return@withContext null

            // Apply EXIF orientation correction
            val exifDegrees = getExifOrientationDegrees(context, uri)
            if (exifDegrees != 0) {
                val matrix = Matrix().apply { postRotate(exifDegrees.toFloat()) }
                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (rotated != bitmap) {
                    bitmap.recycle()
                }
                rotated
            } else {
                bitmap
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun decodeFullResolutionBitmapFromUri(
        context: Context,
        uri: Uri
    ): Bitmap? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        try {
            val bitmap = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: return@withContext null

            val exifDegrees = getExifOrientationDegrees(context, uri)
            if (exifDegrees != 0) {
                val matrix = Matrix().apply { postRotate(exifDegrees.toFloat()) }
                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (rotated != bitmap) {
                    bitmap.recycle()
                }
                rotated
            } else {
                bitmap
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun applyAdjustmentsAndRotation(
        sourceBitmap: Bitmap,
        rotationDegrees: Float,
        brightness: Float, // -100f to +100f
        contrast: Float,   // 0.5f to 2.0f
        saturation: Float  // 0.0f to 2.0f
    ): Bitmap {
        val matrix = Matrix().apply {
            postRotate(rotationDegrees)
        }

        val rotated = if (rotationDegrees != 0f) {
            Bitmap.createBitmap(
                sourceBitmap, 0, 0, sourceBitmap.width, sourceBitmap.height, matrix, true
            )
        } else {
            sourceBitmap
        }

        val cm = ColorMatrix()

        // Saturation
        val satMatrix = ColorMatrix().apply { setSaturation(saturation) }
        cm.postConcat(satMatrix)

        // Contrast & Brightness
        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )
        cm.postConcat(contrastMatrix)

        val result = Bitmap.createBitmap(rotated.width, rotated.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(rotated, 0f, 0f, paint)

        if (rotated != sourceBitmap) {
            rotated.recycle()
        }

        return result
    }

    suspend fun saveEditedImage(
        context: Context,
        bitmap: Bitmap,
        sourceUri: Uri? = null
    ): Uri? = withContext(Dispatchers.IO) {
        val resolver: ContentResolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "EDIT_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val newUri = resolver.insert(collection, contentValues) ?: return@withContext null

        var success = false
        try {
            resolver.openOutputStream(newUri)?.use { outputStream ->
                success = bitmap.compress(Bitmap.CompressFormat.JPEG, 92, outputStream)
            }

            if (success && sourceUri != null) {
                try {
                    copyExifAttributes(context, sourceUri, newUri)
                } catch (e: Exception) {
                    // Ignore non-fatal EXIF copy errors
                }
            }

            if (success && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(newUri, contentValues, null, null)
            }
        } catch (e: Exception) {
            success = false
        }

        if (!success) {
            try {
                resolver.delete(newUri, null, null)
            } catch (e: Exception) {
                // ignore
            }
            null
        } else {
            newUri
        }
    }

    fun copyExifAttributes(context: Context, sourceUri: Uri, destinationUri: Uri) {
        try {
            val sourceExif = context.contentResolver.openInputStream(sourceUri)?.use { stream ->
                ExifInterface(stream)
            } ?: return

            context.contentResolver.openFileDescriptor(destinationUri, "rw")?.use { pfd ->
                val destExif = ExifInterface(pfd.fileDescriptor)

                val attributes = arrayOf(
                    ExifInterface.TAG_DATETIME,
                    ExifInterface.TAG_DATETIME_DIGITIZED,
                    ExifInterface.TAG_DATETIME_ORIGINAL,
                    ExifInterface.TAG_MAKE,
                    ExifInterface.TAG_MODEL,
                    ExifInterface.TAG_FLASH,
                    ExifInterface.TAG_FOCAL_LENGTH,
                    ExifInterface.TAG_WHITE_BALANCE,
                    ExifInterface.TAG_EXPOSURE_TIME,
                    ExifInterface.TAG_F_NUMBER,
                    ExifInterface.TAG_ISO_SPEED_RATINGS
                )

                for (attr in attributes) {
                    val value = sourceExif.getAttribute(attr)
                    if (value != null) {
                        destExif.setAttribute(attr, value)
                    }
                }
                destExif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                destExif.saveAttributes()
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    fun getExifOrientationDegrees(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }
}
