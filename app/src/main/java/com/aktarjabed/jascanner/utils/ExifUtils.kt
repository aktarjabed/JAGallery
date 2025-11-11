package com.aktarjabed.jascanner.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface

/**
 * Utility for handling EXIF orientation data to automatically rotate images
 * based on their camera orientation metadata
 */
object ExifUtils {

    /**
     * Rotates the bitmap based on EXIF orientation data if needed
     * @param context The context to open the URI
     * @param uri The URI of the image
     * @param bitmap The original bitmap that might need rotation
     * @return The correctly oriented bitmap, or original if no rotation needed
     */
    fun rotateIfNeeded(context: Context, uri: Uri, bitmap: Bitmap?): Bitmap? {
        if (bitmap == null) return null

        return try {
            // Open input stream to read EXIF data
            val input = context.contentResolver.openInputStream(uri) ?: return bitmap
            val exif = ExifInterface(input)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            input.close()

            // Calculate rotation angle based on EXIF orientation
            val rotationAngle = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }

            // Return original if no rotation needed
            if (rotationAngle == 0f) {
                bitmap
            } else {
                // Apply rotation matrix
                val matrix = Matrix().apply { postRotate(rotationAngle) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
                    // Don't recycle the original here - let caller handle lifecycle
                }
            }
        } catch (e: Exception) {
            // If EXIF reading fails, return original bitmap
            bitmap
        }
    }

    /**
     * Extracts EXIF orientation as human-readable string for display
     */
    fun getOrientationDescription(context: Context, uri: Uri): String {
        return try {
            val input = context.contentResolver.openInputStream(uri) ?: return "Unknown"
            val exif = ExifInterface(input)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )
            input.close()

            when (orientation) {
                ExifInterface.ORIENTATION_NORMAL -> "Normal"
                ExifInterface.ORIENTATION_ROTATE_90 -> "Rotated 90°"
                ExifInterface.ORIENTATION_ROTATE_180 -> "Rotated 180°"
                ExifInterface.ORIENTATION_ROTATE_270 -> "Rotated 270°"
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> "Flipped Horizontal"
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> "Flipped Vertical"
                ExifInterface.ORIENTATION_TRANSPOSE -> "Transposed"
                ExifInterface.ORIENTATION_TRANSVERSE -> "Transverse"
                else -> "Unknown"
            }
        } catch (e: Exception) {
            "Unknown"
        }
    }

    /**
     * Gets basic EXIF information for display in image info sheet
     */
    fun getExifInfo(context: Context, uri: Uri): Map<String, String> {
        val info = mutableMapOf<String, String>()

        try {
            val input = context.contentResolver.openInputStream(uri) ?: return emptyMap()
            val exif = ExifInterface(input)
            input.close()

            // Camera make and model
            exif.getAttribute(ExifInterface.TAG_MAKE)?.let { make ->
                info["Camera Make"] = make
            }
            exif.getAttribute(ExifInterface.TAG_MODEL)?.let { model ->
                info["Camera Model"] = model
            }

            // Exposure information
            exif.getAttribute(ExifInterface.TAG_APERTURE)?.let { aperture ->
                info["Aperture"] = "ƒ/$aperture"
            }
            exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.let { exposure ->
                info["Exposure"] = exposure
            }
            exif.getAttribute(ExifInterface.TAG_ISO)?.let { iso ->
                info["ISO"] = iso
            }
            exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.let { focal ->
                info["Focal Length"] = "$focal mm"
            }

            // GPS information if available
            exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)?.let { lat ->
                exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF)?.let { latRef ->
                    exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)?.let { lon ->
                        exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF)?.let { lonRef ->
                            info["Location"] = "GPS Available"
                        }
                    }
                }
            }

        } catch (e: Exception) {
            // Silent fail - EXIF data is optional
        }

        return info
    }
}
