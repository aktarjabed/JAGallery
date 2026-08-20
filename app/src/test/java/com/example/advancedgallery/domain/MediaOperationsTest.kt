package com.example.advancedgallery.domain

import com.example.advancedgallery.data.repository.MediaRepository
import com.example.advancedgallery.fixtures.MediaTestData
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
    fun favoriteMedia_callsRepositoryFavoriteMedia() = runTest {
        val item = MediaTestData.image(id = 1L)
        val result = mediaOperations.favoriteMedia(item)
        verify(repository).favoriteMedia(item)
        assertTrue(result is OperationResult.Success)
    }

    @Test
    fun unfavoriteMedia_callsRepositoryUnfavoriteMedia() = runTest {
        val item = MediaTestData.image(id = 1L)
        val result = mediaOperations.unfavoriteMedia(item)
        verify(repository).unfavoriteMedia(item)
        assertTrue(result is OperationResult.Success)
    }

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
}
