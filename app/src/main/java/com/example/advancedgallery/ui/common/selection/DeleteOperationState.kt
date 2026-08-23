package com.example.advancedgallery.ui.common.selection

import com.example.advancedgallery.data.model.PendingDeleteBatch

import android.app.PendingIntent

sealed interface DeleteOperationState {
    data object Idle : DeleteOperationState
    data class Confirming(val batch: PendingDeleteBatch) : DeleteOperationState
    data class SystemConfirmation(
        val batch: PendingDeleteBatch,
        val pendingIntents: List<PendingIntent> = emptyList(),
        val currentIndex: Int = 0,
        val processedIds: List<String> = emptyList()
    ) : DeleteOperationState
    data class Failed(val batch: PendingDeleteBatch, val message: String? = null) : DeleteOperationState
}
