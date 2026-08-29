package com.aktarjabed.jagallery.ui.screens.map

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aktarjabed.jagallery.R
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(
    onBack: () -> Unit,
    onNavigateToViewer: (String) -> Unit = {},
    viewModel: MapViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val geoItems by viewModel.geoItems.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        Configuration.getInstance().userAgentValue = context.packageName
        viewModel.loadGeoItems()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.map_view_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            geoItems.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_gps_photos),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
            else -> {
                OsmMap(
                    geoItems = geoItems,
                    onMarkerClick = { item ->
                        onNavigateToViewer(item.mediaItem.id)
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                )
            }
        }
    }
}

@Composable
private fun OsmMap(
    geoItems: List<com.aktarjabed.jagallery.data.model.GeoMediaItem>,
    onMarkerClick: (com.aktarjabed.jagallery.data.model.GeoMediaItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val appContext = context.applicationContext
    val mapView = remember { MapView(appContext) }

    AndroidView(
        factory = {
            mapView.apply {
                setMultiTouchControls(true)
                controller.setZoom(15.0)
            }
        },
        modifier = modifier,
        update = { map ->
            map.overlays.clear()

            if (geoItems.isNotEmpty()) {
                geoItems.forEach { item ->
                    val marker = Marker(map).apply {
                        position = GeoPoint(item.latitude, item.longitude)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        title = item.mediaItem.name
                        setOnMarkerClickListener { _, _ ->
                            onMarkerClick(item)
                            true
                        }
                    }
                    map.overlays.add(marker)
                }

                // Center on first item or average
                if (geoItems.size == 1) {
                    map.controller.setCenter(GeoPoint(geoItems[0].latitude, geoItems[0].longitude))
                    map.controller.setZoom(15.0)
                } else {
                    val avgLat = geoItems.sumOf { it.latitude } / geoItems.size
                    val avgLng = geoItems.sumOf { it.longitude } / geoItems.size
                    map.controller.setCenter(GeoPoint(avgLat, avgLng))
                    map.controller.setZoom(10.0)
                }
            }
            map.invalidate()
        }
    )

    DisposableEffect(Unit) {
        mapView.onResume()
        onDispose {
            mapView.onPause()
            mapView.onDetach() // OsmDroid MapView doesn't have onDestroy(), it has onDetach()
        }
    }
}
