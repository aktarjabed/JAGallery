package com.example.advancedgallery.ui.screens.viewer.components

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    uri: Uri,
    isPageVisible: Boolean,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {}
) {
    val context = LocalContext.current
    val exoPlayer = remember(uri) {
        ExoPlayer.Builder(context)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build().apply {
            setMediaItem(MediaItem.fromUri(uri))
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    var isAppInForeground = remember { androidx.compose.runtime.mutableStateOf(true) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP) {
                isAppInForeground.value = false
            } else if (event == Lifecycle.Event.ON_RESUME) {
                isAppInForeground.value = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(isPageVisible, isAppInForeground.value) {
        if (isPageVisible && isAppInForeground.value) {
            if (exoPlayer.playbackState == androidx.media3.common.Player.STATE_IDLE) {
                exoPlayer.prepare()
            }
            exoPlayer.play()
        } else {
            exoPlayer.pause()
        }
    }

    DisposableEffect(uri) {
        onDispose {
            exoPlayer.release()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(uri) {
                detectTapGestures(
                    onTap = { onTap() }
                )
            }
    ) {
        AndroidView(
            factory = {
                PlayerView(context).apply {
                    player = exoPlayer
                    useController = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
