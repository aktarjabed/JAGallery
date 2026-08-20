package com.example.advancedgallery

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.advancedgallery.data.model.MediaLoadResult
import com.example.advancedgallery.util.MediaStoreHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MediaStoreOperationsTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val resolver = context.contentResolver
    private val createdUris = mutableListOf<Uri>()

    @Before
    fun setUp() {
        createdUris.clear()
    }

    @After
    fun tearDown() {
        for (uri in createdUris) {
            try {
                resolver.delete(uri, null, null)
            } catch (e: Exception) {
                // Cleanup
            }
        }
        createdUris.clear()
    }

    @Test
    fun insertImageWithIsPending_and_queryViaMediaStoreHelper() = runBlocking {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "test_instrumented_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        assertNotNull(uri)
        uri?.let { createdUris.add(it) }

        // Write test data
        uri?.let {
            resolver.openOutputStream(it)?.use { out ->
                out.write(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()))
            }
        }

        // Publish item by setting IS_PENDING = 0
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && uri != null) {
            val publishValues = ContentValues().apply {
                put(MediaStore.Images.Media.IS_PENDING, 0)
            }
            val updated = resolver.update(uri, publishValues, null, null)
            assertEquals(1, updated)
        }

        // Query media using MediaStoreHelper
        val result = MediaStoreHelper.getMediaItemsResult(resolver, Dispatchers.IO, context)
        assertTrue(result is MediaLoadResult.Success || result is MediaLoadResult.Empty)
        if (result is MediaLoadResult.Success) {
            val found = result.items.any { it.uri.toString() == uri.toString() }
            assertTrue("Inserted test image should be found in MediaStore result", found)
        }
    }

    @Test
    fun multiVolume_getExternalVolumeNames_returnsValidVolumes() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val volumes = MediaStore.getExternalVolumeNames(context)
            assertNotNull(volumes)
            assertTrue("System must report at least 1 external volume", volumes.isNotEmpty())
        }
    }
}
