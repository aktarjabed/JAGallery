package com.example.advancedgallery.ui.common.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
                text = "Sort & Filter",
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Sort By",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
            )

            SortOption.entries.forEach { option ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onApply(option, currentSortOrder, currentFilter) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = currentSortOption == option,
                        onClick = { onApply(option, currentSortOrder, currentFilter) }
                    )
                    Text(
                        text = option.name.lowercase().replaceFirstChar { it.uppercase() },
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Order",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SortOrder.entries.forEach { order ->
                    FilterChip(
                        selected = currentSortOrder == order,
                        onClick = { onApply(currentSortOption, order, currentFilter) },
                        label = { Text(order.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Filter Media",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MediaTypeFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = currentFilter == filter,
                        onClick = { onApply(currentSortOption, currentSortOrder, filter) },
                        label = {
                            Text(
                                when (filter) {
                                    MediaTypeFilter.ALL -> "All"
                                    MediaTypeFilter.IMAGES_ONLY -> "Images Only"
                                    MediaTypeFilter.VIDEOS_ONLY -> "Videos Only"
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
                    Text("Close")
                }
            }
        }
    }
}
