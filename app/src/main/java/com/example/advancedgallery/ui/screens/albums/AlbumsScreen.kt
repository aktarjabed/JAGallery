package com.example.advancedgallery.ui.screens.albums

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
    onNavigateToSearch: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

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
                            onClick = { onNavigateToGrid(MediaSource.Album(album.bucketId)) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AlbumCard(
    title: String,
    count: Int,
    coverUri: Uri,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(4.dp)
            .fillMaxWidth()
            .clickable(onClick = onClick)
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
