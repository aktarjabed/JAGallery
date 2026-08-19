package com.example.advancedgallery.util

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import com.example.advancedgallery.data.model.MediaItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object MediaStoreHelper {
    suspend fun getMediaItems(
        contentResolver: ContentResolver,
        dispatcher: CoroutineDispatcher = Dispatchers.IO
    ): List<MediaItem> = withContext(dispatcher) {
        val items = mutableListOf<MediaItem>()

        val imageCollectionUri = try {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        } catch (e: Throwable) {
            null
        }

        val videoCollectionUri = try {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        } catch (e: Throwable) {
            null
        }

        if (imageCollectionUri != null) {
            items.addAll(
                queryCollection(
                    contentResolver = contentResolver,
                    contentUri = imageCollectionUri,
                    isVideo = false
                )
            )
        }

        if (videoCollectionUri != null) {
            items.addAll(
                queryCollection(
                    contentResolver = contentResolver,
                    contentUri = videoCollectionUri,
                    isVideo = true
                )
            )
        }

        items.sortedByDescending { it.dateAdded }
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
                val id = cursor.getLong(idColumn)
                val name = if (nameColumn != -1) cursor.getString(nameColumn) ?: "" else ""
                val dateAdded = if (dateAddedColumn != -1) cursor.getLong(dateAddedColumn) else 0L
                val mimeType = if (mimeTypeColumn != -1) cursor.getString(mimeTypeColumn) ?: "" else ""
                val bucketId = if (bucketIdColumn != -1) cursor.getLong(bucketIdColumn) else 0L
                val bucketName = if (bucketNameColumn != -1) cursor.getString(bucketNameColumn) ?: "" else ""

                val uri = try {
                    ContentUris.withAppendedId(contentUri, id)
                } catch (e: Throwable) {
                    contentUri
                }

                items.add(
                    MediaItem(
                        id = id,
                        uri = uri,
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
