package com.aktarjabed.jascanner.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.aktarjabed.jascanner.utils.DeleteUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker that automatically cleans up Recycle Bin files older than 30 days
 * Runs daily when device is idle to preserve battery
 */
class RecycleBinCleanupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "RecycleBinCleanupWorker"
        const val WORK_NAME = "recycle_bin_cleanup"
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            Log.i(TAG, "Starting Recycle Bin cleanup...")

            val (deletedCount, message) = DeleteUtils.cleanupExpiredFilesNow(applicationContext)

            val resultData = Data.Builder()
                .putInt("deleted_count", deletedCount)
                .putString("message", message)
                .build()

            Log.i(TAG, "Recycle Bin cleanup completed: $message")

            if (deletedCount >= 0) {
                Result.success(resultData)
            } else {
                Result.retry()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Recycle Bin cleanup failed", e)
            Result.failure()
        }
    }
}
