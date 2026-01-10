package com.suvojeet.suvmusic.util

import android.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES encryption utility for securing sensitive strings.
 * Uses AES/CBC/PKCS5Padding for encryption.
 */
object AESUtil {
    private const val ALGORITHM = "AES"
    private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
    
    /**
     * Encrypts a plain text string using AES.
     * @param plainText The text to encrypt
     * @param key 16-character key for AES-128
     * @return Base64 encoded encrypted string (IV + ciphertext)
     */
    fun encrypt(plainText: String, key: String): String {
        val keySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), ALGORITHM)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec)
        
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        
        // Combine IV + encrypted data
        val combined = iv + encrypted
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }
    
    /**
     * Decrypts an AES encrypted string.
     * @param encryptedText Base64 encoded encrypted string (IV + ciphertext)
     * @param key 16-character key for AES-128
     * @return Decrypted plain text
     */
    fun decrypt(encryptedText: String, key: String): String {
        return try {
            val combined = Base64.decode(encryptedText, Base64.NO_WRAP)
            
            // Extract IV (first 16 bytes) and ciphertext
            val iv = combined.copyOfRange(0, 16)
            val ciphertext = combined.copyOfRange(16, combined.size)
            
            val keySpec = SecretKeySpec(key.toByteArray(Charsets.UTF_8), ALGORITHM)
            val ivSpec = IvParameterSpec(iv)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            
            String(cipher.doFinal(ciphertext), Charsets.UTF_8)
        } catch (e: Exception) {
            // Fallback for safety - should not happen in production
            ""
        }
    }
}
