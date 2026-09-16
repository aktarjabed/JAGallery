package com.aktarjabed.jagallery.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.aktarjabed.jagallery.data.model.MediaItem
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.io.ByteArrayInputStream

class DuplicateDetectorTest {

    @Mock
    private lateinit var mockContext: Context

    @Mock
    private lateinit var mockContentResolver: ContentResolver

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        `when`(mockContext.contentResolver).thenReturn(mockContentResolver)
    }

    private fun createMockMediaItem(
        id: String,
        size: Long,
        dateAdded: Long = 0
    ): MediaItem {
        val uri = mock(Uri::class.java)
        `when`(uri.toString()).thenReturn(id)

        return MediaItem(
            uri = uri,
            mediaStoreId = id.hashCode().toLong(),
            name = "name_$id",
            dateAdded = dateAdded,
            mimeType = "image/jpeg",
            bucketId = 1L,
            bucketName = "Camera",
            isVideo = false,
            size = size
        )
    }

    private fun mockInputStreamForUri(uri: Uri, content: String) {
        val bytes = content.toByteArray()
        val inputStream = ByteArrayInputStream(bytes)
        `when`(mockContentResolver.openInputStream(uri)).thenReturn(inputStream)
    }

    private fun mockInputStreamErrorForUri(uri: Uri, exception: Exception) {
        `when`(mockContentResolver.openInputStream(uri)).thenThrow(exception)
    }

    private fun mockInputStreamNullForUri(uri: Uri) {
        `when`(mockContentResolver.openInputStream(uri)).thenReturn(null)
    }

    @Test
    fun `findDuplicates - identical content, same size - detected as duplicates`() = runTest {
        val size = 100L
        val item1 = createMockMediaItem("1", size, dateAdded = 2000L)
        val item2 = createMockMediaItem("2", size, dateAdded = 1000L)

        mockInputStreamForUri(item1.uri, "identical content")
        mockInputStreamForUri(item2.uri, "identical content")

        val result = DuplicateDetector.findDuplicates(mockContext, listOf(item1, item2))

        assertEquals(1, result.size)
        val group = result[0]
        assertEquals(size, group.size)
        assertEquals(2, group.items.size)
        // Check sorting by dateAdded descending
        assertEquals(item1, group.items[0])
        assertEquals(item2, group.items[1])
    }

    @Test
    fun `findDuplicates - different content, same size - not detected as duplicates`() = runTest {
        val size = 100L
        val item1 = createMockMediaItem("1", size)
        val item2 = createMockMediaItem("2", size)

        mockInputStreamForUri(item1.uri, "content A")
        mockInputStreamForUri(item2.uri, "content B")

        val result = DuplicateDetector.findDuplicates(mockContext, listOf(item1, item2))

        assertTrue(result.isEmpty())
    }

    @Test
    fun `findDuplicates - different sizes - not hashed and not detected`() = runTest {
        val item1 = createMockMediaItem("1", size = 100L)
        val item2 = createMockMediaItem("2", size = 200L)

        val result = DuplicateDetector.findDuplicates(mockContext, listOf(item1, item2))

        assertTrue(result.isEmpty())
        verify(mockContentResolver, never()).openInputStream(any())
    }

    @Test
    fun `findDuplicates - zero size items - ignored`() = runTest {
        val item1 = createMockMediaItem("1", size = 0L)
        val item2 = createMockMediaItem("2", size = 0L)

        val result = DuplicateDetector.findDuplicates(mockContext, listOf(item1, item2))

        assertTrue(result.isEmpty())
        verify(mockContentResolver, never()).openInputStream(any())
    }

    private suspend fun testExceptionHandling(exception: Exception) {
        val size = 100L
        val item1 = createMockMediaItem("1", size)
        val item2 = createMockMediaItem("2", size)
        val item3 = createMockMediaItem("3", size)

        mockInputStreamErrorForUri(item1.uri, exception)
        mockInputStreamForUri(item2.uri, "content")
        mockInputStreamForUri(item3.uri, "content")

        val result = DuplicateDetector.findDuplicates(mockContext, listOf(item1, item2, item3))

        // item1 fails and is skipped. item2 and item3 are duplicates
        assertEquals(1, result.size)
        assertEquals(2, result[0].items.size)
        assertTrue(result[0].items.contains(item2))
        assertTrue(result[0].items.contains(item3))
    }

    @Test
    fun `findDuplicates - SecurityException handled gracefully`() = runTest {
        testExceptionHandling(SecurityException("Partial Android 14 perms"))
    }

    @Test
    fun `findDuplicates - generic Exception handled gracefully`() = runTest {
        testExceptionHandling(RuntimeException("Some generic read error"))
    }

    @Test
    fun `findDuplicates - null InputStream handled gracefully (regression)`() = runTest {
        val size = 100L
        val item1 = createMockMediaItem("1", size)
        val item2 = createMockMediaItem("2", size)
        val item3 = createMockMediaItem("3", size)
        val item4 = createMockMediaItem("4", size)

        // item1 and item2 return null InputStream
        mockInputStreamNullForUri(item1.uri)
        mockInputStreamNullForUri(item2.uri)

        // item3 and item4 are actual duplicates
        mockInputStreamForUri(item3.uri, "content")
        mockInputStreamForUri(item4.uri, "content")

        val result = DuplicateDetector.findDuplicates(mockContext, listOf(item1, item2, item3, item4))

        // If the regression was NOT fixed, item1 and item2 would be hashed as SHA-256(empty) and grouped as duplicates!
        // Since it IS fixed, they are skipped.
        assertEquals(1, result.size)
        assertEquals(2, result[0].items.size)
        assertTrue(result[0].items.contains(item3))
        assertTrue(result[0].items.contains(item4))
    }
}
