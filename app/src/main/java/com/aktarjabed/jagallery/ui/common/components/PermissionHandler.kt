package com.aktarjabed.jagallery.ui.common.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.aktarjabed.jagallery.R

enum class PermissionAccessMode {
    FULL,
    SELECTED_PHOTOS_VIDEOS,
    IMAGES_ONLY,
    VIDEOS_ONLY,
    DENIED
}

private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

fun checkGalleryPermissionAccessMode(context: Context): PermissionAccessMode {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val hasImages = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        val hasVideo = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        val hasSelected = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) == PackageManager.PERMISSION_GRANTED

        when {
            hasImages && hasVideo -> PermissionAccessMode.FULL
            hasSelected -> PermissionAccessMode.SELECTED_PHOTOS_VIDEOS
            hasImages -> PermissionAccessMode.IMAGES_ONLY
            hasVideo -> PermissionAccessMode.VIDEOS_ONLY
            else -> PermissionAccessMode.DENIED
        }
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val hasImages = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        val hasVideo = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        when {
            hasImages && hasVideo -> PermissionAccessMode.FULL
            hasImages -> PermissionAccessMode.IMAGES_ONLY
            hasVideo -> PermissionAccessMode.VIDEOS_ONLY
            else -> PermissionAccessMode.DENIED
        }
    } else {
        val hasStorage = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        if (hasStorage) PermissionAccessMode.FULL else PermissionAccessMode.DENIED
    }
}

@Composable
fun PermissionHandler(
    onPermissionChanged: () -> Unit = {},
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionAccessMode by remember { mutableStateOf(checkGalleryPermissionAccessMode(context)) }
    var hasRequestedOnce by remember { mutableStateOf(false) }

    val permissionsToRequest = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ ->
        hasRequestedOnce = true
        val newMode = checkGalleryPermissionAccessMode(context)
        if (newMode != permissionAccessMode) {
            permissionAccessMode = newMode
            onPermissionChanged()
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START || event == Lifecycle.Event.ON_RESUME) {
                val newMode = checkGalleryPermissionAccessMode(context)
                if (newMode != permissionAccessMode) {
                    permissionAccessMode = newMode
                    onPermissionChanged()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (permissionAccessMode == PermissionAccessMode.DENIED) {
        val activity = context.findActivity()
        val showRationale = activity != null && permissionsToRequest.any { perm ->
            ActivityCompat.shouldShowRequestPermissionRationale(activity, perm)
        }
        val isPermanentlyDenied = hasRequestedOnce && !showRationale

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = stringResource(R.string.permission_denied),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(16.dp))
                if (isPermanentlyDenied) {
                    Button(onClick = {
                        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", context.packageName, null)
                        }
                        context.startActivity(intent)
                    }) {
                        Text(stringResource(R.string.open_settings))
                    }
                } else {
                    Button(onClick = { launcher.launch(permissionsToRequest) }) {
                        Text(stringResource(R.string.grant_permission))
                    }
                }
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            val bannerText = when (permissionAccessMode) {
                PermissionAccessMode.SELECTED_PHOTOS_VIDEOS -> stringResource(R.string.permission_selected_access)
                PermissionAccessMode.IMAGES_ONLY -> stringResource(R.string.permission_images_only)
                PermissionAccessMode.VIDEOS_ONLY -> stringResource(R.string.permission_videos_only)
                else -> null
            }

            if (bannerText != null) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = bannerText,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { launcher.launch(permissionsToRequest) }) {
                            Text(stringResource(R.string.manage_access))
                        }
                    }
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                content()
            }
        }
    }
}
