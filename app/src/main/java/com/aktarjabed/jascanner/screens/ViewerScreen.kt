package com.aktarjabed.jascanner.screens

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aktarjabed.jascanner.repository.MediaRepository
import com.aktarjabed.jascanner.repository.RecoverableDeleteException
import kotlinx.coroutines.launch
import android.provider.MediaStore
import android.content.ContentUris
import android.content.Intent
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.FragmentActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import com.aktarjabed.jascanner.components.DeleteConfirmationDialog
import com.aktarjabed.jascanner.datastore.SettingsDataStore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(
    id: String,
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    var photo by remember { mutableStateOf<com.aktarjabed.jascanner.model.Photo?>(null) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val transformState: TransformableState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(0.1f, 10f)
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
                snackbarHostState.showSnackbar("Image deleted successfully")
            }
            onBack()
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Deletion canceled")
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
                scope.launch {
                    snackbarHostState.showSnackbar("Failed to share image: ${e.message}")
                }
            }
        }
    }

    // Biometric Prompt for Vault
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
                            if (success) "Image moved to Vault" else "Failed to move to Vault"
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
            .setTitle("Secure Image")
            .setSubtitle("Move this photo to your encrypted Vault")
            .setNegativeButtonText("Cancel")
            .build()
    }

    LaunchedEffect(id) {
        photo = repository.getPhotoById(id.toLong())
    }

    // Reset zoom and pan
    fun resetZoom() {
        scale = 1f
        offsetX = 0f
        offsetY = 0f
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        photo?.displayName ?: "Photo",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Info action
                    IconButton(onClick = { showInfoSheet = true }) {
                        Icon(Icons.Default.Info, contentDescription = "Image Information")
                    }

                    // Share action
                    IconButton(onClick = { shareImage() }) {
                        Icon(Icons.Default.Share, contentDescription = "Share Image")
                    }

                    // Move to Vault action
                    IconButton(onClick = {
                        biometricPrompt.authenticate(promptInfo)
                    }) {
                        Icon(Icons.Default.Lock, contentDescription = "Move to Vault")
                    }

                    // Edit action
                    IconButton(onClick = {
                        onEdit(id)
                    }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Image")
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
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading image...")
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Main image with zoom/pan gestures
                AsyncImage(
                    model = photo!!.uri,
                    contentDescription = photo!!.displayName ?: "Photo",
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

                // Zoom controls overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {
                    Card(
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Zoom out
                            IconButton(
                                onClick = {
                                    scale = (scale - 0.25f).coerceAtLeast(0.1f)
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.ZoomOut, "Zoom Out")
                            }

                            // Reset zoom
                            TextButton(
                                onClick = { resetZoom() },
                                modifier = Modifier.padding(horizontal = 4.dp)
                            ) {
                                Text(
                                    "${(scale * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            // Zoom in
                            IconButton(
                                onClick = {
                                    scale = (scale + 0.25f).coerceAtMost(10f)
                                },
                                modifier = Modifier.size(48.dp)
                            ) {
                                Icon(Icons.Default.ZoomIn, "Zoom In")
                            }
                        }
                    }
                }

                // Delete button - positioned separately for safety
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    FilledTonalButton(
                        onClick = { showDeleteDialog = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(Icons.Default.Delete, "Delete Image")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete")
                    }
                }
            }
        }

        val settingsDataStore = remember { SettingsDataStore(context) }
        val settings by settingsDataStore.allSettings.collectAsState(initial = com.aktarjabed.jascanner.datastore.AppSettings())

        if (showDeleteDialog && photo != null) {
            DeleteConfirmationDialog(
                photoName = photo!!.displayName,
                settings = settings,
                onDismiss = { showDeleteDialog = false },
                onDelete = { useSecureDelete, useRecycleBin ->
                    scope.launch {
                        val (success, message) = repository.deletePhotoAdvanced(
                            photo!!.id,
                            useSecureDelete = useSecureDelete,
                            useRecycleBin = useRecycleBin
                        )
                        snackbarHostState.showSnackbar(message)
                        if (success) {
                            onBack()
                        }
                    }
                }
            )
        }

        // Image Information Bottom Sheet
        if (showInfoSheet && photo != null) {
            ModalBottomSheet(
                onDismissRequest = { showInfoSheet = false },
                sheetState = rememberModalBottomSheetState()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        "Image Information",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Basic info
                    InfoRow("Name:", photo!!.displayName ?: "Unknown")
                    InfoRow("Dimensions:", "${photo!!.width ?: 0} × ${photo!!.height ?: 0}")
                    InfoRow("Type:", photo!!.mimeType ?: "Unknown")
                    InfoRow("Album:", photo!!.bucket ?: "Unknown")

                    // Date formatting
                    val dateText = photo!!.dateTaken?.let { dateLong ->
                        try {
                            val date = Date(dateLong)
                            SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault()).format(date)
                        } catch (e: Exception) {
                            "Unknown"
                        }
                    } ?: "Unknown"
                    InfoRow("Date Taken:", dateText)

                    // File size (approximate)
                    val fileSize = try {
                        context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                            val size = pfd.statSize
                            when {
                                size > 1024 * 1024 -> "%.1f MB".format(size / (1024.0 * 1024.0))
                                size > 1024 -> "%.1f KB".format(size / 1024.0)
                                else -> "$size bytes"
                            }
                        } ?: "Unknown"
                    } catch (e: Exception) {
                        "Unknown"
                    }
                    InfoRow("Size:", fileSize)

                    // Image ID
                    InfoRow("Image ID:", photo!!.id)

                    Spacer(modifier = Modifier.height(24.dp))

                    // Close button
                    Button(
                        onClick = { showInfoSheet = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}
