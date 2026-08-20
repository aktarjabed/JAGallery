package com.example.advancedgallery.data.model

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PendingDeleteBatch(
    val ids: List<String>,
    val uris: List<Uri>
) : Parcelable {
    val count: Int get() = uris.size
}
