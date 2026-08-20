package com.example.advancedgallery.util

import android.content.Context
import android.database.ContentObserver
import android.os.Build
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
        stopObserving()

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

        val contentResolver = context.contentResolver
        val obs = observer ?: return

        val targetUris = mutableListOf<android.net.Uri>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                val volumes = MediaStore.getExternalVolumeNames(context)
                for (volume in volumes) {
                    targetUris.add(MediaStore.Images.Media.getContentUri(volume))
                    targetUris.add(MediaStore.Video.Media.getContentUri(volume))
                }
            } catch (e: SecurityException) {
                Log.w("MediaStoreObserver", "SecurityException getting volume names", e)
            } catch (e: IllegalArgumentException) {
                Log.w("MediaStoreObserver", "IllegalArgumentException getting volume names", e)
            }
        }

        if (targetUris.isEmpty()) {
            targetUris.add(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            targetUris.add(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        }

        for (uri in targetUris) {
            try {
                contentResolver.registerContentObserver(uri, true, obs)
            } catch (e: SecurityException) {
                Log.w("MediaStoreObserver", "SecurityException registering observer for $uri", e)
            } catch (e: IllegalArgumentException) {
                Log.w("MediaStoreObserver", "IllegalArgumentException registering observer for $uri", e)
            }
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
