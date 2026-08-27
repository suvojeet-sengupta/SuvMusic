package com.suvojeet.suvmusic.data

import com.suvojeet.suvmusic.core.domain.session.AccountSessionStore
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/** Android host adapter; SessionManager remains responsible for encrypted storage. */
@Singleton
class AndroidAccountSessionStore @Inject constructor(
    private val sessionManager: SessionManager,
) : AccountSessionStore {
    override val isLoggedIn: Flow<Boolean> = sessionManager.isLoggedInFlow
    override fun hasSession(): Boolean = sessionManager.isLoggedIn()
    override suspend fun saveOpaqueSession(value: String) = sessionManager.saveCookies(value)
    override suspend fun clearSession() = sessionManager.clearCookies()
}
