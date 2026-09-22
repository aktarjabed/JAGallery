package com.aktarjabed.jagallery.ui.screens.vault

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aktarjabed.jagallery.R
import com.aktarjabed.jagallery.domain.VaultAuthenticator
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    onBack: () -> Unit,
    onNavigateToVaultViewer: (String) -> Unit = {},
    viewModel: VaultViewModel = hiltViewModel(),
    authenticator: VaultAuthenticator = VaultAuthenticator()
) {
    val isUnlocked by viewModel.isUnlocked.collectAsStateWithLifecycle()
    val vaultItems by viewModel.vaultMedia.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Secure Vault") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isUnlocked) {
                        IconButton(onClick = { viewModel.lock() }) {
                            Icon(Icons.Filled.Lock, contentDescription = "Lock Vault")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            if (!isUnlocked) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(16.dp))
                    Text("Vault is locked")
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = {
                        scope.launch {
                            if (context is FragmentActivity) {
                                val success = authenticator.authenticate(context)
                                if (success) {
                                    viewModel.unlock()
                                }
                            }
                        }
                    }) {
                        Text("Unlock")
                    }
                }
            } else {
                if (vaultItems.isEmpty()) {
                    Text("Vault is empty")
                } else {
                    val dummyMediaItems = remember(vaultItems) {
                        vaultItems.map { entity ->
                            com.aktarjabed.jagallery.data.model.MediaItem(
                                uri = android.net.Uri.parse(entity.originalUriStr), mediaStoreId = -1L, name = entity.originalName,
                                dateAdded = entity.dateAdded, mimeType = entity.mimeType, bucketId = -1L, bucketName = "",
                                relativePath = "", isVideo = entity.mimeType.startsWith("video/"), volumeName = "",
                                size = 0L, isFavorite = false, isTrashed = false, dateTrashed = 0L
                            )
                        }
                    }

                    com.aktarjabed.jagallery.ui.common.components.MediaGrid(
                        items = dummyMediaItems,
                        selectedIds = emptySet(),
                        selectionMode = false,
                        onItemClick = { item ->
                            val vaultId = vaultItems.find { it.originalUriStr == item.uri.toString() }?.id
                            if (vaultId != null) {
                                onNavigateToVaultViewer(vaultId)
                            }
                        },
                        onItemLongClick = {}
                    )
                }
            }
        }
    }
}
