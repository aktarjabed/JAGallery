package com.aktarjabed.jascanner.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aktarjabed.jascanner.repository.MediaRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun SearchScreen(
    onBack: () -> Unit,
    onPhotoClick: (String) -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf(emptyList<com.aktarjabed.jascanner.model.Photo>()) }
    var allPhotos by remember { mutableStateOf(emptyList<com.aktarjabed.jascanner.model.Photo>()) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        allPhotos = MediaRepository(context).getPhotos()
    }

    fun performSearch(query: String) {
        searchResults = if (query.isBlank()) {
            emptyList()
        } else {
            allPhotos.filter { photo ->
                photo.displayName?.contains(query, ignoreCase = true) == true ||
                photo.bucket?.contains(query, ignoreCase = true) == true
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = {
                            searchQuery = it
                            performSearch(it)
                        },
                        placeholder = { Text("Search photos...") },
                        leadingIcon = {
                            androidx.compose.material3.Icon(Icons.Default.Search, "Search")
                        },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                keyboardController?.hide()
                                performSearch(searchQuery)
                            }
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        androidx.compose.material3.Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            if (searchResults.isEmpty() && searchQuery.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text("No results found for \"$searchQuery\"")
                }
            } else if (searchQuery.isBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text("Enter search terms to find photos")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(128.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(searchResults, key = { it.id }) { photo ->
                        Card(
                            onClick = { onPhotoClick(photo.id) },
                            modifier = Modifier
                                .padding(4.dp)
                                .aspectRatio(1f)
                        ) {
                            AsyncImage(
                                model = photo.uri,
                                contentDescription = photo.displayName,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        }
    }
}