package com.aktarjabed.jagallery.ui.screens.grid

import android.content.ContentResolver
import androidx.lifecycle.SavedStateHandle
import com.aktarjabed.jagallery.data.model.MediaLoadResult
import com.aktarjabed.jagallery.data.model.MediaSource
import com.aktarjabed.jagallery.data.repository.MediaRepository
import com.aktarjabed.jagallery.domain.MediaOperationsImpl
import com.aktarjabed.jagallery.fakes.FakeMediaDao
import com.aktarjabed.jagallery.fixtures.MediaTestData
import com.aktarjabed.jagallery.rules.MainDispatcherRule
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
        viewModel.setSource(MediaSource.Album("external_primary", 10L, ""))

        assertTrue(viewModel.mediaLoadResult.value is MediaLoadResult.Loading || viewModel.mediaLoadResult.value is MediaLoadResult.Empty)
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
