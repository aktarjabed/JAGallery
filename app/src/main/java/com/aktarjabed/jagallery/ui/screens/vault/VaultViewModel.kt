package com.aktarjabed.jagallery.ui.screens.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aktarjabed.jagallery.data.local.MediaDao
import com.aktarjabed.jagallery.data.local.VaultMediaEntity
import com.aktarjabed.jagallery.domain.VaultSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.net.Uri

@HiltViewModel
class VaultViewModel @Inject constructor(
    private val vaultSessionManager: com.aktarjabed.jagallery.domain.VaultSessionManager,
    private val mediaDao: MediaDao,
    private val vaultRepository: com.aktarjabed.jagallery.data.repository.VaultRepository
) : ViewModel() {

    val isUnlocked: StateFlow<Boolean> = vaultSessionManager.isUnlocked

    val vaultMedia: StateFlow<List<VaultMediaEntity>> = mediaDao.getVaultMedia().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            isUnlocked.collect { unlocked ->
                if (!unlocked) {
                    vaultSessionManager.requestCleanup()
                }
            }
        }
    }

    suspend fun getDecryptedTempUriSuspended(entity: VaultMediaEntity): Uri? {
        try {
            vaultSessionManager.validateSessionOrThrow()
            val file = vaultRepository.decryptToTemp(entity)
            return android.net.Uri.fromFile(file)
        } catch (e: Exception) {
            return null
        }
    }

    fun getDecryptedTempUri(entity: VaultMediaEntity, onUriReady: (android.net.Uri?) -> Unit) {
        viewModelScope.launch {
            onUriReady(getDecryptedTempUriSuspended(entity))
        }
    }

    fun unlock() {
        vaultSessionManager.unlock()
    }

    fun lock() {
        vaultSessionManager.lock()
    }
}
