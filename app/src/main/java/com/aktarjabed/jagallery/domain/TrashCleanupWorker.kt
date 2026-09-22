package com.aktarjabed.jagallery.domain

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aktarjabed.jagallery.data.local.MediaDatabase
import com.aktarjabed.jagallery.util.FileUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TrashCleanupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val database = androidx.room.Room.databaseBuilder(
                applicationContext,
                MediaDatabase::class.java,
                "gallery_database"
            ).build()
            val settings = SettingsRepository(applicationContext)

            val policy = settings.getTrashRetentionPolicy()
            if (policy == TrashRetentionPolicy.NEVER) {
                return@withContext Result.success()
            }

            val expiryMillis = System.currentTimeMillis() - (policy.days * 24L * 60L * 60L * 1000L)

            val trashedMedia = database.mediaDao().getAllTrashMediaSync()
            val expired = trashedMedia.filter { it.dateTrashed < expiryMillis }

            if (expired.isNotEmpty()) {
                val uris = expired.map { android.net.Uri.parse(it.uri) }
                val deleteResult = FileUtils.deleteMediaItems(applicationContext.contentResolver, uris)
                if (deleteResult.successfulUris.isNotEmpty()) {
                    database.mediaDao().removeTrashMediaBatch(deleteResult.successfulUris.map { it.toString() })
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
