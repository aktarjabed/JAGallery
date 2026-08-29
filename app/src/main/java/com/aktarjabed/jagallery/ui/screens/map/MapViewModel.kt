package com.aktarjabed.jagallery.ui.screens.map

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.jagallery.data.model.GeoMediaItem
import com.aktarjabed.jagallery.data.model.MediaLoadResult
import com.aktarjabed.jagallery.data.repository.MediaRepository
import com.aktarjabed.jagallery.util.ExifGpsExtractor
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
import kotlinx.coroutines.withTimeoutOrNull
import android.util.Log
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

    fun loadGeoItems() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                mediaRepository.loadMedia(force = true, context = context)
                val result = withTimeoutOrNull(30_000L) {
                    mediaRepository.mediaLoadResult
                        .filterIsInstance<MediaLoadResult.Success>()
                        .first()
                }
                if (result == null) {
                    _isLoading.value = false
                    return@launch
                }
                val geoItems = result.items.mapNotNull { item ->
                    val latLng = ExifGpsExtractor.extractLatLng(context, item.uri)
                    latLng?.let { GeoMediaItem(item, it.first, it.second) }
                }
                _geoItems.value = geoItems
            } catch (e: Exception) {
                Log.e("MapViewModel", "Failed to load geo items", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
}
