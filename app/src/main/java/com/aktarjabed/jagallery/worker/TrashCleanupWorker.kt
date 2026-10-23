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
                val resolver = applicationContext.contentResolver
                val existingUris = mutableListOf<android.net.Uri>()

                for (item in expired) {
                    val uri = android.net.Uri.parse(item.uri)
                    try {
                        val cursor = resolver.query(
                            uri,
                            arrayOf(android.provider.MediaStore.MediaColumns._ID),
                            null,
                            null,
                            null
                        )
                        if (cursor != null) {
                            cursor.use {
                                if (it.moveToFirst()) {
                                    existingUris.add(uri)
                                }
                                // An empty result is ambiguous under scoped media access.
                                // Keep the tracking row instead of assuming the item vanished.
                            }
                        }
                    } catch (e: SecurityException) {
                        // A query denial is not evidence that the media was deleted.
                        android.util.Log.w("TrashCleanupWorker", "Cannot inspect tracked Trash item; retaining its record", e)
                    } catch (e: Exception) {
                        // Preserve the record when provider state cannot be confirmed.
                        android.util.Log.w("TrashCleanupWorker", "Cannot inspect tracked Trash item; retaining its record", e)
                    }
                }

                // Only remove tracking rows for entries the provider confirms were deleted.
                // Background work cannot present MediaStore's interactive delete-consent UI.
                if (existingUris.isNotEmpty()) {
                    try {
                        val deleteResult = FileUtils.deleteMediaItems(resolver, existingUris)
                        if (deleteResult.successfulUris.isNotEmpty()) {
                            mediaRepository.mediaDao.removeTrashMediaBatch(
                                deleteResult.successfulUris.map { it.toString() }
                            )
                        }
                        if (!deleteResult.isFullySuccessful && runAttemptCount < MAX_RETRIES_PER_RUN) {
                            return@withContext Result.retry()
                        }
                    } catch (e: kotlinx.coroutines.CancellationException) {
                        throw e
                    } catch (e: SecurityException) {
                        android.util.Log.w("TrashCleanupWorker", "Deletion needs permission unavailable to background worker; retaining records", e)
                    } catch (e: Exception) {
                        android.util.Log.e("TrashCleanupWorker", "Trash purge attempt failed; retaining records", e)
                        if (runAttemptCount < MAX_RETRIES_PER_RUN) {
                            return@withContext Result.retry()
                        }
                    }
                }
            }

            Result.success()
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: SecurityException) {
            android.util.Log.e("TrashCleanupWorker", "SecurityException, skipping retry", e)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val MAX_RETRIES_PER_RUN = 2

        fun schedulePeriodicPurge(context: Context) {
            TrashCleanupScheduler.schedulePeriodicPurge(context)
        }
    }
}
