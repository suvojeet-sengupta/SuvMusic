package com.suvojeet.suvmusic.core.domain.repository

/** Optional capability for hosts that let users manage local-media folders. */
interface LocalMediaRootManager {
    fun configuredRoots(): List<String>
    fun updateRoots(rootPaths: List<String>)
}
