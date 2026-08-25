package com.example.advancedgallery.ui.common.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.advancedgallery.R

@Composable
fun AlbumSelectionDialog(
    albumNames: List<String>,
    onAlbumSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newAlbumName by remember { mutableStateOf("") }
    var isCreatingNew by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.move_to_album)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (!isCreatingNew) {
                    Button(
                        onClick = { isCreatingNew = true },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        Text(stringResource(R.string.create_new_album))
                    }
                    if (albumNames.isEmpty()) {
                        Text(stringResource(R.string.no_existing_albums), style = MaterialTheme.typography.bodyMedium)
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 240.dp)) {
                            items(albumNames) { name ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onAlbumSelected(name) }
                                        .padding(vertical = 12.dp, horizontal = 8.dp)
                                ) {
                                    Text(text = name, style = MaterialTheme.typography.bodyLarge)
                                }
                                HorizontalDivider()
                            }
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = newAlbumName,
                        onValueChange = { newAlbumName = it },
                        label = { Text(stringResource(R.string.album_name_hint)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            if (isCreatingNew) {
                TextButton(
                    onClick = {
                        if (newAlbumName.isNotBlank()) {
                            onAlbumSelected(newAlbumName.trim())
                        }
                    },
                    enabled = newAlbumName.isNotBlank()
                ) {
                    Text(stringResource(R.string.ok))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
