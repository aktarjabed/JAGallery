package com.aktarjabed.feature.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.aktarjabed.core.ai.registry.ModelRegistry
import com.aktarjabed.core.ui.permissions.PermissionState
import com.aktarjabed.core.ui.permissions.RequestMediaPermission
import com.aktarjabed.feature.search.IndexedImage
import com.aktarjabed.feature.search.SearchEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIIntegrationScreen() {
    val context = LocalContext.current
    val viewModel = remember { AIViewModel(context) }
    val searchEngine = remember { SearchEngine() }

    var permissionGranted by remember { mutableStateOf(false) }
    var currentState by remember { mutableStateOf<AIState>(AIState.Idle) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var currentAnalysis by remember { mutableStateOf<ImageAnalysis?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<IndexedImage>>(emptyList()) }
    var showSearch by remember { mutableStateOf(false) }

    // State sync from ViewModel
    LaunchedEffect(viewModel) {
        viewModel.onStateChanged = { state ->
            currentState = state
            if (state is AIState.AnalysisComplete) {
                currentAnalysis = state.analysis
                // Auto-index analyzed image
                selectedImageUri?.let { uri ->
                    val indexed = IndexedImage(
                        id = uri.toString(),
                        path = uri.toString(),
                        tags = state.analysis.tags,
                        extractedText = state.analysis.extractedText,
                        timestamp = System.currentTimeMillis()
                    )
                    searchEngine.indexImage(indexed)
                }
            }
        }
    }

    // Initialize model when permission granted
    LaunchedEffect(permissionGranted) {
        if (permissionGranted) {
            viewModel.prepareModel(ModelRegistry.MOBILENET_V1, useGpu = false)
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            // Load bitmap from URI
            LaunchedEffect(it) {
                val bitmap = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(it)?.use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }
                selectedBitmap = bitmap
                bitmap?.let { bmp -> viewModel.analyzeImage(bmp) }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("JAGallery AI") },
                actions = {
                    IconButton(onClick = { showSearch = !showSearch }) {
                        Icon(Icons.Default.Search, "Search")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Permission check
            if (!permissionGranted) {
                RequestMediaPermission { state ->
                    permissionGranted = state == PermissionState.Granted
                }
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Media Access Required",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Grant permission to analyze your photos",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                return@Scaffold
            }

            // Show search interface if toggled
            if (showSearch) {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                                searchResults = searchEngine.search(it)
                            },
                            label = { Text("Search tags or text") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        if (searchResults.isNotEmpty()) {
                            Text(
                                "${searchResults.size} results",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            LazyColumn(modifier = Modifier.height(200.dp)) {
                                items(searchResults) { result ->
                                    ListItem(
                                        headlineContent = { Text(result.path.substringAfterLast("/")) },
                                        supportingContent = {
                                            Text("Tags: ${result.tags.take(3).joinToString(", ")}")
                                        }
                                    )
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }
            }

            // State-based UI
            when (val state = currentState) {
                is AIState.Idle -> {
                    CircularProgressIndicator()
                    Text("Initializing...", modifier = Modifier.padding(top = 8.dp))
                }

                is AIState.DownloadingModel -> {
                    Text("Downloading AI model...", style = MaterialTheme.typography.titleMedium)
                    LinearProgressIndicator(
                        progress = state.progress / 100f,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                    )
                    Text("${state.progress}%")
                }

                is AIState.InitializingClassifier -> {
                    CircularProgressIndicator()
                    Text("Loading classifier...", modifier = Modifier.padding(top = 8.dp))
                }

                is AIState.Ready -> {
                    Button(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Image to Analyze")
                    }

                    Text(
                        "Ready to analyze photos with AI classification and OCR",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }

                is AIState.Analyzing -> {
                    selectedBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(200.dp).padding(bottom = 16.dp)
                        )
                    }
                    CircularProgressIndicator()
                    Text("Analyzing image...", modifier = Modifier.padding(top = 8.dp))
                }

                is AIState.AnalysisComplete -> {
                    selectedBitmap?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(200.dp).padding(bottom = 16.dp)
                        )
                    }

                    currentAnalysis?.let { analysis ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Classification Results",
                                    style = MaterialTheme.typography.titleMedium
                                )

                                if (analysis.tags.isNotEmpty()) {
                                    Text(
                                        "Tags: ${analysis.tags.joinToString(", ")}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }

                                if (analysis.extractedText.isNotBlank()) {
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                    Text(
                                        "Extracted Text:",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        analysis.extractedText,
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    Button(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    ) {
                        Text("Analyze Another Image")
                    }
                }

                is AIState.Error -> {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Error",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                            Text(
                                state.message,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.prepareModel(ModelRegistry.MOBILENET_V1, useGpu = false)
                        },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}
