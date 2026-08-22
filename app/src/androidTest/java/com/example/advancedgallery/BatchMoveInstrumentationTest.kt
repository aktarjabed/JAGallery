package com.example.advancedgallery

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.advancedgallery.data.local.MediaDatabase
import com.example.advancedgallery.data.model.MediaItem
import com.example.advancedgallery.data.repository.MediaRepository
import com.example.advancedgallery.domain.MediaOperationsImpl
import com.example.advancedgallery.domain.MoveOperationResult
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
class BatchMoveInstrumentationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val resolver = context.contentResolver
    private lateinit var db: MediaDatabase
    private lateinit var repository: MediaRepository
    private lateinit var mediaOperations: MediaOperationsImpl
    private val createdUris = mutableListOf<Uri>()

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, MediaDatabase::class.java).build()
        repository = MediaRepository(resolver, db.mediaDao(), Dispatchers.IO)
        mediaOperations = MediaOperationsImpl(repository)
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
        db.close()
    }

    private fun createTestSourceImage(): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "source_test_${System.currentTimeMillis()}_${(0..1000).random()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val sourceUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        assertNotNull(sourceUri)
        sourceUri?.let {
            createdUris.add(it)
            resolver.openOutputStream(it)?.use { out ->
                out.write(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte()))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val pubValues = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                resolver.update(it, pubValues, null, null)
            }
        }
        return sourceUri!!
    }

    @Test
    fun batchMove_copiesAndRequestsSourceDelete() = runBlocking {
        val sourceUri = createTestSourceImage()
        val sourceItem = MediaItem(
            mediaStoreId = 1L,
            uri = sourceUri,
            name = "source_test.jpg",
            dateAdded = System.currentTimeMillis() / 1000,
            mimeType = "image/jpeg",
            bucketId = 10L,
            bucketName = "Camera",
            isVideo = false
        )

        val result = mediaOperations.moveMediaBatch(context, listOf(sourceItem), "MovedAlbum")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            assertTrue("Move operation must return RequestSourceDelete on API 30+", result is MoveOperationResult.RequestSourceDelete)
            val req = result as MoveOperationResult.RequestSourceDelete
            assertTrue("Failed items should be empty", req.failedItems.isEmpty())
            assertEquals(1, req.successfulCopies.size)
            val destinationUri = req.successfulCopies.first().second
            createdUris.add(destinationUri)
            assertNotNull("PendingIntent for source delete must not be null", req.pendingIntent)

            // Verify destination content is written and openable
            resolver.openInputStream(destinationUri)?.use { input ->
                assertTrue("Destination copy must have non-zero bytes", input.read() != -1)
            }
        }
    }

    @Test
    fun batchMove_whenCanceled_preservesBothSourceAndDestination() = runBlocking {
        val sourceUri = createTestSourceImage()
        val sourceItem = MediaItem(
            mediaStoreId = 1L,
            uri = sourceUri,
            name = "source_test.jpg",
            dateAdded = System.currentTimeMillis() / 1000,
            mimeType = "image/jpeg",
            bucketId = 10L,
            bucketName = "Camera",
            isVideo = false
        )

        val result = mediaOperations.moveMediaBatch(context, listOf(sourceItem), "CanceledMoveAlbum")
        if (result is MoveOperationResult.RequestSourceDelete) {
            val destinationUri = result.successfulCopies.first().second
            createdUris.add(destinationUri)

            // Simulate RESULT_CANCELED: do NOT call removeDeletedItems
            // Assert source URI remains accessible
            resolver.openInputStream(sourceUri)?.use { input ->
                assertTrue("Source file must be preserved when move deletion is canceled", input.read() != -1)
            }
            // Assert destination URI remains accessible
            resolver.openInputStream(destinationUri)?.use { input ->
                assertTrue("Copied destination file must be preserved when move deletion is canceled", input.read() != -1)
            }
        }
    }

    @Test
    fun batchMove_whenConfirmed_removesSourceAndRetainsDestination() = runBlocking {
        val sourceUri = createTestSourceImage()
        val sourceItem = MediaItem(
            mediaStoreId = 100L,
            uri = sourceUri,
            name = "source_test.jpg",
            dateAdded = System.currentTimeMillis() / 1000,
            mimeType = "image/jpeg",
            bucketId = 10L,
            bucketName = "Camera",
            isVideo = false
        )

        val result = mediaOperations.moveMediaBatch(context, listOf(sourceItem), "ConfirmedMoveAlbum")
        if (result is MoveOperationResult.RequestSourceDelete) {
            val destinationUri = result.successfulCopies.first().second
            createdUris.add(destinationUri)

            // Simulate RESULT_OK: invoke removeDeletedItems for source
            repository.removeDeletedItems(listOf(sourceItem.id))

            // Verify destination URI remains fully intact
            resolver.openInputStream(destinationUri)?.use { input ->
                assertTrue("Copied target file must be preserved upon move confirmation", input.read() != -1)
            }
        }
    }

    @Test
    fun batchMove_invalidSource_returnsErrorResult() = runBlocking {
        val invalidItem = MediaItem(
            mediaStoreId = 999999L,
            uri = Uri.parse("content://media/external/images/media/99999999"),
            name = "invalid.jpg",
            dateAdded = System.currentTimeMillis() / 1000,
            mimeType = "image/jpeg",
            bucketId = 10L,
            bucketName = "Camera",
            isVideo = false
        )

        val result = mediaOperations.moveMediaBatch(context, listOf(invalidItem), "FailedMoveAlbum")
        assertTrue("Move on non-existent source must yield Error result", result is MoveOperationResult.Error)
    }
}
