package com.aktarjabed.core.ai.classifier

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.processor.ImageProcessor
import org.tensorflow.lite.support.image.processor.ResizeOp
import org.tensorflow.lite.support.image.processor.NormalizeOp
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.classifier.ImageClassifier as TFLiteImageClassifier
import java.io.File
import java.io.IOException

/**
 * ImageClassifier: loads model from asset name (if modelPath is an asset name) or from absolute file path.
 * - modelPath: if starts with "/", treated as absolute file path; otherwise as asset name.
 * - useGpu: if true, attempt GPU delegate (requires tensorflow-lite-gpu dependency)
 */
class ImageClassifier(
    private val context: Context,
    private val modelPath: String, // asset name or absolute path
    private val numThreads: Int = 4,
    private val maxResults: Int = 5,
    private val scoreThreshold: Float = 0.0f,
    private val inputImageSize: Int = 224,
    private val useGpu: Boolean = false
) {
    private var classifier: TFLiteImageClassifier? = null
    private var isInitialized = false

    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.Default) {
        if (isInitialized) return@withContext Result.success(Unit)
        try {
            val baseOptionsBuilder = BaseOptions.builder().setNumThreads(numThreads)
            if (useGpu) {
                try {
                    baseOptionsBuilder.setUseGpu(true)
                } catch (err: NoSuchMethodError) {
                    Log.w("ImageClassifier", "GPU option unavailable for BaseOptions on this platform", err)
                } catch (ex: Exception) {
                    Log.w("ImageClassifier", "Failed to enable GPU delegate; will fall back to CPU", ex)
                }
            }
            val baseOptions = baseOptionsBuilder.build()

            val options = TFLiteImageClassifier.ImageClassifierOptions.builder()
                .setBaseOptions(baseOptions)
                .setScoreThreshold(scoreThreshold)
                .setMaxResults(maxResults)
                .build()

            // load from absolute file path when provided, otherwise treat as asset name
            classifier = if (modelPath.startsWith("/")) {
                TFLiteImageClassifier.createFromFileAndOptions(File(modelPath), options)
            } else {
                TFLiteImageClassifier.createFromFileAndOptions(context, modelPath, options)
            }

            isInitialized = classifier != null
            if (isInitialized) Result.success(Unit)
            else Result.failure(ClassifierException("Classifier initialization returned null"))
        } catch (ioe: IOException) {
            Result.failure(ClassifierException("I/O error loading model: ${ioe.message}", ioe))
        } catch (ex: Exception) {
            Result.failure(ClassifierException("Failed to initialize classifier: ${ex.message}", ex))
        }
    }

    suspend fun classify(bitmap: Bitmap, rotationDegrees: Int = 0): Result<List<Recognition>> =
        withContext(Dispatchers.Default) {
            if (!isInitialized || classifier == null) {
                return@withContext Result.failure(ClassifierException("Classifier not initialized"))
            }
            try {
                val prepared = if (rotationDegrees != 0) rotateBitmap(bitmap, rotationDegrees) else bitmap

                val imageProcessor = ImageProcessor.Builder()
                    .add(ResizeOp(inputImageSize, inputImageSize, ResizeOp.ResizeMethod.BILINEAR))
                    // Adjust normalization if using quantized models (skip normalize for quantized)
                    .add(NormalizeOp(0f, 255f))
                    .build()

                val tensorImage = TensorImage.fromBitmap(prepared)
                val processed = imageProcessor.process(tensorImage)

                val results = classifier?.classify(processed)
                    ?: return@withContext Result.failure(ClassifierException("Classifier returned null"))

                val recognitions = results.flatMap { classification ->
                    classification.categories.map { category ->
                        Recognition(
                            id = category.label.hashCode().toString(),
                            title = category.label,
                            confidence = category.score
                        )
                    }
                }.sortedByDescending { it.confidence }

                Result.success(recognitions)
            } catch (ex: Exception) {
                Result.failure(ClassifierException("Classification failed: ${ex.message}", ex))
            }
        }

    suspend fun close() = withContext(Dispatchers.Default) {
        try {
            classifier?.close()
            classifier = null
            isInitialized = false
        } catch (_: Exception) {
        }
    }

    private fun rotateBitmap(src: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return src
        return try {
            val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
            Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        } catch (ex: Exception) {
            src
        }
    }

    data class Recognition(val id: String, val title: String, val confidence: Float)
    class ClassifierException(message: String, cause: Throwable? = null) : Exception(message, cause)

    companion object {
        /**
         * Factory for file-based or asset-based model loading.
         * modelPath: absolute path or asset name.
         */
        suspend fun create(
            context: Context,
            modelPath: String,
            numThreads: Int = 4,
            maxResults: Int = 5,
            scoreThreshold: Float = 0.0f,
            inputImageSize: Int = 224,
            useGpu: Boolean = false
        ): Result<ImageClassifier> = withContext(Dispatchers.Default) {
            val inst = ImageClassifier(context, modelPath, numThreads, maxResults, scoreThreshold, inputImageSize, useGpu)
            inst.initialize().fold(onSuccess = { Result.success(inst) }, onFailure = { Result.failure(it) })
        }
    }
}
