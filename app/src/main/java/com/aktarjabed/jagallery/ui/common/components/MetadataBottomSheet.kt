package com.aktarjabed.jagallery.ui.common.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aktarjabed.jagallery.R
import com.aktarjabed.jagallery.data.model.MediaItem
import com.aktarjabed.jagallery.util.ExifDetails
import com.aktarjabed.jagallery.util.ExifEditorUtils
import com.aktarjabed.jagallery.util.FileUtils
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetadataBottomSheet(
    sheetState: SheetState,
    mediaItem: MediaItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var exifDetails by remember { mutableStateOf(ExifDetails()) }
    var isEditingExif by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    var editDateTime by remember { mutableStateOf("") }
    var editMake by remember { mutableStateOf("") }
    var editModel by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }
    var editArtist by remember { mutableStateOf("") }
    var editGps by remember { mutableStateOf("") }

    LaunchedEffect(mediaItem, refreshTrigger) {
        if (!mediaItem.isVideo) {
            val details = ExifEditorUtils.readExifDetails(context, mediaItem.uri)
            exifDetails = details
            editDateTime = details.dateTime
            editMake = details.make
            editModel = details.model
            editDescription = details.description
            editArtist = details.artist
            editGps = details.gpsCoordinates
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(if (isEditingExif) R.string.exif_edit_title else R.string.metadata_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
                if (!mediaItem.isVideo && !mediaItem.isTrashed) {
                    TextButton(
                        onClick = { isEditingExif = !isEditingExif },
                        modifier = Modifier.testTag("toggle_exif_edit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = stringResource(R.string.exif_edit_action)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(if (isEditingExif) R.string.cancel else R.string.exif_edit_action))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedVisibility(visible = !isEditingExif) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    MetadataRow(label = stringResource(R.string.metadata_field_filename), value = mediaItem.name)
                    MetadataRow(
                        label = stringResource(R.string.metadata_field_date),
                        value = DateFormat.getDateTimeInstance().format(Date(mediaItem.dateAdded * 1000L))
                    )
                    MetadataRow(label = stringResource(R.string.metadata_field_type), value = mediaItem.mimeType)
                    MetadataRow(
                        label = stringResource(R.string.label_album),
                        value = mediaItem.bucketName.ifBlank { stringResource(R.string.label_root) }
                    )
                    MetadataRow(
                        label = stringResource(R.string.label_volume),
                        value = mediaItem.volumeName.ifBlank { stringResource(R.string.label_primary) }
                    )
                    if (mediaItem.size > 0L) {
                        MetadataRow(
                            label = stringResource(R.string.metadata_field_size),
                            value = FileUtils.formatFileSize(mediaItem.size)
                        )
                    }
                    MetadataRow(
                        label = stringResource(R.string.metadata_field_path),
                        value = mediaItem.uri.toString(),
                        copyable = true
                    )

                    exifDetails.dimensions?.let {
                        MetadataRow(label = stringResource(R.string.metadata_field_dimensions), value = it, copyable = true)
                    }
                    exifDetails.dateTime.takeIf { it.isNotBlank() }?.let {
                        MetadataRow(label = stringResource(R.string.label_exif_date), value = it, copyable = true)
                    }
                    val cameraCombined = "${exifDetails.make} ${exifDetails.model}".trim()
                    if (cameraCombined.isNotBlank()) {
                        MetadataRow(label = stringResource(R.string.label_camera_model), value = cameraCombined, copyable = true)
                    }
                    exifDetails.cameraSpecs?.let {
                        MetadataRow(label = stringResource(R.string.exif_camera_specs), value = it, copyable = true)
                    }
                    exifDetails.description.takeIf { it.isNotBlank() }?.let {
                        MetadataRow(label = stringResource(R.string.exif_field_description), value = it, copyable = true)
                    }
                    exifDetails.artist.takeIf { it.isNotBlank() }?.let {
                        MetadataRow(label = stringResource(R.string.exif_field_artist), value = it, copyable = true)
                    }
                    exifDetails.gpsCoordinates.takeIf { it.isNotBlank() }?.let {
                        MetadataRow(label = stringResource(R.string.label_location), value = it, copyable = true)
                    }
                }
            }

            AnimatedVisibility(visible = isEditingExif) {
                Surface(
                    tonalElevation = 2.dp,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = editDateTime,
                            onValueChange = { editDateTime = it },
                            label = { Text(stringResource(R.string.exif_field_datetime)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("exif_datetime_input")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = editMake,
                                onValueChange = { editMake = it },
                                label = { Text(stringResource(R.string.exif_field_make)) },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("exif_make_input")
                            )
                            OutlinedTextField(
                                value = editModel,
                                onValueChange = { editModel = it },
                                label = { Text(stringResource(R.string.exif_field_model)) },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("exif_model_input")
                            )
                        }
                        OutlinedTextField(
                            value = editDescription,
                            onValueChange = { editDescription = it },
                            label = { Text(stringResource(R.string.exif_field_description)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("exif_description_input")
                        )
                        OutlinedTextField(
                            value = editArtist,
                            onValueChange = { editArtist = it },
                            label = { Text(stringResource(R.string.exif_field_artist)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("exif_artist_input")
                        )
                        OutlinedTextField(
                            value = editGps,
                            onValueChange = { editGps = it },
                            label = { Text(stringResource(R.string.exif_field_gps)) },
                            placeholder = { Text("37.7749, -122.4194") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("exif_gps_input")
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    coroutineScope.launch {
                                        val ok = ExifEditorUtils.stripSensitiveExif(context, mediaItem.uri)
                                        if (ok) {
                                            Toast.makeText(context, context.getString(R.string.exif_stripped_success), Toast.LENGTH_SHORT).show()
                                            refreshTrigger++
                                            isEditingExif = false
                                        } else {
                                            Toast.makeText(context, context.getString(R.string.exif_saved_failed), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("exif_strip_button")
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.exif_strip_privacy))
                            }

                            Button(
                                onClick = {
                                    coroutineScope.launch {
                                        val ok = ExifEditorUtils.saveExifEdits(
                                            context = context,
                                            uri = mediaItem.uri,
                                            dateTime = editDateTime,
                                            make = editMake,
                                            model = editModel,
                                            description = editDescription,
                                            artist = editArtist,
                                            gpsCoordinates = editGps
                                        )
                                        if (ok) {
                                            Toast.makeText(context, context.getString(R.string.exif_saved_success), Toast.LENGTH_SHORT).show()
                                            refreshTrigger++
                                            isEditingExif = false
                                        } else {
                                            Toast.makeText(context, context.getString(R.string.exif_saved_failed), Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.testTag("exif_save_button")
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(stringResource(R.string.exif_save_action))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("metadata_close_button")
                ) {
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
