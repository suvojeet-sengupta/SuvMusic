package com.suvojeet.suvmusic.core.domain.session

import kotlinx.coroutines.flow.Flow

/**
 * Shared account state boundary. Implementations own secure credential storage;
 * common code receives only login state and explicitly requested opaque handoff.
 */
interface AccountSessionStore {
    val isLoggedIn: Flow<Boolean>

    fun hasSession(): Boolean
    suspend fun saveOpaqueSession(value: String)
    suspend fun clearSession()
}
