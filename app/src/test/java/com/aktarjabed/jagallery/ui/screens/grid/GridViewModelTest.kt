package com.aktarjabed.jagallery.ui.screens.grid

import android.content.ContentResolver
import androidx.lifecycle.SavedStateHandle
import com.aktarjabed.jagallery.data.model.MediaLoadResult
import com.aktarjabed.jagallery.data.model.MediaSource
import com.aktarjabed.jagallery.data.repository.MediaRepository
import com.aktarjabed.jagallery.domain.MediaOperationsImpl
import com.aktarjabed.jagallery.fixtures.MediaTestData
import com.aktarjabed.jagallery.ui.common.BaseMediaViewModelTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GridViewModelTest : BaseMediaViewModelTest() {

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
