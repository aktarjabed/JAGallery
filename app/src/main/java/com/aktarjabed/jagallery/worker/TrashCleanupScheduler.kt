package com.aktarjabed.jagallery.worker

import android.content.Context
import android.provider.MediaStore
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class TrashAutoPurgeWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        runCatching {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                val nowSeconds = System.currentTimeMillis() / 1000L
                val selection = "${MediaStore.MediaColumns.IS_TRASHED} = 1 AND " +
                    "${MediaStore.MediaColumns.DATE_EXPIRES} > 0 AND " +
                    "${MediaStore.MediaColumns.DATE_EXPIRES} <= ?"
                val args = arrayOf(nowSeconds.toString())
                val volumeUri = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL)
                applicationContext.contentResolver.delete(volumeUri, selection, args)
            }
            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "jagallery_30day_trash_autopurge"

        fun schedulePeriodicPurge(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiresBatteryNotLow(true)
                .build()

            val request = PeriodicWorkRequestBuilder<TrashAutoPurgeWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}

object TrashCleanupScheduler {
    fun schedulePeriodicPurge(context: Context) {
        TrashAutoPurgeWorker.schedulePeriodicPurge(context)
    }
}
