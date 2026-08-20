package com.example.advancedgallery.util

import android.content.ContentResolver
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

@RunWith(RobolectricTestRunner::class)
class FileUtilsTest {

    private val contentResolver: ContentResolver = mock(ContentResolver::class.java)
    private val mockUri: Uri = mock(Uri::class.java)

    @Test
    fun deleteMediaItems_returnsTrue_whenRowsDeletedGreaterThanZero() {
        `when`(contentResolver.delete(eq(mockUri), any(), any())).thenReturn(1)

        val success = FileUtils.deleteMediaItems(contentResolver, listOf(mockUri))

        assertTrue(success)
    }

    @Test
    fun deleteMediaItems_returnsFalse_whenZeroRowsDeleted() {
        `when`(contentResolver.delete(eq(mockUri), any(), any())).thenReturn(0)

        val success = FileUtils.deleteMediaItems(contentResolver, listOf(mockUri))

        assertFalse(success)
    }

    @Test
    fun deleteMediaItems_returnsFalse_whenExceptionThrown() {
        `when`(contentResolver.delete(eq(mockUri), any(), any())).thenThrow(RuntimeException("Storage error"))

        val success = FileUtils.deleteMediaItems(contentResolver, listOf(mockUri))

        assertFalse(success)
    }
}
