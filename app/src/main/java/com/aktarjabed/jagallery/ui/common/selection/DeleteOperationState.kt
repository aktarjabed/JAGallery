package com.aktarjabed.jagallery.ui.common.selection

import com.aktarjabed.jagallery.data.model.PendingDeleteBatch

import android.app.PendingIntent

sealed interface DeleteOperationState {
    data object Idle : DeleteOperationState
    data class Confirming(val batch: PendingDeleteBatch) : DeleteOperationState
    data class SystemConfirmation(
        val batch: PendingDeleteBatch,
        val pendingIntents: List<com.aktarjabed.jagallery.data.model.DeleteRequestChunk> = emptyList(),
        val currentIndex: Int = 0,
        val processedIds: List<String> = emptyList()
    ) : DeleteOperationState
    data class Failed(val batch: PendingDeleteBatch, val message: String? = null) : DeleteOperationState
}
