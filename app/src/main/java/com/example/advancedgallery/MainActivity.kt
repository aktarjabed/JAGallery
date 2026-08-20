package com.example.advancedgallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.lifecycleScope
import com.example.advancedgallery.data.repository.MediaRepository
import com.example.advancedgallery.ui.common.components.PermissionHandler
import com.example.advancedgallery.ui.navigation.NavGraph
import com.example.advancedgallery.ui.theme.AdvancedGalleryTheme
import com.example.advancedgallery.util.MediaStoreObserverManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var repository: MediaRepository

    private var observerManager: MediaStoreObserverManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        observerManager = MediaStoreObserverManager(
            context = applicationContext,
            scope = lifecycleScope,
            onMediaStoreChanged = {
                lifecycleScope.launch {
                    repository.loadMedia(force = true, context = this@MainActivity)
                }
            }
        )

        setContent {
            AdvancedGalleryTheme {
                PermissionHandler(
                    onPermissionChanged = {
                        lifecycleScope.launch {
                            repository.loadMedia(force = true, context = this@MainActivity)
                        }
                    }
                ) {
                    NavGraph()
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        observerManager?.startObserving()
    }

    override fun onStop() {
        super.onStop()
        observerManager?.stopObserving()
    }
}
