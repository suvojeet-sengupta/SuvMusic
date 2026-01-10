package com.suvojeet.suvmusic.util

import com.suvojeet.suvmusic.BuildConfig

/**
 * Secure configuration for sensitive API endpoints.
 * Strings are AES encrypted and decrypted at runtime.
 */
object SecureConfig {
    

    
    // AES encrypted strings (pre-encrypted values)
    // New Stable Encrypted URL
    private const val ENC_BASE_URL = "pZDQ0pQNrtTqiEtmoBEGicIvjpFkoN76TEM4mKaLVcNQpIEpD5JrAsOtw+oaMFnJZIGOqemIK3I0ZHD2eo0fVQ=="
    
    // Encrypted Developer Password
    private const val ENC_DEV_PASSWORD = "xRPT8bkdX5W955JnBIE//WQuRXgaLjxR9PD9F4CQmkA="
    
    /**
     * Derives encryption key at runtime from STABLE package name.
     * Use "com.suvojeet.suvmusic" regardless of valid/flavor.
     */
    private fun deriveKey(): String {
        val base = "com.suvojeet.suvmusic" // Hardcoded stable package to avoid debug suffix issues
        val transformed = base.replace(".", "")
            .reversed()
            .take(16)
            .padEnd(16, 'S')
        return transformed
    }
    
    /**
     * Get API base URL (decrypted at runtime).
     */
    fun getJioSaavnBaseUrl(): String {
        return try {
            AESUtil.decrypt(ENC_BASE_URL, deriveKey())
        } catch (e: Exception) {
            ""
        }
    }
    
    /**
     * Get DES key for URL decryption.
     */
    fun getJioSaavnDesKey(): String {
        return byteArrayOf(66, 71, 66, 67, 69, 68, 72, 64)
            .map { (it - 15).toChar() }
            .joinToString("")
    }
    
    /**
     * Check if the provided password matches the encrypted developer password.
     */
    fun checkDeveloperPassword(input: String): Boolean {
        return try {
            val decryptedPassword = AESUtil.decrypt(ENC_DEV_PASSWORD, deriveKey())
            input == decryptedPassword
        } catch (e: Exception) {
            false
        }
    }
}

