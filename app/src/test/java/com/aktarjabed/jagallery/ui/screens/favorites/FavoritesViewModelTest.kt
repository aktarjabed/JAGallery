package com.aktarjabed.jagallery.ui.screens.favorites

import android.content.ContentResolver
import com.aktarjabed.jagallery.data.model.MediaLoadResult
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
class FavoritesViewModelTest : BaseMediaViewModelTest() {

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
