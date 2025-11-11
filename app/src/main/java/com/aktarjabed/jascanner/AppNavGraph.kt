package com.aktarjabed.jascanner

import androidx.compose.material3.ExperimentalMaterial3Crom
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.aktarjabed.jascanner.screens.*
import com.aktarjabed.jascanner.ui.navigation.JAScannerAnimatedNavHost
import com.aktarjabed.jascanner.ui.navigation.animatedComposable

object Routes {
    const val HOME = "home"
    const val VIEWER = "viewer/{id}"
    const val EDITOR = "editor/{id}"
    const val VAULT = "vault"
    const val SEARCH = "search"
    const val STORIES = "stories"
    const val CATEGORIES = "categories"
    const val RECYCLE_BIN = "recycle_bin"
    const val RECYCLE_BIN_DASHBOARD = "recycle_bin_dashboard"
    const val SETTINGS = "settings"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavGraph(
    navController: NavHostController,
    snackbarHostState: SnackbarHostState
) {
    JAScannerAnimatedNavHost(
        navController = navController,
        startDestination = Routes.HOME
    ) {
        animatedComposable(Routes.HOME) {
            ModernHomeScreen(
                navController = navController,
                photos = emptyList(), // Replace with your photos
                onPhotoClick = { id -> navController.navigate("viewer/$id") },
                snackbarHostState = snackbarHostState
            )
        }

        animatedComposable(Routes.VIEWER) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            ViewerScreen(
                id = id,
                onBack = { navController.popBackStack() },
                onEdit = { id -> navController.navigate("editor/$id") },
                snackbarHostState = snackbarHostState
            )
        }

        animatedComposable(Routes.EDITOR) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: ""
            EditorScreen(
                id = id,
                onBack = { navController.popBackStack() },
                onSave = { navController.popBackStack() },
                snackbarHostState = snackbarHostState
            )
        }

        animatedComposable(Routes.VAULT) {
            VaultScreen(
                onBack = { navController.popBackStack() }
            )
        }

        animatedComposable(Routes.SEARCH) {
            SearchScreen(
                onBack = { navController.popBackStack() },
                onPhotoClick = { id -> navController.navigate("viewer/$id") }
            )
        }

        animatedComposable(Routes.STORIES) {
            StoriesScreen(
                onBack = { navController.popBackStack() }
            )
        }

        animatedComposable(Routes.CATEGORIES) {
            CategoriesScreen(
                navController = navController,
                snackbarHostState = snackbarHostState
            )
        }

        animatedComposable(Routes.RECYCLE_BIN) {
            RecycleBinScreen(
                onBack = { navController.popBackStack() },
                snackbarHostState = snackbarHostState
            )
        }

        animatedComposable(Routes.RECYCLE_BIN_DASHBOARD) {
            RecycleBinDashboard(
                onBack = { navController.popBackStack() },
                onViewRecycleBin = { navController.navigate(Routes.RECYCLE_BIN) },
                snackbarHostState = snackbarHostState
            )
        }
    }
}