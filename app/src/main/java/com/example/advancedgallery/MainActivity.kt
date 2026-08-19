package com.example.advancedgallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.advancedgallery.data.repository.MediaRepository
import com.example.advancedgallery.ui.common.components.PermissionHandler
import com.example.advancedgallery.ui.navigation.NavGraph
import com.example.advancedgallery.ui.theme.AdvancedGalleryTheme
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var repository: MediaRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AdvancedGalleryTheme {
                PermissionHandler(
                    onPermissionChanged = {
                        lifecycleScope.launch {
                            repository.loadMedia()
                        }
                    }
                ) {
                    NavGraph()
                }
            }
        }
    }
}
