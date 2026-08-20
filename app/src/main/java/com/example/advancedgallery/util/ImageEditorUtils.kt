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
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

data class TransformationPlan(
    val rotationDegrees: Float = 0f,
    val brightness: Float = 0f,
    val contrast: Float = 1f,
    val saturation: Float = 1f
) {
    fun isIdentity(): Boolean =
        rotationDegrees == 0f && brightness == 0f && contrast == 1f && saturation == 1f
}

object ImageEditorUtils {

    private const val TAG = "ImageEditorUtils"

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
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to decode sampled bitmap", e)
            null
        }
    }

    suspend fun decodeFullResolutionBitmapFromUri(
        context: Context,
        uri: Uri,
        inSampleSize: Int = 1
    ): Bitmap? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        try {
            val options = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
            }
            val bitmap = resolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
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
        } catch (e: OutOfMemoryError) {
            Log.w(TAG, "OOM decoding full resolution bitmap with sample size $inSampleSize")
            null
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to decode full resolution bitmap", e)
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

    suspend fun applyTransformationPlan(
        sourceBitmap: Bitmap,
        plan: TransformationPlan
    ): Bitmap = withContext(Dispatchers.Default) {
        coroutineContext.ensureActive()

        if (plan.isIdentity()) {
            return@withContext sourceBitmap
        }

        val matrix = Matrix().apply {
            postRotate(plan.rotationDegrees)
        }

        val rotated = if (plan.rotationDegrees != 0f) {
            Bitmap.createBitmap(
                sourceBitmap, 0, 0, sourceBitmap.width, sourceBitmap.height, matrix, true
            )
        } else {
            sourceBitmap
        }

        coroutineContext.ensureActive()

        val cm = ColorMatrix()

        val satMatrix = ColorMatrix().apply { setSaturation(plan.saturation) }
        cm.postConcat(satMatrix)

        val contrastMatrix = ColorMatrix(
            floatArrayOf(
                plan.contrast, 0f, 0f, 0f, plan.brightness,
                0f, plan.contrast, 0f, 0f, plan.brightness,
                0f, 0f, plan.contrast, 0f, plan.brightness,
                0f, 0f, 0f, 1f, 0f
            )
        )
        cm.postConcat(contrastMatrix)

        coroutineContext.ensureActive()

        val result = Bitmap.createBitmap(rotated.width, rotated.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(rotated, 0f, 0f, paint)

        if (rotated != sourceBitmap) {
            rotated.recycle()
        }

        result
    }

    suspend fun applyAdjustmentsAndRotation(
        sourceBitmap: Bitmap,
        rotationDegrees: Float,
        brightness: Float,
        contrast: Float,
        saturation: Float
    ): Bitmap = applyTransformationPlan(
        sourceBitmap,
        TransformationPlan(rotationDegrees, brightness, contrast, saturation)
    )

    suspend fun exportEditedImageWithProgressiveFallback(
        context: Context,
        uri: Uri,
        plan: TransformationPlan
    ): Pair<Uri?, String?> = withContext(Dispatchers.IO) {
        var sampleSize = 1
        var resultUri: Uri? = null
        var exportDimensions: String? = null

        while (sampleSize <= 8) {
            coroutineContext.ensureActive()
            try {
                val fullRes = decodeFullResolutionBitmapFromUri(context, uri, sampleSize)
                if (fullRes == null) {
                    sampleSize *= 2
                    continue
                }

                exportDimensions = "${fullRes.width}x${fullRes.height}"

                val edited = applyTransformationPlan(
                    sourceBitmap = fullRes,
                    plan = plan
                )

                resultUri = saveEditedImage(context, edited, uri)

                if (edited != fullRes) {
                    edited.recycle()
                }
                fullRes.recycle()

                if (resultUri != null) {
                    break
                }
            } catch (e: OutOfMemoryError) {
                Log.w(TAG, "OOM during export at sample size $sampleSize, falling back")
                sampleSize *= 2
            } catch (e: Throwable) {
                Log.e(TAG, "Export error at sample size $sampleSize", e)
                break
            }
        }

        Pair(resultUri, exportDimensions)
    }

    suspend fun exportEditedImageWithProgressiveFallback(
        context: Context,
        uri: Uri,
        rotationDegrees: Float,
        brightness: Float,
        contrast: Float,
        saturation: Float
    ): Pair<Uri?, String?> = exportEditedImageWithProgressiveFallback(
        context,
        uri,
        TransformationPlan(rotationDegrees, brightness, contrast, saturation)
    )

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
        var newUri: Uri? = null

        try {
            newUri = resolver.insert(collection, contentValues)
            if (newUri == null) return@withContext null

            var success = false
            resolver.openOutputStream(newUri)?.use { outputStream ->
                success = bitmap.compress(Bitmap.CompressFormat.JPEG, 92, outputStream)
            }

            if (success && sourceUri != null) {
                try {
                    copyExifAttributes(context, sourceUri, newUri)
                } catch (e: Exception) {
                    Log.w(TAG, "EXIF copy failed non-fatally", e)
                }
            }

            if (success && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(newUri, contentValues, null, null)
            }

            if (!success) {
                resolver.delete(newUri, null, null)
                null
            } else {
                newUri
            }
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to save edited image", e)
            if (newUri != null) {
                try {
                    resolver.delete(newUri, null, null)
                } catch (delEx: Exception) {
                    // ignore
                }
            }
            null
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
                    ExifInterface.TAG_F_NUMBER
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
            Log.w(TAG, "Failed to copy EXIF attributes", e)
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
