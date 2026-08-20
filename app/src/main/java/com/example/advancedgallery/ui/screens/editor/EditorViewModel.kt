package com.example.advancedgallery.ui.screens.editor

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.advancedgallery.util.ImageEditorUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class SaveState {
    object Idle : SaveState()
    object Saving : SaveState()
    data class Success(val newUri: Uri) : SaveState()
    data class Error(val message: String) : SaveState()
}

@HiltViewModel
class EditorViewModel @Inject constructor() : ViewModel() {

    private var originalBitmap: Bitmap? = null

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
        if (originalBitmap != null) return
        viewModelScope.launch {
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
        _rotationDegrees.value = 0f
        _brightness.value = 0f
        _contrast.value = 1f
        _saturation.value = 1f
        _previewBitmap.value = originalBitmap
    }

    private fun updatePreview() {
        val base = originalBitmap ?: return
        viewModelScope.launch {
            val updated = ImageEditorUtils.applyAdjustmentsAndRotation(
                sourceBitmap = base,
                rotationDegrees = _rotationDegrees.value,
                brightness = _brightness.value,
                contrast = _contrast.value,
                saturation = _saturation.value
            )
            _previewBitmap.value = updated
        }
    }

    fun save(context: Context) {
        val bitmap = _previewBitmap.value ?: return
        viewModelScope.launch {
            _saveState.value = SaveState.Saving
            val savedUri = ImageEditorUtils.saveEditedImage(context, bitmap)
            if (savedUri != null) {
                _saveState.value = SaveState.Success(savedUri)
            } else {
                _saveState.value = SaveState.Error("Failed to save image")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        originalBitmap?.recycle()
        originalBitmap = null
        _previewBitmap.value?.recycle()
        _previewBitmap.value = null
    }
}
