package com.aktarjabed.jagallery.ui.screens.albums

import android.content.ContentResolver
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.aktarjabed.jagallery.data.model.AlbumKey
import com.aktarjabed.jagallery.data.model.MediaLoadResult
import com.aktarjabed.jagallery.data.repository.MediaRepository
import com.aktarjabed.jagallery.fakes.FakeMediaDao
import com.aktarjabed.jagallery.fixtures.MediaTestData
import com.aktarjabed.jagallery.rules.MainDispatcherRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AlbumsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var fakeDao: FakeMediaDao
    private lateinit var repository: MediaRepository
    private val contentResolver: ContentResolver = mock(ContentResolver::class.java)
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        fakeDao = FakeMediaDao()
        repository = MediaRepository(contentResolver, fakeDao, mainDispatcherRule.testDispatcher)
    }

    @Test
    fun albums_initialStateIsEmptyOrLoading() = runTest {
        val viewModel = AlbumsViewModel(repository, mock(), context)
        val state = viewModel.uiState.value
        assertTrue(state is AlbumsUiState.Loading || state is AlbumsUiState.Empty)
    }

    @Test
    fun sameBucketIdAcrossDifferentVolumes_createsDistinctAlbums() = runTest {
        val itemPrimary = MediaTestData.image(
            id = 1L,
            uriString = "content://media/external_primary/images/media/1",
            bucketId = 100L,
            bucketName = "Camera",
            volumeName = "external_primary"
        )
        val itemSd = MediaTestData.image(
            id = 2L,
            uriString = "content://media/1234-5678/images/media/2",
            bucketId = 100L,
            bucketName = "Camera",
            volumeName = "1234-5678"
        )

        // Directly test grouping logic via AlbumKey
        val items = listOf(itemPrimary, itemSd)
        val albumsList = items.groupBy { it.albumKey }

        assertEquals(2, albumsList.size)
        assertTrue(albumsList.containsKey(AlbumKey("external_primary", 100L, "")))
        assertTrue(albumsList.containsKey(AlbumKey("1234-5678", 100L, "")))
    }
}
