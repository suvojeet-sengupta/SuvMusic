package com.suvojeet.suvmusic.util

internal object Obf {
    private val K = byteArrayOf(0x5C, 0x21, 0x77, 0x3E, 0x6A, 0x0F, 0x52, 0x91.toByte())

    fun d(b: ByteArray): String {
        val out = ByteArray(b.size)
        for (i in b.indices) out[i] = (b[i].toInt() xor K[i % K.size].toInt()).toByte()
        return String(out, Charsets.UTF_8)
    }
}
