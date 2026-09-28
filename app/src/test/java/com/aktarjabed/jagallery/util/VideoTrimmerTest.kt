package com.aktarjabed.jagallery.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class VideoTrimmerTest {

    private val context = mock(Context::class.java)
    private val contentResolver = mock(ContentResolver::class.java)
    private val sourceUri = Uri.parse("content://media/external/video/media/1")

    @Before
    fun setup() {
        `when`(context.contentResolver).thenReturn(contentResolver)
    }

    @Test
    fun trimVideo_negativeStart_returnsNull() = runTest {
        val result = VideoTrimmer.trimVideo(context, sourceUri, -100L, 1000L)
        assertNull(result)
    }

    @Test
    fun trimVideo_zeroLengthRange_returnsNull() = runTest {
        val result = VideoTrimmer.trimVideo(context, sourceUri, 1000L, 1000L)
        assertNull(result)
    }

    @Test
    fun trimVideo_reversedRange_returnsNull() = runTest {
        val result = VideoTrimmer.trimVideo(context, sourceUri, 2000L, 1000L)
        assertNull(result)
    }
}
