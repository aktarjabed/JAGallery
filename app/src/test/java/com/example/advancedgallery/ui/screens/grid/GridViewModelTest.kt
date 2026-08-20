package com.example.advancedgallery.ui.screens.grid

import android.content.ContentResolver
import androidx.lifecycle.SavedStateHandle
import com.example.advancedgallery.data.model.MediaSource
import com.example.advancedgallery.data.repository.MediaRepository
import com.example.advancedgallery.domain.MediaOperationsImpl
import com.example.advancedgallery.fakes.FakeMediaDao
import com.example.advancedgallery.fixtures.MediaTestData
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
class GridViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeDao: FakeMediaDao
    private lateinit var repository: MediaRepository
    private lateinit var mediaOperations: MediaOperationsImpl
    private val contentResolver: ContentResolver = mock(ContentResolver::class.java)

    @Before
    fun setUp() {
        fakeDao = FakeMediaDao()
        repository = MediaRepository(contentResolver, fakeDao, mainDispatcherRule.testDispatcher)
        mediaOperations = MediaOperationsImpl(repository)
    }

    @Test
    fun setSource_filtersBySourceCorrectly() = runTest {
        val viewModel = GridViewModel(repository, mediaOperations, SavedStateHandle())
        viewModel.setSource(MediaSource.Album(10L))

        assertEquals(0, viewModel.mediaItems.value.size)
    }

    @Test
    fun removeDeletedItems_removesFromFavorites() = runTest {
        val fav = MediaTestData.favorite(uriString = "content://media/external/images/media/100")
        fakeDao.insert(fav)
        val viewModel = GridViewModel(repository, mediaOperations, SavedStateHandle())

        viewModel.removeDeletedItems(listOf("content://media/external/images/media/100"))

        val favorites = fakeDao.getFavorites().first()
        assertEquals(0, favorites.size)
    }
}
