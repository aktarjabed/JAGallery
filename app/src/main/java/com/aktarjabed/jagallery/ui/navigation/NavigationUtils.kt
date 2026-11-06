package com.aktarjabed.jagallery.ui.navigation

import androidx.navigation.NavController
import com.aktarjabed.jagallery.data.model.MediaSource

fun NavController.navigateToGridAndPop() {
    navigate(Screen.Grid.createRoute(MediaSource.All)) {
        popUpTo(Screen.Albums.route)
    }
}

fun NavController.navigateToAlbumsAndPop() {
    navigate(Screen.Albums.route) {
        popUpTo(Screen.Albums.route) { inclusive = true }
    }
}

fun NavController.navigateToTrashAndPop() {
    navigate(Screen.Trash.route) {
        popUpTo(Screen.Albums.route)
    }
}

fun NavController.navigateToVaultAndPop() {
    navigate(Screen.Vault.route) {
        popUpTo(Screen.Albums.route)
    }
}
