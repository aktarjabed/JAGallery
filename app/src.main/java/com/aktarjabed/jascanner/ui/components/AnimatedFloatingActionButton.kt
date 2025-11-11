package com.aktarjabed.jascanner.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedFloatingActionButton(
    expanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    items: List<FabItem>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .wrapContentSize(Alignment.BottomEnd)
    ) {
        // Child FABs
        if (expanded) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.End
            ) {
                items.forEach { item ->
                    SmallFloatingActionButton(
                        onClick = item.onClick,
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ) {
                        Icon(item.icon, item.contentDescription)
                    }
                }
            }
        }

        // Main FAB
        val rotation by animateFloatAsState(
            targetValue = if (expanded) 45f else 0f,
            label = "fab_rotation"
        )

        FloatingActionButton(
            onClick = { onExpandChange(!expanded) },
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                Icons.Default.Add,
                contentDescription = if (expanded) "Collapse" else "Expand",
                modifier = Modifier.rotate(rotation)
            )
        }
    }
}

data class FabItem(
    val icon: ImageVector,
    val contentDescription: String,
    val onClick: () -> Unit
)
