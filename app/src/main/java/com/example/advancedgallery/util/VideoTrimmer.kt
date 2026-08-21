package com.example.advancedgallery.util

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
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
    private const val DEFAULT_BUFFER_SIZE = 1024 * 1024

    suspend fun trimVideo(
        context: Context,
        sourceUri: Uri,
        startMs: Long,
        endMs: Long
    ): Uri? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val tempFile = File(context.cacheDir, "trimmed_temp_${System.currentTimeMillis()}.mp4")
        var newUri: Uri? = null

        var extractor: MediaExtractor? = null
        var pfd: ParcelFileDescriptor? = null
        var muxer: MediaMuxer? = null
        var isMuxerStarted = false

        try {
            extractor = MediaExtractor()
            pfd = resolver.openFileDescriptor(sourceUri, "r") ?: return@withContext null
            extractor.setDataSource(pfd.fileDescriptor)

            muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val trackCount = extractor.trackCount
            val trackMap = HashMap<Int, Int>()
            var maxBufferSize = DEFAULT_BUFFER_SIZE

            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("video/") || mime.startsWith("audio/")) {
                    extractor.selectTrack(i)
                    val dstIndex = muxer.addTrack(format)
                    trackMap[i] = dstIndex
                    if (format.containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
                        val trackMax = format.getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
                        if (trackMax > maxBufferSize) {
                            maxBufferSize = trackMax
                        }
                    }
                }
            }

            if (trackMap.isEmpty()) {
                return@withContext null
            }

            muxer.start()
            isMuxerStarted = true

            val startUs = startMs * 1000L
            val endUs = endMs * 1000L
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)

            val buffer = ByteBuffer.allocate(maxBufferSize)
            val bufferInfo = MediaCodec.BufferInfo()
            var baseTimeUs: Long? = null

            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break

                val presentationTimeUs = extractor.sampleTime
                if (presentationTimeUs > endUs) break

                val trackIndex = extractor.sampleTrackIndex
                val dstTrackIndex = trackMap[trackIndex]
                if (dstTrackIndex != null) {
                    if (baseTimeUs == null) {
                        baseTimeUs = presentationTimeUs
                    }
                    val normalizedUs = maxOf(0L, presentationTimeUs - baseTimeUs)
                    bufferInfo.presentationTimeUs = normalizedUs
                    bufferInfo.flags = extractor.sampleFlags
                    muxer.writeSampleData(dstTrackIndex, buffer, bufferInfo)
                }
                extractor.advance()
            }
        } catch (e: CancellationException) {
            if (tempFile.exists()) tempFile.delete()
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed during video trimming pipeline", e)
            if (tempFile.exists()) tempFile.delete()
            return@withContext null
        } finally {
            if (isMuxerStarted) {
                try {
                    muxer?.stop()
                } catch (e: Exception) {
                    Log.w(TAG, "Error stopping MediaMuxer", e)
                }
            }
            try {
                muxer?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing MediaMuxer", e)
            }
            try {
                extractor?.release()
            } catch (e: Exception) {
                Log.w(TAG, "Error releasing MediaExtractor", e)
            }
            try {
                pfd?.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing ParcelFileDescriptor", e)
            }
        }

        try {
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
                } else if (!success) {
                    try { resolver.delete(newUri, null, null) } catch (ignored: Exception) {}
                    newUri = null
                }
            }
        } catch (e: CancellationException) {
            if (newUri != null) try { resolver.delete(newUri, null, null) } catch (ignored: Exception) {}
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert trimmed video into MediaStore", e)
            if (newUri != null) try { resolver.delete(newUri, null, null) } catch (ignored: Exception) {}
            newUri = null
        } finally {
            if (tempFile.exists()) tempFile.delete()
        }

        newUri
    }
}
