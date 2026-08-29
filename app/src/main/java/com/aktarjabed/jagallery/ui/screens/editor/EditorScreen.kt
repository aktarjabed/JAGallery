package com.aktarjabed.jagallery.ui.screens.editor

import android.net.Uri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.RotateLeft
import androidx.compose.material.icons.automirrored.filled.RotateRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Crop
import android.graphics.RectF
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.aktarjabed.jagallery.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    imageUri: Uri,
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    viewModel: EditorViewModel = hiltViewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(imageUri) {
        viewModel.loadImage(context, imageUri)
    }

    val previewBitmap by viewModel.previewBitmap.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val brightness by viewModel.brightness.collectAsStateWithLifecycle()
    val contrast by viewModel.contrast.collectAsStateWithLifecycle()
    val saturation by viewModel.saturation.collectAsStateWithLifecycle()
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val flipHorizontal by viewModel.flipHorizontal.collectAsStateWithLifecycle()
    val flipVertical by viewModel.flipVertical.collectAsStateWithLifecycle()


    var activeTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(saveState) {
        when (val state = saveState) {
            is SaveState.Success -> {
                Toast.makeText(context, context.getString(R.string.image_saved), Toast.LENGTH_SHORT).show()
                onSaveSuccess()
            }
            is SaveState.Error -> {
                Toast.makeText(context, context.getString(state.messageResId), Toast.LENGTH_SHORT).show()
            }
            else -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.edit_photo)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.reset() }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.reset))
                    }
                    IconButton(
                        onClick = { viewModel.save(context) },
                        enabled = saveState !is SaveState.Saving && previewBitmap != null
                    ) {
                        if (saveState is SaveState.Saving) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Check, contentDescription = stringResource(R.string.save))
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 4.dp
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    if (activeTab == 0) {
                        val imageWidth = previewBitmap?.width?.toFloat() ?: 1f
                        val imageHeight = previewBitmap?.height?.toFloat() ?: 1f
                        val aspect = imageWidth / imageHeight

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(onClick = { viewModel.setCropRect(null) }) {
                                Text(stringResource(R.string.crop_original))
                            }
                            Button(onClick = {
                                val rect = if (aspect > 1f) {
                                    // Landscape image: Square crop is limited by height
                                    val size = 1f
                                    val w = 1f / aspect
                                    val left = (1f - w) / 2f
                                    RectF(left, 0f, left + w, 1f)
                                } else {
                                    // Portrait image: Square crop is limited by width
                                    val size = 1f
                                    val h = aspect
                                    val top = (1f - h) / 2f
                                    RectF(0f, top, 1f, top + h)
                                }
                                viewModel.setCropRect(rect)
                            }) {
                                Text(stringResource(R.string.crop_square))
                            }
                            Button(onClick = {
                                // Free crop mapping to an arbitrary central crop
                                viewModel.setCropRect(RectF(0.1f, 0.1f, 0.9f, 0.9f))
                            }) {
                                Text(stringResource(R.string.crop_free))
                            }
                        }
                    } else if (activeTab == 1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(onClick = { viewModel.rotateLeft() }) {
                                Icon(Icons.AutoMirrored.Filled.RotateLeft, contentDescription = null)
                            }
                            Button(onClick = { viewModel.rotateRight() }) {
                                Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = null)
                            }
                            Button(onClick = { viewModel.toggleFlipHorizontal() }) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = "Flip Horizontal")
                            }
                            Button(onClick = { viewModel.toggleFlipVertical() }) {
                                Icon(Icons.Default.SwapVert, contentDescription = "Flip Vertical")
                            }
                        }
                    } else {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.brightness), style = MaterialTheme.typography.bodySmall)
                            Slider(
                                value = brightness,
                                onValueChange = { viewModel.updateBrightness(it) },
                                valueRange = -100f..100f
                            )
                            Text(stringResource(R.string.contrast), style = MaterialTheme.typography.bodySmall)
                            Slider(
                                value = contrast,
                                onValueChange = { viewModel.updateContrast(it) },
                                valueRange = 0.5f..2.0f
                            )
                            Text(stringResource(R.string.saturation), style = MaterialTheme.typography.bodySmall)
                            Slider(
                                value = saturation,
                                onValueChange = { viewModel.updateSaturation(it) },
                                valueRange = 0.0f..2.0f
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    PrimaryTabRow(selectedTabIndex = activeTab) {
                        Tab(
                            selected = activeTab == 0,
                            onClick = { activeTab = 0 },
                            text = { Text(stringResource(R.string.crop)) },
                            icon = { Icon(Icons.Default.Crop, contentDescription = null) }
                        )
                        Tab(
                            selected = activeTab == 1,
                            onClick = { activeTab = 1 },
                            text = { Text(stringResource(R.string.rotate)) },
                            icon = { Icon(Icons.AutoMirrored.Filled.RotateRight, contentDescription = null) }
                        )
                        Tab(
                            selected = activeTab == 2,
                            onClick = { activeTab = 2 },
                            text = { Text(stringResource(R.string.adjust)) },
                            icon = { Icon(Icons.Default.Tune, contentDescription = null) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White)
            } else {
                previewBitmap?.let { bitmap ->
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = stringResource(R.string.edit_photo),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } ?: run {
                    Text(
                        text = stringResource(R.string.unable_to_load_photo),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
