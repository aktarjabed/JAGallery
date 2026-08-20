package com.example.advancedgallery.ui.screens.viewer

import android.content.ClipData
import android.content.Intent
import android.widget.Toast
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
    bucketId: Long? = null,
    searchQuery: String? = null,
    onBack: () -> Unit,
    onNavigateToEditor: (String) -> Unit = {},
    viewModel: ViewerViewModel = hiltViewModel()
) {
    LaunchedEffect(source, bucketId, searchQuery) {
        viewModel.setParams(source, bucketId, searchQuery)
    }

    val mediaItems by viewModel.mediaItems.collectAsState()

    if (mediaItems.isEmpty()) {
        LaunchedEffect(Unit) {
            onBack()
        }
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

    val initialIndex = remember(mediaItems) {
        mediaItems.indexOfFirst { it.id == initialMediaId }.takeIf { it >= 0 } ?: 0
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, (mediaItems.size - 1).coerceAtLeast(0)),
        pageCount = { mediaItems.size }
    )

    val currentItem = mediaItems.getOrNull(pagerState.currentPage)
    if (currentItem == null) {
        LaunchedEffect(Unit) {
            onBack()
        }
        return
    }

    var showControls by remember { mutableStateOf(true) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var pendingDeleteBatch by rememberSaveable { mutableStateOf<PendingDeleteBatch?>(null) }

    val context = LocalContext.current

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        pendingDeleteBatch?.let { pending ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                viewModel.removeDeletedItem(pending.ids.first())
                if (mediaItems.size <= 1) {
                    onBack()
                }
            }
            pendingDeleteBatch = null
        }
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
                    Text(text = "Name: ${currentItem.name}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "MIME Type: ${currentItem.mimeType.ifEmpty { if (currentItem.isVideo) "video/*" else "image/*" }}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Date Added: $dateStr")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Album: ${currentItem.bucketName.ifEmpty { "Internal Storage" }}")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "URI: ${currentItem.uri}")
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    pendingDeleteBatch?.let { pending ->
        AlertDialog(
            onDismissRequest = { pendingDeleteBatch = null },
            title = { Text(stringResource(R.string.delete_media_title)) },
            text = { Text(stringResource(R.string.delete_single_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    val pendingIntent = FileUtils.createDeleteRequest(context.contentResolver, pending.uris)
                    if (pendingIntent != null) {
                        deleteLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                    } else {
                        FileUtils.deleteMediaItems(context.contentResolver, pending.uris)
                        viewModel.removeDeletedItem(pending.ids.first())
                        pendingDeleteBatch = null
                        if (mediaItems.size <= 1) {
                            onBack()
                        }
                    }
                }) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteBatch = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
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
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = if (currentItem.isVideo) "video/*" else "image/*"
                                putExtra(Intent.EXTRA_STREAM, currentItem.uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                clipData = ClipData.newRawUri("Media", currentItem.uri)
                            }
                            try {
                                context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share)))
                            } catch (e: Exception) {
                                Toast.makeText(context, context.getString(R.string.no_app_to_handle_share), Toast.LENGTH_SHORT).show()
                            }
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
                            pendingDeleteBatch = PendingDeleteBatch(
                                ids = listOf(currentItem.id),
                                uris = listOf(currentItem.uri)
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
