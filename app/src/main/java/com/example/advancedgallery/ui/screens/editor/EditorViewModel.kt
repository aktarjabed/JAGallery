package com.example.advancedgallery.ui.screens.editor

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.advancedgallery.R
import com.example.advancedgallery.util.ImageEditorUtils
import com.example.advancedgallery.util.TransformationPlan
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SaveState {
    object Idle : SaveState()
    object Saving : SaveState()
    data class Success(val newUri: Uri, val dimensions: String? = null) : SaveState()
    data class Error(val messageResId: Int) : SaveState()
}

@HiltViewModel
class EditorViewModel @Inject constructor() : ViewModel() {

    private var sourceUri: Uri? = null
    private var originalBitmap: Bitmap? = null
    private var currentPreviewBitmap: Bitmap? = null
    private var previewJob: Job? = null
    private var loadJob: Job? = null

    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    val previewBitmap: StateFlow<Bitmap?> = _previewBitmap.asStateFlow()

    private val _rotationDegrees = MutableStateFlow(0f)
    val rotationDegrees: StateFlow<Float> = _rotationDegrees.asStateFlow()

    private val _brightness = MutableStateFlow(0f)
    val brightness: StateFlow<Float> = _brightness.asStateFlow()

    private val _contrast = MutableStateFlow(1f)
    val contrast: StateFlow<Float> = _contrast.asStateFlow()

    private val _saturation = MutableStateFlow(1f)
    val saturation: StateFlow<Float> = _saturation.asStateFlow()

    private val _saveState = MutableStateFlow<SaveState>(SaveState.Idle)
    val saveState: StateFlow<SaveState> = _saveState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadImage(context: Context, imageUri: Uri) {
        if (_isLoading.value || originalBitmap != null) return
        sourceUri = imageUri
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _isLoading.value = true
            val bitmap = ImageEditorUtils.decodeSampledBitmapFromUri(context, imageUri)
            originalBitmap = bitmap
            _previewBitmap.value = bitmap
            _isLoading.value = false
        }
    }

    fun rotateLeft() {
        _rotationDegrees.value = (_rotationDegrees.value - 90f) % 360f
        updatePreview()
    }

    fun rotateRight() {
        _rotationDegrees.value = (_rotationDegrees.value + 90f) % 360f
        updatePreview()
    }

    fun updateBrightness(value: Float) {
        _brightness.value = value
        updatePreview()
    }

    fun updateContrast(value: Float) {
        _contrast.value = value
        updatePreview()
    }

    fun updateSaturation(value: Float) {
        _saturation.value = value
        updatePreview()
    }

    fun reset() {
        previewJob?.cancel()
        _rotationDegrees.value = 0f
        _brightness.value = 0f
        _contrast.value = 1f
        _saturation.value = 1f

        val oldTransformed = currentPreviewBitmap
        currentPreviewBitmap = null
        _previewBitmap.value = originalBitmap

        if (oldTransformed != null && oldTransformed != originalBitmap && !oldTransformed.isRecycled) {
            oldTransformed.recycle()
        }
    }

    private fun updatePreview() {
        val base = originalBitmap ?: return
        previewJob?.cancel()
        previewJob = viewModelScope.launch {
            val plan = TransformationPlan(
                rotationDegrees = _rotationDegrees.value,
                brightness = _brightness.value,
                contrast = _contrast.value,
                saturation = _saturation.value
            )

            val updated = ImageEditorUtils.applyTransformationPlan(
                sourceBitmap = base,
                plan = plan
            )

            val previous = currentPreviewBitmap
            currentPreviewBitmap = updated
            _previewBitmap.value = updated

            if (previous != null && previous != base && previous != updated && !previous.isRecycled) {
                previous.recycle()
            }
        }
    }

    fun save(context: Context) {
        val uri = sourceUri ?: return
        val plan = TransformationPlan(
            rotationDegrees = _rotationDegrees.value,
            brightness = _brightness.value,
            contrast = _contrast.value,
            saturation = _saturation.value
        )

        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            val (savedUri, dimensions) = ImageEditorUtils.exportEditedImageWithProgressiveFallback(
                context = context,
                uri = uri,
                plan = plan
            )

            if (savedUri != null) {
                _saveState.value = SaveState.Success(savedUri, dimensions)
            } else {
                _saveState.value = SaveState.Error(R.string.failed_to_save_image)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        previewJob?.cancel()
        loadJob?.cancel()

        if (currentPreviewBitmap != null && currentPreviewBitmap != originalBitmap && currentPreviewBitmap?.isRecycled == false) {
            currentPreviewBitmap?.recycle()
        }
        if (originalBitmap != null && originalBitmap?.isRecycled == false) {
            originalBitmap?.recycle()
        }
        originalBitmap = null
        currentPreviewBitmap = null
        _previewBitmap.value = null
    }
}
