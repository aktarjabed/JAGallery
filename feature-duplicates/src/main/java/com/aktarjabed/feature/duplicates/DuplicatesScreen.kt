package com.aktarjabed.feature.duplicates

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun DuplicatesScreen(
    viewModel: DuplicatesViewModel = viewModel()
) {
    val duplicates by viewModel.duplicates.collectAsState()

    // UI to display duplicate photos
}