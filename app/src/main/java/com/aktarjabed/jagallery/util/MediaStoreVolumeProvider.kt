package com.aktarjabed.jagallery.util

import android.content.Context
import android.provider.MediaStore

interface MediaStoreVolumeProvider {
    fun getExternalVolumeNames(context: Context): Set<String>
}

object AndroidMediaStoreVolumeProvider : MediaStoreVolumeProvider {
    override fun getExternalVolumeNames(context: Context): Set<String> {
        return MediaStore.getExternalVolumeNames(context)
    }
}
