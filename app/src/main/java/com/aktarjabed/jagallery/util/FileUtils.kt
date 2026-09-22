package com.aktarjabed.jagallery.util

import android.app.PendingIntent
import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.content.ContentUris
import android.provider.MediaStore
import android.util.Log

object FileUtils {
    private const val TAG = "FileUtils"
    private const val MAX_BATCH_SIZE = com.aktarjabed.jagallery.util.Constants.MAX_BATCH_SIZE

    sealed class RequestCreationResult {
        data class Success(val chunks: List<com.aktarjabed.jagallery.data.model.DeleteRequestChunk>) : RequestCreationResult()
        object Unsupported : RequestCreationResult()
        data class Error(val cause: Exception) : RequestCreationResult()
    }

    data class DeleteMediaResult(
        val successfulUris: List<Uri>,
        val failedUris: List<Uri>
    ) {
        val isFullySuccessful: Boolean
            get() = failedUris.isEmpty()
    }

    private fun createBatchRequests(
        uris: List<Uri>,
        intentCreator: (List<Uri>) -> PendingIntent
    ): RequestCreationResult {
        if (uris.isEmpty()) return RequestCreationResult.Success(emptyList())

        val results = mutableListOf<com.aktarjabed.jagallery.data.model.DeleteRequestChunk>()
        for (chunk in uris.chunked(MAX_BATCH_SIZE)) {
            val intent = try {
                intentCreator(chunk)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to create request for chunk", e)
                return RequestCreationResult.Error(e)
            }
            results.add(
                com.aktarjabed.jagallery.data.model.DeleteRequestChunk(
                    ids = chunk.map { ContentUris.parseId(it).toString() },
                    uris = chunk,
                    pendingIntent = intent
                )
            )
        }
        return RequestCreationResult.Success(results)
    }

    fun createTrashRequests(contentResolver: ContentResolver, uris: List<Uri>, value: Boolean): RequestCreationResult {
        return createBatchRequests(uris) { chunk ->
            MediaStore.createTrashRequest(contentResolver, chunk, value)
        }
    }

    fun createDeleteRequests(contentResolver: ContentResolver, uris: List<Uri>): RequestCreationResult {
        return createBatchRequests(uris) { chunk ->
            MediaStore.createDeleteRequest(contentResolver, chunk)
        }
    }

