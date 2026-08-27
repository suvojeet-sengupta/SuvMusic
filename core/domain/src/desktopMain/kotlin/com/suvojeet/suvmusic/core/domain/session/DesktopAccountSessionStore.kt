package com.suvojeet.suvmusic.core.domain.session

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Linux host session adapter. Credentials remain process-local until a native
 * secret-service backend is introduced; no cookies are written to plaintext files.
 */
class DesktopAccountSessionStore : AccountSessionStore {
    private var opaqueSession: String? = null
    private val _isLoggedIn = MutableStateFlow(false)

    override val isLoggedIn: Flow<Boolean> = _isLoggedIn.asStateFlow()
    override fun hasSession(): Boolean = !opaqueSession.isNullOrBlank()

    override suspend fun saveOpaqueSession(value: String) {
        opaqueSession = value
        _isLoggedIn.value = value.isNotBlank()
    }

    override suspend fun clearSession() {
        opaqueSession = null
        _isLoggedIn.value = false
    }
}
