package com.aktarjabed.jagallery.util

import android.content.ContentResolver
import android.database.MatrixCursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.aktarjabed.jagallery.data.model.MediaLoadResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class MediaStoreHelperTest {

    private val contentResolver = mock(ContentResolver::class.java)

    @Test
    fun getMediaItemsResult_missingIdColumn_returnsError() = runTest {
        // Create cursor without _ID
        val cursor = MatrixCursor(arrayOf(MediaStore.MediaColumns.DISPLAY_NAME))
        cursor.addRow(arrayOf("test.jpg"))

        `when`(contentResolver.query(any(Uri::class.java), any(), any(), any(), any())).thenReturn(cursor)
        `when`(contentResolver.query(any(Uri::class.java), any(), any<android.os.Bundle>(), any())).thenReturn(cursor)

        val result = MediaStoreHelper.getMediaItemsResult(contentResolver, Dispatchers.Unconfined)

        assertTrue(result is MediaLoadResult.Error)
        val error = (result as MediaLoadResult.Error).cause
        assertTrue(error is IllegalArgumentException)
        assertEquals("Missing _ID column in MediaStore result", error.message)
    }

    @Test
    fun getMediaItemsResult_nullCursor_returnsError() = runTest {
        `when`(contentResolver.query(any(Uri::class.java), any(), any(), any(), any())).thenReturn(null)
        `when`(contentResolver.query(any(Uri::class.java), any(), any<android.os.Bundle>(), any())).thenReturn(null)

        val result = MediaStoreHelper.getMediaItemsResult(contentResolver, Dispatchers.Unconfined)

        assertTrue(result is MediaLoadResult.Error)
        val error = (result as MediaLoadResult.Error).cause
        assertTrue(error is NullPointerException)
        assertTrue(error.message?.contains("Cursor returned null") == true)
    }

    @Test
    fun getMediaItemsResult_queryException_returnsError() = runTest {
        `when`(contentResolver.query(any(Uri::class.java), any(), any(), any(), any())).thenThrow(SecurityException("Permission denied"))
        `when`(contentResolver.query(any(Uri::class.java), any(), any<android.os.Bundle>(), any())).thenThrow(SecurityException("Permission denied"))

        val result = MediaStoreHelper.getMediaItemsResult(contentResolver, Dispatchers.Unconfined)

        assertTrue(result is MediaLoadResult.Error)
        val error = (result as MediaLoadResult.Error).cause
        assertTrue(error is SecurityException)
    }

    @Test
    fun getMediaItemsResult_emptyCursor_returnsEmpty() = runTest {
        val cursor = MatrixCursor(arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME))
        // No rows added

        `when`(contentResolver.query(any(Uri::class.java), any(), any(), any(), any())).thenReturn(cursor)
        `when`(contentResolver.query(any(Uri::class.java), any(), any<android.os.Bundle>(), any())).thenReturn(cursor)

        val result = MediaStoreHelper.getMediaItemsResult(contentResolver, Dispatchers.Unconfined)

        assertTrue(result is MediaLoadResult.Empty)
    }
}
