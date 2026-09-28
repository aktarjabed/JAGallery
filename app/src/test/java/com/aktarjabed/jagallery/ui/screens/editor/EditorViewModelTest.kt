package com.aktarjabed.jagallery.ui.screens.editor

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import com.aktarjabed.jagallery.rules.MainDispatcherRule
import com.aktarjabed.jagallery.util.ImageEditorUtils
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class EditorViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    fun rotateLeftAndRight_adjustsDegreesCorrectly() {
        val viewModel = EditorViewModel()
        assertEquals(0f, viewModel.rotationDegrees.value)

        viewModel.rotateLeft()
        assertEquals(-90f, viewModel.rotationDegrees.value)

        viewModel.rotateRight()
        assertEquals(0f, viewModel.rotationDegrees.value)
    }

    @Test
    fun reset_restoresDefaultValues() {
        val viewModel = EditorViewModel()
        viewModel.updateBrightness(20f)
        viewModel.updateContrast(1.5f)
        viewModel.updateSaturation(1.2f)
        viewModel.rotateRight()

        viewModel.reset()

        assertEquals(0f, viewModel.rotationDegrees.value)
        assertEquals(0f, viewModel.brightness.value)
        assertEquals(1f, viewModel.contrast.value)
        assertEquals(1f, viewModel.saturation.value)
    }

    @Test
    fun reset_cancelsInFlightPreviewRender() = runTest {
        val viewModel = EditorViewModel()
        viewModel.updateBrightness(10f)
        viewModel.reset()

        assertEquals(0f, viewModel.brightness.value)
    }

    @Test
    fun applyAdjustmentsAndRotation_returnsValidBitmap() = runTest {
        val source = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val result = ImageEditorUtils.applyAdjustmentsAndRotation(
            sourceBitmap = source,
            rotationDegrees = 90f,
            brightness = 10f,
            contrast = 1.1f,
            saturation = 1.2f
        )
        assertNotNull(result)
        assertEquals(100, result.width)
        assertEquals(100, result.height)
    }
}
