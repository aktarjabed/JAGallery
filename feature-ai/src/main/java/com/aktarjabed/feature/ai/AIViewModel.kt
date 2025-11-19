package com.aktarjabed.feature.ai

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.core.ai.classifier.ImageClassifier
import com.aktarjabed.core.ai.downloader.ModelDownloader
import com.aktarjabed.core.ai.ocr.TextRecognizer
import com.aktarjabed.core.ai.registry.ModelInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

data class ImageAnalysis(
    val tags: List<String> = emptyList(),
    val extractedText: String = "",
    val recognitions: List<ImageClassifier.Recognition> = emptyList()
)

sealed class AIState {
    object Idle : AIState()
    data class DownloadingModel(val progress: Int) : AIState()
    object InitializingClassifier : AIState()
    object Ready : AIState()
    data class Analyzing(val imageId: String) : AIState()
    data class AnalysisComplete(val analysis: ImageAnalysis) : AIState()
    data class Error(val message: String) : AIState()
}

class AIViewModel(private val context: Context) : ViewModel() {
    private val downloader = ModelDownloader(context)
    private var classifier: ImageClassifier? = null
    private val textRecognizer = TextRecognizer()

    // Expose simple state via callback properties (or use StateFlow/LiveData as needed)
    var onStateChanged: ((AIState) -> Unit)? = null

    fun prepareModel(modelInfo: ModelInfo, useGpu: Boolean = false) {
        viewModelScope.launch {
            onStateChanged?.invoke(AIState.DownloadingModel(0))
            val result = withContext(Dispatchers.IO) {
                try {
                    if (!downloader.isModelDownloaded(modelInfo.filename)) {
                        downloader.downloadModel(modelInfo.url, modelInfo.filename, modelInfo.sha256)
                    } else Result.success(downloader.getModelFile(modelInfo.filename))
                } catch (ex: Exception) {
                    Result.failure(ex)
                }
            }

            result.fold(onSuccess = { file ->
                onStateChanged?.invoke(AIState.InitializingClassifier)
                viewModelScope.launch {
                    ImageClassifier.create(
                        context = context,
                        modelPath = file.absolutePath,
                        inputImageSize = modelInfo.inputSize,
                        useGpu = useGpu && modelInfo.supportsGpu
                    ).fold(onSuccess = { c ->
                        classifier = c
                        onStateChanged?.invoke(AIState.Ready)
                    }, onFailure = { e ->
                        onStateChanged?.invoke(AIState.Error("Classifier init failed: ${e.message}"))
                    })
                }
            }, onFailure = {
                onStateChanged?.invoke(AIState.Error("Download failed: ${it.message}"))
            })
        }
    }

    fun analyzeImage(bitmap: Bitmap, rotationDegrees: Int = 0) {
        viewModelScope.launch {
            val imageId = System.currentTimeMillis().toString()
            onStateChanged?.invoke(AIState.Analyzing(imageId))

            try {
                val classifyDeferred = async { classifier?.classify(bitmap, rotationDegrees) }
                val ocrDeferred = async { textRecognizer.extractText(bitmap) }

                val classifyRes = classifyDeferred.await()
                val ocrRes = ocrDeferred.await()

                val recognitions = classifyRes?.getOrNull() ?: emptyList()
                val extractedText = ocrRes.getOrNull() ?: ""
                val tags = recognitions.filter { it.confidence >= 0.3f }.map { it.title }

                onStateChanged?.invoke(AIState.AnalysisComplete(ImageAnalysis(tags, extractedText, recognitions)))
            } catch (ex: Exception) {
                onStateChanged?.invoke(AIState.Error("Analysis failed: ${ex.message}"))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            classifier?.close()
            textRecognizer.close()
        }
    }
}
