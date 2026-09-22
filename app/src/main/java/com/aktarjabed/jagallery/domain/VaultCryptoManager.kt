package com.aktarjabed.jagallery.domain

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultCryptoManager @Inject constructor() {
    private val keyStore = try {
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    } catch (e: Exception) {
        // Fallback for pure JVM tests where AndroidKeyStore isn't available
        KeyStore.getInstance(KeyStore.getDefaultType()).apply { load(null, null) }
    }
    private val alias = "vault_master_key"

    private fun getSecretKey(): SecretKey {
        return try {
            val entry = keyStore.getEntry(alias, null)
            if (entry is KeyStore.SecretKeyEntry) {
                entry.secretKey
            } else {
                createSecretKey()
            }
        } catch (e: Exception) {
            createSecretKey()
        }
    }

    private var testSecretKey: SecretKey? = null

    private fun createSecretKey(): SecretKey {
        try {
            val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
            val spec = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .build()
            keyGenerator.init(spec)
            return keyGenerator.generateKey()
        } catch (e: Exception) {
            // Fallback for tests where AndroidKeyStore isn't available
            if (testSecretKey != null) return testSecretKey!!
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(256)
            val key = keyGenerator.generateKey()
            testSecretKey = key
            return key
        }
    }

    fun getEncryptCipher(): Cipher {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        return cipher
    }

    fun getDecryptCipher(iv: ByteArray): Cipher {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val spec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec)
        return cipher
    }

    fun encrypt(inputStream: InputStream, outputStream: OutputStream) {
        val cipher = getEncryptCipher()
        val iv = cipher.iv
        outputStream.write(iv.size)
        outputStream.write(iv)

        val cipherOutputStream = CipherOutputStream(outputStream, cipher)
        inputStream.use { input ->
            cipherOutputStream.use { output ->
                input.copyTo(output)
            }
        }
    }

    fun decrypt(inputStream: InputStream, outputStream: OutputStream) {
        val ivSize = inputStream.read()
        val iv = ByteArray(ivSize)
        inputStream.read(iv)

        val cipher = getDecryptCipher(iv)
        val cipherInputStream = CipherInputStream(inputStream, cipher)

        cipherInputStream.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
    }
}
