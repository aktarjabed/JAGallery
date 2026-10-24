package com.aktarjabed.jagallery.ui.screens.viewer.components

import android.net.Uri
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.aktarjabed.jagallery.R
import com.aktarjabed.jagallery.util.VideoTrimmer
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(
    uri: Uri,
    isPageVisible: Boolean,
    modifier: Modifier = Modifier,
    onTap: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var playerError by remember { mutableStateOf<androidx.media3.common.PlaybackException?>(null) }
    var isBuffering by remember { mutableStateOf(false) }
    var savedPosition by rememberSaveable { mutableLongStateOf(0L) }
    var savedPlayWhenReady by rememberSaveable { mutableStateOf(true) }
    var isMuted by rememberSaveable { mutableStateOf(false) }
    var isExtractingFrame by remember { mutableStateOf(false) }

    val exoPlayer = remember(uri) {
        ExoPlayer.Builder(context)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build().apply {
                setMediaItem(MediaItem.fromUri(uri))
                volume = if (isMuted) 0f else 1f
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        playerError = error
                    }
                    override fun onPlaybackStateChanged(playbackState: Int) {
                        isBuffering = (playbackState == Player.STATE_BUFFERING)
                    }
                })
            }
    }

    LaunchedEffect(isMuted) {
        exoPlayer.volume = if (isMuted) 0f else 1f
    }

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE, Lifecycle.Event.ON_STOP -> {
                    exoPlayer.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    if (isPageVisible) {
                        if (exoPlayer.playbackState == Player.STATE_IDLE) {
                            exoPlayer.prepare()
                            if (savedPosition > 0L) {
                                exoPlayer.seekTo(savedPosition)
                            }
                        }
                        if (savedPlayWhenReady) {
                            exoPlayer.play()
                        }
                    }
                }
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(isPageVisible) {
        if (isPageVisible && lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
            if (exoPlayer.playbackState == Player.STATE_IDLE) {
                exoPlayer.prepare()
                if (savedPosition > 0L) {
                    exoPlayer.seekTo(savedPosition)
                }
            }
            if (savedPlayWhenReady) {
                exoPlayer.play()
            }
        } else {
            exoPlayer.pause()
        }
    }

    DisposableEffect(uri) {
        onDispose {
            savedPosition = exoPlayer.currentPosition
            savedPlayWhenReady = exoPlayer.playWhenReady
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

        Surface(
            color = Color.Black.copy(alpha = 0.65f),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 72.dp, end = 16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                IconButton(
                    onClick = { isMuted = !isMuted },
                    modifier = Modifier.testTag("video_mute_toggle_button")
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = stringResource(if (isMuted) R.string.video_unmute else R.string.video_mute),
                        tint = Color.White
                    )
                }
                IconButton(
                    enabled = !isExtractingFrame,
                    onClick = {
                        val currentPositionMs = exoPlayer.currentPosition.coerceAtLeast(0L)
                        isExtractingFrame = true
                        coroutineScope.launch {
                            val frameUri = VideoTrimmer.extractVideoFrame(
                                context = context,
                                sourceUri = uri,
                                positionMs = currentPositionMs
                            )
                            isExtractingFrame = false
                            if (frameUri != null) {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.video_frame_saved),
                                    Toast.LENGTH_SHORT
                                ).show()
                            } else {
                                Toast.makeText(
                                    context,
                                    context.getString(R.string.video_frame_failed),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    },
                    modifier = Modifier.testTag("video_extract_frame_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = stringResource(R.string.video_extract_frame),
                        tint = Color.White
                    )
                }
            }
        }

        if (isBuffering) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center),
                color = Color.White
            )
        }

        if (playerError != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.video_playback_error),
                        color = Color.White
                    )
                    TextButton(onClick = {
                        playerError = null
                        exoPlayer.prepare()
                        if (savedPosition > 0L) {
                            exoPlayer.seekTo(savedPosition)
                        }
                    }) {
                        Text(stringResource(R.string.retry), color = Color.White)
                    }
                }
            }
        }
    }
}
