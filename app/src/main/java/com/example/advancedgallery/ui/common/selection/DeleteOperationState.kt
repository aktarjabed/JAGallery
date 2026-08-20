package com.example.advancedgallery.ui.common.selection

import com.example.advancedgallery.data.model.PendingDeleteBatch

sealed interface DeleteOperationState {
    data object Idle : DeleteOperationState
    data class Confirming(val batch: PendingDeleteBatch) : DeleteOperationState
    data class SystemConfirmation(val batch: PendingDeleteBatch) : DeleteOperationState
    data class Failed(val batch: PendingDeleteBatch, val message: String? = null) : DeleteOperationState
}
