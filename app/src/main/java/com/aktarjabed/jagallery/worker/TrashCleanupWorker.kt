package com.aktarjabed.jagallery.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.aktarjabed.jagallery.data.repository.MediaRepository
import com.aktarjabed.jagallery.domain.SettingsRepository
import com.aktarjabed.jagallery.domain.TrashRetentionPolicy
import com.aktarjabed.jagallery.util.FileUtils
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class TrashCleanupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val mediaRepository: MediaRepository,
    private val settingsRepository: SettingsRepository
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val policy = settingsRepository.getTrashRetentionPolicy()
            if (policy == TrashRetentionPolicy.NEVER) {
                return@withContext Result.success()
            }

            val cutoff = System.currentTimeMillis() - (policy.days * 24L * 60L * 60L * 1000L)

            val trashedMedia = mediaRepository.mediaDao.getAllTrashMediaSync()
            val expired = trashedMedia.filter { it.dateTrashed < cutoff }

            if (expired.isNotEmpty()) {
                val uris = expired.map { android.net.Uri.parse(it.uri) }
                val deleteResult = FileUtils.deleteMediaItems(applicationContext.contentResolver, uris)
                if (deleteResult.successfulUris.isNotEmpty()) {
                    mediaRepository.mediaDao.removeTrashMediaBatch(deleteResult.successfulUris.map { it.toString() })
                }

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
