package com.example.advancedgallery.ui.screens.viewer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.advancedgallery.R
import com.example.advancedgallery.data.model.MediaSource
import com.example.advancedgallery.data.model.PendingDeleteBatch
import com.example.advancedgallery.ui.common.OperationEvent
import com.example.advancedgallery.ui.common.selection.shareMediaItems
import com.example.advancedgallery.ui.screens.viewer.components.ImageViewer
import com.example.advancedgallery.ui.screens.viewer.components.VideoPlayer
import com.example.advancedgallery.ui.common.components.MetadataBottomSheet
import androidx.compose.material.icons.filled.MoreVert
import com.example.advancedgallery.util.FileUtils
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    initialMediaId: String,
    source: MediaSource,
    onBack: () -> Unit,
    onNavigateToEditor: (String) -> Unit = {},
    viewModel: ViewerViewModel = hiltViewModel()
) {
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.navigationEvent) {
        viewModel.navigationEvent.collect { event ->
            when (event) {
                is ViewerNavigationEvent.PopBack -> onBack()
            }
        }
    }

    LaunchedEffect(viewModel.operationEvent) {
        viewModel.operationEvent.collect { event ->
            when (event) {
                is OperationEvent.Error -> snackbarHostState.showSnackbar(event.message)
                is OperationEvent.Success -> snackbarHostState.showSnackbar(event.message)
            }
        }
    }

    val viewerState by viewModel.state.collectAsStateWithLifecycle()

    val mediaItems = when (val currentState = viewerState) {
        is ViewerState.Loading -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color.White)
            }
            return
        }
        is ViewerState.Empty -> {
            LaunchedEffect(Unit) {
                onBack()
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_media_found),
                    color = Color.White
                )
            }
            return
        }
        is ViewerState.Error -> {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.failed_to_load_media),
                        color = Color.Red
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBack) {
                        Text(stringResource(R.string.back))
                    }
                }
            }
            return
        }
        is ViewerState.Success -> currentState.items
    }

    val activeMediaId = viewModel.currentMediaId ?: initialMediaId

    val initialIndex = remember(mediaItems) {
        mediaItems.indexOfFirst { it.id == activeMediaId }.takeIf { it >= 0 } ?: 0
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, (mediaItems.size - 1).coerceAtLeast(0)),
        pageCount = { mediaItems.size }
    )

    LaunchedEffect(mediaItems) {
        if (mediaItems.isNotEmpty()) {
            val targetMediaId = viewModel.currentMediaId ?: initialMediaId
            val newIndex = mediaItems.indexOfFirst { it.id == targetMediaId }
            if (newIndex >= 0) {
                if (pagerState.currentPage != newIndex) {
                    pagerState.scrollToPage(newIndex)
                }
            } else {
                val nextIndex = pagerState.currentPage.coerceIn(0, mediaItems.size - 1)
                mediaItems.getOrNull(nextIndex)?.id?.let { nextId ->
                    viewModel.setCurrentMediaId(nextId)
                }
            }
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        viewModel.updateCurrentIndex(pagerState.currentPage)
    }

    val currentItem = mediaItems.getOrNull(pagerState.currentPage)
    if (currentItem == null) {
        LaunchedEffect(Unit) {
            onBack()
        }
        return
    }

    var showControls by remember { mutableStateOf(true) }
    var showRenameDialog by remember { mutableStateOf(false) }

    var isTrimming by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var deleteState by remember { mutableStateOf<com.example.advancedgallery.ui.common.selection.DeleteOperationState>(com.example.advancedgallery.ui.common.selection.DeleteOperationState.Idle) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showVideoTrimSheet by remember { mutableStateOf(false) }
    var trimStartMs by remember { mutableStateOf(0f) }
    var trimEndMs by remember { mutableStateOf(10000f) }
    var videoDurationMs by remember { mutableStateOf(60000f) }

    LaunchedEffect(showVideoTrimSheet, currentItem, context) {
        if (showVideoTrimSheet && currentItem.isVideo) {
            val retriever = android.media.MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, currentItem.uri)
                val time = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
                val duration = time?.toLongOrNull() ?: 60000L
                videoDurationMs = duration.toFloat()
                trimStartMs = 0f
                trimEndMs = duration.toFloat()
            } catch (e: Exception) {
                videoDurationMs = 60000f
            } finally {
                retriever.release()
            }
        }
    }

    var pendingRename by remember { mutableStateOf<com.example.advancedgallery.domain.RenameOperationResult.NeedsPermission?>(null) }
    val renamePermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val pending = pendingRename
        pendingRename = null
        if (pending != null && result.resultCode == android.app.Activity.RESULT_OK) {
            coroutineScope.launch {
                when (val retry = viewModel.renameMedia(context, pending.item, pending.newName)) {
                    is com.example.advancedgallery.domain.RenameOperationResult.Success -> {
                        android.widget.Toast.makeText(context, "Media renamed successfully.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        android.widget.Toast.makeText(context, "Failed to rename media.", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            android.widget.Toast.makeText(context, "Rename cancelled: Permission denied.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }


    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val currentState = deleteState
        if (currentState is com.example.advancedgallery.ui.common.selection.DeleteOperationState.SystemConfirmation) {
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                viewModel.removeDeletedItem(currentState.batch.ids.first())
            }
        }
        deleteState = com.example.advancedgallery.ui.common.selection.DeleteOperationState.Idle
    }

    if (showInfoDialog) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        MetadataBottomSheet(
            sheetState = sheetState,
            mediaItem = currentItem,
            onDismiss = { showInfoDialog = false }
        )
    }


    if (showVideoTrimSheet && currentItem.isVideo) {
        AlertDialog(
            onDismissRequest = { if (!isTrimming) showVideoTrimSheet = false },
            title = { Text("Trim Video") },
            text = {
                Column {
                    if (isTrimming) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                        Text("Trimming video...", modifier = Modifier.padding(top = 8.dp))
                    } else {
                        Text("Start: ${trimStartMs.toInt()} ms")
                        Slider(
                            value = trimStartMs,
                            onValueChange = { if (it < trimEndMs) trimStartMs = it },
                            valueRange = 0f..videoDurationMs
                        )
                        Text("End: ${trimEndMs.toInt()} ms")
                        Slider(
                            value = trimEndMs,
                            onValueChange = { if (it > trimStartMs) trimEndMs = it },
                            valueRange = 0f..videoDurationMs
                        )
                    }
                }
            },
            confirmButton = {
                if (!isTrimming) {
                    TextButton(onClick = {
                        isTrimming = true
                        coroutineScope.launch {
                            val newUri = com.example.advancedgallery.util.VideoTrimmer.trimVideo(
                                context = context,
                                sourceUri = currentItem.uri,
                                startMs = trimStartMs.toLong(),
                                endMs = trimEndMs.toLong()
                            )
                            isTrimming = false
                            showVideoTrimSheet = false
                            if (newUri != null) {
                                android.widget.Toast.makeText(context, "Video trimmed and saved", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                android.widget.Toast.makeText(context, "Failed to trim video", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    }) {
                        Text("Trim")
                    }
                }
            },
            dismissButton = {
                if (!isTrimming) {
                    TextButton(onClick = { showVideoTrimSheet = false }) {
                        Text("Cancel")
                    }
                }
            }
        )
    }

    if (showRenameDialog) {
        var newName by remember { mutableStateOf(currentItem.name) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text(stringResource(R.string.rename_media)) },
            text = {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text(stringResource(R.string.new_name)) },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newName.isNotBlank()) {
                        coroutineScope.launch {
                            when (val result = viewModel.renameMedia(context, currentItem, newName.trim())) {
                                is com.example.advancedgallery.domain.RenameOperationResult.Success -> {
                                    android.widget.Toast.makeText(context, "Media renamed successfully.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                is com.example.advancedgallery.domain.RenameOperationResult.NeedsPermission -> {
                                    pendingRename = result
                                    renamePermissionLauncher.launch(IntentSenderRequest.Builder(result.pendingIntent.intentSender).build())
                                }
                                is com.example.advancedgallery.domain.RenameOperationResult.Error -> {
                                    android.widget.Toast.makeText(context, "Failed to rename media: ${result.message}", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                    showRenameDialog = false
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    when (val currentState = deleteState) {
        is com.example.advancedgallery.ui.common.selection.DeleteOperationState.Confirming -> {
            AlertDialog(
                onDismissRequest = { deleteState = com.example.advancedgallery.ui.common.selection.DeleteOperationState.Idle },
                title = { Text(stringResource(R.string.delete_media_title)) },
                text = { Text(stringResource(R.string.delete_single_confirm_message)) },
                confirmButton = {
                    TextButton(onClick = {
                        val isTrashed = currentItem?.isTrashed == true
                        val pendingIntent = if (isTrashed) {
                            FileUtils.createDeleteRequests(context.contentResolver, currentState.batch.uris).firstOrNull()
                        } else {
                            FileUtils.createTrashRequests(context.contentResolver, currentState.batch.uris, true).firstOrNull()
                                ?: FileUtils.createDeleteRequests(context.contentResolver, currentState.batch.uris).firstOrNull()
                        }

                        if (pendingIntent != null) {
                            deleteState = com.example.advancedgallery.ui.common.selection.DeleteOperationState.SystemConfirmation(currentState.batch)
                            deleteLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                        } else {
                            val success = FileUtils.deleteMediaItems(context.contentResolver, currentState.batch.uris)
                            if (success) {
                                viewModel.removeDeletedItem(currentState.batch.ids.first())
                                deleteState = com.example.advancedgallery.ui.common.selection.DeleteOperationState.Idle
                            } else {
                                android.widget.Toast.makeText(context, context.getString(R.string.failed_to_delete_media), android.widget.Toast.LENGTH_SHORT).show()
                                deleteState = com.example.advancedgallery.ui.common.selection.DeleteOperationState.Failed(currentState.batch)
                            }
                        }
                    }) {
                        Text(stringResource(if (currentItem?.isTrashed == true) R.string.delete_permanently else R.string.delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleteState = com.example.advancedgallery.ui.common.selection.DeleteOperationState.Idle }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            )
        }
        is com.example.advancedgallery.ui.common.selection.DeleteOperationState.Failed -> {
            LaunchedEffect(currentState) {
                deleteState = com.example.advancedgallery.ui.common.selection.DeleteOperationState.Idle
            }
        }
        else -> {}
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            if (showControls) {
                TopAppBar(
                    title = { Text(currentItem.name) },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    actions = {
                        if (!currentItem.isVideo) {
                            IconButton(onClick = { onNavigateToEditor(currentItem.uri.toString()) }) {
                                Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit))
                            }
                        } else {
                            IconButton(onClick = { showVideoTrimSheet = true }) {
                                Icon(Icons.Default.ContentCut, contentDescription = "Trim Video")
                            }
                        }
                        IconButton(onClick = {
                            shareMediaItems(context, listOf(currentItem))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share))
                        }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.info)) },
                                onClick = {
                                    showMenu = false
                                    showInfoDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.rename_media)) },
                                onClick = {
                                    showMenu = false
                                    showRenameDialog = true
                                }
                            )
                        }
                        IconButton(onClick = { viewModel.toggleFavorite(currentItem) }) {
                            Icon(
                                imageVector = if (currentItem.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = stringResource(R.string.favorite),
                                tint = if (currentItem.isFavorite) Color.Red else LocalContentColor.current
                            )
                        }
                        IconButton(onClick = {
                            deleteState = com.example.advancedgallery.ui.common.selection.DeleteOperationState.Confirming(
                                PendingDeleteBatch(
                                    ids = listOf(currentItem.id),
                                    uris = listOf(currentItem.uri)
                                )
                            )
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.5f),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color.Black)
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
                beyondViewportPageCount = 1,
                key = { page -> mediaItems.getOrNull(page)?.id ?: page }
            ) { page ->
                val item = mediaItems.getOrNull(page)
                if (item != null) {
                    val isPageVisible = (page == pagerState.currentPage)
                    if (item.isVideo) {
                        VideoPlayer(
                            uri = item.uri,
                            isPageVisible = isPageVisible,
                            onTap = { showControls = !showControls }
                        )
                    } else {
                        ImageViewer(
                            uri = item.uri,
                            onTap = { showControls = !showControls }
                        )
                    }
                }
            }
        }
    }
}
