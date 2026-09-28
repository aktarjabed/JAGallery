package com.aktarjabed.jagallery.data.model

import android.app.PendingIntent

data class DeleteRequestChunk(
    val ids: List<String>,
    val uris: List<android.net.Uri>,
    val pendingIntent: PendingIntent
)
