package com.aktarjabed.jagallery.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aktarjabed.jagallery.data.model.AlbumKey
import com.aktarjabed.jagallery.data.model.MediaSource
import com.aktarjabed.jagallery.ui.screens.albums.AlbumsScreen
import com.aktarjabed.jagallery.ui.screens.editor.EditorScreen
import com.aktarjabed.jagallery.ui.screens.favorites.FavoritesScreen
import com.aktarjabed.jagallery.ui.screens.grid.GridScreen
import com.aktarjabed.jagallery.ui.screens.hidden.HiddenScreen
import com.aktarjabed.jagallery.ui.screens.search.SearchScreen
import com.aktarjabed.jagallery.ui.screens.trash.TrashScreen
import com.aktarjabed.jagallery.ui.screens.viewer.ViewerScreen
import com.aktarjabed.jagallery.ui.screens.map.MapScreen
import com.aktarjabed.jagallery.ui.screens.duplicates.DuplicateScreen

fun parseMediaSource(
    sourceStr: String?,
    volumeName: String?,
    bucketId: Long?,
    relativePath: String?,
    searchQuery: String?
): MediaSource? {
    return when (sourceStr?.uppercase(java.util.Locale.ROOT)) {
        "FAVORITES" -> MediaSource.Favorites
        "ALBUM" -> {
            if (!volumeName.isNullOrBlank() && bucketId != null) {
                val decodedVolume = com.aktarjabed.jagallery.util.NavCodec.decode(volumeName)
                val decodedRelativePath = if (relativePath != null) com.aktarjabed.jagallery.util.NavCodec.decode(relativePath) else ""
                MediaSource.Album(AlbumKey(decodedVolume, bucketId, decodedRelativePath))
            } else {
                null
            }
        }
        "SEARCH" -> {
            if (!searchQuery.isNullOrBlank()) {
                val decodedQuery = com.aktarjabed.jagallery.util.NavCodec.decode(searchQuery)
                MediaSource.Search(decodedQuery)
            } else {
                null
            }
        }
        "TRASH" -> MediaSource.Trash
        "HIDDEN" -> MediaSource.Hidden
        "ALL" -> MediaSource.All
        null -> MediaSource.All
        else -> null
    }
}

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Albums.route) {
        composable(Screen.Albums.route) {
            AlbumsScreen(
                onNavigateToGrid = { source ->
                    navController.navigate(Screen.Grid.createRoute(source))
                },
                onNavigateToFavorites = {
                    navController.navigate(Screen.Favorites.route)
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route)
                },
                onNavigateToHidden = {
                    navController.navigate(Screen.Hidden.route)
                },
                onNavigateToTrash = {
                    navController.navigate(Screen.Trash.route)
                },
                onNavigateToMap = {
                    navController.navigate(Screen.Map.route)
                },
                onNavigateToDuplicates = {
                    navController.navigate(Screen.Duplicates.route)
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route)
                },
                onNavigateToVault = {
                    navController.navigate(Screen.Vault.route)
                }
            )
        }
        composable(Screen.Settings.route) {
            com.aktarjabed.jagallery.ui.screens.settings.SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Vault.route) {
            com.aktarjabed.jagallery.ui.screens.vault.VaultScreen(
                onBack = { navController.popBackStack() },
                onNavigateToVaultViewer = { vaultMediaId ->
                    navController.navigate(Screen.VaultViewer.createRoute(vaultMediaId))
                }
            )
        }
        composable(
            route = Screen.VaultViewer.route,
            arguments = listOf(
                navArgument("vaultMediaId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val vaultMediaId = backStackEntry.arguments?.getString("vaultMediaId")
            com.aktarjabed.jagallery.ui.screens.vault.VaultViewerScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Map.route) {
            MapScreen(
                onBack = { navController.popBackStack() },
                onNavigateToViewer = { mediaId ->
                    navController.navigate(Screen.Viewer.createRoute(mediaId, MediaSource.All))
                }
            )
        }
        composable(Screen.Duplicates.route) {
            DuplicateScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Hidden.route) {
            HiddenScreen(
                onNavigateToViewer = { mediaId, source ->
                    navController.navigate(Screen.Viewer.createRoute(mediaId, source))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Trash.route) {
            TrashScreen(
                onNavigateToViewer = { mediaId, source ->
                    navController.navigate(Screen.Viewer.createRoute(mediaId, source))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.Grid.route,
            arguments = listOf(
                navArgument("source") {
                    type = NavType.StringType
                    defaultValue = "ALL"
                },
                navArgument("volumeName") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("bucketId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("relativePath") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val sourceStr = backStackEntry.arguments?.getString("source")
            val volumeNameStr = backStackEntry.arguments?.getString("volumeName")
            val bucketIdStr = backStackEntry.arguments?.getString("bucketId")
            val relativePathStr = backStackEntry.arguments?.getString("relativePath") ?: ""
            val bucketId = bucketIdStr?.toLongOrNull()
            val source = parseMediaSource(sourceStr, volumeNameStr, bucketId, relativePathStr, null)

            if (source == null) {
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            } else {
                GridScreen(
                    source = source,
                    onNavigateToViewer = { mediaId, src ->
                        navController.navigate(Screen.Viewer.createRoute(mediaId, src))
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }
        composable(Screen.Favorites.route) {
            FavoritesScreen(
                onNavigateToViewer = { mediaId, source ->
                    navController.navigate(Screen.Viewer.createRoute(mediaId, source))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Search.route) {
            SearchScreen(
                onNavigateToViewer = { mediaId, source ->
                    navController.navigate(Screen.Viewer.createRoute(mediaId, source))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.Viewer.route,
            arguments = listOf(
                navArgument("mediaId") { type = NavType.StringType },
                navArgument("source") { type = NavType.StringType },
                navArgument("volumeName") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("bucketId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("relativePath") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("searchQuery") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val rawMediaId = backStackEntry.arguments?.getString("mediaId") ?: return@composable
            val mediaId = com.aktarjabed.jagallery.util.NavCodec.decode(rawMediaId)
            val sourceStr = backStackEntry.arguments?.getString("source")
            val volumeNameStr = backStackEntry.arguments?.getString("volumeName")
            val bucketIdStr = backStackEntry.arguments?.getString("bucketId")
            val relativePathStr = backStackEntry.arguments?.getString("relativePath") ?: ""
            val bucketId = bucketIdStr?.toLongOrNull()
            val searchQuery = backStackEntry.arguments?.getString("searchQuery")
            val source = parseMediaSource(sourceStr, volumeNameStr, bucketId, relativePathStr, searchQuery)

            if (source == null) {
                LaunchedEffect(Unit) {
                    navController.popBackStack()
                }
            } else {
                ViewerScreen(
                    initialMediaId = mediaId,
                    source = source,
                    onBack = { navController.popBackStack() },
                    onNavigateToEditor = { imageUri ->
                        navController.navigate(Screen.Editor.createRoute(imageUri))
                    }
                )
            }
        }
        @android.annotation.SuppressLint("UseKtx")
        composable(
            route = Screen.Editor.route,
            arguments = listOf(
                navArgument("imageUri") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val uriStr = backStackEntry.arguments?.getString("imageUri") ?: return@composable
            val decodedUriStr = com.aktarjabed.jagallery.util.NavCodec.decode(uriStr)
            val imageUri = android.net.Uri.parse(decodedUriStr)
            EditorScreen(
                imageUri = imageUri,
                onBack = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() }
            )
        }
    }
}
