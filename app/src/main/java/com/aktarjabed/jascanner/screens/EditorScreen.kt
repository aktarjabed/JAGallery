package com.aktarjabed.jascanner.screens

import android.content.ContentUris
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.aktarjabed.jascanner.utils.ImageEditorUtils
import com.aktarjabed.jascanner.utils.ImageEditorUtils.ImageFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import androidx.compose.foundation.selection.selectable

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
id: String,
onBack: () -> Unit,
onSave: () -> Unit,
snackbarHostState: androidx.compose.material3.SnackbarHostState
) {
val context = LocalContext.current
val scope = rememberCoroutineScope()

// State for image editing
var original by remember { mutableStateOf<Bitmap?>(null) }
var working by remember { mutableStateOf<Bitmap?>(null) }
var brightness by remember { mutableFloatStateOf(0f) }
var contrast by remember { mutableFloatStateOf(1f) }
var saturation by remember { mutableFloatStateOf(1f) }
var selectedFilter by remember { mutableStateOf<ImageFilter?>(null) }
var showCropDialog by remember { mutableStateOf(false) }
var cropAspectRatio by remember { mutableStateOf(1f) }

// Load the original image
LaunchedEffect(id) {
withContext(Dispatchers.IO) {
val collection = if (android.os.Build.VERSION.SDK_INT >= 29) {
MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
} else {
MediaStore.Images.Media.EXTERNAL_CONTENT_URI
}
val uri = ContentUris.withAppendedId(collection, id.toLong())

context.contentResolver.openInputStream(uri)?.use { stream ->
val bitmap = BitmapFactory.decodeStream(stream)
original = bitmap
working = bitmap.copy(bitmap.config, true)
}
}
}

// Apply edits when parameters change
fun applyEdits() {
val src = original ?: return
var result = src.copy(src.config, true)

// Apply brightness
if (brightness != 0f) {
result = ImageEditorUtils.adjustBrightness(result, brightness)
}

// Apply contrast
if (contrast != 1f) {
result = ImageEditorUtils.adjustContrast(result, contrast)
}

// Apply saturation
if (saturation != 1f) {
result = ImageEditorUtils.adjustSaturation(result, saturation)
}

// Apply filter
selectedFilter?.let { filter ->
result = ImageEditorUtils.applyFilter(result, filter)
}

working = result
}

LaunchedEffect(brightness, contrast, saturation, selectedFilter) {
applyEdits()
}

// Share function
fun shareImage(bitmap: Bitmap) {
scope.launch(Dispatchers.IO) {
try {
val cachePath = File(context.cacheDir, "images")
cachePath.mkdirs()
val file = File(cachePath, "shared_image_${System.currentTimeMillis()}.jpg")
FileOutputStream(file).use { stream ->
bitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream)
}

val contentUri = FileProvider.getUriForFile(
context,
"${context.packageName}.provider",
file
)

withContext(Dispatchers.Main) {
val shareIntent = Intent().apply {
action = Intent.ACTION_SEND
putExtra(Intent.EXTRA_STREAM, contentUri)
type = "image/jpeg"
addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
}
context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
}
} catch (e: Exception) {
snackbarHostState.showSnackbar("Failed to share image")
}
}
}

// Delete function
fun deleteImage() {
scope.launch {
try {
val collection = if (android.os.Build.VERSION.SDK_INT >= 29) {
MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
} else {
MediaStore.Images.Media.EXTERNAL_CONTENT_URI
}
val uri = ContentUris.withAppendedId(collection, id.toLong())

val deleted = context.contentResolver.delete(uri, null, null)
if (deleted > 0) {
snackbarHostState.showSnackbar("Image deleted")
onBack()
} else {
snackbarHostState.showSnackbar("Failed to delete image")
}
} catch (e: SecurityException) {
snackbarHostState.showSnackbar("Permission denied for deletion")
} catch (e: Exception) {
snackbarHostState.showSnackbar("Failed to delete image")
}
}
}

