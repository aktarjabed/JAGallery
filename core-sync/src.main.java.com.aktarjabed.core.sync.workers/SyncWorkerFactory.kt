package com.aktarjabed.core.sync.workers

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters

class SyncWorkerFactory(
    private val context: Context
) : WorkerFactory() {
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            IndexingWorker::class.java.name -> {
                IndexingWorker(context, workerParameters)
            }
            // Add other workers here
            else -> null
        }
    }
}