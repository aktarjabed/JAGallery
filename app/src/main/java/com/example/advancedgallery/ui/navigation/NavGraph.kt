package com.example.advancedgallery.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.advancedgallery.ui.screens.albums.AlbumsScreen
import com.example.advancedgallery.ui.screens.favorites.FavoritesScreen
import com.example.advancedgallery.ui.screens.grid.GridScreen
import com.example.advancedgallery.ui.screens.search.SearchScreen
import com.example.advancedgallery.ui.screens.viewer.ViewerScreen
import com.example.advancedgallery.util.Constants

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
                onNavigateToViewer = { mediaId ->
                    navController.navigate(Screen.Viewer.createRoute(mediaId, bucketId))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Favorites.route) {
            FavoritesScreen(
                onNavigateToViewer = { mediaId ->
                    navController.navigate(Screen.Viewer.createRoute(mediaId, Constants.BUCKET_ID_FAVORITES))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Search.route) {
            SearchScreen(
                onNavigateToViewer = { mediaId, query ->
                    navController.navigate(Screen.Viewer.createRoute(mediaId, Constants.BUCKET_ID_SEARCH, query))
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.Viewer.route,
            arguments = listOf(
                navArgument("mediaId") { type = NavType.LongType },
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
            val mediaId = backStackEntry.arguments?.getLong("mediaId") ?: return@composable
            val bucketIdStr = backStackEntry.arguments?.getString("bucketId")
            val bucketId = bucketIdStr?.toLongOrNull()
            val searchQuery = backStackEntry.arguments?.getString("searchQuery")
            ViewerScreen(
                initialMediaId = mediaId,
                bucketId = bucketId,
                searchQuery = searchQuery,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
