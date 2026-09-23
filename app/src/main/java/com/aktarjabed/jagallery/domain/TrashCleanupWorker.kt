package com.aktarjabed.jagallery.domain

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aktarjabed.jagallery.data.local.MediaDatabase
import com.aktarjabed.jagallery.util.FileUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class TrashCleanupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val database: MediaDatabase,
    private val settings: SettingsRepository
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
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

                // If not completely successful, retry the worker for remaining failures.
                if (!deleteResult.isFullySuccessful) {
                    return@withContext Result.retry()
                }
            }

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
