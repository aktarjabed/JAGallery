package com.example.advancedgallery.util

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MediaStoreObserverManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onMediaStoreChanged: () -> Unit,
    private val debounceMillis: Long = 400L
) {
    private var debounceJob: Job? = null
    private var observer: ContentObserver? = null

    fun startObserving() {
        if (observer != null) return

        observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                debounceJob?.cancel()
                debounceJob = scope.launch {
                    delay(debounceMillis)
                    onMediaStoreChanged()
                }
            }
        }

        try {
            val contentResolver = context.contentResolver
            observer?.let { obs ->
                contentResolver.registerContentObserver(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    true,
                    obs
                )
                contentResolver.registerContentObserver(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    true,
                    obs
                )
            }
        } catch (e: SecurityException) {
            Log.w("MediaStoreObserver", "SecurityException registering ContentObserver", e)
        }
    }

    fun stopObserving() {
        debounceJob?.cancel()
        debounceJob = null
        observer?.let { obs ->
            try {
                context.contentResolver.unregisterContentObserver(obs)
            } catch (e: Exception) {
                // ignore
            }
        }
        observer = null
    }
}
