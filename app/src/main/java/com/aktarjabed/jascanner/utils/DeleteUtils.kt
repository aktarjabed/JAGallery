package com.aktarjabed.jascanner.utils

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.aktarjabed.jascanner.repository.RecoverableDeleteException
import java.io.File

/**
 * Advanced deletion utilities with proper storage handling
 * Supports secure deletion, batch operations, and proper permissions
 */
object DeleteUtils {

    private const val TAG = "DeleteUtils"
    const val CLEANUP_THRESHOLD_DAYS = 30L
    const val MILLIS_PER_DAY = 24 * 60 * 60 * 1000L

    /**
     * Delete a single photo from MediaStore and storage
     * @return Pair<Boolean, String> (success, message)
     */
    suspend fun deletePhoto(
        context: Context,
        photoId: String,
        useSecureDelete: Boolean = false
    ): Pair<Boolean, String> {
        return try {
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            val uri = ContentUris.withAppendedId(collection, photoId.toLong())

            // Get file path for secure deletion (Android 10+)
            val filePath = if (useSecureDelete && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                getFilePathFromUri(context, uri)
            } else null

            // Delete from MediaStore
            val deleted = context.contentResolver.delete(uri, null, null)

            if (deleted > 0) {
                // Perform secure deletion if requested
                if (useSecureDelete && filePath != null) {
                    secureDeleteFile(filePath)
                }
                Pair(true, "Photo deleted successfully")
            } else {
                Pair(false, "Failed to delete photo from database")
            }
        } catch (e: SecurityException) {
            Pair(false, "Permission denied: ${e.message}")
        } catch (e: android.content.RecoverableSecurityException) {
            throw RecoverableDeleteException(e.userAction.actionIntent.intentSender)
        } catch (e: Exception) {
            Log.e(TAG, "Delete failed", e)
            Pair(false, "Delete failed: ${e.message ?: "Unknown error"}")
        }
    }

    /**
     * Delete multiple photos in batch
     */
    suspend fun deleteMultiplePhotos(
        context: Context,
        photoIds: List<String>,
        useSecureDelete: Boolean = false
    ): Pair<Int, String> {
        var successCount = 0
        var lastError = ""

        photoIds.forEach { photoId ->
            val (success, message) = deletePhoto(context, photoId, useSecureDelete)
            if (success) {
                successCount++
            } else {
                lastError = message
            }
        }

        return when {
            successCount == photoIds.size -> Pair(successCount, "All photos deleted successfully")
            successCount > 0 -> Pair(successCount, "$successCount/${photoIds.size} photos deleted. Last error: $lastError")
            else -> Pair(0, "No photos deleted. Error: $lastError")
        }
    }

