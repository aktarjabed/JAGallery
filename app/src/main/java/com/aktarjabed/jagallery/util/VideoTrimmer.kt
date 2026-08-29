package com.aktarjabed.jagallery.util

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
        var newUri: Uri? = null

        var extractor: MediaExtractor? = null
        var sourcePfd: ParcelFileDescriptor? = null
        var destPfd: ParcelFileDescriptor? = null
        var muxer: MediaMuxer? = null
        var isMuxerStarted = false

        try {
            // Insert into MediaStore first
            val contentValues = android.content.ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, "TRIM_${System.currentTimeMillis()}.mp4")
                put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            }

            newUri = FileUtils.insertPendingMediaEntry(resolver, collection, contentValues)
            if (newUri == null) {
                return@withContext null
            }

            extractor = MediaExtractor()
            sourcePfd = resolver.openFileDescriptor(sourceUri, "r")
            if (sourcePfd == null) {
                try { resolver.delete(newUri, null, null) } catch (ignored: Exception) {}
                return@withContext null
            }
            extractor.setDataSource(sourcePfd.fileDescriptor)

            destPfd = resolver.openFileDescriptor(newUri, "rw")
            if (destPfd == null) {
                try { resolver.delete(newUri, null, null) } catch (ignored: Exception) {}
                return@withContext null
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                muxer = MediaMuxer(destPfd.fileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            } else {
                throw UnsupportedOperationException("MediaMuxer requires API 26+ for FileDescriptor")
            }
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

            // Pass 1: Probe true first timestamps
            val firstSampleTimeUsMap = HashMap<Int, Long>()
            while (true) {
                val size = extractor.readSampleData(buffer, 0)
                if (size < 0) break
                val presentationTimeUs = extractor.sampleTime
                if (presentationTimeUs > endUs) break
                val trackIndex = extractor.sampleTrackIndex
                if (trackMap.containsKey(trackIndex) && !firstSampleTimeUsMap.containsKey(trackIndex)) {
                    firstSampleTimeUsMap[trackIndex] = presentationTimeUs
                }
                if (firstSampleTimeUsMap.size == trackMap.size) break
                extractor.advance()
            }

            val baseTimeUs = if (firstSampleTimeUsMap.isNotEmpty()) {
                firstSampleTimeUsMap.values.minOrNull() ?: 0L
            } else {
                0L
            }

            // Pass 2: Mux using baseTimeUs
            extractor.seekTo(startUs, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
            while (true) {
                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break

                val presentationTimeUs = extractor.sampleTime
                if (presentationTimeUs > endUs) break

                val trackIndex = extractor.sampleTrackIndex
                val dstTrackIndex = trackMap[trackIndex]
                if (dstTrackIndex != null) {
                    val normalizedUs = maxOf(0L, presentationTimeUs - baseTimeUs)
                    bufferInfo.presentationTimeUs = normalizedUs
                    bufferInfo.flags = if (extractor.sampleFlags and android.media.MediaExtractor.SAMPLE_FLAG_SYNC != 0) android.media.MediaCodec.BUFFER_FLAG_KEY_FRAME else 0
                    muxer.writeSampleData(dstTrackIndex, buffer, bufferInfo)
                }
                extractor.advance()
            }

            muxer.stop()
            isMuxerStarted = false

            val finalContentValues = android.content.ContentValues().apply {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.Video.Media.IS_PENDING, 0)
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                resolver.update(newUri, finalContentValues, null, null)
            }

        } catch (e: CancellationException) {
            if (newUri != null) try { resolver.delete(newUri, null, null) } catch (ignored: Exception) {}
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed during video trimming pipeline", e)
            if (newUri != null) try { resolver.delete(newUri, null, null) } catch (ignored: Exception) {}
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
                sourcePfd?.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing ParcelFileDescriptor", e)
            }
            try {
                destPfd?.close()
            } catch (e: Exception) {
                Log.w(TAG, "Error closing dest ParcelFileDescriptor", e)
            }
        }

        newUri
    }
}
