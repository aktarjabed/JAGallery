package com.aktarjabed.jagallery.util

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
import org.robolectric.annotation.Config
import android.os.Build

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
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
        assertTrue(result.isEmpty())
    }

    @Test
    fun createDeleteRequests_returnsEmptyList_onEmptyUris() {
        val result = FileUtils.createDeleteRequests(contentResolver, emptyList())
        assertTrue(result.isEmpty())
    }
}
