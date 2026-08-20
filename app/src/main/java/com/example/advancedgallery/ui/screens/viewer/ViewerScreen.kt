package com.example.advancedgallery.ui.screens.viewer

import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.example.advancedgallery.ui.common.selection.shareMediaItems
import com.example.advancedgallery.ui.screens.viewer.components.ImageViewer
import com.example.advancedgallery.ui.screens.viewer.components.VideoPlayer
import com.example.advancedgallery.util.FileUtils
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
    LaunchedEffect(source) {
        viewModel.setSource(source)
    }

    val viewerState by viewModel.state.collectAsState()

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
        mediaItems.getOrNull(pagerState.currentPage)?.id?.let { id ->
            viewModel.setCurrentMediaId(id)
        }
    }

    val currentItem = mediaItems.getOrNull(pagerState.currentPage)
    if (currentItem == null) {
        LaunchedEffect(Unit) {
            onBack()
        }
        return
    }

    var showControls by remember { mutableStateOf(true) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var deleteState by remember { mutableStateOf<com.example.advancedgallery.ui.common.selection.DeleteOperationState>(com.example.advancedgallery.ui.common.selection.DeleteOperationState.Idle) }

    val context = LocalContext.current

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val currentState = deleteState
        if (currentState is com.example.advancedgallery.ui.common.selection.DeleteOperationState.SystemConfirmation) {
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                viewModel.removeDeletedItem(currentState.batch.ids.first())
                if (mediaItems.size <= 1) {
                    onBack()
                }
            }
        }
        deleteState = com.example.advancedgallery.ui.common.selection.DeleteOperationState.Idle
    }

    if (showInfoDialog) {
        val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }
        val dateStr = remember(currentItem.dateAdded) {
            val millis = if (currentItem.dateAdded > 10000000000L) currentItem.dateAdded else currentItem.dateAdded * 1000
            dateFormat.format(Date(millis))
        }

        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text(stringResource(R.string.media_info_title)) },
            text = {
                Column {
                    Text(text = "${stringResource(R.string.media_info_name)} ${currentItem.name}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "${stringResource(R.string.media_info_mime_type)} ${currentItem.mimeType.ifEmpty { if (currentItem.isVideo) "video/*" else "image/*" }}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "${stringResource(R.string.media_info_date_added)} $dateStr")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "${stringResource(R.string.media_info_album)} ${currentItem.bucketName.ifEmpty { stringResource(R.string.internal_storage) }}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "${stringResource(R.string.media_info_uri)} ${currentItem.uri}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text(stringResource(R.string.ok))
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
                        val pendingIntent = FileUtils.createDeleteRequest(context.contentResolver, currentState.batch.uris)
                        if (pendingIntent != null) {
                            deleteState = com.example.advancedgallery.ui.common.selection.DeleteOperationState.SystemConfirmation(currentState.batch)
                            deleteLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                        } else {
                            val success = FileUtils.deleteMediaItems(context.contentResolver, currentState.batch.uris)
                            if (success) {
                                viewModel.removeDeletedItem(currentState.batch.ids.first())
                                deleteState = com.example.advancedgallery.ui.common.selection.DeleteOperationState.Idle
                                if (mediaItems.size <= 1) {
                                    onBack()
                                }
                            } else {
                                android.widget.Toast.makeText(context, context.getString(R.string.failed_to_delete_media), android.widget.Toast.LENGTH_SHORT).show()
                                deleteState = com.example.advancedgallery.ui.common.selection.DeleteOperationState.Failed(currentState.batch)
                            }
                        }
                    }) {
                        Text(stringResource(R.string.delete))
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
                        }
                        IconButton(onClick = {
                            shareMediaItems(context, listOf(currentItem))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = stringResource(R.string.share))
                        }
                        IconButton(onClick = { showInfoDialog = true }) {
                            Icon(Icons.Default.Info, contentDescription = stringResource(R.string.info))
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
