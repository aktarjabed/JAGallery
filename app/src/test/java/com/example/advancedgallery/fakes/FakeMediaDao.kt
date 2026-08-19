package com.example.advancedgallery.fakes

import com.example.advancedgallery.data.local.MediaDao
import com.example.advancedgallery.data.local.MediaEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeMediaDao : MediaDao {
    val favoritesFlow = MutableStateFlow<List<MediaEntity>>(emptyList())

    override fun getFavorites(): Flow<List<MediaEntity>> = favoritesFlow

    override suspend fun insert(mediaEntity: MediaEntity) {
        val current = favoritesFlow.value.toMutableList()
        current.removeAll { it.id == mediaEntity.id }
        current.add(mediaEntity)
        favoritesFlow.value = current
    }

    override suspend fun removeFavorite(id: Long) {
        val current = favoritesFlow.value.toMutableList()
        current.removeAll { it.id == id }
        favoritesFlow.value = current
    }

    override suspend fun getFavoriteById(id: Long): MediaEntity? {
        return favoritesFlow.value.find { it.id == id }
    }

    override suspend fun removeFavorites(ids: List<Long>) {
        val current = favoritesFlow.value.toMutableList()
        current.removeAll { ids.contains(it.id) }
        favoritesFlow.value = current
    }
}
