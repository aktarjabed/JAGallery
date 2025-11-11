package com.aktarjabed.core.ai.classifier

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.classifier.ImageClassifier

class ImageClassifier(
    private val context: Context,
    private val modelName: String = "mobilenet_v1_1.0_224_quant.tflite",
    private val numThreads: Int = 2,
    private val maxResults: Int = 3
) {

    private var classifier: ImageClassifier? = null

    private fun setupClassifier() {
        val baseOptions = BaseOptions.builder()
            .setNumThreads(numThreads)
            .build()
        val classifierOptions = ImageClassifier.ImageClassifierOptions.builder()
            .setBaseOptions(baseOptions)
            .setMaxResults(maxResults)
            .build()

        try {
            classifier = ImageClassifier.createFromFileAndOptions(
                context,
                modelName,
                classifierOptions
            )
        } catch (e: Exception) {
            // Handle model loading error
        }
    }

    fun classify(bitmap: Bitmap): List<Recognition> {
        if (classifier == null) {
            setupClassifier()
        }

        val imageProcessor = ImageProcessor.builder()
            .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.BILINEAR))
            .build()

        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmap))

        val results = classifier?.classify(tensorImage)

        return results?.flatMap { classification ->
            classification.categories.map { category ->
                Recognition(
                    label = category.label,
                    confidence = category.score
                )
            }
        } ?: emptyList()
    }

    fun close() {
        classifier?.close()
        classifier = null
    }
}