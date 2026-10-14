package com.aktarjabed.jagallery

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.aktarjabed.jagallery.data.repository.MediaRepository
import com.aktarjabed.jagallery.domain.SettingsRepository
import com.aktarjabed.jagallery.domain.ThemeMode
import com.aktarjabed.jagallery.domain.VaultSessionManager
import com.aktarjabed.jagallery.ui.common.components.PermissionHandler
import com.aktarjabed.jagallery.ui.navigation.NavGraph
import com.aktarjabed.jagallery.ui.theme.AdvancedGalleryTheme
import com.aktarjabed.jagallery.util.MediaStoreObserverManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    @Inject
    lateinit var repository: MediaRepository

    @Inject
    lateinit var vaultSessionManager: VaultSessionManager

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private var observerManager: MediaStoreObserverManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        observerManager = MediaStoreObserverManager(
            context = applicationContext,
            scope = lifecycleScope,
            onMediaStoreChanged = {
                lifecycleScope.launch {
                    repository.loadMedia(force = true, context = applicationContext)
                }
            }
        )

        setContent {
            val themeMode by settingsRepository.themeMode.collectAsStateWithLifecycle()
            val dynamicColor by settingsRepository.dynamicColor.collectAsStateWithLifecycle()

            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            AdvancedGalleryTheme(
                darkTheme = darkTheme,
                dynamicColor = dynamicColor
            ) {
                PermissionHandler(
                    onPermissionChanged = {
                        lifecycleScope.launch {
                            repository.loadMedia(force = true, context = applicationContext)
                        }
                    }
                ) {
                    NavGraph()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (vaultSessionManager.checkTimeout()) {
            // Re-locked due to timeout, let VaultRepository clean up
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
