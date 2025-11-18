package com.aktarjabed.core.ui.permissions

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.core.content.ContextCompat

sealed class PermissionState {
    object Granted : PermissionState()
    object Denied : PermissionState()
    object PermanentlyDenied : PermissionState()
}

object MediaPermissions {
    fun getReadPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    fun getReadVideoPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_VIDEO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }

    fun getCameraPermission(): String = Manifest.permission.CAMERA
}

fun Context.hasPermission(permission: String): Boolean =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

private fun Context.openAppSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", packageName, null)
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
    }
    startActivity(intent)
}

/**
 * Compose-friendly permission requester that:
 * - avoids ClassCastException in previews
 * - uses safe activity cast
 * - handles rationale vs permanently denied flows
 */
@Composable
fun RequestPermission(
    permission: String,
    rationaleTitle: String = "Permission Required",
    rationaleMessage: String = "This permission is required for the app to function properly.",
    onPermissionResult: (PermissionState) -> Unit
) {
    val context = LocalContext.current
    val isInPreview = LocalInspectionMode.current

    var showRationaleDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var hasRequestedPermission by remember { mutableStateOf(false) }

    // safe activity reference
    val activity = remember(context) { (context as? Activity) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        when {
            granted -> onPermissionResult(PermissionState.Granted)
            activity != null && activity.shouldShowRequestPermissionRationale(permission) -> {
                onPermissionResult(PermissionState.Denied)
                showRationaleDialog = true
            }
            else -> {
                // Denied permanently or system denied
                onPermissionResult(PermissionState.PermanentlyDenied)
                if (hasRequestedPermission) showSettingsDialog = true
            }
        }
    }

    LaunchedEffect(permission) {
        // Skip permission runtime flow in preview to avoid crashes and noisy logs.
        if (isInPreview) {
            onPermissionResult(PermissionState.Denied)
            return@LaunchedEffect
        }

        when {
            context.hasPermission(permission) -> onPermissionResult(PermissionState.Granted)
            activity != null && activity.shouldShowRequestPermissionRationale(permission) -> {
                showRationaleDialog = true
            }
            else -> {
                hasRequestedPermission = true
                permissionLauncher.launch(permission)
            }
        }
    }

    if (showRationaleDialog) {
        AlertDialog(
            onDismissRequest = { showRationaleDialog = false },
            title = { Text(rationaleTitle) },
            text = { Text(rationaleMessage) },
            confirmButton = {
                TextButton(onClick = {
                    showRationaleDialog = false
                    hasRequestedPermission = true
                    permissionLauncher.launch(permission)
                }) { Text("Grant") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showRationaleDialog = false
                    onPermissionResult(PermissionState.Denied)
                }) { Text("Cancel") }
            }
        )
    }

    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Permission Denied") },
            text = { Text("Permission was permanently denied. Please enable it in app settings.") },
            confirmButton = {
                TextButton(onClick = {
                    showSettingsDialog = false
                    context.openAppSettings()
                }) { Text("Open Settings") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showSettingsDialog = false
                    onPermissionResult(PermissionState.PermanentlyDenied)
                }) { Text("Cancel") }
            }
        )
    }
}

/** Convenience helpers */
@Composable
fun RequestMediaPermission(onPermissionResult: (PermissionState) -> Unit) {
    RequestPermission(
        permission = MediaPermissions.getReadPermission(),
        rationaleTitle = "Media Access Required",
        rationaleMessage = "JAGallery needs access to your photos to display them.",
        onPermissionResult = onPermissionResult
    )
}

@Composable
fun RequestCameraPermission(onPermissionResult: (PermissionState) -> Unit) {
    RequestPermission(
        permission = MediaPermissions.getCameraPermission(),
        rationaleTitle = "Camera Access Required",
        rationaleMessage = "JAGallery needs camera access to take photos.",
        onPermissionResult = onPermissionResult
    )
}
