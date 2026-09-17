package com.aktarjabed.jagallery.util

import android.content.ContentResolver
import org.junit.Assert.assertEquals
import android.net.Uri
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import android.os.Build
import android.provider.MediaStore
import android.database.MatrixCursor

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class FileUtilsTest {

    private val contentResolver: ContentResolver = mock(ContentResolver::class.java)
    private val mockUri: Uri = mock(Uri::class.java)

    @Test
    fun deleteMediaItems_returnsTrue_whenAllRowsDeleted() {
        val testUri = Uri.parse("content://media/external/images/media/100")
        val mockCollection = Uri.parse("content://media/external/images/media")
        `when`(contentResolver.delete(eq(mockCollection), any(), any())).thenReturn(1)

        val result = FileUtils.deleteMediaItems(contentResolver, listOf(testUri))

        assertTrue(result.isFullySuccessful)
        assertTrue(result.failedUris.isEmpty())
        assertEquals(1, result.successfulUris.size)
    }

    @Test
    fun deleteMediaItems_partialSuccess_whenZeroRowsDeleted_verifiesSurvivors() {
        val testUri = Uri.parse("content://media/external/images/media/100")
        val mockCollection = Uri.parse("content://media/external/images/media")
        `when`(contentResolver.delete(eq(mockCollection), any(), any())).thenReturn(0)

        // Mock the query that checks survivors
        val cursor = android.database.MatrixCursor(arrayOf(MediaStore.MediaColumns._ID))
        cursor.addRow(arrayOf(100L)) // 100 survived
        `when`(contentResolver.query(eq(mockCollection), any(), any(), any(), any())).thenReturn(cursor)

        val result = FileUtils.deleteMediaItems(contentResolver, listOf(testUri))

        assertFalse(result.isFullySuccessful)
        assertEquals(1, result.failedUris.size)
    }

    @Test
    fun deleteMediaItems_handlesExceptionsAndFails() {
        `when`(contentResolver.delete(any(), any(), any())).thenThrow(RuntimeException("Storage error"))

        val result = FileUtils.deleteMediaItems(contentResolver, listOf(mockUri))

        assertFalse(result.isFullySuccessful)
    }

    @Test
    fun untrashMediaItems_returnsTrue_whenRowsUpdatedGreaterThanZero() {
        `when`(contentResolver.update(eq(mockUri), any(), any(), any())).thenReturn(1)

        val success = FileUtils.untrashMediaItems(contentResolver, listOf(mockUri))

        assertTrue(success)
    }

    @Test
    fun untrashMediaItems_returnsFalse_whenZeroRowsUpdated() {
        `when`(contentResolver.update(eq(mockUri), any(), any(), any())).thenReturn(0)

        val success = FileUtils.untrashMediaItems(contentResolver, listOf(mockUri))

        assertFalse(success)
    }

    @Test
    fun untrashMediaItems_returnsFalse_whenExceptionThrown() {
        `when`(contentResolver.update(eq(mockUri), any(), any(), any())).thenThrow(RuntimeException("Storage error"))

        val success = FileUtils.untrashMediaItems(contentResolver, listOf(mockUri))

        assertFalse(success)
    }

    @Test
    fun createTrashRequests_returnsEmptyList_onEmptyUris() {
        val result = FileUtils.createTrashRequests(contentResolver, emptyList(), true)
        assertTrue((result as FileUtils.RequestCreationResult.Success).chunks.isEmpty())
    }

    @Test
    fun createDeleteRequests_returnsEmptyList_onEmptyUris() {
        val result = FileUtils.createDeleteRequests(contentResolver, emptyList())
        assertTrue((result as FileUtils.RequestCreationResult.Success).chunks.isEmpty())
    }
}
