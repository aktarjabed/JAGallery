package com.aktarjabed.jagallery.ui.screens.search

import android.widget.Toast
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aktarjabed.jagallery.R
import com.aktarjabed.jagallery.data.model.MediaLoadResult
import com.aktarjabed.jagallery.data.model.MediaSource
import com.aktarjabed.jagallery.ui.common.components.MediaCollectionContent
import com.aktarjabed.jagallery.ui.common.components.OperationToastEffect
import com.aktarjabed.jagallery.ui.theme.PrimaryPurple

private data class QuickSearchChip(
    val labelResId: Int,
    val queryValue: String,
    val icon: ImageVector,
    val tag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateToViewer: (mediaId: String, source: MediaSource) -> Unit,
    onBack: () -> Unit,
    viewModel: SearchViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val searchResult by viewModel.searchResult.collectAsStateWithLifecycle()
    val allAlbums by viewModel.allAlbums.collectAsStateWithLifecycle()
    val indexingState by viewModel.indexingState.collectAsStateWithLifecycle()

    val isSearching = query.isNotEmpty() && searchResult is MediaLoadResult.Loading

    val quickChips = listOf(
        QuickSearchChip(R.string.search_chip_people, "people", Icons.Default.Face, "chip_people"),
        QuickSearchChip(R.string.search_chip_ocr, "document", Icons.Default.Description, "chip_ocr"),
        QuickSearchChip(R.string.search_chip_receipts, "receipt", Icons.Default.Receipt, "chip_receipts"),
        QuickSearchChip(R.string.search_chip_screenshots, "screenshot", Icons.Default.PhoneAndroid, "chip_screenshots"),
        QuickSearchChip(R.string.search_chip_favorites, "favorite", Icons.Default.Favorite, "chip_favorites"),
        QuickSearchChip(R.string.search_chip_videos, "video", Icons.Default.Videocam, "chip_videos")
    )

    OperationToastEffect(viewModel.operationEvent)

    MediaCollectionContent(
        loadResult = searchResult,
        allAlbums = allAlbums,
        batchManager = viewModel.batchManager,
        onRemoveDeletedItems = { viewModel.removeDeletedItems(it) },
        emptyIcon = Icons.Default.Search,
        emptyMessage = if (query.isBlank()) stringResource(R.string.search_hint) else stringResource(R.string.no_search_results),
        topBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                TopAppBar(
                    title = {
                        Column {
                            TextField(
                                value = query,
                                onValueChange = { viewModel.updateSearchQuery(it) },
                                placeholder = { Text(stringResource(R.string.search_hint)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Search,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingIcon = {
                                    if (query.isNotEmpty()) {
                                        IconButton(
                                            onClick = { viewModel.updateSearchQuery("") },
                                            modifier = Modifier.testTag("clear_search_button")
                                        ) {
                                            Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.cancel))
                                        }
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(24.dp),
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    cursorColor = PrimaryPurple
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("search_input_field")
                                    .semantics { contentDescription = "Search input field" }
                            )
                            if (isSearching || indexingState.isIndexing) {
                                LinearProgressIndicator(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = PrimaryPurple
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent
                    )
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = {
                            viewModel.runSmartIndexing(context) { count ->
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.smart_index_complete, count),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        enabled = !indexingState.isIndexing,
                        label = {
                            if (indexingState.isIndexing) {
                                Text(
                                    stringResource(
                                        R.string.smart_indexing_progress,
                                        indexingState.processedCount,
                                        indexingState.totalCount
                                    )
                                )
                            } else {
                                Text(stringResource(R.string.smart_index_action))
                            }
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.testTag("smart_index_button")
                    )

                    quickChips.forEach { chip ->
                        val selected = query.equals(chip.queryValue, ignoreCase = true)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                if (selected) {
                                    viewModel.updateSearchQuery("")
                                } else {
                                    viewModel.updateSearchQuery(chip.queryValue)
                                }
                            },
                            label = { Text(stringResource(chip.labelResId)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = chip.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier.testTag(chip.tag)
                        )
                    }
                }
            }
        },
        onItemClick = { item ->
            onNavigateToViewer(item.id, MediaSource.Search(query))
        }
    )
}
