package com.aktarjabed.jagallery.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.os.Build
import android.provider.MediaStore
import java.io.OutputStream

object ImageEditorUtils {

fun rotateImage(bitmap: Bitmap, degrees: Float): Bitmap {
val matrix = Matrix().apply { postRotate(degrees) }
return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

fun cropToSquare(bitmap: Bitmap): Bitmap {
val size = minOf(bitmap.width, bitmap.height)
val x = (bitmap.width - size) / 2
val y = (bitmap.height - size) / 2
return Bitmap.createBitmap(bitmap, x, y, size, size)
}

fun cropToAspectRatio(bitmap: Bitmap, aspectRatio: Float): Bitmap {
val width = bitmap.width
val height = bitmap.height
val targetAspect = aspectRatio

var cropWidth = width
var cropHeight = height

if (width > height * targetAspect) {
cropWidth = (height * targetAspect).toInt()
} else {
cropHeight = (width / targetAspect).toInt()
}

val x = (width - cropWidth) / 2
val y = (height - cropHeight) / 2

return Bitmap.createBitmap(bitmap, x, y, cropWidth, cropHeight)
}

fun flipImageHorizontal(bitmap: Bitmap): Bitmap {
val matrix = Matrix().apply {
setScale(-1f, 1f)
postTranslate(bitmap.width.toFloat(), 0f)
}
return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

fun flipImageVertical(bitmap: Bitmap): Bitmap {
val matrix = Matrix().apply {
setScale(1f, -1f)
postTranslate(0f, bitmap.height.toFloat())
}
return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

fun adjustBrightness(bitmap: Bitmap, brightness: Float): Bitmap {
val output = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
val canvas = Canvas(output)
val paint = Paint()

val colorMatrix = ColorMatrix().apply {
set(floatArrayOf(
1f, 0f, 0f, 0f, brightness,
0f, 1f, 0f, 0f, brightness,
0f, 0f, 1f, 0f, brightness,
0f, 0f, 0f, 1f, 0f
))
}

paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
canvas.drawBitmap(bitmap, 0f, 0f, paint)
return output
}

fun adjustContrast(bitmap: Bitmap, contrast: Float): Bitmap {
val output = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
val canvas = Canvas(output)
val paint = Paint()

val scale = contrast
val translate = (1f - scale) * 0.5f * 255f

val colorMatrix = ColorMatrix().apply {
set(floatArrayOf(
scale, 0f, 0f, 0f, translate,
0f, scale, 0f, 0f, translate,
0f, 0f, scale, 0f, translate,
0f, 0f, 0f, 1f, 0f
))
}

paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
canvas.drawBitmap(bitmap, 0f, 0f, paint)
return output
}

fun adjustSaturation(bitmap: Bitmap, saturation: Float): Bitmap {
val output = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
val canvas = Canvas(output)
val paint = Paint()

val colorMatrix = ColorMatrix().apply {
setSaturation(saturation)
}

paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
canvas.drawBitmap(bitmap, 0f, 0f, paint)
return output
}

enum class ImageFilter {
GRAYSCALE, SEPIA, INVERT, VIVID, WARM, COOL
}

fun applyFilter(bitmap: Bitmap, filterType: ImageFilter): Bitmap {
val output = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
val canvas = Canvas(output)
val paint = Paint()
val colorMatrix = ColorMatrix()

when (filterType) {
ImageFilter.GRAYSCALE -> colorMatrix.setSaturation(0f)
ImageFilter.SEPIA -> {
val matrix = floatArrayOf(
0.393f, 0.769f, 0.189f, 0f, 0f,
0.349f, 0.686f, 0.168f, 0f, 0f,
0.272f, 0.534f, 0.131f, 0f, 0f,
0f, 0f, 0f, 1f, 0f
)
colorMatrix.set(matrix)
}
ImageFilter.INVERT -> {
val matrix = floatArrayOf(
-1f, 0f, 0f, 0f, 255f,
0f, -1f, 0f, 0f, 255f,
0f, 0f, -1f, 0f, 255f,
0f, 0f, 0f, 1f, 0f
)
colorMatrix.set(matrix)
}
ImageFilter.VIVID -> {
colorMatrix.setSaturation(1.5f)
val contrastMatrix = ColorMatrix().apply {
setScale(1.2f, 1.2f, 1.2f, 1f)
}
colorMatrix.postConcat(contrastMatrix)
}
ImageFilter.WARM -> {
val matrix = floatArrayOf(
1.2f, 0f, 0f, 0f, 20f,
0f, 1.1f, 0f, 0f, 10f,
0f, 0f, 1f, 0f, 0f,
0f, 0f, 0f, 1f, 0f
)
colorMatrix.set(matrix)
}
ImageFilter.COOL -> {
val matrix = floatArrayOf(
1f, 0f, 0f, 0f, 0f,
0f, 1f, 0f, 0f, 0f,
0f, 0f, 1.2f, 0f, 20f,
0f, 0f, 0f, 1f, 0f
)
colorMatrix.set(matrix)
}
}

paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
canvas.drawBitmap(bitmap, 0f, 0f, paint)
return output
}

fun zoomImage(bitmap: Bitmap, scale: Float): Bitmap {
val matrix = Matrix().apply { postScale(scale, scale) }
return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

suspend fun openOutput(context: Context, filename: String): Pair<Uri, OutputStream?> {
val contentValues = ContentValues().apply {
put(MediaStore.Images.Media.DISPLAY_NAME, filename)
put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/JAGallery")
put(MediaStore.Images.Media.IS_PENDING, 1)
}
}

val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
} else {
MediaStore.Images.Media.EXTERNAL_CONTENT_URI
}

val uri = context.contentResolver.insert(collection, contentValues)
val outputStream = uri?.let { context.contentResolver.openOutputStream(it) }

return Pair(uri ?: android.net.Uri.EMPTY, outputStream)
}
}
