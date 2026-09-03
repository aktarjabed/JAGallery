package com.aktarjabed.jagallery.ui.common.selection

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.aktarjabed.jagallery.data.model.DeleteRequestChunk

sealed interface BatchProcessorState {
    data object Idle : BatchProcessorState
    data class Running(
        val chunks: List<DeleteRequestChunk>,
        val currentIndex: Int,
        val processedIds: List<String>,
        val currentTag: String
    ) : BatchProcessorState
    data class Completed(
        val succeededIds: List<String>,
        val failedIds: List<String>,
        val cancelled: Boolean,
        val tag: String
    ) : BatchProcessorState
}

class BatchOperationManager {
    private val _batchState = MutableStateFlow<BatchProcessorState>(BatchProcessorState.Idle)
    val batchState = _batchState.asStateFlow()

    fun startBatch(chunks: List<DeleteRequestChunk>, tag: String) {
        if (chunks.isEmpty()) {
            _batchState.value = BatchProcessorState.Completed(emptyList(), emptyList(), false, tag)
        } else {
            _batchState.value = BatchProcessorState.Running(chunks, 0, emptyList(), tag)
        }
    }

    fun onBatchChunkResult(resultCode: Int) {
        val currentState = _batchState.value as? BatchProcessorState.Running ?: return

        if (resultCode == Activity.RESULT_OK) {
            val confirmedIds = currentState.chunks[currentState.currentIndex].ids
            val newProcessed = currentState.processedIds + confirmedIds
            val nextIndex = currentState.currentIndex + 1

            if (nextIndex < currentState.chunks.size) {
                _batchState.value = currentState.copy(
                    currentIndex = nextIndex,
                    processedIds = newProcessed
                )
            } else {
                _batchState.value = BatchProcessorState.Completed(
                    succeededIds = newProcessed,
                    failedIds = emptyList(),
                    cancelled = false,
                    tag = currentState.currentTag
                )
            }
        } else {
            val failedIds = currentState.chunks.subList(currentState.currentIndex, currentState.chunks.size).flatMap { it.ids }
            _batchState.value = BatchProcessorState.Completed(
                succeededIds = currentState.processedIds,
                failedIds = failedIds,
                cancelled = true,
                tag = currentState.currentTag
            )
        }
    }

    fun clearState() {
        _batchState.value = BatchProcessorState.Idle
    }
}

@Composable
fun BatchOperationObserver(
    batchState: BatchProcessorState,
    onChunkResult: (Int) -> Unit,
    onComplete: (BatchProcessorState.Completed) -> Unit
) {
    val currentOnComplete by rememberUpdatedState(onComplete)
    val currentOnChunkResult by rememberUpdatedState(onChunkResult)

    var lastLaunchedIndex by remember { mutableIntStateOf(-1) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        currentOnChunkResult(result.resultCode)
    }

    LaunchedEffect(batchState) {
        when (batchState) {
            is BatchProcessorState.Running -> {
                if (batchState.currentIndex < batchState.chunks.size && lastLaunchedIndex != batchState.currentIndex) {
                    lastLaunchedIndex = batchState.currentIndex
                    val intentSender = batchState.chunks[batchState.currentIndex].pendingIntent.intentSender
                    launcher.launch(IntentSenderRequest.Builder(intentSender).build())
                }
            }
            is BatchProcessorState.Completed -> {
                lastLaunchedIndex = -1
                currentOnComplete(batchState)
            }
            is BatchProcessorState.Idle -> {
                lastLaunchedIndex = -1
                // Do nothing
            }
        }
    }
}
