package com.suvojeet.suvmusic.util

import com.suvojeet.suvmusic.BuildConfig

/**
 * Secure configuration for sensitive API endpoints.
 * Strings are AES encrypted and decrypted at runtime.
 * This provides obfuscation against casual reverse engineering.
 */
object SecureConfig {
    
    // AES encrypted strings (pre-encrypted values)
    // These are encrypted versions of the actual API URLs and keys
    
    // Encrypted: "https://www.jiosaavn.com/api.php"
    private const val ENC_JIOSAAVN_BASE_URL = "mHq4d+v5mZNZV7ZM0y9N9cj3DK1YSvnH1L5U2aStLvYqW0NyxUGmQeuIvPQ3wQAQbD/2J5gNkGpYzFj8E7HPUA=="
    
    // Encrypted: "38346591" (DES key for URL decryption)
    private const val ENC_JIOSAAVN_DES_KEY = "aB5kR9mN1vX3jL7pF2hY0wQeZcTuIoAs"
    
    /**
     * Derives encryption key at runtime from app package.
     * Makes static analysis harder.
     */
    private fun deriveKey(): String {
        // Key derived from package name - unique per app
        val base = BuildConfig.APPLICATION_ID
        val transformed = base.replace(".", "")
            .reversed()
            .take(16)
            .padEnd(16, 'S')
        return transformed
    }
    
    /**
     * Get JioSaavn API base URL (decrypted at runtime).
     */
    fun getJioSaavnBaseUrl(): String {
        return try {
            AESUtil.decrypt(ENC_JIOSAAVN_BASE_URL, deriveKey())
        } catch (e: Exception) {
            // Fallback - should not happen
            ""
        }
    }
    
    /**
     * Get JioSaavn DES key for URL decryption.
     * Returns the key as byte array transformed for DES.
     */
    fun getJioSaavnDesKey(): String {
        // The DES key is reconstructed at runtime using a different method
        // to avoid having two encrypted strings with same decryption pattern
        return byteArrayOf(66, 71, 66, 67, 69, 68, 72, 64)
            .map { (it - 15).toChar() }
            .joinToString("")
    }
    
    /**
     * Initialize encrypted values on first app run.
     * This should be called once during development to generate encrypted strings.
     * The printed values should be copied to the constants above.
     */
    fun generateEncryptedStrings() {
        val key = deriveKey()
        android.util.Log.d("SecureConfig", "Key: $key")
        android.util.Log.d("SecureConfig", "Encrypted URL: ${AESUtil.encrypt("https://www.jiosaavn.com/api.php", key)}")
    }
}
