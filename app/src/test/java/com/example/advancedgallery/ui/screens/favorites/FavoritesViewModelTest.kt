package com.example.advancedgallery.ui.screens.favorites

import android.content.ContentResolver
import android.net.Uri
import com.example.advancedgallery.data.local.MediaEntity
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
class FavoritesViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeDao: FakeMediaDao
    private lateinit var repository: MediaRepository
    private val contentResolver: ContentResolver = mock(ContentResolver::class.java)

    @Before
    fun setUp() {
        fakeDao = FakeMediaDao()
        repository = MediaRepository(contentResolver, fakeDao, mainDispatcherRule.testDispatcher)
    }

    @Test
    fun favoriteItems_initialStateIsEmpty() = runTest {
        val viewModel = FavoritesViewModel(repository)
        assertEquals(0, viewModel.favoriteItems.value.size)
    }

    @Test
    fun removeDeletedItems_updatesDaoFavorites() = runTest {
        fakeDao.insert(MediaEntity(1L, "uri1", true, 500L))
        fakeDao.insert(MediaEntity(2L, "uri2", true, 600L))

        val viewModel = FavoritesViewModel(repository)
        viewModel.removeDeletedItems(listOf(1L))

        val remaining = fakeDao.getFavorites().first()
        assertEquals(1, remaining.size)
        assertEquals(2L, remaining[0].id)
    }
}
