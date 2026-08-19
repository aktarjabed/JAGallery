package com.example.advancedgallery.ui.screens.viewer

import android.content.ContentResolver
import android.net.Uri
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
        fakeDao = FakeMediaDao()
        repository = MediaRepository(contentResolver, fakeDao, mainDispatcherRule.testDispatcher)
    }

    @Test
    fun toggleFavorite_invokesRepositoryToggle() = runTest {
        val viewModel = ViewerViewModel(repository)
        val item = MediaItem(
            id = 10L,
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
        assertEquals(10L, favorites[0].id)
    }
}
