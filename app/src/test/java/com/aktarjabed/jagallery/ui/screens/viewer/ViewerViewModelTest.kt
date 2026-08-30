package com.aktarjabed.jagallery.ui.screens.viewer

import android.content.ContentResolver
import androidx.lifecycle.SavedStateHandle
import com.aktarjabed.jagallery.data.repository.MediaRepository
import com.aktarjabed.jagallery.domain.MediaOperationsImpl
import com.aktarjabed.jagallery.fakes.FakeMediaDao
import com.aktarjabed.jagallery.fixtures.MediaTestData
import com.aktarjabed.jagallery.rules.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ViewerViewModelTest {

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
    fun toggleFavorite_invokesRepositoryToggle() = runTest {
        val viewModel = ViewerViewModel(mock(android.content.Context::class.java), repository, mediaOperations, SavedStateHandle())
        val item = MediaTestData.image(id = 10L, uriString = "content://media/external/images/media/10")

        viewModel.toggleFavorite(item)

        val favorites = fakeDao.getFavorites().first()
        assertEquals(1, favorites.size)
        assertEquals("content://media/external/images/media/10", favorites[0].uri)
    }

    @Test
    fun removeDeletedItem_clearsFromFavorites() = runTest {
        fakeDao.insert(MediaTestData.favorite(uriString = "content://media/external/images/media/10"))
        val viewModel = ViewerViewModel(mock(android.content.Context::class.java), repository, mediaOperations, SavedStateHandle())

        viewModel.removeDeletedItem("content://media/external/images/media/10")

        val favorites = fakeDao.getFavorites().first()
        assertEquals(0, favorites.size)
    }

    @Test
    fun state_initialStateIsLoading() = runTest {
        val viewModel = ViewerViewModel(mock(android.content.Context::class.java), repository, mediaOperations, SavedStateHandle())
        assertEquals(ViewerState.Loading, viewModel.state.value)
    }

    @Test
    fun invalidRoute_emitsPopBackNavigationEvent() = runTest {
        val savedStateHandle = SavedStateHandle(mapOf("source" to "INVALID_SOURCE"))
        val viewModel = ViewerViewModel(mock(android.content.Context::class.java), repository, mediaOperations, savedStateHandle)

        val navEvent = viewModel.navigationEvent.first()
        assertEquals(ViewerNavigationEvent.PopBack, navEvent)
    }

    @Test
    fun trashSource_callsLoadTrashedMediaWithContext() = runTest {
        val mockContext = mock(android.content.Context::class.java)
        val spyRepository = org.mockito.Mockito.spy(repository)
        val savedStateHandle = SavedStateHandle(mapOf("source" to "TRASH", "mediaId" to "10", "albumName" to "Trash"))
        val viewModel = ViewerViewModel(mockContext, spyRepository, mediaOperations, savedStateHandle)

        org.mockito.Mockito.verify(spyRepository).loadTrashedMedia(mockContext)
    }
}
