package com.example.advancedgallery

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.advancedgallery.ui.common.components.PermissionAccessMode
import com.example.advancedgallery.ui.common.components.checkGalleryPermissionAccessMode
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PermissionModelInstrumentationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun checkGalleryPermissionAccessMode_returnsValidPermissionAccessMode() {
        val mode = checkGalleryPermissionAccessMode(context)
        assertNotNull(mode)
        // Validates that checkGalleryPermissionAccessMode evaluates without throwing exceptions
        val isValidMode = mode in PermissionAccessMode.values()
        org.junit.Assert.assertTrue("PermissionAccessMode must be a valid enum constant", isValidMode)
    }
}
