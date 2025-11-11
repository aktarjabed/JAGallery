package com.aktarjabed.jascanner.repository

import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Observes MediaStore changes and notifies when photos are added, modified, or deleted
 * Provides real-time updates to the gallery
 */
class PhotoContentObserver(
    private val onMediaChanged: () -> Unit
) : ContentObserver(Handler(Looper.getMainLooper())) {

    private val scope = CoroutineScope(Dispatchers.Main)
    private var debounceJob: Job? = null

    // MediaStore URIs to observe
    private val observedUris = arrayOf(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
    )

    override fun onChange(selfChange: Boolean) {
        onChange(selfChange, null)
    }

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        // Debounce rapid changes (like burst photos)
        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(300) // Wait 300ms for multiple changes to settle
            onMediaChanged()
        }
    }

    fun getObservedUris(): Array<Uri> = observedUris
}