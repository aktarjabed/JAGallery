package com.example.advancedgallery.data.repository

import android.content.ContentResolver
import android.net.Uri
import com.example.advancedgallery.data.local.MediaEntity
import com.example.advancedgallery.data.model.MediaItem
import com.example.advancedgallery.fakes.FakeMediaDao
import com.example.advancedgallery.rules.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class MediaRepositoryTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeDao: FakeMediaDao
    private lateinit var repository: MediaRepository
    private val contentResolver: ContentResolver = mock(ContentResolver::class.java)
    private val mockImageUri: Uri = mock(Uri::class.java)
    private val mockVideoUri: Uri = mock(Uri::class.java)

    @Before
    fun setUp() {
        fakeDao = FakeMediaDao()
        repository = MediaRepository(contentResolver, fakeDao, mainDispatcherRule.testDispatcher)
    }

    @Test
    fun toggleFavorite_addsAndRemovesFavorite() = runTest {
        val testItem = MediaItem(
            id = "content://media/external/images/media/100",
            mediaStoreId = 100L,
            uri = mockImageUri,
            name = "test_image.jpg",
            dateAdded = 1000L,
            mimeType = "image/jpeg",
            bucketId = 1L,
            bucketName = "Camera",
            isVideo = false,
            isFavorite = false
        )

        // Toggle to true
        repository.toggleFavorite(testItem)
        var favorites = fakeDao.getFavorites().first()
        assertEquals(1, favorites.size)
        assertEquals("content://media/external/images/media/100", favorites[0].id)
        assertTrue(favorites[0].isFavorite)

        // Toggle to false
        val favoritedItem = testItem.copy(isFavorite = true)
        repository.toggleFavorite(favoritedItem)
        favorites = fakeDao.getFavorites().first()
        assertTrue(favorites.isEmpty())
    }

    @Test
    fun removeDeletedItems_removesFromFavorites() = runTest {
        fakeDao.insert(MediaEntity("content://media/external/images/media/1", true, 100L))
        fakeDao.insert(MediaEntity("content://media/external/video/media/2", true, 200L))

        assertEquals(2, fakeDao.getFavorites().first().size)

        repository.removeDeletedItems(listOf("content://media/external/images/media/1"))

        val remaining = fakeDao.getFavorites().first()
        assertEquals(1, remaining.size)
        assertEquals("content://media/external/video/media/2", remaining[0].id)
    }

    @Test
    fun idCollision_imageAndVideoWithSameMediaStoreId_areDistinct() = runTest {
        val imageItem = MediaItem(
            id = "content://media/external/images/media/123",
            mediaStoreId = 123L,
            uri = mockImageUri,
            name = "photo.jpg",
            dateAdded = 1000L,
            mimeType = "image/jpeg",
            bucketId = 10L,
            bucketName = "Camera",
            isVideo = false
        )

        val videoItem = MediaItem(
            id = "content://media/external/video/media/123",
            mediaStoreId = 123L,
            uri = mockVideoUri,
            name = "video.mp4",
            dateAdded = 2000L,
            mimeType = "video/mp4",
            bucketId = 10L,
            bucketName = "Camera",
            isVideo = true
        )

        assertNotEquals(imageItem.id, videoItem.id)

        // Toggle favorite on image only
        repository.toggleFavorite(imageItem)
        var favorites = fakeDao.getFavorites().first()
        assertEquals(1, favorites.size)
        assertEquals("content://media/external/images/media/123", favorites[0].id)

        // Toggle favorite on video as well
        repository.toggleFavorite(videoItem)
        favorites = fakeDao.getFavorites().first()
        assertEquals(2, favorites.size)
    }
}
