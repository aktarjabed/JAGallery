package com.aktarjabed.jagallery.util

import android.content.Context
import android.location.Geocoder
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.aktarjabed.jagallery.data.local.SmartTagDao
import com.aktarjabed.jagallery.data.local.SmartTagEntity
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.Locale

class SmartAnalyzer(
    private val context: Context,
    private val smartTagDao: SmartTagDao
) {

    suspend fun analyzeAndSave(uri: Uri) = withContext(Dispatchers.IO) {
        try {
            val inputImage = InputImage.fromFilePath(context, uri)

            val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)
            val textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

            val labels = labeler.process(inputImage).await()
            val textResult = textRecognizer.process(inputImage).await()

            val bestLabel = labels.maxByOrNull { it.confidence }
            val labelName = bestLabel?.text
            val confidenceScore = bestLabel?.confidence

            val extractedText = textResult.text.takeIf { it.isNotBlank() }
            val locationName = extractLocation(uri)

            val entity = SmartTagEntity(
                uriStr = uri.toString(),
                primaryLabel = labelName,
                confidence = confidenceScore,
                locationName = locationName,
                extractedText = extractedText
            )

            smartTagDao.insertTag(entity)

        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun extractLocation(uri: Uri): String? {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                val latLong = exif.latLong
                if (latLong != null) {
                    val geocoder = Geocoder(context, Locale.getDefault())
                    @Suppress("DEPRECATION")
                    val addresses = geocoder.getFromLocation(latLong[0], latLong[1], 1)
                    if (!addresses.isNullOrEmpty()) {
                        val address = addresses[0]
                        return address.locality ?: address.subAdminArea ?: address.adminArea ?: address.countryName
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}
