package com.aktarjabed.jagallery.fakes

import com.aktarjabed.jagallery.data.local.MediaDao
import com.aktarjabed.jagallery.data.local.MediaEntity
import com.aktarjabed.jagallery.data.local.HiddenMediaEntity
import com.aktarjabed.jagallery.data.local.TrashMediaEntity
import com.aktarjabed.jagallery.data.local.VaultMediaEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * In-memory stub of [MediaDao] for unit-test usage.
 * Only the methods exercised by the current tests are fully implemented.
 * Canonical location: src/test/java (compiled by Kotlin in the same source set as all test files).
 */
class FakeMediaDao : MediaDao {
    // ---------- Favorites ----------
    private val favoritesFlow = MutableStateFlow<List<MediaEntity>>(emptyList())
    override fun getFavorites(): Flow<List<MediaEntity>> = favoritesFlow
    override suspend fun insert(mediaEntity: MediaEntity) {
        val list = favoritesFlow.value.toMutableList()
        list.removeAll { it.uri == mediaEntity.uri }
        list.add(mediaEntity)
        favoritesFlow.value = list
    }
    override suspend fun removeFavorite(uri: String) {
        val list = favoritesFlow.value.toMutableList()
        list.removeAll { it.uri == uri }
        favoritesFlow.value = list
    }
    override suspend fun getFavoriteById(uri: String): MediaEntity? =
        favoritesFlow.value.find { it.uri == uri }
    override suspend fun removeFavorites(uris: List<String>) {
        val list = favoritesFlow.value.toMutableList()
        list.removeAll { uris.contains(it.uri) }
        favoritesFlow.value = list
    }

    // ---------- Hidden Media ----------
    private val hiddenFlow = MutableStateFlow<List<HiddenMediaEntity>>(emptyList())
    override fun getHiddenMedia(): Flow<List<HiddenMediaEntity>> = hiddenFlow
    override suspend fun getHiddenMediaById(uri: String): HiddenMediaEntity? =
        hiddenFlow.value.find { it.uri == uri }
    override suspend fun hideMedia(hiddenEntity: HiddenMediaEntity) {
        val list = hiddenFlow.value.toMutableList()
        list.removeAll { it.uri == hiddenEntity.uri }
        list.add(hiddenEntity)
        hiddenFlow.value = list
    }
    override suspend fun hideMediaBatch(hiddenEntities: List<HiddenMediaEntity>) {
        hiddenEntities.forEach { hideMedia(it) }
    }
    override suspend fun unhideMedia(uri: String) {
        val list = hiddenFlow.value.toMutableList()
        list.removeAll { it.uri == uri }
        hiddenFlow.value = list
    }
    override suspend fun unhideMediaBatch(uris: List<String>) {
        val list = hiddenFlow.value.toMutableList()
        list.removeAll { uris.contains(it.uri) }
        hiddenFlow.value = list
    }

    // ---------- Trash Media ----------
    private val trashFlow = MutableStateFlow<List<TrashMediaEntity>>(emptyList())
    override suspend fun insertTrashMediaBatch(trashEntities: List<TrashMediaEntity>) {
        val list = trashFlow.value.toMutableList()
        val urisToAdd = trashEntities.map { it.uri }.toSet()
        list.removeAll { urisToAdd.contains(it.uri) }
        list.addAll(trashEntities)
        trashFlow.value = list
    }
    override suspend fun removeTrashMediaBatch(uris: List<String>) {
        val list = trashFlow.value.toMutableList()
        list.removeAll { uris.contains(it.uri) }
        trashFlow.value = list
    }
    override suspend fun getAllTrashMediaSync(): List<TrashMediaEntity> = trashFlow.value

    // ---------- Vault Media ----------
    private val vaultFlow = MutableStateFlow<List<VaultMediaEntity>>(emptyList())
    override fun getVaultMedia(): Flow<List<VaultMediaEntity>> = vaultFlow
    override suspend fun insertVaultMedia(vaultEntity: VaultMediaEntity) {
        val list = vaultFlow.value.toMutableList()
        list.removeAll { it.id == vaultEntity.id }
        list.add(vaultEntity)
        vaultFlow.value = list
    }
    override suspend fun insertAll(items: List<VaultMediaEntity>) {
        val list = vaultFlow.value.toMutableList()
        val ids = items.map { it.id }.toSet()
        list.removeAll { ids.contains(it.id) }
        list.addAll(items)
        vaultFlow.value = list
    }
    override suspend fun insertVaultMediaBatch(items: List<VaultMediaEntity>) {
        insertAll(items)
    }
    override suspend fun deleteVaultMedia(vaultEntity: VaultMediaEntity) {
        val list = vaultFlow.value.toMutableList()
        list.removeAll { it.id == vaultEntity.id }
        vaultFlow.value = list
    }
}
