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
                mediaRepository.loadMedia(force = false, context = context)
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
