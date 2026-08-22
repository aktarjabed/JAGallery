package com.example.advancedgallery.ui.common.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.grid.GridItemSpan
import java.util.Calendar
import java.util.Date
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.text.font.FontWeight
import com.example.advancedgallery.data.model.MediaItem

@Composable
fun MediaGrid(
    items: List<MediaItem>,
    selectedIds: Set<String>,
    selectionMode: Boolean,
    onItemClick: (MediaItem) -> Unit,
    onItemLongClick: (MediaItem) -> Unit,
    modifier: Modifier = Modifier,
    emptyIcon: ImageVector? = null,
    emptyMessage: String = "No media found"
) {
    if (items.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (emptyIcon != null) {
                    Icon(
                        imageVector = emptyIcon,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                Text(
                    text = emptyMessage,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        // Group items by timeline
        val groupedItems = remember(items) {
            items.groupBy { item ->
                val time = if (item.dateAdded > 10000000000L) item.dateAdded else item.dateAdded * 1000
                val cal = Calendar.getInstance().apply { timeInMillis = time }
                val now = Calendar.getInstance()
                val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }

                if (cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) && cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) {
                    "Today"
                } else if (cal.get(Calendar.YEAR) == yesterday.get(Calendar.YEAR) && cal.get(Calendar.DAY_OF_YEAR) == yesterday.get(Calendar.DAY_OF_YEAR)) {
                    "Yesterday"
                } else if (cal.get(Calendar.YEAR) == now.get(Calendar.YEAR)) {
                    val month = cal.getDisplayName(Calendar.MONTH, Calendar.LONG, java.util.Locale.getDefault())
                    month ?: "Earlier"
                } else {
                    cal.get(Calendar.YEAR).toString()
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(2.dp)
        ) {
            groupedItems.forEach { (header, list) ->
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        text = header,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 16.dp)
                    )
                }
                items(list, key = { it.id }) { item ->
                    MediaThumbnail(
                        mediaItem = item,
                        isSelected = selectedIds.contains(item.id),
                        selectionMode = selectionMode,
                        onClick = { onItemClick(item) },
                        onLongClick = { onItemLongClick(item) }
                    )
                }
            }
        }
    }
}
