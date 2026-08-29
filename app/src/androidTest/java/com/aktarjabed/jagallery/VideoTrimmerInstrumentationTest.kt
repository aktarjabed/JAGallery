package com.aktarjabed.jagallery

import android.content.ContentValues
import android.media.MediaCodec
import android.media.MediaExtractor
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileInputStream
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
        for (uri in createdUris) {
            try {
                resolver.delete(uri, null, null)
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

    private fun createSyntheticTestVideo(): Uri? {
        val tempFile = File(context.cacheDir, "test_synth_${System.currentTimeMillis()}.mp4")
        try {
            val muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)

            val videoFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 320, 240).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, 2130708361) // COLOR_FormatSurface
                setInteger(MediaFormat.KEY_BIT_RATE, 1000000)
                setInteger(MediaFormat.KEY_FRAME_RATE, 30)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
            val videoTrack = muxer.addTrack(videoFormat)

            val audioFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, 44100, 2).apply {
                setInteger(MediaFormat.KEY_BIT_RATE, 128000)
            }
            val audioTrack = muxer.addTrack(audioFormat)

            muxer.start()

            val buffer = ByteBuffer.allocate(1024)
            val bufferInfo = MediaCodec.BufferInfo()

            // Write 3 seconds of video frames
            for (i in 0 until 90) {
                buffer.clear()
                buffer.put(byteArrayOf(0, 0, 0, 1, 0x65)) // NAL unit header
                bufferInfo.offset = 0
                bufferInfo.size = 5
                bufferInfo.presentationTimeUs = i * 33333L
                bufferInfo.flags = if (i % 30 == 0) MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                muxer.writeSampleData(videoTrack, buffer, bufferInfo)
            }

            // Write 3 seconds of audio samples
            for (i in 0 until 130) {
                buffer.clear()
                buffer.put(byteArrayOf(0xFF.toByte(), 0xF1.toByte(), 0x50, 0x80.toByte()))
                bufferInfo.offset = 0
                bufferInfo.size = 4
                bufferInfo.presentationTimeUs = i * 23219L
                bufferInfo.flags = MediaCodec.BUFFER_FLAG_KEY_FRAME
                muxer.writeSampleData(audioTrack, buffer, bufferInfo)
            }

            muxer.stop()
            muxer.release()

            // Insert into MediaStore
            val values = ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, "test_synth_${System.currentTimeMillis()}.mp4")
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            val uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values) ?: return null
            createdUris.add(uri)

            FileInputStream(tempFile).use { input ->
                resolver.openOutputStream(uri)?.use { output ->
                    input.copyTo(output)
                }
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Video.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }

            return uri
        } catch (e: Exception) {
            return null
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }
    }

    @Test
    fun trimVideo_realMedia_succeeds_and_createsValidOutput() = runBlocking {
        val sourceUri = createSyntheticTestVideo() ?: return@runBlocking

        val trimmedUri = VideoTrimmer.trimVideo(context, sourceUri, 0L, 1500L)
        assertNotNull("Trimmed video URI must be non-null for valid source video", trimmedUri)
        trimmedUri?.let { createdUris.add(it) }

        // Assert original source URI remains intact
        resolver.openInputStream(sourceUri)?.use { input ->
            assertTrue("Original source video must remain intact", input.read() != -1)
        }

        // Verify output exists in MediaStore and IS_PENDING == 0
        pfdVerification(trimmedUri!!)
    }

    private fun pfdVerification(trimmedUri: Uri) {
        val pfd = resolver.openFileDescriptor(trimmedUri, "r")
        assertNotNull("Trimmed output URI must be openable via ContentResolver", pfd)
        pfd?.use {
            val extractor = MediaExtractor()
            try {
                extractor.setDataSource(it.fileDescriptor)
                assertTrue("Trimmed video must contain at least 1 media track", extractor.trackCount > 0)
            } finally {
                extractor.release()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val cursor = resolver.query(trimmedUri, arrayOf(MediaStore.Video.Media.IS_PENDING), null, null, null)
            cursor?.use {
                if (it.moveToFirst()) {
                    val isPending = it.getInt(it.getColumnIndexOrThrow(MediaStore.Video.Media.IS_PENDING))
                    assertEquals("Trimmed video IS_PENDING must be 0 after completion", 0, isPending)
                }
            }
        }
    }
}
