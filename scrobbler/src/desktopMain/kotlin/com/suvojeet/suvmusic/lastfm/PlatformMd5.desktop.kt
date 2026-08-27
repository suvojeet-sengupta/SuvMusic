package com.suvojeet.suvmusic.lastfm

import java.security.MessageDigest

internal actual fun md5Hex(value: String): String =
    MessageDigest.getInstance("MD5")
        .digest(value.encodeToByteArray())
        .joinToString("") { "%02x".format(it) }
