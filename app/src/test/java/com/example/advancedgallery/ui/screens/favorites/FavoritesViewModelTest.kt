package com.example.advancedgallery.ui.screens.favorites

import android.content.ContentResolver
import com.example.advancedgallery.data.model.MediaLoadResult
import com.example.advancedgallery.data.repository.MediaRepository
import com.example.advancedgallery.domain.MediaOperationsImpl
import com.example.advancedgallery.fakes.FakeMediaDao
import com.example.advancedgallery.fixtures.MediaTestData
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
class FavoritesViewModelTest {

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
    fun mediaLoadResult_initialStateIsLoadingOrEmpty() = runTest {
        val viewModel = FavoritesViewModel(repository, mediaOperations)
        val result = viewModel.mediaLoadResult.value
        assertTrue(result is MediaLoadResult.Loading || result is MediaLoadResult.Empty)
    }

    @Test
    fun removeDeletedItems_updatesDaoFavorites() = runTest {
        fakeDao.insert(MediaTestData.favorite(uriString = "content://media/external/images/media/1"))
        fakeDao.insert(MediaTestData.favorite(uriString = "content://media/external/images/media/2"))

        val viewModel = FavoritesViewModel(repository, mediaOperations)
        viewModel.removeDeletedItems(listOf("content://media/external/images/media/1"))

        val remaining = fakeDao.getFavorites().first()
        assertEquals(1, remaining.size)
        assertEquals("content://media/external/images/media/2", remaining[0].uri)
    }
}
