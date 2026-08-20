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

    @Test
    fun batchMove_copiesAndRequestsSourceDelete() = runBlocking {
        // Insert source image
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "source_test_${System.currentTimeMillis()}.jpg")
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
                out.write(byteArrayOf(0x12, 0x34, 0x56, 0x78))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val pubValues = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                resolver.update(it, pubValues, null, null)
            }
        }

        val sourceItem = MediaItem(
            mediaStoreId = 1L,
            uri = sourceUri!!,
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
            assertEquals(1, req.successfulCopies.size)
            req.successfulCopies.forEach { createdUris.add(it.second) }
            assertNotNull("PendingIntent for source delete must not be null", req.pendingIntent)
        }
    }
}
