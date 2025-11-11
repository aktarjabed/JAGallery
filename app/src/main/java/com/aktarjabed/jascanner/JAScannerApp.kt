package com.aktarjabed.jascanner

import android.app.Application
import com.aktarjabed.jascanner.repository.MediaRepository
import com.aktarjabed.jascanner.workers.WorkManagerInitializer
import androidx.startup.AppInitializer
import com.aktarjabed.jascanner.utils.DeleteUtils

class JAScannerApp : Application() {

    val mediaRepository: MediaRepository by lazy {
        MediaRepository(this).also {
            it.registerContentObserver()
            // Trigger immediate cleanup check on app start
            cleanupExpiredFilesOnStart()
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize WorkManager (will be called automatically via Startup)
        AppInitializer.getInstance(this).initializeComponent(WorkManagerInitializer::class.java)

        // Additional setup
        setupNotificationChannels()
    }

    override fun onTerminate() {
        super.onTerminate()
        mediaRepository.unregisterContentObserver()
    }

    private fun setupNotificationChannels() {
        // Setup notification channels for WorkManager (if needed)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            // You can add notification channels here for WorkManager progress
        }
    }

    private fun cleanupExpiredFilesOnStart() {
        // Run a quick cleanup check on app start
        Thread {
            try {
                val expiredFiles = DeleteUtils.getExpiredRecycleBinFiles(this)
                if (expiredFiles.isNotEmpty()) {
                    DeleteUtils.cleanupExpiredFilesNow(this)
                }
            } catch (e: Exception) {
                // Silent fail - regular worker will handle it
            }
        }.start()
    }
}
