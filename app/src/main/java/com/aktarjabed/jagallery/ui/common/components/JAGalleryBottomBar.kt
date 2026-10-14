package com.aktarjabed.jagallery.ui.common.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.aktarjabed.jagallery.R
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aktarjabed.jagallery.ui.theme.PrimaryPurple

enum class MainBottomTab {
    PHOTOS, ALBUMS, TRASH, VAULT
}

@Composable
fun JAGalleryBottomBar(
    currentTab: MainBottomTab,
    onTabSelected: (MainBottomTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = 0.dp,
        modifier = modifier
    ) {
        BottomNavItem(
            selected = currentTab == MainBottomTab.PHOTOS,
            onClick = { onTabSelected(MainBottomTab.PHOTOS) },
            selectedIcon = Icons.Default.PhotoLibrary,
            unselectedIcon = Icons.Outlined.PhotoLibrary,
            label = "Photos"
        )
        BottomNavItem(
            selected = currentTab == MainBottomTab.ALBUMS,
            onClick = { onTabSelected(MainBottomTab.ALBUMS) },
            selectedIcon = Icons.Default.Folder,
            unselectedIcon = Icons.Outlined.Folder,
            label = "Albums"
        )
        BottomNavItem(
            selected = currentTab == MainBottomTab.TRASH,
            onClick = { onTabSelected(MainBottomTab.TRASH) },
            selectedIcon = Icons.Default.Delete,
            unselectedIcon = Icons.Outlined.Delete,
            label = stringResource(R.string.tab_trash)
        )
        BottomNavItem(
            selected = currentTab == MainBottomTab.VAULT,
            onClick = { onTabSelected(MainBottomTab.VAULT) },
            selectedIcon = Icons.Default.Lock,
            unselectedIcon = Icons.Outlined.Lock,
            label = stringResource(R.string.vault_title).let { "Vault" }
        )
    }
}

@Composable
private fun RowScope.BottomNavItem(
    selected: Boolean,
    onClick: () -> Unit,
    selectedIcon: ImageVector,
    unselectedIcon: ImageVector,
    label: String
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        icon = {
            Icon(
                imageVector = if (selected) selectedIcon else unselectedIcon,
                contentDescription = label,
                modifier = Modifier.size(24.dp)
            )
        },
        label = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) PrimaryPurple else MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (selected) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Box(
                        modifier = Modifier
                            .width(18.dp)
                            .height(2.5.dp)
                            .background(PrimaryPurple, RoundedCornerShape(2.dp))
                    )
                }
            }
        },
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = PrimaryPurple,
            selectedTextColor = PrimaryPurple,
            indicatorColor = Color.Transparent,
            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    )
}
