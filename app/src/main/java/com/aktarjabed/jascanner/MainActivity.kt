package com.aktarjabed.jascanner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.aktarjabed.jascanner.ui.theme.ThemeEngine
import com.aktarjabed.jascanner.ui.theme.dynamicColorScheme
import com.aktarjabed.jascanner.ui.navigation.rememberJAScannerNavController

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val systemUiController = rememberSystemUiController()
            val isSystemInDarkTheme = isSystemInDarkTheme()
            val dynamicColorScheme = dynamicColorScheme(
                theme = ThemeEngine.currentTheme,
                isSystemInDarkTheme = isSystemInDarkTheme
            )

            // Update system bars
            LaunchedEffect(ThemeEngine.currentTheme, dynamicColorScheme) {
                systemUiController.setSystemBarsColor(
                    color = dynamicColorScheme.surface,
                    darkIcons = !isSystemInDarkTheme
                )
            }

            MaterialTheme(
                colorScheme = dynamicColorScheme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberJAScannerNavController()
                    val snackbarHostState = remember { SnackbarHostState() }

                    AppNavGraph(
                        navController = navController,
                        snackbarHostState = snackbarHostState
                    )
                }
            }
        }
    }
}