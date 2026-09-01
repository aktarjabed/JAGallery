package com.aktarjabed.jagallery.fakes

import com.aktarjabed.jagallery.data.local.MediaDao
import com.aktarjabed.jagallery.data.local.MediaEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeMediaDao : MediaDao {
    val favoritesFlow = MutableStateFlow<List<MediaEntity>>(emptyList())

    override fun getFavorites(): Flow<List<MediaEntity>> = favoritesFlow

    override suspend fun insert(mediaEntity: MediaEntity) {
        val current = favoritesFlow.value.toMutableList()
        current.removeAll { it.uri == mediaEntity.uri }
        current.add(mediaEntity)
        favoritesFlow.value = current
    }

    override suspend fun removeFavorite(uri: String) {
        val current = favoritesFlow.value.toMutableList()
        current.removeAll { it.uri == uri }
        favoritesFlow.value = current
    }

    override suspend fun getFavoriteById(uri: String): MediaEntity? {
        return favoritesFlow.value.find { it.uri == uri }
    }

    override suspend fun removeFavorites(uris: List<String>) {
        val current = favoritesFlow.value.toMutableList()
        current.removeAll { uris.contains(it.uri) }
        favoritesFlow.value = current
    }

    val hiddenFlow = MutableStateFlow<List<com.aktarjabed.jagallery.data.local.HiddenMediaEntity>>(emptyList())

    override fun getHiddenMedia(): Flow<List<com.aktarjabed.jagallery.data.local.HiddenMediaEntity>> = hiddenFlow

    override suspend fun getHiddenMediaById(uri: String): com.aktarjabed.jagallery.data.local.HiddenMediaEntity? {
        return hiddenFlow.value.find { it.uri == uri }
    }

    override suspend fun hideMedia(hiddenEntity: com.aktarjabed.jagallery.data.local.HiddenMediaEntity) {
        val current = hiddenFlow.value.toMutableList()
        current.removeAll { it.uri == hiddenEntity.uri }
        current.add(hiddenEntity)
        hiddenFlow.value = current
    }

    override suspend fun hideMediaBatch(hiddenEntities: List<com.aktarjabed.jagallery.data.local.HiddenMediaEntity>) {
        val current = hiddenFlow.value.toMutableList()
        val urisToAdd = hiddenEntities.map { it.uri }.toSet()
        current.removeAll { urisToAdd.contains(it.uri) }
        current.addAll(hiddenEntities)
        hiddenFlow.value = current
    }

    override suspend fun unhideMedia(uri: String) {
        val current = hiddenFlow.value.toMutableList()
        current.removeAll { it.uri == uri }
        hiddenFlow.value = current
    }

    override suspend fun unhideMediaBatch(uris: List<String>) {
        val current = hiddenFlow.value.toMutableList()
        val uriSet = uris.toSet()
        current.removeAll { uriSet.contains(it.uri) }
        hiddenFlow.value = current
    }
}