Scaffold(
topBar = {
CenterAlignedTopAppBar(
title = { Text("Edit Image") },
navigationIcon = {
IconButton(onClick = onBack) {
androidx.compose.material3.Icon(Icons.Default.ArrowBack, "Back")
}
},
actions = {
// Share action
IconButton(onClick = {
working?.let { shareImage(it) }
}) {
androidx.compose.material3.Icon(Icons.Default.Share, "Share")
}

// Delete action
IconButton(onClick = { deleteImage() }) {
androidx.compose.material3.Icon(Icons.Default.Delete, "Delete")
}

// Save action
IconButton(onClick = {
scope.launch {
working?.let { bitmap ->
withContext(Dispatchers.IO) {
val outputInfo = ImageEditorUtils.openOutput(
context,
"edited_${System.currentTimeMillis()}.jpg"
)
outputInfo.second?.use { outputStream ->
bitmap.compress(Bitmap.CompressFormat.JPEG, 92, outputStream)
}
}
snackbarHostState.showSnackbar("Saved successfully")
onSave()
} ?: run {
snackbarHostState.showSnackbar("Nothing to save")
}
}
}) {
androidx.compose.material3.Icon(Icons.Default.Save, "Save")
}
}
)
},
bottomBar = {
Column {
// Adjustment Controls
Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
// Brightness
Row(verticalAlignment = Alignment.CenterVertically) {
androidx.compose.material3.Icon(
Icons.Default.Brightness4,
"Brightness",
modifier = Modifier.size(20.dp)
)
Spacer(modifier = Modifier.width(8.dp))
Text("Brightness", style = MaterialTheme.typography.labelSmall)
Spacer(modifier = Modifier.width(8.dp))
Slider(
value = brightness,
onValueChange = { brightness = it },
valueRange = -100f..100f,
modifier = Modifier.weight(1f)
)
}

// Contrast
Row(verticalAlignment = Alignment.CenterVertically) {
androidx.compose.material3.Icon(
Icons.Default.Contrast,
"Contrast",
modifier = Modifier.size(20.dp)
)
Spacer(modifier = Modifier.width(8.dp))
Text("Contrast", style = MaterialTheme.typography.labelSmall)
Spacer(modifier = Modifier.width(8.dp))
Slider(
value = contrast,
onValueChange = { contrast = it },
valueRange = 0.5f..2f,
modifier = Modifier.weight(1f)
)
}

// Saturation
Row(verticalAlignment = Alignment.CenterVertically) {
androidx.compose.material3.Icon(
Icons.Default.Palette,
"Saturation",
modifier = Modifier.size(20.dp)
)
Spacer(modifier = Modifier.width(8.dp))
Text("Saturation", style = MaterialTheme.typography.labelSmall)
Spacer(modifier = Modifier.width(8.dp))
Slider(
value = saturation,
onValueChange = { saturation = it },
valueRange = 0f..2f,
modifier = Modifier.weight(1f)
)
}
}

// Filter Selection
Row(
horizontalArrangement = Arrangement.SpaceEvenly,
modifier = Modifier
.fillMaxWidth()
.padding(horizontal = 8.dp)
) {
FilterChip(
selected = selectedFilter == ImageFilter.GRAYSCALE,
onClick = {
selectedFilter = if (selectedFilter == ImageFilter.GRAYSCALE) null
else ImageFilter.GRAYSCALE
},
label = { Text("B&W") }
)
FilterChip(
selected = selectedFilter == ImageFilter.SEPIA,
onClick = {
selectedFilter = if (selectedFilter == ImageFilter.SEPIA) null
else ImageFilter.SEPIA
},
label = { Text("Sepia") }
)
FilterChip(
selected = selectedFilter == ImageFilter.INVERT,
onClick = {
selectedFilter = if (selectedFilter == ImageFilter.INVERT) null
else ImageFilter.INVERT
},
label = { Text("Invert") }
)
FilterChip(
selected = selectedFilter == ImageFilter.VIVID,
onClick = {
selectedFilter = if (selectedFilter == ImageFilter.VIVID) null
else ImageFilter.VIVID
},
label = { Text("Vivid") }
)
}

// Edit Tools
Row(
horizontalArrangement = Arrangement.SpaceEvenly,
modifier = Modifier
.fillMaxWidth()
.padding(16.dp)
) {
// Flip Horizontal
IconButton(onClick = {
working?.let { bitmap ->
working = ImageEditorUtils.flipImageHorizontal(bitmap)
}
}) {
Column(horizontalAlignment = Alignment.CenterHorizontally) {
androidx.compose.material3.Icon(Icons.Default.Flip, "Flip Horizontal")
Text("Flip H", style = MaterialTheme.typography.labelSmall)
}
}

// Flip Vertical
IconButton(onClick = {
working?.let { bitmap ->
working = ImageEditorUtils.flipImageVertical(bitmap)
}
}) {
Column(horizontalAlignment = Alignment.CenterHorizontally) {
androidx.compose.material3.Icon(Icons.Default.Flip, "Flip Vertical")
Text("Flip V", style = MaterialTheme.typography.labelSmall)
}
}

// Rotate Left
IconButton(onClick = {
working?.let { bitmap ->
working = ImageEditorUtils.rotateImage(bitmap, -90f)
}
}) {
Column(horizontalAlignment = Alignment.CenterHorizontally) {
androidx.compose.material3.Icon(Icons.Default.RotateLeft, "Rotate Left")
Text("Left", style = MaterialTheme.typography.labelSmall)
}
}

// Rotate Right
IconButton(onClick = {
working?.let { bitmap ->
working = ImageEditorUtils.rotateImage(bitmap, 90f)
}
}) {
Column(horizontalAlignment = Alignment.CenterHorizontally) {
androidx.compose.material3.Icon(Icons.Default.RotateRight, "Rotate Right")
Text("Right", style = MaterialTheme.typography.labelSmall)
}
}

// Crop
IconButton(onClick = { showCropDialog = true }) {
Column(horizontalAlignment = Alignment.CenterHorizontally) {
androidx.compose.material3.Icon(Icons.Default.Crop, "Crop")
Text("Crop", style = MaterialTheme.typography.labelSmall)
}
}
}
}
}
) { padding ->
Box(
modifier = Modifier
.fillMaxSize()
.padding(padding),
contentAlignment = Alignment.Center
) {
when {
working != null -> {
Image(
bitmap = working!!.asImageBitmap(),
contentDescription = "Edited image",
modifier = Modifier.fillMaxSize()
)
}
original != null -> {
Image(
bitmap = original!!.asImageBitmap(),
contentDescription = "Original image",
modifier = Modifier.fillMaxSize()
)
}
else -> {
CircularProgressIndicator()
}
}
}

// Crop Dialog
if (showCropDialog) {
AlertDialog(
onDismissRequest = { showCropDialog = false },
title = { Text("Crop Image") },
text = {
Column {
Text("Select crop aspect ratio:")
Spacer(modifier = Modifier.height(16.dp))

// Aspect ratio options
val ratios = listOf(
"Original" to 0f,
"Square (1:1)" to 1f,
"Landscape (16:9)" to 16f/9f,
"Portrait (9:16)" to 9f/16f,
"Widescreen (21:9)" to 21f/9f
)

ratios.forEach { (label, ratio) ->
Row(
modifier = Modifier
.fillMaxWidth()
.padding(vertical = 4.dp)
.selectable(
selected = cropAspectRatio == ratio,
onClick = { cropAspectRatio = ratio }
),
verticalAlignment = Alignment.CenterVertically
) {
RadioButton(
selected = cropAspectRatio == ratio,
onClick = { cropAspectRatio = ratio }
)
Text(label, modifier = Modifier.padding(start = 8.dp))
}
}
}
},
confirmButton = {
TextButton(
onClick = {
working?.let { bitmap ->
working = if (cropAspectRatio == 0f) {
bitmap // Original
} else if (cropAspectRatio == 1f) {
ImageEditorUtils.cropToSquare(bitmap)
} else {
ImageEditorUtils.cropToAspectRatio(bitmap, cropAspectRatio)
}
}
showCropDialog = false
}
) {
Text("Apply Crop")
}
},
dismissButton = {
TextButton(onClick = { showCropDialog = false }) {
Text("Cancel")
}
}
)
}
}
}