package com.aktarjabed.jagallery.ui.screens.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.aktarjabed.jagallery.ui.screens.viewer.components.ImageViewer
import com.aktarjabed.jagallery.ui.screens.viewer.components.VideoPlayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultViewerScreen(
    onBack: () -> Unit,
    viewModel: VaultViewerViewModel = hiltViewModel()
) {
    val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()
    val currentEntity by viewModel.currentEntity.collectAsStateWithLifecycle()
    val tempUri by viewModel.tempUri.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(isUnlocked) {
        if (!isUnlocked) {
            viewModel.cleanTemp()
            onBack()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.cleanTemp()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentEntity?.originalName ?: "Secure Viewer") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (tempUri != null && currentEntity != null) {
                        val localEntity = currentEntity
                        if (localEntity != null) {
                            IconButton(onClick = {
                                scope.launch {
                                    val success = viewModel.restoreItem(context, localEntity)
                                    if (success) {
                                        android.widget.Toast.makeText(context, "Restored to Gallery", android.widget.Toast.LENGTH_SHORT).show()
                                        onBack()
                                    } else {
                                        android.widget.Toast.makeText(context, "Restore failed", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }) {
                                Icon(Icons.Filled.Restore, contentDescription = "Restore")
                            }
                            IconButton(onClick = {
                                scope.launch {
                                    viewModel.deleteItem(localEntity)
                                    onBack()
                                }
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete Permanently")
                            }
                            IconButton(onClick = {
                                val secureUri = viewModel.getSecureContentUri(context)
                                if (secureUri != null) {
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = localEntity.mimeType
                                        putExtra(android.content.Intent.EXTRA_STREAM, secureUri)
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    try {
                                        context.startActivity(android.content.Intent.createChooser(intent, "Share"))
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, "No app to handle share", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    android.widget.Toast.makeText(context, "Cannot share locked media", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Filled.Share, contentDescription = "Share")
                            }
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(padding),
            contentAlignment = Alignment.Center
        ) {
            if (!isUnlocked) {
                Text("Vault locked", color = Color.White)
            } else {
                val safeTempUri = tempUri
                val entity = currentEntity
                if (safeTempUri == null) {
                    CircularProgressIndicator(color = Color.White)
                } else if (entity != null) {
                    if (entity.mimeType.startsWith("video/")) {
                        VideoPlayer(
                            uri = safeTempUri,
                            isPageVisible = true
                        )
                    } else {
                        ImageViewer(
                            uri = safeTempUri
                        )
                    }
                }
            }
        }
    }
}
