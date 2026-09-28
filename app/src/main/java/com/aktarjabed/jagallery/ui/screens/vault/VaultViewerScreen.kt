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
import androidx.compose.ui.res.stringResource
import com.aktarjabed.jagallery.R
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

    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.vault_delete_confirm_title)) },
            text = { Text(stringResource(R.string.vault_delete_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    val localEntity = currentEntity
                    if (localEntity != null) {
                        scope.launch {
                            val result = viewModel.deleteItem(localEntity)
                            when (result) {
                                is com.aktarjabed.jagallery.data.repository.VaultDeleteResult.Success,
                                is com.aktarjabed.jagallery.data.repository.VaultDeleteResult.FileNotFound -> {
                                    android.widget.Toast.makeText(context, context.getString(R.string.vault_deleted_permanently), android.widget.Toast.LENGTH_SHORT).show()
                                    onBack()
                                }
                                else -> {
                                    android.widget.Toast.makeText(context, context.getString(R.string.vault_delete_failed), android.widget.Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(currentEntity?.originalName ?: stringResource(R.string.vault_viewer_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
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
                                        android.widget.Toast.makeText(context, context.getString(R.string.vault_restored), android.widget.Toast.LENGTH_SHORT).show()
                                        onBack()
                                    } else {
                                        android.widget.Toast.makeText(context, context.getString(R.string.vault_restore_failed), android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }) {
                                Icon(Icons.Filled.Restore, contentDescription = stringResource(R.string.cd_restore))
                            }
                            IconButton(onClick = {
                                showDeleteConfirm = true
                            }) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.cd_delete_permanently))
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
                                        context.startActivity(android.content.Intent.createChooser(intent, context.getString(R.string.cd_share)))
                                    } catch (e: Exception) {
                                        android.widget.Toast.makeText(context, context.getString(R.string.vault_no_share_app), android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    android.widget.Toast.makeText(context, context.getString(R.string.vault_cannot_share_locked), android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }) {
                                Icon(Icons.Filled.Share, contentDescription = stringResource(R.string.cd_share))
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
                Text(stringResource(R.string.vault_locked), color = Color.White)
            } else {
                var decryptionFailed by remember { mutableStateOf(false) }
                LaunchedEffect(currentEntity) {
                    kotlinx.coroutines.delay(5000L) // Wait 5 seconds for decryption
                    if (tempUri == null) {
                        decryptionFailed = true
                    }
                }

                val safeTempUri = tempUri
                val entity = currentEntity
                if (safeTempUri == null) {
                    if (decryptionFailed) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(stringResource(R.string.vault_decryption_failed), color = Color.White)
                            Spacer(Modifier.height(16.dp))
                            Button(onClick = onBack) {
                                Text(stringResource(R.string.cd_back))
                            }
                        }
                    } else {
                        CircularProgressIndicator(color = Color.White)
                    }
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
