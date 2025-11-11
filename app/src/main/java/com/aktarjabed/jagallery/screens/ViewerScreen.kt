package com.aktarjabed.jagallery.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.gestures.TransformableState
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import coil.compose.AsyncImage
import com.aktarjabed.core.data.model.MediaItem
import com.aktarjabed.core.data.repository.MediaRepository
import com.aktarjabed.core.data.repository.RecoverableDeleteException
import kotlinx.coroutines.launch
import android.provider.MediaStore
import android.content.ContentUris
import android.content.Intent
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

@Composable
fun ViewerScreen(
    id: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    var photo by remember { mutableStateOf<MediaItem?>(null) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var showInfo by remember { mutableStateOf(false) }

    val transformState: TransformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.5f, 5f)
        offsetX += panChange.x
        offsetY += panChange.y
    }

    val scope = rememberCoroutineScope()
    val repository = remember { MediaRepository(context) }

    val collection = if (android.os.Build.VERSION.SDK_INT >= 29) {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
    } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }
    val uri = ContentUris.withAppendedId(collection, id.toLong())

    val deleteLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            scope.launch {
                snackbarHostState.showSnackbar("Deleted successfully")
            }
            onBack()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Delete canceled")
            }
        }
    }

    // Share function
    fun shareImage() {
        scope.launch {
            try {
                val cachePath = File(context.cacheDir, "images")
                cachePath.mkdirs()
                val file = File(cachePath, "shared_${System.currentTimeMillis()}.jpg")

                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                val contentUri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.provider",
                    file
                )

                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    type = "image/jpeg"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Failed to share image")
            }
        }
    }

    // Biometric Prompt
    val biometricPrompt = remember {
        val executor = ContextCompat.getMainExecutor(context)
        BiometricPrompt(
            context as FragmentActivity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    scope.launch {
                        val success = repository.moveToVault(id.toLong(), deleteOriginal = false)
                        snackbarHostState.showSnackbar(
                            if (success) "Moved to Vault" else "Failed to move to Vault"
                        )
                    }
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    scope.launch {
                        snackbarHostState.showSnackbar("Authentication failed: $errString")
                    }
                }
            }
        )
    }

    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Authenticate to Secure")
            .setSubtitle("Move this photo to your Vault")
            .setNegativeButtonText("Cancel")
            .build()
    }

    LaunchedEffect(id) {
        photo = repository.getPhotoById(id.toLong())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(photo?.displayName ?: "Photo") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Info action
                    IconButton(onClick = { showInfo = true }) {
                        Icon(Icons.Default.Info, "Info")
                    }

                    // Share action
                    IconButton(onClick = { shareImage() }) {
                        Icon(Icons.Default.Share, "Share")
                    }

                    // Move to Vault action
                    IconButton(onClick = {
                        biometricPrompt.authenticate(promptInfo)
                    }) {
                        Icon(Icons.Default.Lock, "Move to Vault")
                    }

                    // Edit action
                    IconButton(onClick = {
                        onEdit(id)
                    }) {
                        Icon(Icons.Default.Edit, "Edit")
                    }

                    // Delete action
                    IconButton(onClick = {
                        scope.launch {
                            try {
                                repository.deletePhoto(uri)
                                snackbarHostState.showSnackbar("Deleted")
                                onBack()
                            } catch (e: RecoverableDeleteException) {
                                deleteLauncher.launch(IntentSenderRequest.Builder(e.sender).build())
                            } catch (t: Throwable) {
                                snackbarHostState.showSnackbar("Delete failed: ${t.message ?: ""}")
                            }
                        }
                    }) {
                        Icon(Icons.Default.Delete, "Delete")
                    }
                }
            )
        }
    ) { padding ->
        if (photo == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                AsyncImage(
                    model = photo!!.uri,
                    contentDescription = photo!!.displayName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offsetX
                            translationY = offsetY
                        }
                        .transformable(transformState)
                )

                // Zoom controls
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Card {
                        Row {
                            IconButton(
                                onClick = {
                                    scale = (scale - 0.2f).coerceAtLeast(0.5f)
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            ) {
                                Icon(Icons.Default.ZoomOut, "Zoom Out")
                            }

                            IconButton(
                                onClick = {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                }
                            ) {
                                Text("100%", style = MaterialTheme.typography.labelSmall)
                            }

                            IconButton(
                                onClick = {
                                    scale = (scale + 0.2f).coerceAtMost(5f)
                                }
                            ) {
                                Icon(Icons.Default.ZoomIn, "Zoom In")
                            }
                        }
                    }
                }
            }
        }

        // Info Dialog
        if (showInfo && photo != null) {
            AlertDialog(
                onDismissRequest = { showInfo = false },
                title = { Text("Photo Information") },
                text = {
                    Column {
                        Text("Name: ${photo!!.displayName ?: "Unknown"}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Size: ${photo!!.width ?: 0} x ${photo!!.height ?: 0}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Type: ${photo!!.mimeType ?: "Unknown"}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Album: ${photo!!.bucket ?: "Unknown"}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Date: ${photo!!.dateTaken?.let {
                            java.text.SimpleDateFormat("MMM dd, yyyy HH:mm", java.util.Locale.getDefault())
                                .format(java.util.Date(it))
                        } ?: "Unknown"}")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showInfo = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}