package com.example.advancedgallery.ui.screens.map

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.advancedgallery.data.model.GeoMediaItem
import com.example.advancedgallery.data.model.MediaLoadResult
import com.example.advancedgallery.data.repository.MediaRepository
import com.example.advancedgallery.util.ExifGpsExtractor
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class MapViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _geoItems = MutableStateFlow<List<GeoMediaItem>>(emptyList())
    val geoItems: StateFlow<List<GeoMediaItem>> = _geoItems.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _permissionDenied = MutableStateFlow(false)
    val permissionDenied: StateFlow<Boolean> = _permissionDenied.asStateFlow()

    fun onPermissionResult(granted: Boolean) {
        if (granted) {
            _permissionDenied.value = false
            loadGeoItems()
        } else {
            _permissionDenied.value = true
        }
    }

    fun loadGeoItems() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = mediaRepository.mediaLoadResult
                    .filterIsInstance<MediaLoadResult.Success>()
                    .first()

                val items = withContext(Dispatchers.IO) {
                    result.items.mapNotNull { item ->
                        ExifGpsExtractor.extractLatLng(context, item.uri)?.let { (lat, lng) ->
                            GeoMediaItem(item, lat, lng)
                        }
                    }
                }
                _geoItems.value = items
            } catch (e: Exception) {
                _geoItems.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
