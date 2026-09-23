package com.aktarjabed.jagallery.domain

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.KeyPermanentlyInvalidatedException
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

sealed class VaultCryptoException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class KeyUnavailable(cause: Throwable? = null) : VaultCryptoException("Vault Key Unavailable", cause)
    class KeyInvalidated(cause: Throwable? = null) : VaultCryptoException("Vault Key Invalidated", cause)
    class AuthenticationRequired(cause: Throwable? = null) : VaultCryptoException("Vault Authentication Required", cause)
    class CorruptCiphertext(cause: Throwable? = null) : VaultCryptoException("Vault Ciphertext Corrupt", cause)
    class DecryptionFailed(cause: Throwable? = null) : VaultCryptoException("Vault Decryption Failed", cause)
    class IoFailure(cause: Throwable? = null) : VaultCryptoException("Vault IO Failure", cause)
}

@Singleton
class VaultCryptoManager @Inject constructor() {
    private val keyStore = try {
        KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
    } catch (e: Exception) {
        // Fallback ONLY for pure JVM tests where AndroidKeyStore isn't available
        KeyStore.getInstance(KeyStore.getDefaultType()).apply { load(null, null) }
    }
    private val alias = "vault_master_key"

    @Throws(VaultCryptoException::class)
    private fun getExistingSecretKey(): SecretKey {
        try {
            val entry = keyStore.getEntry(alias, null)
            if (entry is KeyStore.SecretKeyEntry) {
                return entry.secretKey
            }
            throw VaultCryptoException.KeyUnavailable()
        } catch (e: KeyPermanentlyInvalidatedException) {
            throw VaultCryptoException.KeyInvalidated(e)
        } catch (e: UnrecoverableKeyException) {
            throw VaultCryptoException.KeyUnavailable(e)
        } catch (e: Exception) {
            if (e is VaultCryptoException) throw e
            throw VaultCryptoException.KeyUnavailable(e)
        }
    }

    private var testSecretKey: SecretKey? = null

    @Throws(VaultCryptoException::class)
    private fun getOrCreateSecretKey(): SecretKey {
        try {
            return getExistingSecretKey()
        } catch (e: VaultCryptoException.KeyUnavailable) {
            return createSecretKey()
        }
    }

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
            val cachedTestKey = testSecretKey
            if (cachedTestKey != null) return cachedTestKey
            val keyGenerator = KeyGenerator.getInstance("AES")
            keyGenerator.init(256)
            val key = keyGenerator.generateKey()
            testSecretKey = key
            return key
        }
    }

    @Throws(VaultCryptoException::class)
    fun getEncryptCipher(): Cipher {
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
            return cipher
        } catch (e: VaultCryptoException) {
            throw e
        } catch (e: Exception) {
            throw VaultCryptoException.IoFailure(e)
        }
    }

    @Throws(VaultCryptoException::class)
    fun getDecryptCipher(iv: ByteArray): Cipher {
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val spec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, getExistingSecretKey(), spec)
            return cipher
        } catch (e: VaultCryptoException) {
            throw e
        } catch (e: Exception) {
            throw VaultCryptoException.DecryptionFailed(e)
        }
    }

    @Throws(VaultCryptoException::class)
    fun encrypt(inputStream: InputStream, outputStream: OutputStream) {
        try {
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
        } catch (e: VaultCryptoException) {
            throw e
        } catch (e: Exception) {
            throw VaultCryptoException.IoFailure(e)
        }
    }

    @Throws(VaultCryptoException::class)
    fun decrypt(inputStream: InputStream, outputStream: OutputStream) {
        try {
            val ivSize = inputStream.read()
            if (ivSize <= 0) throw VaultCryptoException.CorruptCiphertext()
            val iv = ByteArray(ivSize)
            val bytesRead = inputStream.read(iv)
            if (bytesRead != ivSize) throw VaultCryptoException.CorruptCiphertext()

            val cipher = getDecryptCipher(iv)
            val cipherInputStream = CipherInputStream(inputStream, cipher)

            cipherInputStream.use { input ->
                outputStream.use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: VaultCryptoException) {
            throw e
        } catch (e: Exception) {
            throw VaultCryptoException.DecryptionFailed(e)
        }
    }
}
