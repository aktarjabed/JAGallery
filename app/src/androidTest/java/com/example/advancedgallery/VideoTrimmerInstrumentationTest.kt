package com.example.advancedgallery

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.advancedgallery.util.VideoTrimmer
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoTrimmerInstrumentationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun trimVideo_withInvalidUri_returnsNullSafelyWithoutCrashing() = runBlocking {
        val invalidUri = Uri.parse("content://media/external/video/media/999999999")
        val result = VideoTrimmer.trimVideo(context, invalidUri, 0L, 1000L)
        assertNull("Invalid source URI must return null safely without throwing unhandled exceptions", result)
    }
}
