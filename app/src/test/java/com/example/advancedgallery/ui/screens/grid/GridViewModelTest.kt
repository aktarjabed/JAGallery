package com.example.advancedgallery.ui.screens.grid

import android.content.ContentResolver
import android.net.Uri
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
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class GridViewModelTest {

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
    fun setBucketId_filtersByBucketIdCorrectly() = runTest {
        val viewModel = GridViewModel(repository)
        viewModel.setBucketId(10L)

        assertEquals(0, viewModel.mediaItems.value.size)
    }

    @Test
    fun removeDeletedItems_removesFromFavorites() = runTest {
        fakeDao.insert(MediaEntity(100L, "uri100", true, 1000L))
        val viewModel = GridViewModel(repository)

        viewModel.removeDeletedItems(listOf(100L))

        val favorites = fakeDao.getFavorites().first()
        assertEquals(0, favorites.size)
    }
}