    /**
     * Move photo to recycle bin instead of permanent deletion
     */
    suspend fun moveToRecycleBin(
        context: Context,
        photoId: String
    ): Pair<Boolean, String> {
        return try {
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            val uri = ContentUris.withAppendedId(collection, photoId.toLong())

            // Create recycle bin directory if it doesn't exist
            val recycleBinDir = File(context.filesDir, "RecycleBin").apply { mkdirs() }
            val sourceFile = getFileFromUri(context, uri)

            if (sourceFile != null && sourceFile.exists()) {
                val destination = File(recycleBinDir, "${System.currentTimeMillis()}_${sourceFile.name}")
                if (sourceFile.renameTo(destination)) {
                    // Remove from MediaStore
                    context.contentResolver.delete(uri, null, null)
                    Pair(true, "Photo moved to recycle bin")
                } else {
                    Pair(false, "Failed to move photo to recycle bin")
                }
            } else {
                Pair(false, "Source file not found")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Move to recycle bin failed", e)
            Pair(false, "Failed to move to recycle bin: ${e.message}")
        }
    }

    /**
     * Secure deletion by overwriting file before deletion (basic implementation)
     */
    private fun secureDeleteFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                // Overwrite with zeros (basic secure deletion)
                file.writeBytes(ByteArray(file.length().toInt()))
                file.delete()
            } else {
                true // File already doesn't exist
            }
        } catch (e: Exception) {
            Log.e(TAG, "Secure deletion failed", e)
            false
        }
    }

    /**
     * Get file path from MediaStore URI (Android 10+) - Scoped Storage Safe
     */
    private fun getFilePathFromUri(context: Context, uri: Uri): String? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Use RELATIVE_PATH and DISPLAY_NAME for Android 10+
                context.contentResolver.query(
                    uri,
                    arrayOf(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        MediaStore.Images.Media.DISPLAY_NAME
                    ),
                    null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val relativePath = cursor.getString(
                            cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
                        )
                        val displayName = cursor.getString(
                            cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                        )

                        if (relativePath != null && displayName != null) {
                            // Build absolute path from public directories
                            val publicDir = when {
                                relativePath.contains("DCIM", ignoreCase = true) ->
                                    android.os.Environment.DIRECTORY_DCIM
                                relativePath.contains("Pictures", ignoreCase = true) ->
                                    android.os.Environment.DIRECTORY_PICTURES
                                relativePath.contains("Download", ignoreCase = true) ->
                                    android.os.Environment.DIRECTORY_DOWNLOADS
                                else -> android.os.Environment.DIRECTORY_PICTURES
                            }

                            val storageDir = android.os.Environment.getExternalStoragePublicDirectory(publicDir)
                            File(storageDir, "$relativePath$displayName").absolutePath
                        } else {
                            null
                        }
                    } else null
                }
            } else {
                // Legacy method for Android 9 and below
                context.contentResolver.query(
                    uri,
                    arrayOf(MediaStore.Images.Media.DATA),
                    null, null, null
                )?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))
                    } else null
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get file path from URI", e)
            null
        }
    }

    /**
     * Get File object from URI
     */
    private fun getFileFromUri(context: Context, uri: Uri): File? {
        return getFilePathFromUri(context, uri)?.let { File(it) }
    }

    /**
     * Get recycle bin contents
     */
    fun getRecycleBinContents(context: Context): List<File> {
        val recycleBinDir = File(context.filesDir, "RecycleBin")
        return if (recycleBinDir.exists() && recycleBinDir.isDirectory) {
            recycleBinDir.listFiles()?.toList() ?: emptyList()
        } else {
            emptyList()
        }
    }

    /**
     * Get expired recycle bin files (older than 30 days)
     */
    fun getExpiredRecycleBinFiles(context: Context): List<File> {
        val recycleBinDir = File(context.filesDir, "RecycleBin")
        val thresholdTime = System.currentTimeMillis() - (CLEANUP_THRESHOLD_DAYS * MILLIS_PER_DAY)

        return if (recycleBinDir.exists() && recycleBinDir.isDirectory) {
            recycleBinDir.listFiles()?.filter { file ->
                file.lastModified() < thresholdTime
            } ?: emptyList()
        } else {
            emptyList()
        }
    }

    /**
     * Restore photo from recycle bin
     */
    suspend fun restoreFromRecycleBin(
        context: Context,
        file: File,
        originalPath: String? = null
    ): Pair<Boolean, String> {
        return try {
            val destination = if (originalPath != null) {
                File(originalPath)
            } else {
                // Try to determine original location from filename
                val fileName = file.name.substringAfter('_')
                File(
                    android.os.Environment.getExternalStoragePublicDirectory(
                        android.os.Environment.DIRECTORY_PICTURES
                    ),
                    fileName
                )
            }

            // Create parent directories if they don't exist
            destination.parentFile?.mkdirs()

            if (file.renameTo(destination)) {
                // Re-scan to add back to MediaStore
                val mediaScanIntent = android.content.Intent(android.content.Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                mediaScanIntent.data = Uri.fromFile(destination)
                context.sendBroadcast(mediaScanIntent)

                Pair(true, "Photo restored successfully")
            } else {
                Pair(false, "Failed to restore photo")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Restore from recycle bin failed", e)
            Pair(false, "Restore failed: ${e.message}")
        }
    }

    /**
     * Empty recycle bin
     */
    fun emptyRecycleBin(context: Context): Pair<Boolean, String> {
        return try {
            val recycleBinDir = File(context.filesDir, "RecycleBin")
            val files = recycleBinDir.listFiles()
            var deletedCount = 0

            files?.forEach { file ->
                if (file.delete()) {
                    deletedCount++
                }
            }

            Pair(true, "Recycle bin emptied ($deletedCount items deleted)")
        } catch (e: Exception) {
            Log.e(TAG, "Empty recycle bin failed", e)
            Pair(false, "Failed to empty recycle bin: ${e.message}")
        }
    }

    /**
     * Manual cleanup of expired files
     */
    fun cleanupExpiredFilesNow(context: Context): Pair<Int, String> {
        return try {
            val expiredFiles = getExpiredRecycleBinFiles(context)
            var deletedCount = 0

            expiredFiles.forEach { file ->
                if (file.delete()) {
                    deletedCount++
                }
            }

            Pair(deletedCount, "Cleaned up $deletedCount expired files")
        } catch (e: Exception) {
            Log.e(TAG, "Manual cleanup failed", e)
            Pair(0, "Cleanup failed: ${e.message}")
        }
    }
}
