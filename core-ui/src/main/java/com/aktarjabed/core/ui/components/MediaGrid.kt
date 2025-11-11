package com.aktarjabed.core.ui.components

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.aktarjabed.core.data.model.MediaItem

@Composable
fun MediaGrid(
    mediaItems: List<MediaItem>,
    onItemClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 128.dp),
        modifier = modifier
    ) {
        items(mediaItems) { item ->
            MediaGridItem(
                item = item,
                onClick = { onItemClick(item) }
            )
        }
    }
}