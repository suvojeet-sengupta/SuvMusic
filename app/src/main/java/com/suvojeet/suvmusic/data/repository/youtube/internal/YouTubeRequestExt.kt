package com.suvojeet.suvmusic.data.repository.youtube.internal

import com.suvojeet.suvmusic.data.YouTubeAuthUtils
import okhttp3.Request

/**
 * Attaches the standard YouTube Music headers to a request, plus the
 * authenticated headers when [cookies] are available.
 *
 * Centralizing this here keeps every authenticated request consistent and
 * closes a set of plumbing bugs that came from duplicating the header block
 * across ~18 call sites:
 *
 *  - [Authorization] is only added when a SAPISIDHASH can actually be built,
 *    so we never send an empty `Authorization:` header.
 *  - [X-Goog-AuthUser] always reflects the active account index ([authUser]),
 *    instead of being hardcoded to "0".
 *  - [X-Origin] is sent alongside [Origin] and matches the origin the hash was
 *    signed with, which is what the server expects.
 *
 * Pass `null`/blank [cookies] for best-effort (logged-out) requests: only the
 * public headers (User-Agent, Origin, X-Origin) are added in that case.
 */
fun Request.Builder.addYouTubeAuthHeaders(cookies: String?, authUser: Int): Request.Builder {
    addHeader("User-Agent", YouTubeConfig.USER_AGENT)
    addHeader("Origin", YouTubeConfig.ORIGIN)
    addHeader("X-Origin", YouTubeConfig.ORIGIN)
    if (!cookies.isNullOrBlank()) {
        addHeader("Cookie", cookies)
        YouTubeAuthUtils.getAuthorizationHeader(cookies)?.let { addHeader("Authorization", it) }
        addHeader("X-Goog-AuthUser", authUser.toString())
    }
    return this
}
