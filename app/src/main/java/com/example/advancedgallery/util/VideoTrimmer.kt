package com.example.advancedgallery.util

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.nio.ByteBuffer

object VideoTrimmer {
    private const val TAG = "VideoTrimmer"

    suspend fun trimVideo(
        context: Context,
        sourceUri: Uri,
        startMs: Long,
        endMs: Long
    ): Uri? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val tempFile = File(context.cacheDir, "trimmed_temp_${System.currentTimeMillis()}.mp4")
        var newUri: Uri? = null

        try {
            val extractor = MediaExtractor()
            val pfd = resolver.openFileDescriptor(sourceUri, "r") ?: return@withContext null
            extractor.setDataSource(pfd.fileDescriptor)

            val muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val trackCount = extractor.trackCount
            val trackMap = HashMap<Int, Int>()

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    val dstIndex = muxer.addTrack(format)
                    trackMap[i] = dstIndex
                }
            }

            muxer.start()

            val startUs = startMs * 1000L
            val endUs = endMs * 1000L
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            val bufferSize = 1024 * 1024
            val buffer = ByteBuffer.allocate(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break

                val presentationTimeUs = extractor.sampleTime
                if (presentationTimeUs > endUs) break

                val trackIndex = extractor.sampleTrackIndex
                if (trackMap.containsKey(trackIndex)) {
                    bufferInfo.presentationTimeUs = presentationTimeUs - startUs
                    bufferInfo.flags = extractor.sampleFlags
                    muxer.writeSampleData(trackMap[trackIndex]!!, buffer, bufferInfo)
                }
                extractor.advance()
            }

            muxer.stop()
            muxer.release()
            extractor.release()
            pfd.close()

            // Insert into MediaStore
            val contentValues = android.content.ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, "TRIM_${System.currentTimeMillis()}.mp4")
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.IS_PENDING, 1)
                }
            }

            newUri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (newUri != null) {
                var success = false
                FileInputStream(tempFile).use { input ->
                    resolver.openOutputStream(newUri)?.use { output ->
                        input.copyTo(output)
                        success = true
                    }
                }

                if (success && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.Video.Media.IS_PENDING, 0)
                    resolver.update(newUri, contentValues, null, null)
                }
            }

            tempFile.delete()
            newUri
        } catch (e: CancellationException) {
            if (tempFile.exists()) tempFile.delete()
            if (newUri != null) try { resolver.delete(newUri, null, null) } catch (ignored: Exception) {}
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to trim video", e)
            if (tempFile.exists()) tempFile.delete()
            if (newUri != null) try { resolver.delete(newUri, null, null) } catch (ignored: Exception) {}
            null
        }
    }
}
