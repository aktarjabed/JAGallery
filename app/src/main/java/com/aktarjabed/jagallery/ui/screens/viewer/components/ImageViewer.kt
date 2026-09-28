package com.aktarjabed.jagallery.ui.screens.viewer.components

import android.net.Uri
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import com.aktarjabed.jagallery.R
import kotlinx.coroutines.launch

@Composable
fun ImageViewer(
    uri: Uri,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {}
) {
    val animatedScale = remember(uri) { Animatable(1f) }
    var offset by remember(uri) { mutableStateOf(Offset.Zero) }
    val scope = rememberCoroutineScope()
    var isError by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(uri) {
                detectTapGestures(
                    onTap = { onTap() },
                    onDoubleTap = {
                        scope.launch {
                            val targetScale = if (animatedScale.value > 1f) 1f else 2.5f
                            animatedScale.animateTo(targetScale, animationSpec = spring())
                            if (animatedScale.value == 1f) {
                                offset = Offset.Zero
                            }
                        }
                    }
                )
            }
            .pointerInput(uri) {
                detectTransformGestures { centroid, pan, zoom, _ ->
                    scope.launch {
                        val prevScale = animatedScale.value
                        animatedScale.snapTo((animatedScale.value * zoom).coerceIn(1f, 5f))

                        val centroidOffset = centroid - Offset(size.width / 2f, size.height / 2f)
                        offset = offset + (centroidOffset * (1 - zoom)) + pan

                        val maxOffsetX = (size.width * (animatedScale.value - 1)) / 2
                        val maxOffsetY = (size.height * (animatedScale.value - 1)) / 2

                        offset = Offset(
                            offset.x.coerceIn(-maxOffsetX, maxOffsetX),
                            offset.y.coerceIn(-maxOffsetY, maxOffsetY)
                        )
                    }
                }
            }
            .graphicsLayer(
                scaleX = animatedScale.value,
                scaleY = animatedScale.value,
                translationX = if (animatedScale.value > 1f) offset.x else 0f,
                translationY = if (animatedScale.value > 1f) offset.y else 0f
            )
    ) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val imageRequest = coil.request.ImageRequest.Builder(context)
            .data(uri)
            .crossfade(true)
            .build()
        AsyncImage(
            model = imageRequest,
            contentDescription = stringResource(R.string.cd_full_image),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
            error = ColorPainter(MaterialTheme.colorScheme.errorContainer),
            onError = { isError = true }
        )
        if (isError) {
            Text(
                text = stringResource(R.string.image_load_error),
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}
