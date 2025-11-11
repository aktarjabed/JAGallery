package com.aktarjabed.jascanner.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavBackStackEntry
import com.google.accompanist.navigation.animation.composable

@Composable
fun rememberJAScannerNavController() = rememberAnimatedNavController()

@Composable
fun JAScannerAnimatedNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    route: String? = null,
    builder: NavGraphBuilder.() -> Unit
) {
    AnimatedNavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        route = route
    ) {
        builder()
    }
}

fun NavGraphBuilder.animatedComposable(
    route: String,
    enterTransition: (AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition) = {
        slideIntoContainer(
            towards = AnimatedContentScope.SlideDirection.Left,
            animationSpec = tween(400)
        ) + fadeIn(animationSpec = tween(400))
    },
    exitTransition: (AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition) = {
        slideOutOfContainer(
            towards = AnimatedContentScope.SlideDirection.Left,
            animationSpec = tween(400)
        ) + fadeOut(animationSpec = tween(400))
    },
    popEnterTransition: (AnimatedContentScope<NavBackStackEntry>.() -> EnterTransition) = {
        slideIntoContainer(
            towards = AnimatedContentScope.SlideDirection.Right,
            animationSpec = tween(400)
        ) + fadeIn(animationSpec = tween(400))
    },
    popExitTransition: (AnimatedContentScope<NavBackStackEntry>.() -> ExitTransition) = {
        slideOutOfContainer(
            towards = AnimatedContentScope.SlideDirection.Right,
            animationSpec = tween(400)
        ) + fadeOut(animationSpec = tween(400))
    },
    content: @Composable AnimatedVisibilityScope.(NavBackStackEntry) -> Unit
) {
    composable(
        route = route,
        enterTransition = enterTransition,
        exitTransition = exitTransition,
        popEnterTransition = popEnterTransition,
        popExitTransition = popExitTransition,
        content = content
    )
}
