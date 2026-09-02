package com.aktarjabed.jagallery.ui.screens.duplicates

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.aktarjabed.jagallery.R
import com.aktarjabed.jagallery.util.DuplicateGroup
import com.aktarjabed.jagallery.util.FileUtils

private data class DeleteState(
    val pendingIntents: List<com.aktarjabed.jagallery.data.model.DeleteRequestChunk>,
    val deletedIds: List<String>,
    val currentIndex: Int = 0
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuplicateScreen(
    onBack: () -> Unit,
    viewModel: DuplicateViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selections by viewModel.selections.collectAsStateWithLifecycle()

    var deleteState by remember { mutableStateOf<DeleteState?>(null) }

    val batchState by viewModel.batchManager.batchState.collectAsStateWithLifecycle()

    com.aktarjabed.jagallery.ui.common.selection.BatchOperationObserver(
        batchState = batchState,
        onChunkResult = { resultCode -> viewModel.batchManager.onBatchChunkResult(resultCode) },
        onComplete = { result ->
            if (result.tag == "DUPLICATE_DELETE") {
                if (result.succeededIds.isNotEmpty()) {
                    viewModel.onDeletePermissionResult(true, result.succeededIds)
                } else if (result.cancelled) {
                    viewModel.onDeletePermissionResult(false, emptyList())
                }
                deleteState = null
            }
            viewModel.batchManager.clearState()
        }
    )

    LaunchedEffect(deleteState) {
        val state = deleteState ?: return@LaunchedEffect
        if (state.currentIndex == 0 && state.pendingIntents.isNotEmpty()) {
            viewModel.batchManager.startBatch(state.pendingIntents, "DUPLICATE_DELETE")
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadDuplicates()
    }

    LaunchedEffect(viewModel.operationEvent) {
        viewModel.operationEvent.collect { event ->
            when (event) {
                is com.aktarjabed.jagallery.ui.common.OperationEvent.Error -> {
                    android.widget.Toast.makeText(context, event.message, android.widget.Toast.LENGTH_SHORT).show()
                }
                is com.aktarjabed.jagallery.ui.common.OperationEvent.Success -> {
                    android.widget.Toast.makeText(context, event.message, android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val totalSelected = selections.values.sumOf { it.size }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.duplicates_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (uiState is DuplicateViewModel.UiState.Success) {
                        TextButton(onClick = { viewModel.loadDuplicates() }) {
                            Text(stringResource(R.string.rescan))
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (totalSelected > 0) {
                FloatingActionButton(
                    onClick = {
                        val state = uiState as? DuplicateViewModel.UiState.Success ?: return@FloatingActionButton
                        viewModel.deleteSelected(state.groups) { intents, ids ->
                            deleteState = DeleteState(intents, ids)
                        }
                    }
                ) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_selected))
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is DuplicateViewModel.UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is DuplicateViewModel.UiState.Empty -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = stringResource(R.string.no_duplicates_found),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.no_duplicates_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                is DuplicateViewModel.UiState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                is DuplicateViewModel.UiState.Success -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        itemsIndexed(state.groups) { index, group ->
                            DuplicateGroupCard(
                                groupIndex = index,
                                group = group,
                                selectedIds = selections[index] ?: emptySet(),
                                onToggle = { itemId -> viewModel.toggleSelection(index, itemId) },
                                onSelectAll = { viewModel.selectAllInGroup(index, group) },
                                onDeselectAll = { viewModel.deselectAllInGroup(index) },
                                onKeepFirst = { viewModel.keepOnlyFirst(index, group) }
                            )
                        }
                        item { Spacer(modifier = Modifier.height(80.dp)) } // FAB padding
                    }
                }
            }
        }
    }
}

@Composable
private fun DuplicateGroupCard(
    groupIndex: Int,
    group: DuplicateGroup,
    selectedIds: Set<String>,
    onToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onDeselectAll: () -> Unit,
    onKeepFirst: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = pluralStringResource(R.plurals.duplicate_count, group.items.size, group.items.size),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = "${FileUtils.formatFileSize(group.size)} each • ${FileUtils.formatFileSize(group.size * group.items.size)} total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onKeepFirst) {
                    Text(stringResource(R.string.keep_first))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            group.items.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = selectedIds.contains(item.id),
                        onCheckedChange = { onToggle(item.id) }
                    )
                    AsyncImage(
                        model = item.uri,
                        contentDescription = item.name,
                        modifier = Modifier.size(56.dp),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.name,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = item.uri.lastPathSegment ?: "",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (selectedIds.contains(item.id)) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDeselectAll) {
                    Text(stringResource(R.string.deselect_all))
                }
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = onSelectAll) {
                    Text(stringResource(R.string.select_all))
                }
            }
        }
    }
}
