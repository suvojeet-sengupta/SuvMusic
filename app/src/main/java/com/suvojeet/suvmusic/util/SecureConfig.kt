package com.suvojeet.suvmusic.util

import com.suvojeet.suvmusic.BuildConfig

/**
 * Secure configuration for sensitive API endpoints.
 * Strings are AES encrypted and decrypted at runtime.
 * Key derivation is handled in native code to resist reverse-engineering.
 */
object SecureConfig {
    
    init {
        System.loadLibrary("suvmusic_native")
    }

    // Native key derivation — implemented in secure_config.cpp
    private external fun nDeriveKey(): String

    // Encrypted Developer Password
    private const val ENC_DEV_PASSWORD = "xRPT8bkdX5W955JnBIE//WQuRXgaLjxR9PD9F4CQmkA="

    /**
     * Check if the provided password matches the encrypted developer password.
     */
    fun checkDeveloperPassword(input: String): Boolean {
        return try {
            val decryptedPassword = AESUtil.decrypt(ENC_DEV_PASSWORD, nDeriveKey())
            input == decryptedPassword
        } catch (e: Exception) {
            false
        }
    }
}
