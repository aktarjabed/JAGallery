package com.aktarjabed.jascanner.workers

import android.content.Context
import androidx.startup.Initializer
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeUnit

class WorkManagerInitializer : Initializer<WorkManager> {

    override fun create(context: Context): WorkManager {
        val workManager = WorkManager.getInstance(context)
        setupRecycleBinCleanup(context, workManager)
        return workManager
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()

    private fun setupRecycleBinCleanup(context: Context, workManager: WorkManager) {
        // Check if auto-cleanup is enabled before scheduling
        val settingsDataStore = com.aktarjabed.jascanner.datastore.SettingsDataStore(context)

        runBlocking {
            val isAutoCleanupEnabled = settingsDataStore.autoCleanupEnabled.first()

            if (isAutoCleanupEnabled) {
                scheduleCleanupWorker(workManager)
            } else {
                cancelCleanupWorker(workManager)
            }
        }
    }

    private fun scheduleCleanupWorker(workManager: WorkManager) {
        val cleanupRequest = PeriodicWorkRequestBuilder<RecycleBinCleanupWorker>(
            1, TimeUnit.DAYS, // Repeat interval
            15, TimeUnit.MINUTES // Flexible execution window
        ).build()

        workManager.enqueueUniquePeriodicWork(
            RecycleBinCleanupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE, // Update if already exists
            cleanupRequest
        )
    }

    private fun cancelCleanupWorker(workManager: WorkManager) {
        workManager.cancelUniqueWork(RecycleBinCleanupWorker.WORK_NAME)
    }
}
