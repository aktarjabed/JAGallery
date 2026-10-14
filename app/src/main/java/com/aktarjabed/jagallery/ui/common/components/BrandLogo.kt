package com.aktarjabed.jagallery.ui.common.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Brush
import com.aktarjabed.jagallery.ui.theme.PrimaryPurple
import com.aktarjabed.jagallery.ui.theme.PrimaryPurpleLight

@Composable
fun BrandLogo(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    primaryColor: Color = PrimaryPurple,
    highlightColor: Color = PrimaryPurpleLight
) {
    val petalBrush = Brush.radialGradient(
        colors = listOf(highlightColor, primaryColor),
        center = Offset.Unspecified
    )

    Canvas(modifier = modifier.size(size)) {
        val radius = this.size.minDimension / 2f
        val center = Offset(this.size.width / 2f, this.size.height / 2f)

        // 5 graceful organic petals around the center
        val petalCount = 5
        val petalRadius = radius * 0.48f
        val distance = radius * 0.42f

        for (i in 0 until petalCount) {
            val angle = i * (360f / petalCount) - 18f
            rotate(degrees = angle, pivot = center) {
                drawCircle(
                    brush = petalBrush,
                    radius = petalRadius,
                    center = Offset(center.x, center.y - distance),
                    alpha = 0.92f
                )
            }
        }

        // Soft center core for depth
        drawCircle(
            color = primaryColor.copy(alpha = 0.5f),
            radius = radius * 0.28f,
            center = center
        )
    }
}
