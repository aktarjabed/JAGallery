package com.aktarjabed.jagallery.ui.common.selection

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.aktarjabed.jagallery.data.model.DeleteRequestChunk

data class BatchOperationResult(
    val succeededIds: List<String>,
    val failedIds: List<String>,
    val cancelled: Boolean
)

class PendingIntentBatchProcessor(
    private val onStartBatch: (List<DeleteRequestChunk>) -> Unit
) {
    fun processBatch(chunks: List<DeleteRequestChunk>) {
        onStartBatch(chunks)
    }
}

@Composable
fun rememberPendingIntentBatchProcessor(
    onComplete: (BatchOperationResult) -> Unit
): PendingIntentBatchProcessor {
    var state by remember { mutableStateOf<BatchProcessorState?>(null) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val currentState = state ?: return@rememberLauncherForActivityResult
        if (result.resultCode == Activity.RESULT_OK) {
            val confirmedIds = currentState.chunks[currentState.currentIndex].ids
            val newProcessed = currentState.processedIds + confirmedIds
            val nextIndex = currentState.currentIndex + 1
            if (nextIndex < currentState.chunks.size) {
                state = currentState.copy(currentIndex = nextIndex, processedIds = newProcessed)
            } else {
                onComplete(BatchOperationResult(newProcessed, emptyList(), false))
                state = null
            }
        } else {
            val failedIds = currentState.chunks.subList(currentState.currentIndex, currentState.chunks.size).flatMap { it.ids }
            onComplete(BatchOperationResult(currentState.processedIds, failedIds, true))
            state = null
        }
    }

    LaunchedEffect(state) {
        val s = state ?: return@LaunchedEffect
        if (s.currentIndex < s.chunks.size) {
            launcher.launch(IntentSenderRequest.Builder(s.chunks[s.currentIndex].pendingIntent.intentSender).build())
        }
    }

    return remember(launcher) {
        PendingIntentBatchProcessor(
            onStartBatch = { chunks ->
                if (chunks.isNotEmpty()) {
                    state = BatchProcessorState(chunks, 0, emptyList())
                } else {
                    onComplete(BatchOperationResult(emptyList(), emptyList(), false))
                }
            }
        )
    }
}

private data class BatchProcessorState(
    val chunks: List<DeleteRequestChunk>,
    val currentIndex: Int,
    val processedIds: List<String>
)
