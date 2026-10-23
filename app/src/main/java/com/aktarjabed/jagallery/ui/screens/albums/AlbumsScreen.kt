package com.aktarjabed.jagallery.ui.screens.albums

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aktarjabed.jagallery.R
import com.aktarjabed.jagallery.data.model.Album
import com.aktarjabed.jagallery.data.model.MediaSource
import com.aktarjabed.jagallery.ui.common.components.*
import com.aktarjabed.jagallery.ui.common.selection.BatchOperationObserver

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AlbumsScreen(
    viewModel: AlbumsViewModel = hiltViewModel(),
    onNavigateToGrid: (MediaSource) -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToHidden: () -> Unit,
    onNavigateToTrash: () -> Unit,
    onNavigateToMap: () -> Unit,
    onNavigateToDuplicates: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToVault: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var albumToRename by remember { mutableStateOf<Album?>(null) }
    var showMoreMenu by rememberSaveable { mutableStateOf(false) }

    OperationToastEffect(operationEvent = viewModel.operationEvent)

    val batchState by viewModel.batchManager.batchState.collectAsStateWithLifecycle()

    BatchOperationObserver(
        batchState = batchState,
        onChunkResult = { resultCode -> viewModel.batchManager.onBatchChunkResult(resultCode) },
        onComplete = { result ->
            if (result.tag == "ALBUMS_RENAME_DELETE") {
                if (result.succeededIds.isNotEmpty()) {
                    viewModel.removeDeletedItems(result.succeededIds)
                }
                if (result.cancelled) {
                    Toast.makeText(context, context.getString(R.string.move_partial_copied), Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(context, context.getString(R.string.album_renamed), Toast.LENGTH_SHORT).show()
                }
            }
            viewModel.batchManager.clearState()
        }
    )

    LaunchedEffect(viewModel.albumRenameDeleteEvent) {
        viewModel.albumRenameDeleteEvent.collect { (_, pendingIntents) ->
            if (pendingIntents.isNotEmpty()) {
                viewModel.batchManager.startBatch(pendingIntents, "ALBUMS_RENAME_DELETE")
            }
        }
    }

    Scaffold(
        topBar = {
            Column {
                JAGalleryTopBar(
                    onNavigateToSearch = onNavigateToSearch,
                    onNavigateToFavorites = onNavigateToFavorites,
                    onMoreOptionsClick = { showMoreMenu = true }
                )
                FilterChipRow(
                    selectedTab = GalleryFilterTab.ALBUMS,
                    onTabSelected = { tab ->
                        when (tab) {
                            GalleryFilterTab.ALBUMS -> {}
                            GalleryFilterTab.PHOTOS -> onNavigateToGrid(MediaSource.TypeFiltered(isVideo = false))
                            GalleryFilterTab.VIDEOS -> onNavigateToGrid(MediaSource.TypeFiltered(isVideo = true))
                            GalleryFilterTab.FAVORITES -> onNavigateToFavorites()
                        }
                    }
                )
            }
        },
        bottomBar = {
            JAGalleryBottomBar(
                currentTab = MainBottomTab.ALBUMS,
                onTabSelected = { tab ->
                    when (tab) {
                        MainBottomTab.PHOTOS -> onNavigateToGrid(MediaSource.All)
                        MainBottomTab.ALBUMS -> {}
                        MainBottomTab.TRASH -> onNavigateToTrash()
                        MainBottomTab.VAULT -> onNavigateToVault()
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            DropdownMenu(
                expanded = showMoreMenu,
                onDismissRequest = { showMoreMenu = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.tab_hidden_media)) },
                    leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null) },
                    onClick = {
                        showMoreMenu = false
                        onNavigateToHidden()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.map_view_title)) },
                    leadingIcon = { Icon(Icons.Default.Map, contentDescription = null) },
                    onClick = {
                        showMoreMenu = false
                        onNavigateToMap()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.duplicates_title)) },
                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                    onClick = {
                        showMoreMenu = false
                        onNavigateToDuplicates()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.title_settings)) },
                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    onClick = {
                        showMoreMenu = false
                        onNavigateToSettings()
                    }
                )
            }

            when (val state = uiState) {
                is AlbumsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is AlbumsUiState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = state.cause.localizedMessage ?: "Failed to load media",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                is AlbumsUiState.Empty -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.PhotoLibrary,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.no_media_found),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                is AlbumsUiState.Success -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.albums, key = { it.id }) { album ->
                            AlbumMosaicCard(
                                title = album.name.ifBlank { stringResource(R.string.album_default_title) },
                                count = album.mediaCount,
                                coverUris = album.coverUris,
                                isVideo = album.isVideo,
                                onClick = { onNavigateToGrid(MediaSource.Album(album.key)) },
                                onLongClick = { albumToRename = album },
                                onMenuClick = { albumToRename = album }
                            )
                        }

                        // Bottom Section: Category Cards for Trash & Vault
                        item(span = { GridItemSpan(1) }) {
                            TrashCategoryCard(
                                itemCount = state.trashCount,
                                onClick = onNavigateToTrash
                            )
                        }
                        item(span = { GridItemSpan(1) }) {
                            VaultCategoryCard(
                                itemCount = state.vaultCount,
                                onClick = onNavigateToVault
                            )
                        }
                    }
                }
            }
        }
    }

    if (albumToRename != null) {
        val album = albumToRename ?: return@AlbumsScreen
        RenameDialog(
            initialName = album.name,
            title = stringResource(R.string.rename_album),
            onDismissRequest = { albumToRename = null },
            onConfirm = { newName ->
                val items = (uiState as? AlbumsUiState.Success)?.rawItems?.filter { it.albumKey == album.key } ?: emptyList()
                viewModel.renameAlbum(context, album, newName, items)
                albumToRename = null
            }
        )
    }
}
