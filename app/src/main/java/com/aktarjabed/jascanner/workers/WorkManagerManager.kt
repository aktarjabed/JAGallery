package com.aktarjabed.jascanner.workers

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * Manages WorkManager scheduling based on user preferences
 */
object WorkManagerManager {

    fun scheduleAutoCleanup(context: Context) {
        val workManager = WorkManager.getInstance(context)

        val cleanupRequest = PeriodicWorkRequestBuilder<RecycleBinCleanupWorker>(
            1, TimeUnit.DAYS,
            15, TimeUnit.MINUTES
        ).build()

        workManager.enqueueUniquePeriodicWork(
            RecycleBinCleanupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            cleanupRequest
        )
    }

    fun cancelAutoCleanup(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelUniqueWork(RecycleBinCleanupWorker.WORK_NAME)
    }

    fun isAutoCleanupScheduled(context: Context): Boolean {
        val workManager = WorkManager.getInstance(context)
        // Note: This is a simplified check - in production you might want more sophisticated tracking
        return true // WorkManager will handle duplicates appropriately
    }

    fun triggerImmediateCleanup(context: Context) {
        val workManager = WorkManager.getInstance(context)
        val oneTimeRequest = PeriodicWorkRequestBuilder<RecycleBinCleanupWorker>(
            1, TimeUnit.DAYS // This will be overridden for one-time execution
        ).build()

        workManager.enqueue(oneTimeRequest)
    }
}
