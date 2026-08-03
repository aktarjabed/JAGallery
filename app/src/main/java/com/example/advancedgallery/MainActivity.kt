package com.example.advancedgallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.advancedgallery.ui.common.components.PermissionHandler
import com.example.advancedgallery.ui.navigation.NavGraph
import com.example.advancedgallery.ui.theme.AdvancedGalleryTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AdvancedGalleryTheme {
                PermissionHandler {
                    NavGraph()
                }
            }
        }
    }
}
