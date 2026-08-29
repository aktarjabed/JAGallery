package com.aktarjabed.jagallery.domain

import com.aktarjabed.jagallery.data.repository.MediaRepository
import com.aktarjabed.jagallery.fixtures.MediaTestData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@OptIn(ExperimentalCoroutinesApi::class)
class MediaOperationsTest {

    private val repository: MediaRepository = mock(MediaRepository::class.java)
    private val mediaOperations: MediaOperations = MediaOperationsImpl(repository)

    @Test
    fun toggleFavorite_callsRepositoryToggleFavorite() = runTest {
        val item = MediaTestData.image(id = 1L)
        val result = mediaOperations.toggleFavorite(item)
        verify(repository).toggleFavorite(item)
        assertTrue(result is OperationResult.Success)
    }

    @Test
    fun removeDeletedItems_callsRepositoryRemoveDeletedItems_whenListNotEmpty() = runTest {
        val deletedIds = listOf("content://media/external/images/media/1")
        val result = mediaOperations.removeDeletedItems(deletedIds)
        verify(repository).removeDeletedItems(deletedIds)
        assertTrue(result is OperationResult.Success)
    }

    @Test
    fun hideMediaBatch_callsRepositoryHideMediaBatch() = runTest {
        val items = listOf(MediaTestData.image(id = 10L))
        val result = mediaOperations.hideMediaBatch(items)
        verify(repository).hideMediaBatch(items)
        assertTrue(result is OperationResult.Success)
    }

    @Test
    fun unhideMediaBatch_callsRepositoryUnhideMediaBatch() = runTest {
        val items = listOf(MediaTestData.image(id = 10L))
        val result = mediaOperations.unhideMediaBatch(items)
        verify(repository).unhideMediaBatch(items)
        assertTrue(result is OperationResult.Success)
    }
}
