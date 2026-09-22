package com.aktarjabed.jagallery.ui.screens.vault

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.jagallery.data.local.MediaDao
import com.aktarjabed.jagallery.data.local.VaultMediaEntity
import com.aktarjabed.jagallery.domain.MediaOperations
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VaultViewerViewModel @Inject constructor(
    private val vaultSessionManager: com.aktarjabed.jagallery.domain.VaultSessionManager,
    private val mediaDao: MediaDao,
    private val vaultRepository: com.aktarjabed.jagallery.data.repository.VaultRepository,
    private val mediaOperations: MediaOperations,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val vaultMediaId: String? = savedStateHandle.get<String>("vaultMediaId")

    private val _currentEntity = MutableStateFlow<VaultMediaEntity?>(null)
    val currentEntity: StateFlow<VaultMediaEntity?> = _currentEntity

    private val _tempUri = MutableStateFlow<android.net.Uri?>(null)
    val tempUri: StateFlow<android.net.Uri?> = _tempUri

    val isUnlocked: StateFlow<Boolean> = vaultSessionManager.isUnlocked

    init {
        viewModelScope.launch {
            if (vaultMediaId != null) {
                mediaDao.getVaultMedia().collect { list ->
                    val entity = list.find { it.id == vaultMediaId }
                    _currentEntity.value = entity
                    if (entity != null && isUnlocked.value) {
                        decryptAndLoad(entity)
                    }
                }
            }
        }
    }

    private suspend fun decryptAndLoad(entity: VaultMediaEntity) {
        try {
            val file = vaultRepository.decryptToTemp(entity)
            _tempUri.value = android.net.Uri.fromFile(file)
        } catch (e: Exception) {
            _tempUri.value = null
        }
    }

    fun getSecureContentUri(context: Context): android.net.Uri? {
        val fileUri = tempUri.value ?: return null
        val file = java.io.File(fileUri.path ?: return null)
        if (!file.exists()) return null
        return androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
    }

    fun cleanTemp() {
        viewModelScope.launch {
            vaultRepository.clearTemp()
            _tempUri.value = null
        }
    }

    suspend fun restoreItem(context: Context, entity: VaultMediaEntity): Boolean {
        if (!isUnlocked.value) return false
        val dest = com.aktarjabed.jagallery.data.model.AlbumDestination.ExistingAlbum(
            com.aktarjabed.jagallery.data.model.Album(
                com.aktarjabed.jagallery.data.model.AlbumKey(android.provider.MediaStore.VOLUME_EXTERNAL, 0L, "Restored/"),
                "Restored", 0, android.net.Uri.EMPTY
            )
        )
        val result = mediaOperations.restoreFromVault(context, listOf(entity), dest)
        return result is com.aktarjabed.jagallery.domain.MoveOperationResult.Success
    }

    suspend fun deleteItem(entity: VaultMediaEntity) {
        if (!isUnlocked.value) return
        vaultRepository.deleteVaultItem(entity)
    }

    override fun onCleared() {
        super.onCleared()
        cleanTemp()
    }
}
