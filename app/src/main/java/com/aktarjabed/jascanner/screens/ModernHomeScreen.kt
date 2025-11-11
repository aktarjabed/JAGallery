package com.aktarjabed.jascanner.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.aktarjabed.jascanner.model.Photo
import com.aktarjabed.jascanner.ui.components.AnimatedFloatingActionButton
import com.aktarjabed.jascanner.ui.components.FrostedGlassPanel
import com.aktarjabed.jascanner.ui.components.GlassCard
import com.aktarjabed.jascanner.ui.theme.ThemeEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ModernHomeScreen(
    navController: NavController,
    photos: List<Photo>,
    onPhotoClick: (String) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var fabExpanded by remember { mutableStateOf(false) }
    var scrollState by remember { mutableStateOf(0f) }
    var currentTime by remember { mutableStateOf("") }

    // Auto-update time
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                .format(java.util.Date())
            delay(1000)
        }
    }

    // Parallax effect
    val parallaxOffset by animateFloatAsState(
        targetValue = scrollState * 0.5f,
        animationSpec = tween(300),
        label = "parallax"
    )

    val fabItems = listOf(
        AnimatedFloatingActionButton.FabItem(
            icon = Icons.Default.CameraAlt,
            contentDescription = "Take Photo",
            onClick = { /* Open camera */ }
        ),
        AnimatedFloatingActionButton.FabItem(
            icon = Icons.Default.Folder,
            contentDescription = "Create Album",
            onClick = { /* Create album */ }
        ),
        AnimatedFloatingActionButton.FabItem(
            icon = Icons.Default.Share,
            contentDescription = "Share",
            onClick = { /* Share */ }
        )
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "JAScanner",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        floatingActionButton = {
            AnimatedFloatingActionButton(
                expanded = fabExpanded,
                onExpandChange = { fabExpanded = it },
                items = fabItems
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            // Header with time and greeting
            FrostedGlassPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        currentTime,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        getGreeting(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Photo Grid with Staggered Layout
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Adaptive(150.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalItemSpacing = 8.dp,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(photos, key = { it.id }) { photo ->
                    GlassCard(
                        modifier = Modifier
                            .animateItemPlacement()
                            .aspectRatio(0.8f + (Math.random() * 0.4).toFloat()),
                        elevation = 4.dp,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            AsyncImage(
                                model = photo.uri,
                                contentDescription = photo.displayName,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(MaterialTheme.shapes.medium)
                            )

                            // Gradient overlay
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Transparent,
                                                MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
                                            ),
                                            startY = 100f
                                        )
                                    )
                            )

                            // Photo info
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(12.dp)
                            ) {
                                Text(
                                    photo.displayName ?: "Untitled",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    maxLines = 1
                                )
                                Text(
                                    "${photo.width}×${photo.height}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // Quick Actions Panel
            FrostedGlassPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.BottomCenter)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    QuickActionButton(
                        icon = Icons.Default.Collections,
                        label = "Albums",
                        onClick = { navController.navigate("categories") }
                    )
                    QuickActionButton(
                        icon = Icons.Default.Search,
                        label = "Search",
                        onClick = { navController.navigate("search") }
                    )
                    QuickActionButton(
                        icon = Icons.Default.Recycling,
                        label = "Bin",
                        onClick = { navController.navigate("recycle_bin_dashboard") }
                    )
                    QuickActionButton(
                        icon = Icons.Default.Settings,
                        label = "Settings",
                        onClick = { navController.navigate("settings") }
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(70.dp)
    ) {
        IconButton(
            onClick = onClick,
            modifier = Modifier
                .size(50.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium
                )
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun getGreeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good Morning"
        hour < 17 -> "Good Afternoon"
        else -> "Good Evening"
    }
}
