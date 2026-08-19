package com.example.advancedgallery.ui.screens.viewer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.advancedgallery.ui.screens.viewer.components.ImageViewer
import com.example.advancedgallery.ui.screens.viewer.components.VideoPlayer
import com.example.advancedgallery.util.FileUtils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    initialMediaId: Long,
    bucketId: Long?,
    searchQuery: String? = null,
    onBack: () -> Unit,
    viewModel: ViewerViewModel = hiltViewModel()
) {
    LaunchedEffect(bucketId, searchQuery) {
        viewModel.setParams(bucketId, searchQuery)
    }

    val mediaItems by viewModel.mediaItems.collectAsState()

    if (mediaItems.isEmpty()) {
        return
    }

    val initialIndex = remember(mediaItems) {
        mediaItems.indexOfFirst { it.id == initialMediaId }.takeIf { it >= 0 } ?: 0
    }

    val pagerState = rememberPagerState(
        initialPage = initialIndex,
        pageCount = { mediaItems.size }
    )

    val currentItem = mediaItems.getOrNull(pagerState.currentPage) ?: return

    var showControls by remember { mutableStateOf(true) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val currentDeletingId = rememberUpdatedState(currentItem.id)
    val currentDeletingUri = rememberUpdatedState(currentItem.uri)

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            viewModel.removeDeletedItem(currentDeletingId.value)
            if (mediaItems.size <= 1) {
                onBack()
            }
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
            title = { Text("Media Info") },
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
                    Text("OK")
                }
            }
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Media") },
            text = { Text("Are you sure you want to delete this item?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirmation = false
                    val pendingIntent = FileUtils.createDeleteRequest(context.contentResolver, listOf(currentDeletingUri.value))
                    if (pendingIntent != null) {
                        deleteLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                    } else {
                        val deleted = FileUtils.deleteMediaItems(context.contentResolver, listOf(currentDeletingUri.value))
                        viewModel.removeDeletedItem(currentDeletingId.value)
                        if (mediaItems.size <= 1) {
                            onBack()
                        }
                    }
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
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
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = if (currentItem.isVideo) "video/*" else "image/*"
                                putExtra(Intent.EXTRA_STREAM, currentItem.uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
                        }) {
                            Icon(Icons.Default.Share, contentDescription = "Share")
                        }
                        IconButton(onClick = { showInfoDialog = true }) {
                            Icon(Icons.Default.Info, contentDescription = "Info")
                        }
                        IconButton(onClick = { viewModel.toggleFavorite(currentItem) }) {
                            Icon(
                                imageVector = if (currentItem.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (currentItem.isFavorite) Color.Red else LocalContentColor.current
                            )
                        }
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
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
                beyondBoundsPageCount = 1
            ) { page ->
                val item = mediaItems[page]
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
