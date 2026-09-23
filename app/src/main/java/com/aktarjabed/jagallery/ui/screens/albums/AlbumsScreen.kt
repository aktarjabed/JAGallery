package com.aktarjabed.jagallery.ui.screens.albums

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.material.icons.filled.Edit
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.aktarjabed.jagallery.R
import com.aktarjabed.jagallery.data.model.MediaSource
import com.aktarjabed.jagallery.ui.common.components.RenameDialog

@OptIn(ExperimentalMaterial3Api::class)
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

    var albumToRename by remember { mutableStateOf<com.aktarjabed.jagallery.data.model.Album?>(null) }

    com.aktarjabed.jagallery.ui.common.components.OperationToastEffect(operationEvent = viewModel.operationEvent)

    val batchState by viewModel.batchManager.batchState.collectAsStateWithLifecycle()

    com.aktarjabed.jagallery.ui.common.selection.BatchOperationObserver(
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

    var showMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onNavigateToSearch) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search_hint))
                    }
                    IconButton(onClick = onNavigateToFavorites) {
                        Icon(Icons.Default.Favorite, contentDescription = stringResource(R.string.tab_favorites))
                    }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.tab_hidden_media)) },
                            leadingIcon = { Icon(Icons.Default.VisibilityOff, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onNavigateToHidden()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.tab_trash)) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onNavigateToTrash()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.map_view_title)) },
                            leadingIcon = { Icon(Icons.Default.Map, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onNavigateToMap()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.duplicates_title)) },
                            leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onNavigateToDuplicates()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.settings)) },
                            leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onNavigateToSettings()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.vault_title)) },
                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                            onClick = {
                                showMenu = false
                                onNavigateToVault()
                            }
                        )
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is AlbumsUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is AlbumsUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text(text = state.cause.localizedMessage ?: "Failed to load media", color = MaterialTheme.colorScheme.error)
                }
            }
            is AlbumsUiState.Empty -> {
                Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(text = stringResource(R.string.no_media_found), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            is AlbumsUiState.Success -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(4.dp)
                ) {
                    item {
                        AlbumCard(
                            title = stringResource(R.string.tab_all_media),
                            count = state.totalCount,
                            coverUri = state.coverUri ?: Uri.EMPTY,
                            onClick = { onNavigateToGrid(MediaSource.All) }
                        )
                    }
                    items(state.albums) { album ->
                        AlbumCard(
                            title = album.name.ifBlank { stringResource(R.string.album_default_title) },
                            count = album.mediaCount,
                            coverUri = album.coverUri,
                            onClick = { onNavigateToGrid(MediaSource.Album(album.key)) },
                            onLongClick = { albumToRename = album }
                        )
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

@Composable
fun AlbumCard(
    title: String,
    count: Int,
    coverUri: Uri,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    Card(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = {
                        onLongClick?.invoke()
                    }
                )
            }
    ) {
        Column {
            AsyncImage(
                model = coverUri,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    text = pluralStringResource(R.plurals.media_item_count, count, count),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}
