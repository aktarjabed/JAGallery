package com.aktarjabed.core.data.model

import android.net.Uri

data class MediaItem(
    val id: Long,
    val displayName: String,
    val dateTaken: Long,
    val bucket: String,
    val uri: Uri
)