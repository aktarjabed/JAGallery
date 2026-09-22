package com.aktarjabed.jagallery.data.repository

import android.content.Context
import android.net.Uri
import com.aktarjabed.jagallery.data.local.MediaDao
import com.aktarjabed.jagallery.data.local.VaultMediaEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vaultCryptoManager: com.aktarjabed.jagallery.domain.VaultCryptoManager,
    private val mediaDao: MediaDao
) {
    private val vaultDir = File(context.filesDir, "vault").apply { mkdirs() }
    private val tempDir = File(context.cacheDir, "vault_temp").apply { mkdirs() }

    suspend fun moveToVault(originalUri: Uri, mimeType: String, originalName: String): VaultMediaEntity = withContext(Dispatchers.IO) {
        val id = UUID.randomUUID().toString()
        val encryptedFile = File(vaultDir, id)

        try {
            context.contentResolver.openInputStream(originalUri)?.use { input ->
                FileOutputStream(encryptedFile).use { output ->
                    vaultCryptoManager.encrypt(input, output)
                }
            } ?: throw IllegalStateException("Could not open input stream")

            val entity = VaultMediaEntity(
                id = id,
                originalUriStr = originalUri.toString(),
                mimeType = mimeType,
                encryptedFilePath = encryptedFile.absolutePath,
                dateAdded = System.currentTimeMillis(),
                originalName = originalName
            )
            mediaDao.insertVaultMedia(entity)
            entity
        } catch (e: Exception) {
            // Clean up partial encrypted file if encryption or DB insert fails
            if (encryptedFile.exists()) {
                encryptedFile.delete()
            }
            throw e
        }
    }

    suspend fun decryptToTemp(entity: VaultMediaEntity): File = withContext(Dispatchers.IO) {
        val tempFile = File(tempDir, entity.id)
        if (tempFile.exists()) return@withContext tempFile

        val encryptedFile = File(entity.encryptedFilePath)
        FileInputStream(encryptedFile).use { input ->
            FileOutputStream(tempFile).use { output ->
                vaultCryptoManager.decrypt(input, output)
            }
        }
        tempFile
    }

    suspend fun clearTemp() = withContext(Dispatchers.IO) {
        tempDir.listFiles()?.forEach { it.delete() }
    }

    suspend fun deleteVaultItem(entity: VaultMediaEntity) = withContext(Dispatchers.IO) {
        val encryptedFile = File(entity.encryptedFilePath)
        if (encryptedFile.exists()) {
            encryptedFile.delete()
        }
        val tempFile = File(tempDir, entity.id)
        if (tempFile.exists()) {
            tempFile.delete()
        }
        mediaDao.deleteVaultMedia(entity)
    }
}
