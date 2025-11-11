package com.aktarjabed.jagallery

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.aktarjabed.feature_editor.EditorScreen
import com.aktarjabed.feature_viewer.ViewerScreen

@Composable
fun JAGalleryApp() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        // Add other composables here
        composable("viewer/{mediaId}") { backStackEntry ->
            val mediaId = backStackEntry.arguments?.getString("mediaId")
            requireNotNull(mediaId) { "mediaId parameter wasn't found. Please make sure it's set!" }
            ViewerScreen(mediaId = mediaId, onEdit = {
                navController.navigate("editor/$it")
            })
        }
        composable("editor/{mediaId}") { backStackEntry ->
            val mediaId = backStackEntry.arguments?.getString("mediaId")
            requireNotNull(mediaId) { "mediaId parameter wasn't found. Please make sure it's set!" }
            EditorScreen(mediaId = mediaId)
        }
    }
}