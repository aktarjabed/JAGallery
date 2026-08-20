package com.example.advancedgallery.util

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.example.advancedgallery.data.model.MediaItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MediaStoreHelper {
    private const val PREFS_NAME = "mediastore_sync_prefs"
    private const val KEY_MEDIASTORE_VERSION = "key_mediastore_version"

    fun getPersistedMediaStoreVersion(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_MEDIASTORE_VERSION, null)
    }

    fun persistMediaStoreVersion(context: Context, version: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_MEDIASTORE_VERSION, version).apply()
    }

    suspend fun getMediaItemsResult(
        contentResolver: ContentResolver,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        context: Context? = null
    ): com.example.advancedgallery.data.model.MediaLoadResult = withContext(dispatcher) {
        try {
            val items = mutableListOf<MediaItem>()

            val imageCollectionUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val videoCollectionUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

            items.addAll(
                queryCollection(
                    contentResolver = contentResolver,
                    contentUri = imageCollectionUri,
                    isVideo = false
                )
            )

            items.addAll(
                queryCollection(
                    contentResolver = contentResolver,
                    contentUri = videoCollectionUri,
                    isVideo = true
                )
            )

            if (context != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                try {
                    val currentVersion = MediaStore.getVersion(context)
                    persistMediaStoreVersion(context, currentVersion)
                } catch (e: Exception) {
                    // Ignore
                }
            }

            val sorted = items.sortedByDescending { it.dateAdded }
            if (sorted.isEmpty()) {
                com.example.advancedgallery.data.model.MediaLoadResult.Empty
            } else {
                com.example.advancedgallery.data.model.MediaLoadResult.Success(sorted)
            }
        } catch (e: Throwable) {
            com.example.advancedgallery.data.model.MediaLoadResult.Error(e)
        }
    }

    suspend fun getMediaItems(
        contentResolver: ContentResolver,
        dispatcher: CoroutineDispatcher = Dispatchers.IO,
        context: Context? = null
    ): List<MediaItem> = withContext(dispatcher) {
        when (val result = getMediaItemsResult(contentResolver, dispatcher, context)) {
            is com.example.advancedgallery.data.model.MediaLoadResult.Success -> result.items
            else -> emptyList()
        }
    }

    fun isMediaStoreVersionCurrent(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return try {
            val currentVersion = MediaStore.getVersion(context)
            val persisted = getPersistedMediaStoreVersion(context)
            persisted != null && persisted == currentVersion
        } catch (e: Exception) {
            false
        }
    }

    private fun queryCollection(
        contentResolver: ContentResolver,
        contentUri: Uri,
        isVideo: Boolean
    ): List<MediaItem> {
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
        } catch (e: Exception) {
            null
        }

        query?.use { cursor ->
            val idColumn = cursor.getColumnIndex(MediaStore.MediaColumns._ID)
            val nameColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            val dateAddedColumn = cursor.getColumnIndex(MediaStore.MediaColumns.DATE_ADDED)
            val mimeTypeColumn = cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)
            val bucketIdColumn = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)

            if (idColumn == -1) return items

            while (cursor.moveToNext()) {
                val mediaStoreId = cursor.getLong(idColumn)
                val name = if (nameColumn != -1) cursor.getString(nameColumn) ?: "" else ""
                val dateAdded = if (dateAddedColumn != -1) cursor.getLong(dateAddedColumn) else 0L
                val mimeType = if (mimeTypeColumn != -1) cursor.getString(mimeTypeColumn) ?: "" else ""
                val bucketId = if (bucketIdColumn != -1) cursor.getLong(bucketIdColumn) else 0L
                val bucketName = if (bucketNameColumn != -1) cursor.getString(bucketNameColumn) ?: "" else ""

                val uri = try {
                    ContentUris.withAppendedId(contentUri, mediaStoreId)
                } catch (e: Exception) {
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
                        isVideo = isVideo
                    )
                )
            }
        }
        return items
    }
}
