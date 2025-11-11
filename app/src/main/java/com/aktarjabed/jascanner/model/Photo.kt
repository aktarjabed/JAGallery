package com.aktarjabed.jascanner.model

import android.net.Uri

data class Photo(
    val id: String,
    val uri: Uri,
    val displayName: String?,
    val dateTaken: Long?,
    val width: Int?,
    val height: Int?,
    val mimeType: String?,
    val bucket: String?
)
