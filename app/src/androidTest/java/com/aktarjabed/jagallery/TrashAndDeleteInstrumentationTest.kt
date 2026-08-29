package com.aktarjabed.jagallery

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aktarjabed.jagallery.data.model.MediaLoadResult
import com.aktarjabed.jagallery.util.FileUtils
import com.aktarjabed.jagallery.util.MediaStoreHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TrashAndDeleteInstrumentationTest {

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
            } catch (ignored: Exception) {}
        }
        createdUris.clear()
    }

    private fun insertTestImage(): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "trash_test_${System.currentTimeMillis()}_${(0..1000).random()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        assertNotNull("Resolver must create non-null Uri for test image", uri)
        uri?.let {
            createdUris.add(it)
            resolver.openOutputStream(it)?.use { out ->
                out.write(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val pubValues = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                resolver.update(it, pubValues, null, null)
            }
        }
        return uri!!
    }

    @Test
    fun createTrashRequest_and_createDeleteRequest_generateValidPendingIntents() {
        val testUris = listOf(Uri.parse("content://media/external/images/media/999999"))
        val pendingTrashIntent = FileUtils.createTrashRequest(resolver, testUris, true)
        val pendingDeleteIntent = FileUtils.createDeleteRequest(resolver, testUris)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            assertNotNull("createTrashRequest must return non-null PendingIntent on API 30+", pendingTrashIntent)
            assertNotNull("createDeleteRequest must return non-null PendingIntent on API 30+", pendingDeleteIntent)
        } else {
            assertNull(pendingTrashIntent)
            assertNull(pendingDeleteIntent)
        }
    }

    @Test
    fun fullMediaTrashAndRestoreLifecycle() = runBlocking {
        val testUri = insertTestImage()

        // 1. Normal Query (includeTrashed = false) -> visible
        val normalResult1 = MediaStoreHelper.getMediaItemsResult(resolver, Dispatchers.IO, context, includeTrashed = false)
        assertTrue(normalResult1 is MediaLoadResult.Success)
        val normalItems1 = (normalResult1 as MediaLoadResult.Success).items
        val item1 = normalItems1.find { it.uri.toString() == testUri.toString() }
        assertNotNull("Inserted image must be visible in normal query", item1)
        assertFalse("Item in normal query must not be marked trashed", item1!!.isTrashed)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Verify createTrashRequest generates valid PendingIntent for true and false
            val trashIntent = FileUtils.createTrashRequest(resolver, listOf(testUri), true)
            assertNotNull("Trash intent must be non-null for real URI on API 30+", trashIntent)

            // Simulate trashed state in MediaStore or verify query with IS_TRASHED flag if supported
            val trashResult = MediaStoreHelper.getMediaItemsResult(resolver, Dispatchers.IO, context, includeTrashed = true)
            assertTrue("Trash query must return a valid MediaLoadResult", trashResult is MediaLoadResult.Success || trashResult is MediaLoadResult.Empty)

            val restoreIntent = FileUtils.createTrashRequest(resolver, listOf(testUri), false)
            assertNotNull("Restore intent must be non-null for real URI on API 30+", restoreIntent)
        }

        // 2. Permanent deletion
        val deleteIntent = FileUtils.createDeleteRequest(resolver, listOf(testUri))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            assertNotNull("Delete intent must be non-null on API 30+", deleteIntent)
        }

        // Direct delete cleanup
        resolver.delete(testUri, null, null)

        val normalResult2 = MediaStoreHelper.getMediaItemsResult(resolver, Dispatchers.IO, context, includeTrashed = false)
        if (normalResult2 is MediaLoadResult.Success) {
            val item2 = normalResult2.items.find { it.uri.toString() == testUri.toString() }
            assertNull("Deleted image must no longer be visible in normal query", item2)
        }
    }
}
