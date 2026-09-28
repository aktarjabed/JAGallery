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
import org.mockito.ArgumentMatchers.nullable
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import androidx.test.core.app.ApplicationProvider
import android.content.Context
import android.os.Bundle
import android.os.CancellationSignal

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R])
class MediaStoreHelperTest {

    private val contentResolver = mock(ContentResolver::class.java)
    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val testVolumeProvider = object : MediaStoreVolumeProvider {
        override fun getExternalVolumeNames(context: Context): Set<String> = setOf("external")
    }

    private fun <T> anyNullable(type: Class<T>): T? = nullable(type)
    private fun anyUri() = any(Uri::class.java)

    @Test
    fun getMediaItemsResult_missingIdColumn_returnsError() = runTest {
        `when`(
            contentResolver.query(
                anyUri(),
                anyNullable(Array<String>::class.java),
                anyNullable(String::class.java),
                anyNullable(Array<String>::class.java),
                anyNullable(String::class.java)
            )
        ).thenAnswer {
            MatrixCursor(arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)).apply {
                addRow(arrayOf("test.jpg"))
            }
        }

        `when`(
            contentResolver.query(
                anyUri(),
                anyNullable(Array<String>::class.java),
                anyNullable(Bundle::class.java),
                anyNullable(CancellationSignal::class.java)
            )
        ).thenAnswer {
            MatrixCursor(arrayOf(MediaStore.MediaColumns.DISPLAY_NAME)).apply {
                addRow(arrayOf("test.jpg"))
            }
        }

        val result = MediaStoreHelper.getMediaItemsResult(
            contentResolver = contentResolver,
            dispatcher = Dispatchers.Unconfined,
            context = context,
            volumeProvider = testVolumeProvider
        )

        assertTrue(result is MediaLoadResult.Error)
        val error = (result as MediaLoadResult.Error).cause
        assertTrue(error is IllegalArgumentException)
        assertEquals("Missing _ID column in MediaStore result", error.message)
    }

    @Test
    fun getMediaItemsResult_nullCursor_returnsError() = runTest {
        `when`(
            contentResolver.query(
                anyUri(),
                anyNullable(Array<String>::class.java),
                anyNullable(String::class.java),
                anyNullable(Array<String>::class.java),
                anyNullable(String::class.java)
            )
        ).thenAnswer { null }

        `when`(
            contentResolver.query(
                anyUri(),
                anyNullable(Array<String>::class.java),
                anyNullable(Bundle::class.java),
                anyNullable(CancellationSignal::class.java)
            )
        ).thenAnswer { null }

        val result = MediaStoreHelper.getMediaItemsResult(
            contentResolver = contentResolver,
            dispatcher = Dispatchers.Unconfined,
            context = context,
            volumeProvider = testVolumeProvider
        )

        assertTrue(result is MediaLoadResult.Error)
        val error = (result as MediaLoadResult.Error).cause
        assertTrue(error is NullPointerException)
        assertTrue(error.message?.contains("Cursor returned null") == true)
    }

    @Test
    fun getMediaItemsResult_queryException_returnsError() = runTest {
        `when`(
            contentResolver.query(
                anyUri(),
                anyNullable(Array<String>::class.java),
                anyNullable(String::class.java),
                anyNullable(Array<String>::class.java),
                anyNullable(String::class.java)
            )
        ).thenAnswer { throw SecurityException("Permission denied") }

        `when`(
            contentResolver.query(
                anyUri(),
                anyNullable(Array<String>::class.java),
                anyNullable(Bundle::class.java),
                anyNullable(CancellationSignal::class.java)
            )
        ).thenAnswer { throw SecurityException("Permission denied") }

        val result = MediaStoreHelper.getMediaItemsResult(
            contentResolver = contentResolver,
            dispatcher = Dispatchers.Unconfined,
            context = context,
            volumeProvider = testVolumeProvider
        )

        assertTrue(result is MediaLoadResult.Error)
        val error = (result as MediaLoadResult.Error).cause
        assertTrue(error is SecurityException)
    }

    @Test
    fun getMediaItemsResult_emptyCursor_returnsEmpty() = runTest {
        `when`(
            contentResolver.query(
                anyUri(),
                anyNullable(Array<String>::class.java),
                anyNullable(String::class.java),
                anyNullable(Array<String>::class.java),
                anyNullable(String::class.java)
            )
        ).thenAnswer {
            MatrixCursor(arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME))
        }

        `when`(
            contentResolver.query(
                anyUri(),
                anyNullable(Array<String>::class.java),
                anyNullable(Bundle::class.java),
                anyNullable(CancellationSignal::class.java)
            )
        ).thenAnswer {
            MatrixCursor(arrayOf(MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME))
        }

        val result = MediaStoreHelper.getMediaItemsResult(
            contentResolver = contentResolver,
            dispatcher = Dispatchers.Unconfined,
            context = context,
            volumeProvider = testVolumeProvider
        )

        assertTrue(result is MediaLoadResult.Empty)
    }

    @Test
    fun getMediaItemsResult_failedVolumeDiscovery_returnsError() = runTest {
        val failingVolumeProvider = object : MediaStoreVolumeProvider {
            override fun getExternalVolumeNames(context: Context): Set<String> {
                throw IllegalStateException("Failed to enumerate volumes")
            }
        }

        val result = MediaStoreHelper.getMediaItemsResult(
            contentResolver = contentResolver,
            dispatcher = Dispatchers.Unconfined,
            context = context,
            volumeProvider = failingVolumeProvider
        )

        assertTrue(result is MediaLoadResult.Error)
        val cause = (result as MediaLoadResult.Error).cause
        assertTrue(cause is IllegalStateException)
        assertEquals("Failed to enumerate volumes", cause.message)
    }
}
