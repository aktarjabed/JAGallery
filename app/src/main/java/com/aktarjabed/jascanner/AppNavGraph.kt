package com.aktarjabed.jascanner

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.aktarjabed.jascanner.screens.*

object Routes {
    const val HOME = "home"
    const val VIEWER = "viewer/{id}"
    const val EDITOR = "editor/{id}"
    const val VAULT = "vault"
    const val SEARCH = "search"
    const val STORIES = "stories"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onPhotoClick = { id -> navController.navigate("viewer/$id") },
                onVaultClick = { navController.navigate(Routes.VAULT) },
                onSearchClick = { navController.navigate(Routes.SEARCH) },
                onStoriesClick = { navController.navigate(Routes.STORIES) },
                snackbarHostState = snackbarHostState
            )
        }

        composable(Routes.VIEWER) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            ViewerScreen(
                id = id,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate("editor/$id") },
                snackbarHostState = snackbarHostState
            )
        }

        composable(Routes.EDITOR) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            EditorScreen(
                id = id,
                onBack = { navController.popBackStack() },
                onSave = { navController.popBackStack() },
                snackbarHostState = snackbarHostState
            )
        }

        composable(Routes.VAULT) {
            VaultScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SEARCH) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onPhotoClick = { id -> navController.navigate("viewer/$id") }
            )
        }

        composable(Routes.STORIES) {
            StoriesScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}