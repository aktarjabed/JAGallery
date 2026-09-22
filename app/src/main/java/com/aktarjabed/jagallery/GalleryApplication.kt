package com.aktarjabed.jagallery

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class GalleryApplication : Application(), ImageLoaderFactory {

    @Inject
    lateinit var imageLoader: ImageLoader

    override fun onCreate() {
        super.onCreate()

        val workRequest = androidx.work.PeriodicWorkRequestBuilder<com.aktarjabed.jagallery.domain.TrashCleanupWorker>(1, java.util.concurrent.TimeUnit.DAYS)
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
