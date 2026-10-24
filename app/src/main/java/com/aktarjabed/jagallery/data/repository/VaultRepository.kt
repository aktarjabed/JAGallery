package com.aktarjabed.jagallery.data.repository

import android.content.Context
import android.net.Uri
import com.aktarjabed.jagallery.data.local.MediaDao
import com.aktarjabed.jagallery.data.local.VaultMediaEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

sealed class VaultDeleteResult {
    object Success : VaultDeleteResult()
    object FileDeletionFailed : VaultDeleteResult()
    object DatabaseDeletionFailed : VaultDeleteResult()
    object FileNotFound : VaultDeleteResult()
}

@Singleton
class VaultRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vaultCryptoManager: com.aktarjabed.jagallery.domain.VaultCryptoManager,
    private val mediaDao: MediaDao
) {
    private val vaultDir = File(context.filesDir, "vault").apply { mkdirs() }
    private val tempDir = File(context.cacheDir, "vault_temp").apply { mkdirs() }

    private val batchVaultSemaphore = Semaphore(4)

    suspend fun <T, R> runBoundedBatchIo(
        items: List<T>,
        block: suspend (T) -> R?
    ): List<R> = coroutineScope {
        items.map { item ->
            async {
                batchVaultSemaphore.withPermit {
                    try {
                        block(item)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        null
                    }
                }
            }
        }.awaitAll().filterNotNull()
    }

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

    /**
     * Batch-encrypts files concurrently (bounded by [batchVaultSemaphore]) and performs
     * a single atomic Room [MediaDao.insertVaultMediaBatch]. If the DB insert throws,
     * all staged encrypted files are rolled back.
     */
    suspend fun batchMoveToVault(
        items: List<Triple<Uri, String, String>> // (uri, mimeType, originalName)
    ): List<VaultMediaEntity> = withContext(Dispatchers.IO) {
        // Stage encrypted files concurrently with bounded parallelism
        data class StagedItem(val entity: VaultMediaEntity, val encryptedFile: File)

        val stagedFiles = java.util.concurrent.ConcurrentLinkedQueue<File>()
        val staged: List<StagedItem> = try {
            coroutineScope {
                items.map { (uri, mimeType, originalName) ->
                    async {
                        batchVaultSemaphore.withPermit {
                            val id = UUID.randomUUID().toString()
                            val encryptedFile = File(vaultDir, id)
                            // Register before IO starts so parent cancellation can remove
                            // partial files and files completed by sibling tasks.
                            stagedFiles.add(encryptedFile)
                            try {
                                context.contentResolver.openInputStream(uri)?.use { input ->
                                    FileOutputStream(encryptedFile).use { output ->
                                        vaultCryptoManager.encrypt(input, output)
                                    }
                                } ?: throw IllegalStateException("Could not open input stream for $uri")

                                StagedItem(
                                    VaultMediaEntity(
                                        id = id,
                                        originalUriStr = uri.toString(),
                                        mimeType = mimeType,
                                        encryptedFilePath = encryptedFile.absolutePath,
                                        dateAdded = System.currentTimeMillis(),
                                        originalName = originalName
                                    ),
                                    encryptedFile
                                )
                            } catch (e: CancellationException) {
                                encryptedFile.delete()
                                throw e
                            } catch (_: Exception) {
                                encryptedFile.delete()
                                null
                            }
                        }
                    }
                }.awaitAll().filterNotNull()
            }
        } catch (e: CancellationException) {
            stagedFiles.forEach { it.delete() }
            throw e
        } catch (e: Exception) {
            stagedFiles.forEach { it.delete() }
            throw e
        }

        if (staged.isEmpty()) return@withContext emptyList()

        // Atomic batch DB insert — rollback staged files on failure
        try {
            mediaDao.insertVaultMediaBatch(staged.map { it.entity })
        } catch (e: Exception) {
            staged.forEach { it.encryptedFile.delete() }
            throw e
        }

        staged.map { it.entity }
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

    suspend fun deleteVaultItem(entity: VaultMediaEntity): VaultDeleteResult = withContext(Dispatchers.IO) {
        // Rollback paths may only know the vault ID and intentionally carry a blank path.
        // Resolve blank paths within the private vault directory; never interpret File("") as
        // a vault object or attempt to delete a directory outside vaultDir.
        val encryptedFile = if (entity.encryptedFilePath.isBlank()) {
            File(vaultDir, entity.id)
        } else {
            File(entity.encryptedFilePath).canonicalFile
        }
        val canonicalVaultDir = vaultDir.canonicalFile
        if (encryptedFile.canonicalFile.parentFile != canonicalVaultDir) {
            return@withContext VaultDeleteResult.FileDeletionFailed
        }
        if (encryptedFile.exists() && !encryptedFile.delete()) {
            return@withContext VaultDeleteResult.FileDeletionFailed
        }
        val tempFile = File(tempDir, entity.id)
        if (tempFile.exists()) {
            tempFile.delete()
        }

        try {
            mediaDao.deleteVaultMedia(entity)
            return@withContext VaultDeleteResult.Success
        } catch (e: Exception) {
            return@withContext VaultDeleteResult.DatabaseDeletionFailed
        }
    }
}

