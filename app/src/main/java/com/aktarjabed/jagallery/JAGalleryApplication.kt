package com.aktarjabed.jagallery

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.aktarjabed.core.sync.workers.SyncWorkerFactory

class JAGalleryApplication : Application(), Configuration.Provider {
    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .setWorkerFactory(SyncWorkerFactory(this))
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        WorkManager.initialize(this, workManagerConfiguration)
    }
}