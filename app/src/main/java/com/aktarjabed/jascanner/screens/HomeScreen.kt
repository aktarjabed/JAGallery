package com.aktarjabed.jascanner.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aktarjabed.jascanner.repository.MediaRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onPhotoClick: (String) -> Unit,
    onVaultClick: () -> Unit,
    onSearchClick: () -> Unit,
    onStoriesClick: () -> Unit,
    snackbarHostState: androidx.compose.material3.SnackbarHostState
) {
    val context = LocalContext.current
    var photos by remember { mutableStateOf(emptyList<com.aktarjabed.jascanner.model.Photo>()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        photos = MediaRepository(context).getPhotos()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("JAGallery") }
            )
        },
        floatingActionButton = {
            Row {
                ExtendedFloatingActionButton(
                    onClick = onVaultClick,
                    icon = { androidx.compose.material3.Icon(Icons.Default.Storage, "Vault") },
                    text = { Text("Vault") },
                    modifier = Modifier.padding(end = 8.dp)
                )
                ExtendedFloatingActionButton(
                    onClick = onStoriesClick,
                    icon = { androidx.compose.material3.Icon(Icons.Default.Theaters, "Stories") },
                    text = { Text("Stories") }
                )
            }
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Search Bar
            Card(
                onClick = onSearchClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                ) {
                    androidx.compose.material3.Icon(Icons.Default.Search, "Search")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Search your photos...")
                }
            }

            // Photo Grid
            LazyVerticalGrid(
                columns = GridCells.Adaptive(128.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(photos, key = { it.id }) { photo ->
                    var tags by remember { mutableStateOf(emptyList<String>()) }

                    LaunchedEffect(photo.id) {
                        tags = MediaRepository(context).getAITags(photo.id.toLong())
                    }

                    Card(
                        onClick = { onPhotoClick(photo.id) },
                        modifier = Modifier
                            .padding(4.dp)
                            .aspectRatio(1f)
                    ) {
                        Box {
                            AsyncImage(
                                model = photo.uri,
                                contentDescription = photo.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )

                            // AI Tags Overlay
                            if (tags.isNotEmpty()) {
                                androidx.compose.material3.Text(
                                    text = tags.take(2).joinToString(", "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier
                                        .align(androidx.compose.ui.Alignment.BottomStart)
                                        .padding(4.dp)
                                        .background(
                                            Color.Black.copy(alpha = 0.7f),
                                            MaterialTheme.shapes.small
                                        )
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}