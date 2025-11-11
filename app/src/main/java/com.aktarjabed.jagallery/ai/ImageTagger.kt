package com.aktarjabed.jagallery.ai

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.classifier.ImageClassifier

class ImageTagger(context: Context) {
    private var classifier: ImageClassifier? = null

    init {
        try {
            val options = ImageClassifier.ImageClassifierOptions.builder()
                .setMaxResults(5)
                .setScoreThreshold(0.3f)
                .build()

            classifier = ImageClassifier.createFromFileAndOptions(
                context,
                "mobilenet_v1_1.0_224_quantized.tflite",
                options
            )
        } catch (_: Exception) {
            // Fallback to generic tags
        }
    }

    fun analyzeImage(bitmap: Bitmap): List<String> {
        return try {
            classifier?.classify(TensorImage.fromBitmap(bitmap))
                ?.firstOrNull()
                ?.categories
                ?.map { it.label }
                ?.take(5)
                ?: fallback()
        } catch (_: Exception) {
            fallback()
        }
    }

    private fun fallback(): List<String> = listOf("photo", "image", "picture", "media", "content")
}