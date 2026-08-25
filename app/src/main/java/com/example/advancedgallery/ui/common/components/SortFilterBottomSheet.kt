package com.example.advancedgallery.ui.common.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.example.advancedgallery.R

enum class SortOption { DATE, NAME, SIZE }
enum class SortOrder { ASCENDING, DESCENDING }
enum class MediaTypeFilter { ALL, IMAGES_ONLY, VIDEOS_ONLY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortFilterBottomSheet(
    sheetState: SheetState,
    currentSortOption: SortOption,
    currentSortOrder: SortOrder,
    currentFilter: MediaTypeFilter,
    onApply: (SortOption, SortOrder, MediaTypeFilter) -> Unit,
    onDismiss: () -> Unit
) {
    var pendingSortOption by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(currentSortOption) }
    var pendingSortOrder by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(currentSortOrder) }
    var pendingFilter by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(currentFilter) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                text = androidx.compose.ui.res.stringResource(com.example.advancedgallery.R.string.sort_filter_title),
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = androidx.compose.ui.res.stringResource(com.example.advancedgallery.R.string.sort_by_label),
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
            )

            SortOption.entries.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { pendingSortOption = option }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = pendingSortOption == option,
                        onClick = { pendingSortOption = option }
                    )
                    Text(
                        text = option.name.lowercase().replaceFirstChar { it.uppercase() },
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = androidx.compose.ui.res.stringResource(com.example.advancedgallery.R.string.sort_order_label),
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SortOrder.entries.forEach { order ->
                    FilterChip(
                        selected = pendingSortOrder == order,
                        onClick = { pendingSortOrder = order },
                        label = { Text(order.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = androidx.compose.ui.res.stringResource(com.example.advancedgallery.R.string.filter_media_label),
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MediaTypeFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = pendingFilter == filter,
                        onClick = { pendingFilter = filter },
                        label = {
                            Text(
                                when (filter) {
                                    MediaTypeFilter.ALL -> androidx.compose.ui.res.stringResource(com.example.advancedgallery.R.string.filter_all)
                                    MediaTypeFilter.IMAGES_ONLY -> androidx.compose.ui.res.stringResource(com.example.advancedgallery.R.string.filter_images_only)
                                    MediaTypeFilter.VIDEOS_ONLY -> androidx.compose.ui.res.stringResource(com.example.advancedgallery.R.string.filter_videos_only)
                                }
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(androidx.compose.ui.res.stringResource(com.example.advancedgallery.R.string.sort_filter_close))
                }
                Spacer(modifier = Modifier.padding(8.dp))
                androidx.compose.material3.Button(onClick = {
                    onApply(pendingSortOption, pendingSortOrder, pendingFilter)
                }) {
                    Text(androidx.compose.ui.res.stringResource(com.example.advancedgallery.R.string.sort_filter_apply))
                }
            }
        }
    }
}
