package com.example.advancedgallery.ui.screens.viewer

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import com.example.advancedgallery.data.local.MediaEntity
import com.example.advancedgallery.data.model.MediaItem
import com.example.advancedgallery.data.repository.MediaRepository
import com.example.advancedgallery.fakes.FakeMediaDao
import com.example.advancedgallery.rules.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class ViewerViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeDao: FakeMediaDao
    private lateinit var repository: MediaRepository
    private val contentResolver: ContentResolver = mock(ContentResolver::class.java)
    private val mockUri: Uri = mock(Uri::class.java)

    @Before
    fun setUp() {
        `when`(mockUri.toString()).thenReturn("content://media/external/images/media/10")
        fakeDao = FakeMediaDao()
        repository = MediaRepository(contentResolver, fakeDao, mainDispatcherRule.testDispatcher)
    }

    @Test
    fun toggleFavorite_invokesRepositoryToggle() = runTest {
        val viewModel = ViewerViewModel(repository, SavedStateHandle())
        val item = MediaItem(
            mediaStoreId = 10L,
            uri = mockUri,
            name = "pic.png",
            dateAdded = 500L,
            mimeType = "image/png",
            bucketId = 1L,
            bucketName = "DCIM",
            isVideo = false,
            isFavorite = false
        )

        viewModel.toggleFavorite(item)

        val favorites = fakeDao.getFavorites().first()
        assertEquals(1, favorites.size)
        assertEquals("content://media/external/images/media/10", favorites[0].uri)
    }

    @Test
    fun removeDeletedItem_clearsFromFavorites() = runTest {
        fakeDao.insert(MediaEntity("content://media/external/images/media/10", true, 500L))
        val viewModel = ViewerViewModel(repository, SavedStateHandle())

        viewModel.removeDeletedItem("content://media/external/images/media/10")

        val favorites = fakeDao.getFavorites().first()
        assertEquals(0, favorites.size)
    }
}
