package com.aktarjabed.jascanner.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.aktarjabed.jascanner.JAScannerApp
import com.aktarjabed.jascanner.model.Photo
import com.aktarjabed.jascanner.repository.MediaRepository
import kotlinx.coroutines.launch

// ViewModel for managing photo state
class HomeViewModel(private val mediaRepository: MediaRepository) : ViewModel() {

    // Collect media changes and trigger refresh
    val mediaChanged = mediaRepository.mediaChanged

    suspend fun getPhotos(useCache: Boolean = true): List<Photo> {
        return mediaRepository.getPhotos(useCache)
    }

    fun clearCache() {
        mediaRepository.clearCache()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    onPhotoClick: (String) -> Unit,
    onVaultClick: () -> Unit,
    onSearchClick: () -> Unit,
    onStoriesClick: () -> Unit,
    onCategoriesClick: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val app = context.applicationContext as JAScannerApp
    val mediaRepository = remember { app.mediaRepository }
    val viewModel: HomeViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(mediaRepository) as T
            }
        }
    )

    var photos by remember { mutableStateOf(emptyList<Photo>()) }
    var isLoading by remember { mutableStateOf(true) }

    val scope = rememberCoroutineScope()

    // Observe media changes
    val mediaChanged by viewModel.mediaChanged.collectAsState()

    // Load photos when screen composes or media changes
    LaunchedEffect(mediaChanged) {
        isLoading = true
        photos = viewModel.getPhotos()
        isLoading = false
    }

    // Manual refresh function
    fun refreshPhotos() {
        scope.launch {
            isLoading = true
            viewModel.clearCache() // Force fresh load
            photos = viewModel.getPhotos(useCache = false)
            isLoading = false
            snackbarHostState.showSnackbar("Gallery updated")
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "JAScanner",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    // Refresh button
                    IconButton(onClick = { refreshPhotos() }) {
                        val rotation = animateFloatAsState(targetValue = if (isLoading) 360f else 0f, label = "refresh animation").value
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Gallery",
                            modifier = Modifier
                                .size(24.dp)
                                .rotate(rotation)
                        )
                    }
                },
                actions = {
                    // Categories button
                    IconButton(onClick = onCategoriesClick) {
                        Icon(Icons.Default.Collections, contentDescription = "Categories")
                    }
                    IconButton(onClick = { navController.navigate("recycle_bin_dashboard") }) {
                        Icon(Icons.Default.Recycling, "Recycle Bin Dashboard")
                    }
                }
            )
        },
        floatingActionButton = {
            Row {
                ExtendedFloatingActionButton(
                    onClick = onCategoriesClick,
                    icon = { Icon(Icons.Default.Collections, "Categories") },
                    text = { Text("Albums") },
                    modifier = Modifier.padding(end = 8.dp)
                )
                ExtendedFloatingActionButton(
                    onClick = onVaultClick,
                    icon = { Icon(Icons.Default.Lock, "Vault") },
                    text = { Text("Vault") },
                    modifier = Modifier.padding(end = 8.dp)
                )
                ExtendedFloatingActionButton(
                    onClick = onStoriesClick,
                    icon = { Icon(Icons.Default.Theaters, "Stories") },
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
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Search, "Search")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Search your photos...")
                }
            }

            // Loading state
            if (isLoading && photos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading your photos...")
                    }
                }
            } else {
                // Photo Grid
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(128.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(photos.size, key = { photos[it].id }) { index ->
                        val photo = photos[index]
                        PhotoGridItem(
                            photo = photo,
                            onClick = { onPhotoClick(photo.id) },
                            mediaRepository = mediaRepository
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PhotoGridItem(
    photo: Photo,
    onClick: () -> Unit,
    mediaRepository: MediaRepository
) {
    var thumbnail by remember(photo.id) {
        mutableStateOf<android.graphics.Bitmap?>(null)
    }

    LaunchedEffect(photo.id) {
        thumbnail = mediaRepository.getThumbnail(photo.id, 256, 256)
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .padding(4.dp)
            .aspectRatio(1f)
    ) {
        Box {
            if (thumbnail != null) {
                AsyncImage(
                    model = thumbnail,
                    contentDescription = photo.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                AsyncImage(
                    model = photo.uri,
                    contentDescription = photo.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
