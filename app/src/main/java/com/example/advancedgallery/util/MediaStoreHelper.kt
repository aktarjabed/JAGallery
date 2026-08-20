package com.example.advancedgallery.util

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.advancedgallery.data.model.MediaItem
import com.example.advancedgallery.data.model.MediaLoadResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

object MediaStoreHelper {
    private const val TAG = "MediaStoreHelper"
    private const val PREFS_NAME = "mediastore_sync_prefs"
    private const val KEY_VERSION_PREFIX = "key_mediastore_version_"
    private const val KEY_GENERATION_PREFIX = "key_mediastore_generation_"

    fun getPersistedVolumeVersion(context: Context, volumeName: String): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_VERSION_PREFIX + volumeName, null)
    }

    fun getPersistedVolumeGeneration(context: Context, volumeName: String): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_GENERATION_PREFIX + volumeName, -1L)
    }

    fun persistVolumeSyncInfo(context: Context, volumeName: String, version: String?, generation: Long) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_VERSION_PREFIX + volumeName, version)
            .putLong(KEY_GENERATION_PREFIX + volumeName, generation)
            .apply()
    }

    suspend fun getMediaItemsResult(
        contentResolver: ContentResolver,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        context: Context? = null
    ): MediaLoadResult = withContext(dispatcher) {
        try {
            val items = mutableListOf<MediaItem>()
            val targets = getCollectionUris(context)
            var totalQueriesAttempted = 0
            var successfulQueriesCount = 0
            val queryErrors = mutableListOf<Pair<String, Throwable>>()

            for ((imageUri, videoUri, volumeName) in targets) {
                val effectiveVolume = volumeName ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.VOLUME_EXTERNAL_PRIMARY else "external"
                var volumeHasSuccess = false

                totalQueriesAttempted++
                val imageQueryResult = queryCollectionResult(contentResolver, imageUri, isVideo = false, volumeName = effectiveVolume)
                if (imageQueryResult is QueryResult.Error) {
                    Log.w(TAG, "Image query failed for volume $effectiveVolume", imageQueryResult.cause)
                    queryErrors.add("Image ($effectiveVolume)" to imageQueryResult.cause)
                } else if (imageQueryResult is QueryResult.Success) {
                    successfulQueriesCount++
                    volumeHasSuccess = true
                    items.addAll(imageQueryResult.items)
                }

                totalQueriesAttempted++
                val videoQueryResult = queryCollectionResult(contentResolver, videoUri, isVideo = true, volumeName = effectiveVolume)
                if (videoQueryResult is QueryResult.Error) {
                    Log.w(TAG, "Video query failed for volume $effectiveVolume", videoQueryResult.cause)
                    queryErrors.add("Video ($effectiveVolume)" to videoQueryResult.cause)
                } else if (videoQueryResult is QueryResult.Success) {
                    successfulQueriesCount++
                    volumeHasSuccess = true
                    items.addAll(videoQueryResult.items)
                }

                if (volumeHasSuccess && context != null && volumeName != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    try {
                        val version = MediaStore.getVersion(context, volumeName)
                        val generation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                            MediaStore.getGeneration(context, volumeName)
                        } else {
                            0L
                        }
                        persistVolumeSyncInfo(context, volumeName, version, generation)
                    } catch (e: SecurityException) {
                        Log.w(TAG, "SecurityException persisting sync info for volume $volumeName", e)
                    } catch (e: IllegalArgumentException) {
                        Log.w(TAG, "IllegalArgumentException persisting sync info for volume $volumeName", e)
                    }
                }
            }

            if (successfulQueriesCount == 0 && queryErrors.isNotEmpty()) {
                MediaLoadResult.Error(queryErrors.first().second)
            } else {
                val sorted = items.sortedByDescending { it.dateAdded }
                if (sorted.isEmpty()) {
                    MediaLoadResult.Empty
                } else {
                    MediaLoadResult.Success(sorted)
                }
            }
        } catch (e: SecurityException) {
            MediaLoadResult.Error(e)
        } catch (e: IOException) {
            MediaLoadResult.Error(e)
        } catch (e: IllegalArgumentException) {
            MediaLoadResult.Error(e)
        }
    }

    fun isMediaStoreVersionCurrent(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return try {
            val volumeNames = MediaStore.getExternalVolumeNames(context)
            if (volumeNames.isEmpty()) return false
            for (volumeName in volumeNames) {
                val currentVersion = MediaStore.getVersion(context, volumeName)
                val persistedVersion = getPersistedVolumeVersion(context, volumeName)
                if (persistedVersion == null || persistedVersion != currentVersion) {
                    return false
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    val currentGeneration = MediaStore.getGeneration(context, volumeName)
                    val persistedGeneration = getPersistedVolumeGeneration(context, volumeName)
                    if (persistedGeneration == -1L || persistedGeneration != currentGeneration) {
                        return false
                    }
                }
            }
            true
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException checking MediaStore version", e)
            false
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "IllegalArgumentException checking MediaStore version", e)
            false
        }
    }

    private data class CollectionTarget(
        val imageUri: Uri,
        val videoUri: Uri,
        val volumeName: String?
    )

    private fun getCollectionUris(context: Context?): List<CollectionTarget> {
        if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val volumes = MediaStore.getExternalVolumeNames(context)
                if (volumes.isNotEmpty()) {
                    return volumes.map { volume ->
                        CollectionTarget(
                            imageUri = MediaStore.Images.Media.getContentUri(volume),
                            videoUri = MediaStore.Video.Media.getContentUri(volume),
                            volumeName = volume
                        )
                    }
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "SecurityException getting volume names", e)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "IllegalArgumentException getting volume names", e)
            }
        }
        return listOf(
            CollectionTarget(
                imageUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                videoUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                volumeName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.VOLUME_EXTERNAL_PRIMARY else null
            )
        )
    }

    private sealed interface QueryResult {
        data class Success(val items: List<MediaItem>) : QueryResult
        data class Error(val cause: Throwable) : QueryResult
    }

    private fun queryCollectionResult(
        contentResolver: ContentResolver,
        contentUri: Uri,
        isVideo: Boolean,
        volumeName: String
    ): QueryResult {
        val items = mutableListOf<MediaItem>()
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME
        )

        val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"

        val query: Cursor? = try {
            contentResolver.query(
                contentUri,
                projection,
                null,
                null,
                sortOrder
            )
        } catch (e: SecurityException) {
            return QueryResult.Error(e)
        } catch (e: IllegalArgumentException) {
            return QueryResult.Error(e)
        } catch (e: IllegalStateException) {
            return QueryResult.Error(e)
        } catch (e: IOException) {
            return QueryResult.Error(e)
        }

        if (query == null) {
            return QueryResult.Error(NullPointerException("Cursor returned null for $contentUri"))
        }

        query.use { cursor ->
            val idColumn = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
            val nameColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            val dateAddedColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
            val mimeTypeColumn = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
            val bucketIdColumn = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)

            if (idColumn == -1) return QueryResult.Success(items)

            while (cursor.moveToNext()) {
                val mediaStoreId = cursor.getLong(idColumn)
                val name = if (nameColumn != -1) cursor.getString(nameColumn) ?: "" else ""
                val dateAdded = if (dateAddedColumn != -1) cursor.getLong(dateAddedColumn) else 0L
                val mimeType = if (mimeTypeColumn != -1) cursor.getString(mimeTypeColumn) ?: "" else ""
                val bucketId = if (bucketIdColumn != -1) cursor.getLong(bucketIdColumn) else 0L
                val bucketName = if (bucketNameColumn != -1) cursor.getString(bucketNameColumn) ?: "" else ""

                val uri = try {
                    ContentUris.withAppendedId(contentUri, mediaStoreId)
                } catch (e: IllegalArgumentException) {
                    continue
                }

                items.add(
                    MediaItem(
                        uri = uri,
                        mediaStoreId = mediaStoreId,
                        name = name,
                        dateAdded = dateAdded,
                        mimeType = mimeType,
                        bucketId = bucketId,
                        bucketName = bucketName,
                        isVideo = isVideo,
                        volumeName = volumeName
                    )
                )
            }
        }
        return QueryResult.Success(items)
    }
}
