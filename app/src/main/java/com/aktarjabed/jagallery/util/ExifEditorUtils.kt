package com.aktarjabed.jagallery.util

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.abs

data class ExifDetails(
    val dimensions: String? = null,
    val dateTime: String = "",
    val make: String = "",
    val model: String = "",
    val description: String = "",
    val artist: String = "",
    val cameraSpecs: String? = null,
    val gpsCoordinates: String = ""
)

object ExifEditorUtils {

    suspend fun readExifDetails(context: Context, uri: Uri): ExifDetails = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
                val height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
                val dimensions = if (width > 0 && height > 0) "$width x $height" else null

                val dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                    ?: ""
                val make = exif.getAttribute(ExifInterface.TAG_MAKE) ?: ""
                val model = exif.getAttribute(ExifInterface.TAG_MODEL) ?: ""
                val description = exif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION) ?: ""
                val artist = exif.getAttribute(ExifInterface.TAG_ARTIST) ?: ""

                val aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.let { "f/$it" }
                val exposure = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.let { "${it}s" }
                val iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)?.let { "ISO $it" }
                val focal = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, 0.0)
                    .takeIf { it > 0.0 }?.let { "${String.format("%.1f", it)}mm" }

                val specsList = listOfNotNull(aperture, exposure, iso, focal)
                val cameraSpecs = if (specsList.isNotEmpty()) specsList.joinToString(" • ") else null

                val latLong = exif.latLong
                val gpsCoordinates = if (latLong != null) {
                    "${String.format("%.6f", latLong[0])}, ${String.format("%.6f", latLong[1])}"
                } else {
                    ""
                }

                ExifDetails(
                    dimensions = dimensions,
                    dateTime = dateTime,
                    make = make,
                    model = model,
                    description = description,
                    artist = artist,
                    cameraSpecs = cameraSpecs,
                    gpsCoordinates = gpsCoordinates
                )
            } ?: ExifDetails()
        } catch (e: Exception) {
            ExifDetails()
        }
    }

    suspend fun saveExifEdits(
        context: Context,
        uri: Uri,
        dateTime: String,
        make: String,
        model: String,
        description: String,
        artist: String,
        gpsCoordinates: String
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                val exif = ExifInterface(pfd.fileDescriptor)
                exif.setAttribute(ExifInterface.TAG_DATETIME, dateTime.trim().ifEmpty { null })
                exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, dateTime.trim().ifEmpty { null })
                exif.setAttribute(ExifInterface.TAG_MAKE, make.trim().ifEmpty { null })
                exif.setAttribute(ExifInterface.TAG_MODEL, model.trim().ifEmpty { null })
                exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION, description.trim().ifEmpty { null })
                exif.setAttribute(ExifInterface.TAG_ARTIST, artist.trim().ifEmpty { null })

                val trimmedGps = gpsCoordinates.trim()
                if (trimmedGps.isEmpty()) {
                    clearGpsAttributes(exif)
                } else {
                    val parts = trimmedGps.split(",")
                    if (parts.size == 2) {
                        val lat = parts[0].trim().toDoubleOrNull()
                        val lon = parts[1].trim().toDoubleOrNull()
                        if (lat != null && lon != null && lat in -90.0..90.0 && lon in -180.0..180.0) {
                            setGpsLatLon(exif, lat, lon)
                        }
                    }
                }

                exif.saveAttributes()
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun stripSensitiveExif(context: Context, uri: Uri): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openFileDescriptor(uri, "rw")?.use { pfd ->
                val exif = ExifInterface(pfd.fileDescriptor)
                clearGpsAttributes(exif)
                exif.setAttribute(ExifInterface.TAG_MAKE, null)
                exif.setAttribute(ExifInterface.TAG_MODEL, null)
                exif.setAttribute(ExifInterface.TAG_SOFTWARE, null)
                exif.setAttribute(ExifInterface.TAG_ARTIST, null)
                exif.setAttribute(ExifInterface.TAG_COPYRIGHT, null)
                exif.setAttribute(ExifInterface.TAG_USER_COMMENT, null)
                exif.setAttribute(ExifInterface.TAG_CAMERA_OWNER_NAME, null)
                exif.setAttribute(ExifInterface.TAG_BODY_SERIAL_NUMBER, null)
                exif.setAttribute(ExifInterface.TAG_LENS_SERIAL_NUMBER, null)
                exif.saveAttributes()
                true
            } ?: false
        } catch (e: Exception) {
            false
        }
    }

    private fun clearGpsAttributes(exif: ExifInterface) {
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, null)
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, null)
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, null)
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, null)
        exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE, null)
        exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF, null)
        exif.setAttribute(ExifInterface.TAG_GPS_TIMESTAMP, null)
        exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, null)
    }

    private fun setGpsLatLon(exif: ExifInterface, latitude: Double, longitude: Double) {
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE, decimalToDms(latitude))
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, if (latitude >= 0) "N" else "S")
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, decimalToDms(longitude))
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, if (longitude >= 0) "E" else "W")
    }

    private fun decimalToDms(coord: Double): String {
        val absCoord = abs(coord)
        val degrees = absCoord.toInt()
        val minutesFull = (absCoord - degrees) * 60.0
        val minutes = minutesFull.toInt()
        val seconds = ((minutesFull - minutes) * 60.0 * 10000).toInt()
        return "$degrees/1,$minutes/1,$seconds/10000"
    }
}
