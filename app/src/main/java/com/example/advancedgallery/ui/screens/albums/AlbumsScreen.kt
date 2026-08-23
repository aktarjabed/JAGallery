package com.example.advancedgallery.ui.screens.albums

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
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.example.advancedgallery.R
import com.example.advancedgallery.data.model.MediaSource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    viewModel: AlbumsViewModel = hiltViewModel(),
    onNavigateToGrid: (MediaSource) -> Unit,
    onNavigateToFavorites: () -> Unit,
    onNavigateToSearch: () -> Unit,
    onNavigateToHidden: () -> Unit,
    onNavigateToTrash: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var albumToRename by remember { mutableStateOf<com.example.advancedgallery.data.model.Album?>(null) }

    LaunchedEffect(Unit) {
        viewModel.operationEvent.collect { event ->
            when (event) {
                is com.example.advancedgallery.ui.common.OperationEvent.Error -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
                is com.example.advancedgallery.ui.common.OperationEvent.Success -> {
                    Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    data class AlbumRenameState(
        val uris: List<String>,
        val pendingIntents: List<android.app.PendingIntent>,
        val currentIndex: Int = 0,
        val processedIds: List<String> = emptyList()
    )

    var albumRenameState by remember { mutableStateOf<AlbumRenameState?>(null) }

    val deleteLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val state = albumRenameState
        if (state != null) {
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val chunkSize = com.example.advancedgallery.util.Constants.MAX_BATCH_SIZE
                val processedBatchIds = state.uris.drop(state.currentIndex * chunkSize).take(chunkSize)
                val updatedProcessedIds = state.processedIds + processedBatchIds

                if (state.currentIndex + 1 < state.pendingIntents.size) {
                    val nextIndex = state.currentIndex + 1
                    albumRenameState = state.copy(currentIndex = nextIndex, processedIds = updatedProcessedIds)
                } else {
                    viewModel.removeDeletedItems(updatedProcessedIds)
                    Toast.makeText(context, context.getString(R.string.album_renamed), Toast.LENGTH_SHORT).show()
                    albumRenameState = null
                }
            } else {
                if (state.processedIds.isNotEmpty()) {
                    viewModel.removeDeletedItems(state.processedIds)
                }
                Toast.makeText(context, context.getString(R.string.move_partial_copied), Toast.LENGTH_LONG).show()
                albumRenameState = null
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.albumRenameDeleteEvent.collect { (uris, pendingIntents) ->
            if (pendingIntents.isNotEmpty()) {
                albumRenameState = AlbumRenameState(uris, pendingIntents)
            }
        }
    }

    LaunchedEffect(albumRenameState) {
        val state = albumRenameState
        if (state != null && state.pendingIntents.isNotEmpty()) {
            val intentSender = state.pendingIntents[state.currentIndex].intentSender
            deleteLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(intentSender).build())
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
                    }
                }
            )
        }
    ) { padding ->
        when (val state = uiState) {
            is AlbumsUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is AlbumsUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = state.cause.localizedMessage ?: "Failed to load media",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            is AlbumsUiState.Empty -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
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
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
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
        var newName by remember { mutableStateOf(albumToRename!!.name) }
        AlertDialog(
            onDismissRequest = { albumToRename = null },
            title = { Text(stringResource(R.string.rename_album)) },
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
                    val items = (uiState as? AlbumsUiState.Success)?.rawItems?.filter { it.albumKey == albumToRename!!.key } ?: emptyList()
                    viewModel.renameAlbum(context, items, newName)
                    albumToRename = null
                }) {
                    Text(stringResource(R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { albumToRename = null }) {
                    Text(stringResource(R.string.cancel))
                }
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
