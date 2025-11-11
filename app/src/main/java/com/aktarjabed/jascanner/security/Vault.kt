package com.aktarjabed.jascanner.security

import android.content.Context
import android.net.Uri
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKeys
import java.io.File

class Vault(private val context: Context) {
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    private val vaultDir = File(context.filesDir, "vault").apply { mkdirs() }

    fun encryptAndStore(bytes: ByteArray, name: String) {
        val file = File(vaultDir, name)
        val enc = EncryptedFile.Builder(
            file,
            context,
            masterKeyAlias,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        enc.openFileOutput().use { it.write(bytes) }
    }

    fun encryptFromUri(uri: Uri, name: String) {
        val resolver = context.contentResolver
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return
        encryptAndStore(bytes, name)
    }

    fun list(): List<String> = vaultDir.list()?.toList() ?: emptyList()

    fun getVaultFile(name: String): File = File(vaultDir, name)
}