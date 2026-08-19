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
    private val mockUri: Uri = mock(Uri::class.java)

    @Before
    fun setUp() {
        fakeDao = FakeMediaDao()
        repository = MediaRepository(contentResolver, fakeDao, mainDispatcherRule.testDispatcher)
    }

    @Test
    fun toggleFavorite_addsAndRemovesFavorite() = runTest {
        val testItem = MediaItem(
            id = 100L,
            uri = mockUri,
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
        assertEquals(100L, favorites[0].id)
        assertTrue(favorites[0].isFavorite)

        // Toggle to false
        val favoritedItem = testItem.copy(isFavorite = true)
        repository.toggleFavorite(favoritedItem)
        favorites = fakeDao.getFavorites().first()
        assertTrue(favorites.isEmpty())
    }

    @Test
    fun removeDeletedItems_removesFromFavorites() = runTest {
        fakeDao.insert(MediaEntity(1L, "uri1", true, 100L))
        fakeDao.insert(MediaEntity(2L, "uri2", true, 200L))

        assertEquals(2, fakeDao.getFavorites().first().size)

        repository.removeDeletedItems(listOf(1L))

        val remaining = fakeDao.getFavorites().first()
        assertEquals(1, remaining.size)
        assertEquals(2L, remaining[0].id)
    }
}
