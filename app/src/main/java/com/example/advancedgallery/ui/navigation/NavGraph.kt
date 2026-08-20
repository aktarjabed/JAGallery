package com.example.advancedgallery.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.advancedgallery.data.model.MediaSource
import com.example.advancedgallery.ui.screens.albums.AlbumsScreen
import com.example.advancedgallery.ui.screens.editor.EditorScreen
import com.example.advancedgallery.ui.screens.favorites.FavoritesScreen
import com.example.advancedgallery.ui.screens.grid.GridScreen
import com.example.advancedgallery.ui.screens.search.SearchScreen
import com.example.advancedgallery.ui.screens.viewer.ViewerScreen

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Albums.route) {
        composable(Screen.Albums.route) {
            AlbumsScreen(
                onNavigateToGrid = { bucketId ->
                    navController.navigate(Screen.Grid.createRoute(bucketId))
                },
                onNavigateToFavorites = {
                    navController.navigate(Screen.Favorites.route)
                },
                onNavigateToSearch = {
                    navController.navigate(Screen.Search.route)
                }
            )
        }
        composable(
            route = Screen.Grid.route,
            arguments = listOf(
                navArgument("bucketId") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val bucketIdStr = backStackEntry.arguments?.getString("bucketId")
            val bucketId = bucketIdStr?.toLongOrNull()
            GridScreen(
                bucketId = bucketId,
                onNavigateToViewer = { mediaId, source ->
                    navController.navigate(Screen.Viewer.createRoute(mediaId, source))
                },
                onBack = { navController.popBackStack() }
            )
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
                navArgument("bucketId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("searchQuery") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val encodedMediaId = backStackEntry.arguments?.getString("mediaId") ?: return@composable
            val mediaId = android.net.Uri.decode(encodedMediaId)
            val sourceStr = backStackEntry.arguments?.getString("source")
            val bucketIdStr = backStackEntry.arguments?.getString("bucketId")
            val bucketId = bucketIdStr?.toLongOrNull()
            val searchQuery = backStackEntry.arguments?.getString("searchQuery")?.let { android.net.Uri.decode(it) }
            val source = MediaSource.from(sourceStr, bucketId, searchQuery)

            ViewerScreen(
                initialMediaId = mediaId,
                source = source,
                onBack = { navController.popBackStack() },
                onNavigateToEditor = { imageUri ->
                    navController.navigate(Screen.Editor.createRoute(imageUri))
                }
            )
        }
        composable(
            route = Screen.Editor.route,
            arguments = listOf(
                navArgument("imageUri") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val encodedUri = backStackEntry.arguments?.getString("imageUri") ?: return@composable
            val imageUri = android.net.Uri.parse(android.net.Uri.decode(encodedUri))
            EditorScreen(
                imageUri = imageUri,
                onBack = { navController.popBackStack() },
                onSaveSuccess = { navController.popBackStack() }
            )
        }
    }
}
