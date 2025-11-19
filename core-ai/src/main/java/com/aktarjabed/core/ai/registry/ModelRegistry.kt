package com.aktarjabed.core.ai.registry

data class ModelInfo(
    val filename: String,
    val url: String,
    val sha256: String?,
    val inputSize: Int,
    val supportsGpu: Boolean = false
)

object ModelRegistry {
    // Replace URLs with your GitHub Releases or chosen host
    val MOBILENET_V1 = ModelInfo(
        filename = "mobilenet_v1_1.0_224_quant.tflite",
        url = "https://github.com/aktarjabed/JAGallery/releases/download/models/mobilenet_v1_1.0_224_quant.tflite",
        sha256 = null, // put real sha256 if you have it
        inputSize = 224,
        supportsGpu = false
    )

    // Add OCR model only if you plan local OCR. For ML Kit you don't need model here.
    val ALL = listOf(MOBILENET_V1)
}
