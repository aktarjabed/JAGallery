package com.aktarjabed.jagallery.fakes

import com.aktarjabed.jagallery.data.local.MediaDao
import com.aktarjabed.jagallery.data.local.MediaEntity
import com.aktarjabed.jagallery.data.local.HiddenMediaEntity
import com.aktarjabed.jagallery.data.local.TrashMediaEntity
import com.aktarjabed.jagallery.data.local.VaultMediaEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory test implementation of [MediaDao]. */
class FakeMediaDao : MediaDao {
    private val favoritesFlow = MutableStateFlow<List<MediaEntity>>(emptyList())
    override fun getFavorites(): Flow<List<MediaEntity>> = favoritesFlow
    override suspend fun insert(mediaEntity: MediaEntity) {
        val list = favoritesFlow.value.toMutableList()
        list.removeAll { it.uri == mediaEntity.uri }
        list.add(mediaEntity)
        favoritesFlow.value = list
    }
    override suspend fun removeFavorite(uri: String) {
        favoritesFlow.value = favoritesFlow.value.filterNot { it.uri == uri }
    }
    override suspend fun getFavoriteById(uri: String): MediaEntity? = favoritesFlow.value.find { it.uri == uri }
    override suspend fun removeFavorites(uris: List<String>) {
        favoritesFlow.value = favoritesFlow.value.filterNot { it.uri in uris }
    }

    private val hiddenFlow = MutableStateFlow<List<HiddenMediaEntity>>(emptyList())
    override fun getHiddenMedia(): Flow<List<HiddenMediaEntity>> = hiddenFlow
    override suspend fun getHiddenMediaById(uri: String): HiddenMediaEntity? = hiddenFlow.value.find { it.uri == uri }
    override suspend fun hideMedia(hiddenEntity: HiddenMediaEntity) {
        hiddenFlow.value = (hiddenFlow.value.filterNot { it.uri == hiddenEntity.uri } + hiddenEntity)
    }
    override suspend fun hideMediaBatch(hiddenEntities: List<HiddenMediaEntity>) = hiddenEntities.forEach { hideMedia(it) }
    override suspend fun unhideMedia(uri: String) {
        hiddenFlow.value = hiddenFlow.value.filterNot { it.uri == uri }
    }
    override suspend fun unhideMediaBatch(uris: List<String>) {
        hiddenFlow.value = hiddenFlow.value.filterNot { it.uri in uris }
    }

    private val trashFlow = MutableStateFlow<List<TrashMediaEntity>>(emptyList())
    override fun getTrashMedia(): Flow<List<TrashMediaEntity>> = trashFlow
    override suspend fun insertTrashMediaBatch(trashEntities: List<TrashMediaEntity>) {
        val uris = trashEntities.map { it.uri }.toSet()
        trashFlow.value = trashFlow.value.filterNot { it.uri in uris } + trashEntities
    }
    override suspend fun removeTrashMediaBatch(uris: List<String>) {
        trashFlow.value = trashFlow.value.filterNot { it.uri in uris }
    }
    override suspend fun getAllTrashMediaSync(): List<TrashMediaEntity> = trashFlow.value

    private val vaultFlow = MutableStateFlow<List<VaultMediaEntity>>(emptyList())
    override fun getVaultMedia(): Flow<List<VaultMediaEntity>> = vaultFlow
    override suspend fun insertVaultMedia(vaultEntity: VaultMediaEntity) {
        vaultFlow.value = vaultFlow.value.filterNot { it.id == vaultEntity.id } + vaultEntity
    }
    override suspend fun insertAll(items: List<VaultMediaEntity>) {
        val ids = items.map { it.id }.toSet()
        vaultFlow.value = vaultFlow.value.filterNot { it.id in ids } + items
    }
    override suspend fun insertVaultMediaBatch(items: List<VaultMediaEntity>) = insertAll(items)
    override suspend fun deleteVaultMedia(vaultEntity: VaultMediaEntity) {
        vaultFlow.value = vaultFlow.value.filterNot { it.id == vaultEntity.id }
    }
}
