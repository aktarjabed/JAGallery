package com.aktarjabed.feature.album

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AlbumScreen(
    viewModel: AlbumViewModel = viewModel(),
    onMediaItemClick: (String) -> Unit
) {
    val albums by viewModel.albums.collectAsState()

    // UI to display albums
}