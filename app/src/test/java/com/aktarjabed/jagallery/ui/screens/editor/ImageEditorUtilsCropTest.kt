package com.aktarjabed.jagallery.ui.screens.editor

import android.graphics.Bitmap
import android.graphics.RectF
import com.aktarjabed.jagallery.util.ImageEditorUtils
import com.aktarjabed.jagallery.util.TransformationPlan
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ImageEditorUtilsCropTest {

    @Test
    fun testCrop_center_quarterSize() = runTest {
        val original = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val plan = TransformationPlan(cropRect = RectF(0.25f, 0.25f, 0.75f, 0.75f))

        val cropped = ImageEditorUtils.applyTransformationPlan(original, plan)

        assertEquals(50, cropped.width)
        assertEquals(50, cropped.height)
    }

    @Test
    fun testCrop_fullImage_doesNotRecreate() = runTest {
        val original = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val plan = TransformationPlan(cropRect = RectF(0f, 0f, 1f, 1f))

        val cropped = ImageEditorUtils.applyTransformationPlan(original, plan)

        // Exact same instance
        assertEquals(original, cropped)
    }

    @Test
    fun testCrop_outOfBounds_clampsCorrectly() = runTest {
        val original = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val plan = TransformationPlan(cropRect = RectF(-0.5f, -0.5f, 1.5f, 1.5f))

        val cropped = ImageEditorUtils.applyTransformationPlan(original, plan)

        // Clamps to 0f, 0f, 1f, 1f, hence no recreation
        assertEquals(original, cropped)
    }

    @Test
    fun testCrop_rotation_dimensionsCorrect() = runTest {
        val original = Bitmap.createBitmap(100, 50, Bitmap.Config.ARGB_8888)
        val plan = TransformationPlan(
            rotationDegrees = 90f,
            cropRect = RectF(0.0f, 0.0f, 0.5f, 1.0f) // crop left half (50x50), then rotate 90
        )

        val cropped = ImageEditorUtils.applyTransformationPlan(original, plan)

        assertEquals(50, cropped.width)
        assertEquals(50, cropped.height)
    }
}
