package com.aktarjabed.jagallery.ui.common.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aktarjabed.jagallery.R
import com.aktarjabed.jagallery.ui.theme.PrimaryPurple

enum class GalleryFilterTab {
    ALBUMS, PHOTOS, VIDEOS, FAVORITES
}

@Composable
fun FilterChipRow(
    selectedTab: GalleryFilterTab,
    onTabSelected: (GalleryFilterTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterTabChip(
            label = stringResource(R.string.tab_albums),
            icon = Icons.Default.Folder,
            selected = selectedTab == GalleryFilterTab.ALBUMS,
            onClick = { onTabSelected(GalleryFilterTab.ALBUMS) }
        )
        FilterTabChip(
            label = stringResource(R.string.tab_photos),
            icon = Icons.Default.Photo,
            selected = selectedTab == GalleryFilterTab.PHOTOS,
            onClick = { onTabSelected(GalleryFilterTab.PHOTOS) }
        )
        FilterTabChip(
            label = stringResource(R.string.tab_videos),
            icon = Icons.Default.PlayCircle,
            selected = selectedTab == GalleryFilterTab.VIDEOS,
            onClick = { onTabSelected(GalleryFilterTab.VIDEOS) }
        )
        FilterTabChip(
            label = stringResource(R.string.tab_favorites),
            icon = Icons.Default.Favorite,
            selected = selectedTab == GalleryFilterTab.FAVORITES,
            onClick = { onTabSelected(GalleryFilterTab.FAVORITES) }
        )
    }
}

@Composable
private fun FilterTabChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text = label, style = MaterialTheme.typography.labelLarge) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        shape = RoundedCornerShape(24.dp),
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            labelColor = MaterialTheme.colorScheme.onSurface,
            iconColor = MaterialTheme.colorScheme.onSurface,
            selectedContainerColor = PrimaryPurple,
            selectedLabelColor = Color.Black,
            selectedLeadingIconColor = Color.Black
        ),
        border = null
    )
}
