package com.aktarjabed.core.security.vault

import android.content.Context
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.File

class VaultManager(private val context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val vaultDir = File(context.filesDir, "vault").apply {
        if (!exists()) {
            mkdirs()
        }
    }

    fun addFileToVault(file: File): File {
        val encryptedFile = File(vaultDir, "enc_${file.name}")

        EncryptedFile.Builder(
            context,
            encryptedFile,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build().openFileOutput().use { outputStream ->
            file.inputStream().use { inputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        return encryptedFile
    }

    fun retrieveFileFromVault(encryptedFile: File): File {
        val decryptedFile = File(context.cacheDir, "dec_${encryptedFile.name.removePrefix("enc_")}")

        EncryptedFile.Builder(
            context,
            encryptedFile,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build().openFileInput().use { inputStream ->
            decryptedFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        return decryptedFile
    }

    fun getVaultFiles(): List<File> {
        return vaultDir.listFiles()?.toList() ?: emptyList()
    }
}