    fun deleteMediaItems(contentResolver: ContentResolver, uris: List<Uri>): DeleteMediaResult {
        val successfulUris = mutableListOf<Uri>()
        val failedUris = mutableListOf<Uri>()

        if (uris.isEmpty()) return DeleteMediaResult(successfulUris, failedUris)

        // Group by collection URI to perform bulk deletes efficiently
        val groupedUris = mutableMapOf<Uri, MutableList<Uri>>()
        for (uri in uris) {
            try {
                val id = ContentUris.parseId(uri)
                val collectionUri = ContentUris.removeId(uri)
                groupedUris.getOrPut(collectionUri) { mutableListOf() }.add(uri)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse URI: $uri", e)
                failedUris.add(uri)
            }
        }

        for ((collectionUri, group) in groupedUris) {
            for (chunk in group.chunked(MAX_BATCH_SIZE)) {
                try {
                    val ids = chunk.map { ContentUris.parseId(it).toString() }.toTypedArray()
                    val selection = "${MediaStore.MediaColumns._ID} IN (${ids.joinToString(",") { "?" }})"
                    val rows = contentResolver.delete(collectionUri, selection, ids)
                    if (rows == chunk.size) {
                        successfulUris.addAll(chunk)
                    } else {
                        // Partial failure, verify survivors
                        val projection = arrayOf(MediaStore.MediaColumns._ID)
                        val survivors = mutableSetOf<Long>()
                        var queryFailed = false
                        try {
                            val cursor = contentResolver.query(collectionUri, projection, selection, ids, null)
                            if (cursor != null) {
                                cursor.use { c ->
                                    val idColumn = c.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
                                    while (c.moveToNext()) {
                                        survivors.add(c.getLong(idColumn))
                                    }
                                }
                            } else {
                                queryFailed = true
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Exception verifying survivors for chunk", e)
                            queryFailed = true
                        }

                        if (queryFailed) {
                            // If we can't verify, we must assume failure to be safe
                            failedUris.addAll(chunk)
                        } else {
                            for (uri in chunk) {
                                if (survivors.contains(ContentUris.parseId(uri))) {
                                    failedUris.add(uri)
                                } else {
                                    successfulUris.add(uri)
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception deleting chunk for $collectionUri", e)
                    failedUris.addAll(chunk)
                }
            }
        }
        return DeleteMediaResult(successfulUris, failedUris)
    }

    fun untrashMediaItems(contentResolver: ContentResolver, uris: List<Uri>): Boolean {
        var success = true
        for (uri in uris) {
            try {
                val contentValues = android.content.ContentValues().apply {
                    put(MediaStore.MediaColumns.IS_TRASHED, 0)
                }
                val rows = contentResolver.update(uri, contentValues, null, null)
                if (rows <= 0) {
                    Log.w(TAG, "No rows updated for untrashing URI $uri")
                    success = false
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception untrashing URI $uri", e)
                success = false
            }
        }
        return success
    }

    fun copyMediaFile(contentResolver: ContentResolver, sourceUri: Uri, destUri: Uri): Boolean {
        return try {
            contentResolver.openInputStream(sourceUri)?.use { input ->
                contentResolver.openOutputStream(destUri)?.use { output ->
                    // Use a 64KB buffer for faster copying of large media files
                    val buffer = ByteArray(64 * 1024)
                    var bytesRead: Int
                    while (true) {
                        bytesRead = input.read(buffer)
                        if (bytesRead < 0) break
                        if (bytesRead > 0) {
                            output.write(buffer, 0, bytesRead)
                        }
                    }
                    output.flush()
                    true
                }
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy media file from $sourceUri to $destUri", e)
            false
        }
    }

    suspend fun copyFileToMediaStore(
        context: android.content.Context,
        sourceUri: Uri,
        destination: com.aktarjabed.jagallery.data.model.AlbumDestination,
        originalName: String,
        mimeType: String
    ): Uri? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val resolver = context.contentResolver
        val isVideo = mimeType.startsWith("video/")
        val relativePath = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            when (destination) {
                is com.aktarjabed.jagallery.data.model.AlbumDestination.NewAlbum -> {
                    if (destination.relativePath.isNotEmpty()) destination.relativePath
                    else if (isVideo) "Movies/${destination.name}/" else "Pictures/${destination.name}/"
                }
                is com.aktarjabed.jagallery.data.model.AlbumDestination.ExistingAlbum -> destination.album.key.relativePath
            }
        } else ""

        val volumeName = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            when (destination) {
                is com.aktarjabed.jagallery.data.model.AlbumDestination.NewAlbum -> {
                    if (destination.volumeName.isNotEmpty()) destination.volumeName else android.provider.MediaStore.VOLUME_EXTERNAL
                }
                is com.aktarjabed.jagallery.data.model.AlbumDestination.ExistingAlbum -> destination.album.volumeName
            }
        } else android.provider.MediaStore.VOLUME_EXTERNAL

        val collection = if (isVideo) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) android.provider.MediaStore.Video.Media.getContentUri(volumeName) else android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } else {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) android.provider.MediaStore.Images.Media.getContentUri(volumeName) else android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val contentValues = android.content.ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, originalName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath)
            }
        }

        var newUri: Uri? = null
        try {
            newUri = insertPendingMediaEntry(resolver, collection, contentValues) ?: return@withContext null
            val success = copyMediaFile(resolver, sourceUri, newUri)

            if (success && publishPendingEntry(resolver, newUri, contentValues)) {
                newUri
            } else {
                try { resolver.delete(newUri, null, null) } catch (e: Exception) {}
                null
            }
        } catch (e: Exception) {
            if (newUri != null) {
                try { resolver.delete(newUri, null, null) } catch (delEx: Exception) {}
            }
            null
        }
    }

    fun insertPendingMediaEntry(
        contentResolver: ContentResolver,
        collection: Uri,
        contentValues: android.content.ContentValues
    ): Uri? {
        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 1)
        return try {
            contentResolver.insert(collection, contentValues)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert pending media entry", e)
            null
        }
    }

    fun publishPendingEntry(
        contentResolver: ContentResolver,
        uri: Uri,
        contentValues: android.content.ContentValues
    ): Boolean {
        contentValues.clear()
        contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
        return try {
            val updated = contentResolver.update(uri, contentValues, null, null)
            updated == 1
        } catch (e: Exception) {
            Log.e(TAG, "Failed to publish pending entry", e)
            false
        }
    }

    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0L) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        val clamped = digitGroups.coerceIn(0, units.size - 1)
        return String.format(java.util.Locale.US, "%.1f %s", bytes / Math.pow(1024.0, clamped.toDouble()), units[clamped])
    }
}
