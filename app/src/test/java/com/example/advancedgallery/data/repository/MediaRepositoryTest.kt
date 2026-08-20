package com.example.advancedgallery.data.repository

import android.content.ContentResolver
import android.net.Uri
import com.example.advancedgallery.data.local.MediaEntity
import com.example.advancedgallery.data.model.MediaItem
import com.example.advancedgallery.fakes.FakeMediaDao
import com.example.advancedgallery.fixtures.MediaTestData
import com.example.advancedgallery.rules.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
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
        `when`(mockImageUri.toString()).thenReturn("content://media/external/images/media/100")
        `when`(mockVideoUri.toString()).thenReturn("content://media/external/video/media/123")
        fakeDao = FakeMediaDao()
        repository = MediaRepository(contentResolver, fakeDao, mainDispatcherRule.testDispatcher)
    }

    @Test
    fun favoriteMedia_and_unfavoriteMedia_operateExplicitly() = runTest {
        val item = MediaTestData.image(id = 100L, uriString = "content://media/external/images/media/100")

        repository.favoriteMedia(item)
        var favorites = fakeDao.getFavorites().first()
        assertEquals(1, favorites.size)
        assertEquals("content://media/external/images/media/100", favorites[0].uri)

        repository.unfavoriteMedia(item)
        favorites = fakeDao.getFavorites().first()
        assertTrue(favorites.isEmpty())
    }

    @Test
    fun toggleFavorite_addsAndRemovesFavorite() = runTest {
        val testItem = MediaItem(
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
        assertEquals("content://media/external/images/media/100", favorites[0].uri)
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
        assertEquals("content://media/external/video/media/2", remaining[0].uri)
    }

    @Test
    fun idCollision_imageAndVideoWithSameMediaStoreId_areDistinct() = runTest {
        val (imageItem, videoItem) = MediaTestData.collisionFixtures(123L)

        assertNotEquals(imageItem.id, videoItem.id)
        assertEquals("content://media/external/images/media/123", imageItem.id)
        assertEquals("content://media/external/video/media/123", videoItem.id)

        // Toggle favorite on image only
        repository.toggleFavorite(imageItem)
        var favorites = fakeDao.getFavorites().first()
        assertEquals(1, favorites.size)
        assertEquals("content://media/external/images/media/123", favorites[0].uri)

        // Toggle favorite on video as well
        repository.toggleFavorite(videoItem)
        favorites = fakeDao.getFavorites().first()
        assertEquals(2, favorites.size)
    }

    @Test
    fun duplicateUriInsertion_replacesOrIgnoresWithoutDuplication() = runTest {
        val item = MediaTestData.image(id = 50L, uriString = "content://media/external/images/media/50")

        repository.favoriteMedia(item)
        repository.favoriteMedia(item) // Insert duplicate

        val favorites = fakeDao.getFavorites().first()
        assertEquals(1, favorites.size)
        assertEquals("content://media/external/images/media/50", favorites[0].uri)
    }

    @Test
    fun deletionIsolation_deletingImageDoesNotAffectVideoWithSameMediaStoreId() = runTest {
        val (imageItem, videoItem) = MediaTestData.collisionFixtures(999L)

        repository.favoriteMedia(imageItem)
        repository.favoriteMedia(videoItem)

        assertEquals(2, fakeDao.getFavorites().first().size)

        repository.removeDeletedItems(listOf(imageItem.id))

        val remaining = fakeDao.getFavorites().first()
        assertEquals(1, remaining.size)
        assertEquals(videoItem.id, remaining[0].uri)
    }

    @Test
    fun concurrentLoadMedia_coalescesScanJobs() = runTest {
        val job1 = launch { repository.loadMedia(force = false) }
        val job2 = launch { repository.loadMedia(force = true) }

        job1.join()
        job2.join()

        val result = repository.mediaLoadResult.first()
        assertTrue(result is com.example.advancedgallery.data.model.MediaLoadResult)
    }
}
