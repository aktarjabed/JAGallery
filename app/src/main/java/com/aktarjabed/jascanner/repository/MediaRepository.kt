package com.aktarjabed.jascanner.repository

import android.content.ContentUris
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Build
import android.provider.MediaStore
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.core.database.getIntOrNull
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.aktarjabed.jascanner.model.Photo
import com.aktarjabed.jascanner.security.Vault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import com.aktarjabed.jascanner.utils.DeleteUtils
import java.io.File

class RecoverableDeleteException(val sender: android.content.IntentSender) : Exception()

// Optimized Paging Source for large photo collections
class PhotosPagingSource(
    private val context: Context,
    private val category: String = "all"
) : PagingSource<Int, Photo>() {

    companion object {
        private const val PAGE_SIZE = 60
        private const val MAX_PHOTOS = 2000 // Prevent memory issues with huge libraries
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Photo> {
        return try {
            val page = params.key ?: 0
            val photos = withContext(Dispatchers.IO) {
                MediaRepository(context).getPhotosPage(context, category, page, PAGE_SIZE)
            }

            LoadResult.Page(
                data = photos,
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (photos.size < PAGE_SIZE || (page + 1) * PAGE_SIZE >= MAX_PHOTOS) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Photo>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }
}

// Thumbnail cache for smooth scrolling
object ThumbnailCache {
    private val cache = mutableMapOf<String, android.graphics.Bitmap>()
    private const val MAX_CACHE_SIZE = 100

    fun getThumbnail(key: String): android.graphics.Bitmap? = cache[key]

    fun putThumbnail(key: String, bitmap: android.graphics.Bitmap) {
        if (cache.size >= MAX_CACHE_SIZE) {
            cache.remove(cache.keys.first())
        }
        cache[key] = bitmap
    }

    fun clear() = cache.clear()
}

class MediaRepository(private val context: Context) {

    // Flow for observing media changes
    private val _mediaChanged = MutableStateFlow(0L)
    val mediaChanged: StateFlow<Long> get() = _mediaChanged

    private var contentObserver: PhotoContentObserver? = null

    // Initialize content observer
    fun registerContentObserver() {
        if (contentObserver == null) {
            contentObserver = PhotoContentObserver {
                // Increment to trigger recomposition in UI
                _mediaChanged.value = System.currentTimeMillis()
                // Clear caches since media changed
                clearCache()
            }.also { observer ->
                observer.getObservedUris().forEach { uri ->
                    context.contentResolver.registerContentObserver(
                        uri,
                        true, // Observe sub-directories
                        observer
                    )
                }
            }
        }
    }

    // Clean up when no longer needed
    fun unregisterContentObserver() {
        contentObserver?.let { observer ->
            context.contentResolver.unregisterContentObserver(observer)
            contentObserver = null
        }
    }

    // Force refresh (manual trigger)
    fun notifyMediaChanged() {
        _mediaChanged.value = System.currentTimeMillis()
        clearCache()
    }

    // Memory cache for categories to avoid recomputation
    private var categoriesCache: Map<String, Pair<String, Int>>? = null
    private var bucketsCache: Map<String, Int>? = null
    private var allPhotosCache: List<Photo>? = null

    // Get photos with pagination
    suspend fun getPhotosPagingSource(category: String = "all"): PhotosPagingSource {
        return PhotosPagingSource(context, category)
    }

    // Get single page of photos (optimized for paging)
    suspend fun getPhotosPage(
        context: Context,
        category: String = "all",
        page: Int = 0,
        pageSize: Int = 60
    ): List<Photo> = withContext(Dispatchers.IO) {
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
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.RELATIVE_PATH
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        val limit = "$pageSize OFFSET ${page * pageSize}"

        context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            "$sortOrder LIMIT $limit"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getStringOrNull(nameColumn)
                val dateTaken = cursor.getLongOrNull(dateColumn)
                val width = cursor.getIntOrNull(widthColumn)
                val height = cursor.getIntOrNull(heightColumn)
                val mimeType = cursor.getStringOrNull(mimeColumn)
                val bucket = cursor.getStringOrNull(bucketColumn)
                val bucketId = cursor.getStringOrNull(bucketIdColumn)
                val relativePath = cursor.getStringOrNull(pathColumn)
                val contentUri = ContentUris.withAppendedId(collection, id)

                val photo = Photo(
                    id = id.toString(),
                    uri = contentUri,
                    displayName = name,
                    dateTaken = dateTaken,
                    width = width,
                    height = height,
                    mimeType = mimeType,
                    bucket = bucket,
                    bucketId = bucketId,
                    relativePath = relativePath
                )

                // Filter by category if needed
                if (category == "all" || photo.matchesCategory(category)) {
                    photos.add(photo)
                }
            }
        }

        return@withContext photos
    }

    // Get all photos (with caching for small collections)
    suspend fun getPhotos(useCache: Boolean = true): List<Photo> = withContext(Dispatchers.IO) {
        if (useCache && allPhotosCache != null) {
            return@withContext allPhotosCache!!
        }

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
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.RELATIVE_PATH
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_TAKEN} DESC"
        val limit = "2000" // Safety limit

        context.contentResolver.query(
            collection,
            projection,
            null,
            null,
            "$sortOrder LIMIT $limit"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val widthColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val bucketColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getStringOrNull(nameColumn)
                val dateTaken = cursor.getLongOrNull(dateColumn)
                val width = cursor.getIntOrNull(widthColumn)
                val height = cursor.getIntOrNull(heightColumn)
                val mimeType = cursor.getStringOrNull(mimeColumn)
                val bucket = cursor.getStringOrNull(bucketColumn)
                val bucketId = cursor.getStringOrNull(bucketIdColumn)
                val relativePath = cursor.getStringOrNull(pathColumn)
                val contentUri = ContentUris.withAppendedId(collection, id)

                photos.add(Photo(
                    id = id.toString(),
                    uri = contentUri,
                    displayName = name,
                    dateTaken = dateTaken,
                    width = width,
                    height = height,
                    mimeType = mimeType,
                    bucket = bucket,
                    bucketId = bucketId,
                    relativePath = relativePath
                ))
            }
        }

        allPhotosCache = photos
        return@withContext photos
    }

    // Get optimized thumbnail for smooth scrolling
    suspend fun getThumbnail(photoId: String, width: Int = 256, height: Int = 256): android.graphics.Bitmap? =
        withContext(Dispatchers.IO) {
            // Check cache first
            ThumbnailCache.getThumbnail("$photoId-$width-$height")?.let { return@withContext it }

            try {
                val collection = if (Build.VERSION.SDK_INT >= 29) {
                    MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
                } else {
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                }
                val uri = ContentUris.withAppendedId(collection, photoId.toLong())

                // Use MediaStore thumbnails for better performance
                if (Build.VERSION.SDK_INT >= 29) {
                    val thumbnail = context.contentResolver.loadThumbnail(uri, android.util.Size(width, height), null)
                    ThumbnailCache.putThumbnail("$photoId-$width-$height", thumbnail)
                    return@withContext thumbnail
                } else {
                    // Fallback for older APIs
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        val options = BitmapFactory.Options().apply {
                            inJustDecodeBounds = false
                            inSampleSize = calculateInSampleSize(this, width, height)
                            inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
                        }
                        val bitmap = BitmapFactory.decodeStream(input, null, options)
                        ThumbnailCache.putThumbnail("$photoId-$width-$height", bitmap)
                        return@withContext bitmap
                    }
                }
            } catch (e: Exception) {
                null
            }
        }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    // Category-based filtering with lazy evaluation
    suspend fun getPhotosByCategory(category: String): List<Photo> = withContext(Dispatchers.IO) {
        val allPhotos = getPhotos()

        return@withContext when (category) {
            "all" -> allPhotos
            "camera" -> allPhotos.filter { it.isCamera() }
            "screenshots" -> allPhotos.filter { it.isScreenshot() }
            "whatsapp" -> allPhotos.filter { it.isWhatsApp() }
            "downloads" -> allPhotos.filter { it.isDownloads() }
            "social" -> allPhotos.filter { it.isSocialMedia() }
            else -> allPhotos.filter { it.bucket?.contains(category, ignoreCase = true) == true }
        }
    }

    // Get photo buckets with caching
    suspend fun getPhotoBuckets(useCache: Boolean = true): Map<String, Int> = withContext(Dispatchers.IO) {
        if (useCache && bucketsCache != null) {
            return@withContext bucketsCache!!
        }

        val photos = getPhotos(useCache)
        val buckets = mutableMapOf<String, Int>()

        photos.forEach { photo ->
            val bucketName = photo.bucket ?: "Other"
            buckets[bucketName] = buckets.getOrDefault(bucketName, 0) + 1
        }

        bucketsCache = buckets
        return@withContext buckets
    }

    // Get categories with smart caching
    suspend fun getCategories(useCache: Boolean = true): Map<String, Pair<String, Int>> = withContext(Dispatchers.IO) {
        if (useCache && categoriesCache != null) {
            return@withContext categoriesCache!!
        }

        val photos = getPhotos(useCache)
        val categories = mutableMapOf<String, Pair<String, Int>>()

        // Auto-detect common categories with minimum count threshold
        val cameraCount = photos.count { it.isCamera() }
        val screenshotCount = photos.count { it.isScreenshot() }
        val whatsappCount = photos.count { it.isWhatsApp() }
        val downloadsCount = photos.count { it.isDownloads() }
        val socialCount = photos.count { it.isSocialMedia() }

        if (cameraCount > 0) categories["camera"] = Pair("Camera", cameraCount)
        if (screenshotCount > 0) categories["screenshots"] = Pair("Screenshots", screenshotCount)
        if (whatsappCount > 0) categories["whatsapp"] = Pair("WhatsApp", whatsappCount)
        if (downloadsCount > 0) categories["downloads"] = Pair("Downloads", downloadsCount)
        if (socialCount > 0) categories["social"] = Pair("Social Media", socialCount)

        categoriesCache = categories
        return@withContext categories
    }

    // Get single photo by ID
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
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.RELATIVE_PATH
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
                val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
                val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)

                val uri = ContentUris.withAppendedId(collection, id)
                Photo(
                    id = id.toString(),
                    uri = uri,
                    displayName = cursor.getStringOrNull(nameColumn),
                    dateTaken = cursor.getLongOrNull(dateColumn),
                    width = cursor.getIntOrNull(widthColumn),
                    height = cursor.getIntOrNull(heightColumn),
                    mimeType = cursor.getStringOrNull(mimeColumn),
                    bucket = cursor.getStringOrNull(bucketColumn),
                    bucketId = cursor.getStringOrNull(bucketIdColumn),
                    relativePath = cursor.getStringOrNull(pathColumn)
                )
            } else null
        }
    }

    // Clear caches (call when photos might have changed)
    fun clearCache() {
        categoriesCache = null
        bucketsCache = null
        allPhotosCache = null
        ThumbnailCache.clear()
    }

    // Existing functions (delete, moveToVault, etc.)
    suspend fun deletePhoto(uri: android.net.Uri) = withContext(Dispatchers.IO) {
        try {
            val deleted = context.contentResolver.delete(uri, null, null)
            if (deleted == 0) {
                throw IOException("Failed to delete photo")
            }
            clearCache() // Invalidate cache after deletion
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
            clearCache() // Invalidate cache after moving
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

    // Advanced deletion with options
suspend fun deletePhotoAdvanced(
    photoId: String,
    useSecureDelete: Boolean = false,
    useRecycleBin: Boolean = false
): Pair<Boolean, String> {
    return if (useRecycleBin) {
        DeleteUtils.moveToRecycleBin(context, photoId)
    } else {
        DeleteUtils.deletePhoto(context, photoId, useSecureDelete)
    }.also { result ->
        // Clear cache if deletion was successful
        if (result.first) {
            clearCache()
            notifyMediaChanged()
        }
    }
}

// Batch deletion
suspend fun deleteMultiplePhotosAdvanced(
    photoIds: List<String>,
    useSecureDelete: Boolean = false,
    useRecycleBin: Boolean = false
): Pair<Int, String> {
    return if (useRecycleBin) {
        // For recycle bin, we need to process individually
        var successCount = 0
        var lastError = ""

        photoIds.forEach { photoId ->
            val (success, message) = DeleteUtils.moveToRecycleBin(context, photoId)
            if (success) {
                successCount++
            } else {
                lastError = message
            }
        }

        Pair(successCount, if (successCount == photoIds.size) {
            "All photos moved to recycle bin"
        } else {
            "$successCount/${photoIds.size} photos moved to recycle bin. Last error: $lastError"
        })
    } else {
        DeleteUtils.deleteMultiplePhotos(context, photoIds, useSecureDelete)
    }.also { result ->
        // Clear cache if any deletions were successful
        if (result.first > 0) {
            clearCache()
            notifyMediaChanged()
        }
    }
}

// Recycle bin management
fun getRecycleBinContents(): List<File> {
    return DeleteUtils.getRecycleBinContents(context)
}

suspend fun restoreFromRecycleBin(file: File, originalPath: String? = null): Pair<Boolean, String> {
    return DeleteUtils.restoreFromRecycleBin(context, file, originalPath).also { result ->
        if (result.first) {
            clearCache()
            notifyMediaChanged()
        }
    }
}

suspend fun emptyRecycleBin(): Pair<Boolean, String> {
    return DeleteUtils.emptyRecycleBin(context)
}
}