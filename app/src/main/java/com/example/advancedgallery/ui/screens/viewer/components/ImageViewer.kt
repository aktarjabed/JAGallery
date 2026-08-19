package com.example.advancedgallery.ui.screens.viewer.components

import android.net.Uri
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

@Composable
fun ImageViewer(
    uri: Uri,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {}
) {
    var scale by remember(uri) { mutableFloatStateOf(1f) }
    var offset by remember(uri) { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(uri) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            scale = 2.5f
                            offset = Offset.Zero
                        }
                    }
                )
            }
            .pointerInput(uri) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(1f, 5f)
                    val maxOffsetX = (size.width * (scale - 1)) / 2
                    val maxOffsetY = (size.height * (scale - 1)) / 2

                    val newOffset = offset + pan
                    offset = Offset(
                        newOffset.x.coerceIn(-maxOffsetX, maxOffsetX),
                        newOffset.y.coerceIn(-maxOffsetY, maxOffsetY)
                    )
                }
            }
            .graphicsLayer(
                scaleX = scale,
                scaleY = scale,
                translationX = if (scale > 1f) offset.x else 0f,
                translationY = if (scale > 1f) offset.y else 0f
            )
    ) {
        AsyncImage(
            model = uri,
            contentDescription = "Full Image",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )
    }
}
