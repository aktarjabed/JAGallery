package com.aktarjabed.core.ai.downloader

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.DigestInputStream
import java.security.MessageDigest
import kotlin.math.min

typealias ProgressCallback = (Int) -> Unit

class ModelDownloader(private val context: Context) {
    private val modelsDir = File(context.filesDir, "models").apply { if (!exists()) mkdirs() }
    private val BUFFER_SIZE = 16 * 1024

    fun getModelFile(name: String): File = File(modelsDir, name)
    fun isModelDownloaded(name: String): Boolean = getModelFile(name).exists()

    /**
     * Downloads a model. Returns Result<File>.
     * onProgress receives 0..100.
     */
    @Throws(Exception::class)
    fun downloadModel(
        modelUrl: String,
        modelName: String,
        expectedSha256: String? = null,
        onProgress: ProgressCallback? = null
    ): Result<File> {
        val dest = getModelFile(modelName)
        if (dest.exists()) {
            if (expectedSha256.isNullOrBlank()) return Result.success(dest)
            val current = sha256Hex(dest.inputStream())
            if (current.equals(expectedSha256, ignoreCase = true)) return Result.success(dest)
            // mismatch -> delete and redownload
            dest.delete()
        }

        val tmp = File(modelsDir, "$modelName.download")
        if (tmp.exists()) tmp.delete()

        var connection: HttpURLConnection? = null
        var input: InputStream? = null
        var output: FileOutputStream? = null

        try {
            val url = URL(modelUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 60000
                requestMethod = "GET"
                doInput = true
            }
            connection.connect()
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                return Result.failure(Exception("HTTP $responseCode"))
            }
            val length = connection.contentLength.takeIf { it > 0 } ?: -1
            input = connection.inputStream
            output = FileOutputStream(tmp)

            val digest = if (!expectedSha256.isNullOrBlank()) MessageDigest.getInstance("SHA-256") else null
            val dis = if (digest != null) DigestInputStream(input, digest) else null
            val streamForRead: InputStream = dis ?: input

            val buffer = ByteArray(BUFFER_SIZE)
            var read: Int
            var total = 0
            while (streamForRead.read(buffer).also { read = it } != -1) {
                output.write(buffer, 0, read)
                total += read
                if (length > 0) {
                    val progress = min(100, (total * 100 / length))
                    onProgress?.invoke(progress)
                }
            }
            output.flush()

            if (digest != null) {
                val computed = digest.digest().joinToString("") { "%02x".format(it) }
                if (!computed.equals(expectedSha256, ignoreCase = true)) {
                    tmp.delete()
                    return Result.failure(Exception("Checksum mismatch: expected $expectedSha256 got $computed"))
                }
            }

            // atomic move
            if (!tmp.renameTo(dest)) {
                tmp.copyTo(dest, overwrite = true)
                tmp.delete()
            }
            onProgress?.invoke(100)
            return Result.success(dest)
        } catch (ex: Exception) {
            tmp.delete()
            return Result.failure(ex)
        } finally {
            try { output?.close() } catch (_: Exception) {}
            try { input?.close() } catch (_: Exception) {}
            connection?.disconnect()
        }
    }

    private fun sha256Hex(input: InputStream): String {
        input.use {
            val md = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(BUFFER_SIZE)
            var read: Int
            while (it.read(buffer).also { read = it } != -1) {
                md.update(buffer, 0, read)
            }
            return md.digest().joinToString("") { "%02x".format(it) }
        }
    }
}
