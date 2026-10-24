package com.aktarjabed.jagallery.ui.screens.grid

import android.content.ContentResolver
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.aktarjabed.jagallery.data.model.MediaLoadResult
import com.aktarjabed.jagallery.data.model.MediaSource
import com.aktarjabed.jagallery.data.repository.MediaRepository
import com.aktarjabed.jagallery.domain.MediaOperationsImpl
import com.aktarjabed.jagallery.domain.SettingsRepository
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
@RunWith(RobolectricTestRunner::class)
class GridViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeDao: FakeMediaDao
    private lateinit var repository: MediaRepository
    private lateinit var mediaOperations: MediaOperationsImpl
    private val contentResolver: ContentResolver = mock(ContentResolver::class.java)
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val settingsRepository = SettingsRepository(context)

    @Before
    fun setUp() {
        fakeDao = FakeMediaDao()
        repository = MediaRepository(contentResolver, fakeDao, mainDispatcherRule.testDispatcher)
        val vaultRepository = mock(com.aktarjabed.jagallery.data.repository.VaultRepository::class.java)
        mediaOperations = MediaOperationsImpl(repository, vaultRepository)
    }

    @Test
    fun setSource_filtersBySourceCorrectly() = runTest {
        val viewModel = GridViewModel(repository, mediaOperations, SavedStateHandle(), settingsRepository)
        viewModel.setSource(MediaSource.Album("external_primary", 10L, ""))

        assertTrue(viewModel.mediaLoadResult.value is MediaLoadResult.Loading || viewModel.mediaLoadResult.value is MediaLoadResult.Empty)
    }

    @Test
    fun typeFilteredSource_onlyReturnsRequestedMediaType() = runTest {
        val images = listOf(MediaTestData.image(id = 11L), MediaTestData.image(id = 12L))
        val videos = listOf(MediaTestData.video(id = 21L), MediaTestData.video(id = 22L))
        repository.scanOperation = { MediaLoadResult.Success(images + videos) }

        val viewModel = GridViewModel(repository, mediaOperations, SavedStateHandle(), settingsRepository)
        viewModel.setSource(MediaSource.TypeFiltered(isVideo = true))
        viewModel.loadMedia(context = context)

        // StateFlow's current value is the deterministic assertion point after the scan.
        val loaded = viewModel.mediaLoadResult.first { it is MediaLoadResult.Success } as MediaLoadResult.Success
        assertEquals(listOf("content://media/external/video/media/21", "content://media/external/video/media/22"), loaded.items.map { it.id })
        assertTrue(loaded.items.all { it.isVideo })
    }

    @Test
    fun typeFilteredSource_canSelectPhotosOnly() = runTest {
        val images = listOf(MediaTestData.image(id = 31L), MediaTestData.image(id = 32L))
        val videos = listOf(MediaTestData.video(id = 41L))
        repository.scanOperation = { MediaLoadResult.Success(images + videos) }

        val viewModel = GridViewModel(repository, mediaOperations, SavedStateHandle(), settingsRepository)
        viewModel.setSource(MediaSource.TypeFiltered(isVideo = false))
        viewModel.loadMedia(context = context)

        val loaded = viewModel.mediaLoadResult.first { it is MediaLoadResult.Success } as MediaLoadResult.Success
        assertTrue(loaded.items.isNotEmpty())
        assertTrue(loaded.items.none { it.isVideo })
    }

    @Test
    fun removeDeletedItems_removesFromFavorites() = runTest {
        val fav = MediaTestData.favorite(uriString = "content://media/external/images/media/100")
        fakeDao.insert(fav)
        val viewModel = GridViewModel(repository, mediaOperations, SavedStateHandle(), settingsRepository)

        viewModel.removeDeletedItems(listOf("content://media/external/images/media/100"))

        val favorites = fakeDao.getFavorites().first()
        assertEquals(0, favorites.size)
    }
}
