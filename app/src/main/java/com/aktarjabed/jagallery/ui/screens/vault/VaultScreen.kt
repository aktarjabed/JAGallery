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

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Shield
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.aktarjabed.jagallery.ui.common.components.JAGalleryBottomBar
import com.aktarjabed.jagallery.ui.common.components.MainBottomTab
import com.aktarjabed.jagallery.ui.theme.PrimaryContainerDark
import com.aktarjabed.jagallery.ui.theme.PrimaryPurple

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultScreen(
    onBack: () -> Unit,
    onNavigateToVaultViewer: (String) -> Unit = {},
    onNavigateToPhotos: () -> Unit = onBack,
    onNavigateToAlbums: () -> Unit = onBack,
    onNavigateToTrash: () -> Unit = {},
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
                title = {
                    Text(
                        stringResource(R.string.vault_title),
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    if (isUnlocked) {
                        IconButton(onClick = { viewModel.lock() }) {
                            Icon(Icons.Filled.Lock, contentDescription = stringResource(R.string.cd_lock_vault), tint = PrimaryPurple)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )
        },
        bottomBar = {
            JAGalleryBottomBar(
                currentTab = MainBottomTab.VAULT,
                onTabSelected = { tab ->
                    when (tab) {
                        MainBottomTab.PHOTOS -> onNavigateToPhotos()
                        MainBottomTab.ALBUMS -> onNavigateToAlbums()
                        MainBottomTab.TRASH -> onNavigateToTrash()
                        MainBottomTab.VAULT -> {}
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            if (!isUnlocked) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(PrimaryContainerDark, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = PrimaryPurple
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.vault_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.vault_locked),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(28.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                if (context is FragmentActivity) {
                                    val success = authenticator.authenticate(context)
                                    if (success) {
                                        viewModel.unlock()
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryPurple,
                            contentColor = Color.Black
                        ),
                        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = stringResource(R.string.vault_unlock),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                if (vaultItems.isEmpty()) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = PrimaryPurple
                            )
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.vault_empty),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Media moved here is encrypted on-device with Android Keystore.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    var decryptedThumbnails by remember { mutableStateOf<Map<String, android.net.Uri>>(emptyMap()) }
                    var isDecrypting by remember { mutableStateOf(false) }

                    LaunchedEffect(vaultItems, isUnlocked) {
                        if (isUnlocked) {
                            isDecrypting = true
                            val map = mutableMapOf<String, android.net.Uri>()
                            for (item in vaultItems) {
                                val uri = viewModel.getDecryptedTempUriSuspended(item)
                                if (uri != null) {
                                    map[item.id] = uri
                                }
                            }
                            decryptedThumbnails = map
                            isDecrypting = false
                        } else {
                            decryptedThumbnails = emptyMap()
                            isDecrypting = false
                        }
                    }

                    if (isDecrypting) {
                        CircularProgressIndicator(color = PrimaryPurple)
                    }

                    val safeMediaItems = remember(vaultItems, decryptedThumbnails) {
                        vaultItems.mapNotNull { entity ->
                            val safeUri = decryptedThumbnails[entity.id]
                            if (safeUri != null) {
                                com.aktarjabed.jagallery.data.model.MediaItem(
                                    uri = safeUri, mediaStoreId = -1L, name = entity.originalName,
                                    dateAdded = entity.dateAdded, mimeType = entity.mimeType, bucketId = -1L, bucketName = "",
                                    relativePath = "", isVideo = entity.mimeType.startsWith("video/"), volumeName = "",
                                    size = 0L, isFavorite = false, isTrashed = false, dateTrashed = 0L
                                )
                            } else {
                                null
                            }
                        }
                    }

                    com.aktarjabed.jagallery.ui.common.components.MediaGrid(
                        items = safeMediaItems,
                        selectedIds = emptySet(),
                        selectionMode = false,
                        onItemClick = { item ->
                            val vaultId = vaultItems.find { decryptedThumbnails[it.id] == item.uri }?.id
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
