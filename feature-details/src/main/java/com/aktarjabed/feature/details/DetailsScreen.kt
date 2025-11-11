package com.aktarjabed.feature.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun DetailsScreen(
    mediaId: String,
    viewModel: DetailsViewModel = viewModel()
) {
    viewModel.loadMediaItem(mediaId)
    val mediaItem by viewModel.mediaItem.collectAsState()

    mediaItem?.let {
        // UI to display media details
    }
}