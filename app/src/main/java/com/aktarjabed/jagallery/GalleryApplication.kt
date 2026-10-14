package com.aktarjabed.jagallery

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration

@HiltAndroidApp
class GalleryApplication : Application(), ImageLoaderFactory, Configuration.Provider {

    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()

        Thread {
            try {
                val tempDir = java.io.File(cacheDir, "vault_temp")
                if (tempDir.exists() && tempDir.isDirectory) {
                    tempDir.listFiles()?.forEach { it.delete() }
                }
            } catch (e: Exception) {
                // Ignore cleanup failures
            }
        }.start()

        val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.aktarjabed.jagallery.worker.TrashCleanupWorker>(1, java.util.concurrent.TimeUnit.DAYS)
            .build()
        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "TrashCleanup",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    override fun newImageLoader(): ImageLoader {
        return imageLoader
    }
}
