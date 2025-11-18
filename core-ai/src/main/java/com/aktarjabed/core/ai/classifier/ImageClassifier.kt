package com.aktarjabed.core.ai.classifier

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.processor.ImageProcessor
import org.tensorflow.lite.support.image.processor.ResizeOp
import org.tensorflow.lite.support.image.processor.NormalizeOp
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.classifier.ImageClassifier as TFLiteImageClassifier
import java.io.IOException
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Image classifier using TensorFlow Lite Task Library.
 *
 * Features:
 * - Runs initialization + inference off the main thread.
 * - Resizes + normalizes input to model input size.
 * - Optionally corrects orientation via rotationDegrees parameter.
 * - Safe factory method that returns Result<ImageClassifier>.
 *
 * Notes:
 * - Provide valid model file in assets (e.g. "mobilenet_v1_1.0_224_quant.tflite").
 * - If you want GPU acceleration, add GPU delegate setup in initialize() and include the GPU dependency.
 */
class ImageClassifier(
    private val context: Context,
    private val modelName: String = "mobilenet_v1_1.0_224_quant.tflite",
    private val numThreads: Int = 4,
    private val maxResults: Int = 5,
    private val scoreThreshold: Float = 0.0f, // allow thresholding in caller
    private val inputImageSize: Int = 224 // default for MobileNet; change for other models
) {
    private var classifier: TFLiteImageClassifier? = null
    private var isInitialized = false

    /**
     * Initialize classifier. Idempotent.
     */
    suspend fun initialize(): Result<Unit> = withContext(Dispatchers.Default) {
        if (isInitialized) return@withContext Result.success(Unit)

        try {
            // Build BaseOptions (CPU). If you want GPU, modify here to attach GPU delegate.
            val baseOptions = BaseOptions.builder()
                .setNumThreads(numThreads)
                .build()

            val options = TFLiteImageClassifier.ImageClassifierOptions.builder()
                .setBaseOptions(baseOptions)
                .setScoreThreshold(scoreThreshold)
                .setMaxResults(maxResults)
                .build()

            // This loads the model from assets (modelName must match an asset path)
            classifier = TFLiteImageClassifier.createFromFileAndOptions(context, modelName, options)

            isInitialized = classifier != null
            if (isInitialized) Result.success(Unit)
            else Result.failure(ClassifierException("Failed to initialize classifier (null instance)."))
        } catch (ioe: IOException) {
            Result.failure(ClassifierException("I/O error loading model: ${ioe.message}", ioe))
        } catch (ex: Exception) {
            Result.failure(ClassifierException("Failed to initialize classifier: ${ex.message}", ex))
        }
    }

    /**
     * Classify bitmap.
     * - rotationDegrees: rotation to apply before classification (useful for camera images).
     * Returns sorted recognitions by confidence descending.
     */
    suspend fun classify(bitmap: Bitmap, rotationDegrees: Int = 0): Result<List<Recognition>> =
        withContext(Dispatchers.Default) {
            if (!isInitialized || classifier == null) {
                return@withContext Result.failure(ClassifierException("Classifier not initialized."))
            }
            try {
                // Step 1: optionally rotate bitmap (cheap when bitmap small; if large prefer decode with rotation)
                val prepared = if (rotationDegrees != 0) rotateBitmap(bitmap, rotationDegrees) else bitmap

                // Step 2: Resize + normalize using ImageProcessor to model input size
                val imageProcessor = ImageProcessor.Builder()
                    .add(ResizeOp(inputImageSize, inputImageSize, ResizeOp.ResizeMethod.BILINEAR))
                    // MobileNet-like normalization: [0,255] -> [-1,1] or [0,1]. Adjust if model expects different.
                    // Common quantized models expect raw ints; normalization might be no-op for quantized models.
                    // Here we normalize to [0,1] (change if your model requires [-1,1]).
                    .add(NormalizeOp(0f, 255f))
                    .build()

                val tensorImage = TensorImage.fromBitmap(prepared)
                val processed = imageProcessor.process(tensorImage)

                // Step 3: inference
                val results = classifier?.classify(processed)
                    ?: return@withContext Result.failure(ClassifierException("Classifier returned null."))

                // Map to Recognition list
                val recognitions = results
                    .flatMap { classification ->
                        classification.categories.map { category ->
                            Recognition(
                                id = category.label.hashCode().toString(),
                                title = category.label,
                                confidence = category.score
                            )
                        }
                    }
                    .sortedByDescending { it.confidence }

                Result.success(recognitions)
            } catch (ex: Exception) {
                Result.failure(ClassifierException("Classification failed: ${ex.message}", ex))
            }
        }

    /**
     * Cleanup resources.
     */
    suspend fun close() = withContext(Dispatchers.Default) {
        try {
            classifier?.close()
            classifier = null
            isInitialized = false
        } catch (_: Exception) {
            // best-effort cleanup
        }
    }

    /** Rotate a bitmap by degrees (returns same instance if degrees == 0) */
    private fun rotateBitmap(src: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return src
        return try {
            val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
            Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
        } catch (ex: Exception) {
            src // fallback: return original to avoid crash
        }
    }

    data class Recognition(val id: String, val title: String, val confidence: Float) {
        override fun toString(): String = "$title (${String.format("%.2f%%", confidence * 100)})"
    }

    class ClassifierException(message: String, cause: Throwable? = null) : Exception(message, cause)

    companion object {
        /**
         * Factory: initializes classifier and returns Result<ImageClassifier>.
         * Use from a coroutine scope (e.g., ViewModelScope).
         */
        suspend fun create(
            context: Context,
            modelName: String = "mobilenet_v1_1.0_224_quant.tflite",
            numThreads: Int = 4,
            maxResults: Int = 5,
            scoreThreshold: Float = 0.0f,
            inputImageSize: Int = 224
        ): Result<ImageClassifier> = withContext(Dispatchers.Default) {
            val calc = ImageClassifier(context, modelName, numThreads, maxResults, scoreThreshold, inputImageSize)
            calc.initialize().fold(
                onSuccess = { Result.success(calc) },
                onFailure = { Result.failure(it) }
            )
        }
    }
}
