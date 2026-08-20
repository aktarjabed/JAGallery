package com.example.advancedgallery

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.advancedgallery.data.local.HiddenMediaEntity
import com.example.advancedgallery.data.local.MediaDatabase
import com.example.advancedgallery.data.model.MediaItem
import com.example.advancedgallery.data.model.MediaLoadResult
import com.example.advancedgallery.data.repository.MediaRepository
import com.example.advancedgallery.fixtures.MediaTestData
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
    fun hideMedia_persistsInRoom_andExcludesFromMainLoadResult() = runBlocking {
        val testItem = MediaTestData.image(id = 100L, uriString = "content://media/external/images/media/100")

        // Hide item
        repository.hideMedia(testItem)

        // Verify Room persistence
        val hiddenEntities = db.mediaDao().getHiddenMedia().first()
        assertEquals(1, hiddenEntities.size)
        assertEquals(testItem.id, hiddenEntities[0].uri)

        // Unhide item
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
        val item = MediaTestData.image(id = 300L, uriString = testUri)

        // Hide item
        repository.hideMedia(item)

        val hiddenList = db.mediaDao().getHiddenMedia().first()
        assertEquals(1, hiddenList.size)
        assertFalse("Hidden state in Room must not set IS_TRASHED in MediaStore", item.isTrashed)
    }
}
