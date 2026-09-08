package com.aktarjabed.jagallery.ui.screens.editor.components

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.max
import kotlin.math.min

@Composable
fun InteractiveCropOverlay(
    cropRect: RectF,
    onCropChange: (RectF) -> Unit,
    modifier: Modifier = Modifier
) {
    val handleRadius = 40f

    Canvas(modifier = modifier
        .fillMaxSize()
        .pointerInput(cropRect) {
            var draggedHandle: Handle? = null

            detectDragGestures(
                onDragStart = { offset ->
                    val width = size.width
                    val height = size.height

                    val left = cropRect.left * width
                    val top = cropRect.top * height
                    val right = cropRect.right * width
                    val bottom = cropRect.bottom * height

                    // Simple heuristic: which corner is closest
                    val distTL = Offset(left, top).getDistanceSquared(offset)
                    val distTR = Offset(right, top).getDistanceSquared(offset)
                    val distBL = Offset(left, bottom).getDistanceSquared(offset)
                    val distBR = Offset(right, bottom).getDistanceSquared(offset)

                    val thresholdSq = handleRadius * handleRadius

                    draggedHandle = when {
                        distTL < thresholdSq -> Handle.TopLeft
                        distTR < thresholdSq -> Handle.TopRight
                        distBL < thresholdSq -> Handle.BottomLeft
                        distBR < thresholdSq -> Handle.BottomRight
                        else -> null
                    }
                },
                onDrag = { change, dragAmount ->
                    if (draggedHandle != null) {
                        change.consume()
                        val width = size.width
                        val height = size.height

                        var newLeft = cropRect.left
                        var newTop = cropRect.top
                        var newRight = cropRect.right
                        var newBottom = cropRect.bottom

                        val dx = dragAmount.x / width
                        val dy = dragAmount.y / height

                        when (draggedHandle) {
                            Handle.TopLeft -> {
                                newLeft = (newLeft + dx).coerceIn(0f, newRight - 0.1f)
                                newTop = (newTop + dy).coerceIn(0f, newBottom - 0.1f)
                            }
                            Handle.TopRight -> {
                                newRight = (newRight + dx).coerceIn(newLeft + 0.1f, 1f)
                                newTop = (newTop + dy).coerceIn(0f, newBottom - 0.1f)
                            }
                            Handle.BottomLeft -> {
                                newLeft = (newLeft + dx).coerceIn(0f, newRight - 0.1f)
                                newBottom = (newBottom + dy).coerceIn(newTop + 0.1f, 1f)
                            }
                            Handle.BottomRight -> {
                                newRight = (newRight + dx).coerceIn(newLeft + 0.1f, 1f)
                                newBottom = (newBottom + dy).coerceIn(newTop + 0.1f, 1f)
                            }
                            null -> {}
                        }

                        onCropChange(RectF(newLeft, newTop, newRight, newBottom))
                    }
                },
                onDragEnd = {
                    draggedHandle = null
                },
                onDragCancel = {
                    draggedHandle = null
                }
            )
        }
    ) {
        val width = size.width
        val height = size.height

        val left = cropRect.left * width
        val top = cropRect.top * height
        val right = cropRect.right * width
        val bottom = cropRect.bottom * height

        // Draw dimmed background
        drawRect(Color.Black.copy(alpha = 0.5f), size = Size(width, top)) // Top
        drawRect(Color.Black.copy(alpha = 0.5f), topLeft = Offset(0f, bottom), size = Size(width, height - bottom)) // Bottom
        drawRect(Color.Black.copy(alpha = 0.5f), topLeft = Offset(0f, top), size = Size(left, bottom - top)) // Left
        drawRect(Color.Black.copy(alpha = 0.5f), topLeft = Offset(right, top), size = Size(width - right, bottom - top)) // Right

        // Draw crop box border
        drawRect(
            color = Color.White,
            topLeft = Offset(left, top),
            size = Size(right - left, bottom - top),
            style = Stroke(width = 4f)
        )

        // Draw handles (corners)
        val handleColor = Color.White
        val handleSize = 30f
        val strokeWidth = 8f

        // Top Left
        drawLine(handleColor, Offset(left, top), Offset(left + handleSize, top), strokeWidth)
        drawLine(handleColor, Offset(left, top), Offset(left, top + handleSize), strokeWidth)

        // Top Right
        drawLine(handleColor, Offset(right, top), Offset(right - handleSize, top), strokeWidth)
        drawLine(handleColor, Offset(right, top), Offset(right, top + handleSize), strokeWidth)

        // Bottom Left
        drawLine(handleColor, Offset(left, bottom), Offset(left + handleSize, bottom), strokeWidth)
        drawLine(handleColor, Offset(left, bottom), Offset(left, bottom - handleSize), strokeWidth)

        // Bottom Right
        drawLine(handleColor, Offset(right, bottom), Offset(right - handleSize, bottom), strokeWidth)
        drawLine(handleColor, Offset(right, bottom), Offset(right, bottom - handleSize), strokeWidth)
    }
}

private enum class Handle {
    TopLeft, TopRight, BottomLeft, BottomRight
}

private fun Offset.getDistanceSquared(other: Offset): Float {
    val dx = this.x - other.x
    val dy = this.y - other.y
    return dx * dx + dy * dy
}
