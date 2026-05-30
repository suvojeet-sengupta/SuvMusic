package com.suvojeet.suvmusic.data

import java.security.MessageDigest

/**
 * Utility for generating YouTube Music internal API authorization headers.
 */
object YouTubeAuthUtils {

    fun getCookieValue(cookieString: String, cookieName: String): String? {
        return cookieString.split(";")
            .map { it.trim().split("=", limit = 2) }
            .find { it.first() == cookieName }
            ?.getOrNull(1)
    }

    /**
     * Generates the SAPISIDHASH required for authenticated requests.
     */
    fun getAuthorizationHeader(cookieString: String): String? {
        // Google sets the API session id under several names. Some accounts (notably
        // partitioned / __Secure-only cookie jars) never expose a bare "SAPISID",
        // only the __Secure- variants. Fall back through all of them so the login
        // gate's accepted cookie always yields a valid SAPISIDHASH instead of a
        // silently-unauthenticated request.
        val sapisid = getCookieValue(cookieString, "SAPISID")
            ?: getCookieValue(cookieString, "__Secure-3PAPISID")
            ?: getCookieValue(cookieString, "__Secure-1PAPISID")
            ?: return null
        val timestamp = System.currentTimeMillis() / 1000
        val origin = "https://music.youtube.com"
        
        // The hash format is: timestamp + space + sapisid + space + origin
        val input = "$timestamp $sapisid $origin"
        
        // SHA-1 Hashing
        val digest = MessageDigest.getInstance("SHA-1")
        val bytes = digest.digest(input.toByteArray())
        val hash = bytes.joinToString("") { "%02x".format(it) }

        return "SAPISIDHASH ${timestamp}_${hash}"
    }
}
