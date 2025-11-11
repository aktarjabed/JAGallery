package com.aktarjabed.jascanner.repository

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.MediaStore
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import com.aktarjabed.jascanner.model.Photo
import com.aktarjabed.jascanner.security.Vault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

class RecoverableDeleteException(val sender: android.content.IntentSender) : Exception()

class MediaRepository(private val context: Context) {

    suspend fun getPhotos(): List<Photo> = withContext(Dispatchers.IO) {
        val photos = mutableListOf<Photo>()
        val collection = if (Build.VERSION.SDK_INT >= 29) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"

        context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getStringOrNull(nameColumn)
                val dateTaken = cursor.getLongOrNull(dateColumn)
                val width = cursor.getIntOrNull(widthColumn)
                val height = cursor.getIntOrNull(heightColumn)
                val mimeType = cursor.getStringOrNull(mimeColumn)
                val bucket = cursor.getStringOrNull(bucketColumn)
                val contentUri = ContentUris.withAppendedId(collection, id)

                photos.add(
                    Photo(
                        id = id.toString(),
                        uri = contentUri,
                        displayName = name,
                        dateTaken = dateTaken,
                        width = width,
                        height = height,
                        mimeType = mimeType,
                        bucket = bucket
                    )
                )
            }
        }

        return@withContext photos
    }

    suspend fun getPhotoById(id: Long): Photo? = withContext(Dispatchers.IO) {
        val collection = if (Build.VERSION.SDK_INT >= 29) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME
        )

        context.contentResolver.query(
            collection,
            projection,
            "${MediaStore.Images.Media._ID}=?",
            arrayOf(id.toString()),
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
                val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

                val uri = ContentUris.withAppendedId(collection, id)
                Photo(
                    id = id.toString(),
                    uri = uri,
                    displayName = cursor.getStringOrNull(nameColumn),
                    dateTaken = cursor.getLongOrNull(dateColumn),
                    width = cursor.getIntOrNull(widthColumn),
                    height = cursor.getIntOrNull(heightColumn),
                    mimeType = cursor.getStringOrNull(mimeColumn),
                    bucket = cursor.getStringOrNull(bucketColumn)
                )
            } else null
        }
    }

    suspend fun deletePhoto(uri: android.net.Uri) = withContext(Dispatchers.IO) {
        try {
            val deleted = context.contentResolver.delete(uri, null, null)
            if (deleted == 0) {
                throw IOException("Failed to delete photo")
            }
        } catch (e: android.content.RecoverableSecurityException) {
            throw RecoverableDeleteException(e.userAction.actionIntent.intentSender)
        }
    }

    suspend fun moveToVault(photoId: Long, deleteOriginal: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        val photo = getPhotoById(photoId) ?: return@withContext false
        try {
            Vault(context).encryptFromUri(photo.uri, (photo.displayName ?: "${photoId}") + ".enc")
            if (deleteOriginal) {
                try {
                    deletePhoto(photo.uri)
                } catch (_: RecoverableDeleteException) {
                    // Let caller handle recoverable delete
                }
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun getAITags(photoId: Long): List<String> = withContext(Dispatchers.IO) {
        val photo = getPhotoById(photoId) ?: return@withContext emptyList()
        val inputStream = context.contentResolver.openInputStream(photo.uri) ?: return@withContext emptyList()

        inputStream.use { stream ->
            val bmp = BitmapFactory.decodeStream(stream)
            val tags = com.aktarjabed.jascanner.ai.ImageTagger(context).analyzeImage(bmp)
            bmp.recycle()
            tags
        }
    }
}