package com.aktarjabed.jascanner.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.aktarjabed.jascanner.JAScannerApp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriesScreen(
    navController: NavController,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    val app = context.applicationContext as JAScannerApp
    val mediaRepository = remember { app.mediaRepository }

    // Observe media changes
    val mediaChanged by mediaRepository.mediaChanged.collectAsState()

    var categories by remember { mutableStateOf<Map<String, Pair<String, Int>>>(emptyMap()) }
    var buckets by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }

    LaunchedEffect(mediaChanged) { // ← Add mediaChanged as trigger
        categories = mediaRepository.getCategories()
        buckets = mediaRepository.getPhotoBuckets()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Categories") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            item {
                Text(
                    "Smart Albums",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
            items(categories.toList()) { (key, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* Handle category click */ }
                        .padding(16.dp)
                ) {
                    Text(
                        text = value.first,
                        modifier = Modifier.weight(1f)
                    )
                    Text(text = value.second.toString())
                }
            }
            item {
                Text(
                    "Albums",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
            items(buckets.toList()) { (name, count) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { /* Handle bucket click */ }
                        .padding(16.dp)
                ) {
                    Text(
                        text = name,
                        modifier = Modifier.weight(1f)
                    )
                    Text(text = count.toString())
                }
            }
        }
    }
}