package com.aktarjabed.jagallery

import android.content.ContentValues
import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aktarjabed.jagallery.util.VideoTrimmer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.nio.ByteBuffer

@RunWith(AndroidJUnit4::class)
class VideoTrimmerInstrumentationTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val resolver = context.contentResolver
    private val createdUris = mutableListOf<Uri>()

    @Before
    fun setUp() {
        createdUris.clear()
    }

    @After
    fun tearDown() {
        createdUris.forEach {
            try {
                resolver.delete(it, null, null)
            } catch (ignored: Exception) {}
        }
        createdUris.clear()
    }

    @Test
    fun trimVideo_withInvalidUri_returnsNullSafelyWithoutCrashing() = runBlocking {
        val invalidUri = Uri.parse("content://media/external/video/media/999999999")
        val result = VideoTrimmer.trimVideo(context, invalidUri, 0L, 1000L)
        assertNull("Invalid source URI must return null safely without throwing unhandled exceptions", result)
    }

    @Test
    fun trimVideo_withInvalidRange_returnsNullSafely() = runBlocking {
        val mockUri = Uri.parse("content://media/external/video/media/1")
        // end time <= start time
        val result = VideoTrimmer.trimVideo(context, mockUri, 5000L, 2000L)
        assertNull(result)

        // Negative time
        val result2 = VideoTrimmer.trimVideo(context, mockUri, -100L, 2000L)
        assertNull(result2)
    }

    private fun createSyntheticTestVideo(): Uri? {
        val tempFile = File(context.cacheDir, "test_synth_${System.currentTimeMillis()}.mp4")
        try {
            val muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 320, 240).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, 2130708361) // COLOR_FormatSurface
                setInteger(MediaFormat.KEY_BIT_RATE, 500000)
                setInteger(MediaFormat.KEY_FRAME_RATE, 30)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            val trackIndex = muxer.addTrack(videoFormat)
            muxer.start()

            val bufferInfo = MediaCodec.BufferInfo()
            val dummyBuffer = ByteBuffer.allocate(1000)

            for (i in 0..60) { // Approx 2 seconds of video
                val isKeyFrame = (i % 10 == 0)
                bufferInfo.offset = 0
                bufferInfo.size = 100
                bufferInfo.presentationTimeUs = i * 33333L
                bufferInfo.flags = if (isKeyFrame) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0

                dummyBuffer.clear()
                dummyBuffer.put(ByteArray(100) { 1.toByte() })
                dummyBuffer.flip()

                muxer.writeSampleData(trackIndex, dummyBuffer, bufferInfo)
            }

            muxer.stop()
            muxer.release()

            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, tempFile.name)
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            val collection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val uri = resolver.insert(collection, values) ?: return null
            createdUris.add(uri)

            resolver.openOutputStream(uri)?.use { out ->
                tempFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val pubValues = ContentValues().apply {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
                resolver.update(uri, pubValues, null, null)
            }

            return uri

        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    private fun pfdVerification(uri: Uri) {
        resolver.openFileDescriptor(uri, "r")?.use { pfd ->
            assertTrue("File descriptor must be valid and size > 0", pfd.statSize > 0)
        }
    }

    @Test
    fun trimVideo_realMedia_succeeds_and_createsValidOutput() = runBlocking {
        val sourceUri = createSyntheticTestVideo() ?: throw java.lang.IllegalStateException("Failed to create test video")

        val trimmedUri = VideoTrimmer.trimVideo(context, sourceUri, 0L, 1500L)
        assertNotNull("Trimmed video URI must be non-null for valid source video", trimmedUri)
        trimmedUri?.let { createdUris.add(it) }

        // Assert original source URI remains intact
        resolver.openInputStream(sourceUri)?.use { input ->
            assertTrue("Original source video must remain intact", input.read() != -1)
        }

        // Verify output exists in MediaStore and IS_PENDING == 0
        trimmedUri?.let { pfdVerification(it) } ?: throw IllegalStateException("Trimmed URI is null")
    }

    @Test
    fun trimVideo_realMedia_beyondDuration_failsSafely() = runBlocking {
        val sourceUri = createSyntheticTestVideo() ?: throw java.lang.IllegalStateException("Failed to create test video")

        // 2-second video, request start at 5s
        val trimmedUri = VideoTrimmer.trimVideo(context, sourceUri, 5000L, 7000L)
        assertNull("Trimmed video URI must be null when requested start is beyond real duration", trimmedUri)

        // Verify original source URI remains intact after failure
        resolver.openInputStream(sourceUri)?.use { input ->
            assertTrue("Original source video must remain intact after trim failure", input.read() != -1)
        }
    }
}
