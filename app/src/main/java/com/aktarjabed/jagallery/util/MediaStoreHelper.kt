package com.aktarjabed.jagallery.util

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.content.edit
import com.aktarjabed.jagallery.data.model.MediaItem
import com.aktarjabed.jagallery.data.model.MediaLoadResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

object MediaStoreHelper {
    private const val TAG = "MediaStoreHelper"
    private const val PREFS_NAME = "mediastore_sync_prefs"
    private const val KEY_VERSION_PREFIX = "key_mediastore_version_"
    private const val KEY_GENERATION_PREFIX = "key_mediastore_generation_"

    fun getPersistedVolumeVersion(context: Context, volumeName: String, isTrash: Boolean = false): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_VERSION_PREFIX + volumeName + getVolumeSyncKeySuffix(isTrash), null)
    }

    fun getPersistedVolumeGeneration(context: Context, volumeName: String, isTrash: Boolean = false): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_GENERATION_PREFIX + volumeName + getVolumeSyncKeySuffix(isTrash), -1L)
    }

    private fun getVolumeSyncKeySuffix(isTrash: Boolean) = if (isTrash) "_trash" else ""

    fun persistVolumeSyncInfo(context: Context, volumeName: String, version: String?, generation: Long, isTrash: Boolean = false) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            val suffix = getVolumeSyncKeySuffix(isTrash)
            putString(KEY_VERSION_PREFIX + volumeName + suffix, version)
            putLong(KEY_GENERATION_PREFIX + volumeName + suffix, generation)
        }
    }

    suspend fun getMediaItemsResult(
        contentResolver: ContentResolver,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        context: Context? = null,
        includeTrashed: Boolean = false
    ): MediaLoadResult = withContext(dispatcher) {
        try {
            val items = mutableListOf<MediaItem>()
            val targets = getCollectionUris(context)
            var totalQueriesAttempted = 0
            var successfulQueriesCount = 0
            val queryErrors = mutableListOf<Pair<String, Throwable>>()

            val initialGenerationMap = mutableMapOf<String, Long>()
            val initialVersionMap = mutableMapOf<String, String?>()

            for ((imageUri, videoUri, volumeName) in targets) {
                val effectiveVolume = volumeName ?: if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) MediaStore.VOLUME_EXTERNAL_PRIMARY else "external"

                if (context != null && volumeName != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                     try {
                         initialVersionMap[volumeName] = MediaStore.getVersion(context, volumeName)
                         initialGenerationMap[volumeName] = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) MediaStore.getGeneration(context, volumeName) else 0L
                     } catch (e: Exception) {}
                }

                totalQueriesAttempted++
                val imageQueryResult = queryCollectionResult(contentResolver, imageUri, isVideo = false, volumeName = effectiveVolume, includeTrashed = includeTrashed)
                if (imageQueryResult is QueryResult.Error) {
                    Log.w(TAG, "Image query failed for volume $effectiveVolume", imageQueryResult.cause)
                    queryErrors.add("Image ($effectiveVolume)" to imageQueryResult.cause)
                } else if (imageQueryResult is QueryResult.Success) {
                    successfulQueriesCount++
                    items.addAll(imageQueryResult.items)
                }

                totalQueriesAttempted++
                val videoQueryResult = queryCollectionResult(contentResolver, videoUri, isVideo = true, volumeName = effectiveVolume, includeTrashed = includeTrashed)
                if (videoQueryResult is QueryResult.Error) {
                    Log.w(TAG, "Video query failed for volume $effectiveVolume", videoQueryResult.cause)
                    queryErrors.add("Video ($effectiveVolume)" to videoQueryResult.cause)
                } else if (videoQueryResult is QueryResult.Success) {
                    successfulQueriesCount++
                    items.addAll(videoQueryResult.items)
                }
            }

            if (queryErrors.isNotEmpty()) {
                MediaLoadResult.Error(queryErrors.first().second)
            } else {
                var isGenerationConsistent = true
                for ((_, _, volumeName) in targets) {
                    if (context != null && volumeName != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        try {
                            val finalVersion = MediaStore.getVersion(context, volumeName)
                            val finalGeneration = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) MediaStore.getGeneration(context, volumeName) else 0L
                            if (initialVersionMap[volumeName] != finalVersion || initialGenerationMap[volumeName] != finalGeneration) {
                                isGenerationConsistent = false
                                break
                            }
                        } catch (e: Exception) {
                            isGenerationConsistent = false
                        }
                    }
                }

                if (isGenerationConsistent) {
                    for ((_, _, volumeName) in targets) {
                        if (context != null && volumeName != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            val version = initialVersionMap[volumeName]
                            val generation = initialGenerationMap[volumeName] ?: 0L
                            persistVolumeSyncInfo(context, volumeName, version, generation, isTrash = includeTrashed)
                        }
                    }
                }

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

    fun isMediaStoreVersionCurrent(context: Context, isTrash: Boolean = false): Boolean {
        return try {
            val targets = getCollectionUris(context)
            if (targets.isEmpty()) return false
            for (target in targets) {
                val volumeName = target.volumeName ?: continue
                val currentVersion = MediaStore.getVersion(context, volumeName)
                val persistedVersion = getPersistedVolumeVersion(context, volumeName, isTrash)
                if (persistedVersion == null || persistedVersion != currentVersion) {
                    return false
                }
                val currentGeneration = MediaStore.getGeneration(context, volumeName)
                val persistedGeneration = getPersistedVolumeGeneration(context, volumeName, isTrash)
                if (persistedGeneration == -1L || persistedGeneration != currentGeneration) {
                    return false
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
        if (context != null) {
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
            } catch (e: NoSuchMethodError) {
                // Ignore in Robolectric for older SDK versions
                Log.w(TAG, "NoSuchMethodError getting volume names", e)
            }
        }
        return emptyList()
    }

    private sealed interface QueryResult {
        data class Success(val items: List<MediaItem>) : QueryResult
        data class Error(val cause: Throwable) : QueryResult
    }

    private fun queryCollectionResult(
        contentResolver: ContentResolver,
        contentUri: Uri,
        isVideo: Boolean,
        volumeName: String,
        includeTrashed: Boolean = false
    ): QueryResult {
        val projectionList = mutableListOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.SIZE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projectionList.add(MediaStore.MediaColumns.RELATIVE_PATH)
        }

        projectionList.add(MediaStore.MediaColumns.IS_TRASHED)
        projectionList.add(MediaStore.MediaColumns.DATE_EXPIRES)

        val projection = projectionList.toTypedArray()
        val sortOrder = "${MediaStore.MediaColumns.DATE_ADDED} DESC"

        val query: Cursor? = try {
            try {
                val bundle = android.os.Bundle().apply {
                    putString(ContentResolver.QUERY_ARG_SQL_SORT_ORDER, sortOrder)
                    if (includeTrashed) {
                        putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_ONLY)
                    } else {
                        putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, MediaStore.MATCH_EXCLUDE)
                    }
                }
                contentResolver.query(contentUri, projection, bundle, null)
            } catch (e: NoSuchMethodError) {
                // Fallback for older Robolectric environments
                contentResolver.query(contentUri, projection, null, null, sortOrder)
            }
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

        val items = mutableListOf<MediaItem>()
        query.use { cursor ->
            val idColumn = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
            val nameColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            val dateAddedColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
            val mimeTypeColumn = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
            val bucketIdColumn = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
            val relativePathColumn = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
            val sizeColumn = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val isTrashedColumn = cursor.getColumnIndex(MediaStore.MediaColumns.IS_TRASHED)
            val dateTrashedColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_EXPIRES)

            if (idColumn == -1) return QueryResult.Error(IllegalArgumentException("Missing _ID column in MediaStore result"))

            while (cursor.moveToNext()) {
                val mediaStoreId = cursor.getLong(idColumn)
                val name = if (nameColumn != -1) cursor.getString(nameColumn) ?: "" else ""
                val dateAdded = if (dateAddedColumn != -1) cursor.getLong(dateAddedColumn) else 0L
                val mimeType = if (mimeTypeColumn != -1) cursor.getString(mimeTypeColumn) ?: "" else ""
                val bucketId = if (bucketIdColumn != -1) cursor.getLong(bucketIdColumn) else 0L
                val bucketName = if (bucketNameColumn != -1) cursor.getString(bucketNameColumn) ?: "" else ""
                val relativePath = if (relativePathColumn != -1) cursor.getString(relativePathColumn) ?: "" else ""
                val size = if (sizeColumn != -1) cursor.getLong(sizeColumn) else 0L
                val isTrashed = if (isTrashedColumn != -1) cursor.getInt(isTrashedColumn) == 1 else false
                val dateTrashed = if (dateTrashedColumn != -1) cursor.getLong(dateTrashedColumn) else 0L

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
                        relativePath = relativePath,
                        isVideo = isVideo,
                        volumeName = volumeName,
                        size = size,
                        isTrashed = isTrashed,
                        dateTrashed = dateTrashed
                    )
                )
            }
        }
        return QueryResult.Success(items)
    }
}
