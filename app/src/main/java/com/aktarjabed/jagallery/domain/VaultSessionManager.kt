package com.aktarjabed.jagallery.domain

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VaultSessionManager @Inject constructor() {
    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked

    private var lastUnlockTime = 0L
    private val TIMEOUT_MS = 60 * 1000L // 1 minute auto-lock

    fun unlock() {
        _isUnlocked.value = true
        lastUnlockTime = System.currentTimeMillis()
    }

    fun lock() {
        _isUnlocked.value = false
        lastUnlockTime = 0L
    }

    fun checkTimeout(): Boolean {
        if (_isUnlocked.value && System.currentTimeMillis() - lastUnlockTime > TIMEOUT_MS) {
            lock()
            return true
        }
        return false
    }

    fun validateSessionOrThrow() {
        if (checkTimeout() || !_isUnlocked.value) {
            throw VaultCryptoException.AuthenticationRequired()
        }
        // refresh session on valid usage to prevent timeout during active vault work
        lastUnlockTime = System.currentTimeMillis()
    }
}
