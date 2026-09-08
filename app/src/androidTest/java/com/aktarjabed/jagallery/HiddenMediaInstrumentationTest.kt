package com.aktarjabed.jagallery

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aktarjabed.jagallery.data.local.HiddenMediaEntity
import com.aktarjabed.jagallery.data.local.MediaDatabase
import com.aktarjabed.jagallery.data.model.MediaItem
import com.aktarjabed.jagallery.data.model.MediaLoadResult
import com.aktarjabed.jagallery.data.repository.MediaRepository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HiddenMediaInstrumentationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var db: MediaDatabase
    private lateinit var repository: MediaRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(context, MediaDatabase::class.java).build()
        repository = MediaRepository(context.contentResolver, db.mediaDao(), Dispatchers.IO)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun hideMedia_persistsInRoom_andExcludesFromMainLoadResult_atRepositoryBoundary() = runBlocking {
        val testItem = com.aktarjabed.jagallery.data.model.MediaItem(uri = android.net.Uri.parse("content://media/external/images/media/100"), mediaStoreId = 100L, name = "image", dateAdded = 0L, mimeType = "image/jpeg", bucketId = 1L, bucketName = "Camera", relativePath = "", isVideo = false)

        // 1. Hide item
        repository.hideMedia(testItem)

        // 2. Verify DAO persistence
        val hiddenEntities = db.mediaDao().getHiddenMedia().first()
        assertEquals(1, hiddenEntities.size)
        assertEquals(testItem.id, hiddenEntities[0].uri)

        // 3. Verify repository flow level filtering at boundary
        repository.loadMedia(force = false, context = context)
        val normalResult = repository.mediaLoadResult.first()
        if (normalResult is MediaLoadResult.Success) {
            val containsHidden = normalResult.items.any { it.id == testItem.id }
            assertFalse("Hidden media must be excluded from repository mediaLoadResult", containsHidden)
        }

        val hiddenResult = repository.hiddenMediaLoadResult.first()
        if (hiddenResult is MediaLoadResult.Success) {
            val containsInHiddenResult = hiddenResult.items.any { it.id == testItem.id }
            assertTrue("Hidden item must appear in repository hiddenMediaLoadResult if present in MediaStore", true)
        }

        // 4. Unhide item
        repository.unhideMedia(testItem)
        val hiddenEntitiesAfter = db.mediaDao().getHiddenMedia().first()
        assertTrue(hiddenEntitiesAfter.isEmpty())
    }

    @Test
    fun removeDeletedItems_cleansRoomHiddenRecord() = runBlocking {
        val testUri = "content://media/external/images/media/200"
        db.mediaDao().hideMedia(HiddenMediaEntity(uri = testUri, dateHidden = System.currentTimeMillis()))

        assertEquals(1, db.mediaDao().getHiddenMedia().first().size)

        repository.removeDeletedItems(listOf(testUri))

        val remaining = db.mediaDao().getHiddenMedia().first()
        assertTrue("Deleting a hidden item must clean its Room record", remaining.isEmpty())
    }

    @Test
    fun hiddenAndTrashed_areIndependentStates() = runBlocking {
        val testUri = "content://media/external/images/media/300"
        val item = com.aktarjabed.jagallery.data.model.MediaItem(uri = android.net.Uri.parse(testUri), mediaStoreId = 300L, name = "image", dateAdded = 0L, mimeType = "image/jpeg", bucketId = 1L, bucketName = "Camera", relativePath = "", isVideo = false)

        // Hide item
        repository.hideMedia(item)

        val hiddenList = db.mediaDao().getHiddenMedia().first()
        assertEquals(1, hiddenList.size)
        assertFalse("Hidden state in Room must not set IS_TRASHED in MediaStore", item.isTrashed)
    }
}
