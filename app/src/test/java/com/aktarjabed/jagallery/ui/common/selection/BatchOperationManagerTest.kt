package com.aktarjabed.jagallery.ui.common.selection

import android.app.Activity
import com.aktarjabed.jagallery.data.model.DeleteRequestChunk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class BatchOperationManagerTest {

    private lateinit var manager: BatchOperationManager

    @Before
    fun setup() {
        manager = BatchOperationManager()
    }

    private fun createDummyChunk(ids: List<String>): DeleteRequestChunk {
        return DeleteRequestChunk(
            ids = ids,
            uris = emptyList(), // URI list doesn't matter for state testing
            pendingIntent = mock()
        )
    }

    @Test
    fun `test empty selection creates Completed state`() = runTest {
        manager.startBatch(emptyList(), "TEST_TAG")
        val state = manager.batchState.value as BatchProcessorState.Completed
        assertTrue(state.succeededIds.isEmpty())
        assertTrue(state.failedIds.isEmpty())
        assertFalse(state.cancelled)
    }

    @Test
    fun `test single chunk success`() = runTest {
        val chunk1 = createDummyChunk(listOf("id1", "id2"))
        manager.startBatch(listOf(chunk1), "TEST_TAG")

        assertTrue(manager.batchState.value is BatchProcessorState.Running)

        manager.onBatchChunkResult(Activity.RESULT_OK)

        val state = manager.batchState.value as BatchProcessorState.Completed
        assertEquals(listOf("id1", "id2"), state.succeededIds)
        assertTrue(state.failedIds.isEmpty())
        assertFalse(state.cancelled)
        assertEquals("TEST_TAG", state.tag)
    }

    @Test
    fun `test multiple chunks all succeed`() = runTest {
        val chunk1 = createDummyChunk(listOf("id1", "id2"))
        val chunk2 = createDummyChunk(listOf("id3", "id4"))
        val chunk3 = createDummyChunk(listOf("id5", "id6"))

        manager.startBatch(listOf(chunk1, chunk2, chunk3), "TEST_TAG")

        manager.onBatchChunkResult(Activity.RESULT_OK)
        val runningState = manager.batchState.value as BatchProcessorState.Running
        assertEquals(1, runningState.currentIndex)
        assertEquals(listOf("id1", "id2"), runningState.processedIds)

        manager.onBatchChunkResult(Activity.RESULT_OK)
        manager.onBatchChunkResult(Activity.RESULT_OK)

        val completedState = manager.batchState.value as BatchProcessorState.Completed
        assertEquals(listOf("id1", "id2", "id3", "id4", "id5", "id6"), completedState.succeededIds)
        assertTrue(completedState.failedIds.isEmpty())
        assertFalse(completedState.cancelled)
    }

    @Test
    fun `test chunk cancelled halts further processing and preserves previous success`() = runTest {
        val chunk1 = createDummyChunk(listOf("id1", "id2"))
        val chunk2 = createDummyChunk(listOf("id3", "id4"))
        val chunk3 = createDummyChunk(listOf("id5", "id6"))

        manager.startBatch(listOf(chunk1, chunk2, chunk3), "TEST_TAG")

        manager.onBatchChunkResult(Activity.RESULT_OK) // Chunk 1 succeeds
        manager.onBatchChunkResult(Activity.RESULT_CANCELED) // Chunk 2 cancelled

        val completedState = manager.batchState.value as BatchProcessorState.Completed
        assertEquals(listOf("id1", "id2"), completedState.succeededIds)

        // Chunk 2 and Chunk 3 are considered failed since the batch was aborted midway
        assertEquals(listOf("id3", "id4", "id5", "id6"), completedState.failedIds)
        assertTrue(completedState.cancelled)
    }

    @Test
    fun `test manager clearState resets to Idle`() = runTest {
        val chunk1 = createDummyChunk(listOf("id1"))
        manager.startBatch(listOf(chunk1), "TEST_TAG")
        manager.onBatchChunkResult(Activity.RESULT_OK)

        assertTrue(manager.batchState.value is BatchProcessorState.Completed)

        manager.clearState()
        assertTrue(manager.batchState.value is BatchProcessorState.Idle)
    }
}
