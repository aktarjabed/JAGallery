package com.example.advancedgallery.ui.common.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.advancedgallery.data.model.MediaItem
import java.text.DateFormat
import androidx.compose.ui.res.stringResource
import com.example.advancedgallery.R
import androidx.compose.foundation.clickable
import java.util.Date
import androidx.compose.foundation.clickable
import androidx.exifinterface.media.ExifInterface
import android.graphics.BitmapFactory
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataBottomSheet(
    sheetState: SheetState,
    mediaItem: MediaItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var dimensions by remember { mutableStateOf<String?>(null) }
    var exifDate by remember { mutableStateOf<String?>(null) }
    var exifModel by remember { mutableStateOf<String?>(null) }
    var location by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(mediaItem) {
        withContext(Dispatchers.IO) {
            try {
                if (!mediaItem.isVideo) {
                    context.contentResolver.openInputStream(mediaItem.uri)?.use { stream ->
                        val exif = ExifInterface(stream)
                        val width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
                        val height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
                        if (width > 0 && height > 0) {
                            dimensions = "$width x $height"
                        }

                        exifDate = exif.getAttribute(ExifInterface.TAG_DATETIME)
                        val make = exif.getAttribute(ExifInterface.TAG_MAKE)
                        val model = exif.getAttribute(ExifInterface.TAG_MODEL)
                        if (make != null || model != null) {
                            exifModel = "${make ?: ""} ${model ?: ""}".trim()
                        }
                        val latLong = exif.latLong
                        if (latLong != null) {
                            location = "${latLong[0]}, ${latLong[1]}"
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore EXIF parsing errors safely
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.metadata_title),
                style = MaterialTheme.typography.titleLarge
            )

            Spacer(modifier = Modifier.height(16.dp))

            MetadataRow(label = stringResource(R.string.metadata_field_filename), value = mediaItem.name)
            MetadataRow(
                label = stringResource(R.string.metadata_field_date),
                value = DateFormat.getDateTimeInstance().format(Date(mediaItem.dateAdded * 1000L))
            )
            MetadataRow(label = stringResource(R.string.metadata_field_type), value = mediaItem.mimeType)
            MetadataRow(label = "Album", value = mediaItem.bucketName.ifBlank { "Root" })
            MetadataRow(label = "Volume", value = mediaItem.volumeName.ifBlank { "Primary" })
            if (mediaItem.size > 0L) {
                MetadataRow(label = stringResource(R.string.metadata_field_size), value = com.example.advancedgallery.util.FileUtils.formatFileSize(mediaItem.size))
            }
            MetadataRow(label = stringResource(R.string.metadata_field_path), value = mediaItem.uri.toString(), copyable = true)

            if (dimensions != null) MetadataRow(label = stringResource(R.string.metadata_field_dimensions), value = dimensions!!, copyable = true)
            if (exifDate != null) MetadataRow(label = "EXIF Date", value = exifDate!!, copyable = true)
            if (exifModel != null) MetadataRow(label = "Camera Model", value = exifModel!!, copyable = true)
            if (location != null) MetadataRow(label = "Location", value = location!!, copyable = true)

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.metadata_close))
                }
            }
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String, copyable: Boolean = false) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .then(
                if (copyable) {
                    val copiedMessage = stringResource(R.string.metadata_uri_copied)
                    Modifier.clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText(label, value)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                    }
                } else Modifier
            )
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

