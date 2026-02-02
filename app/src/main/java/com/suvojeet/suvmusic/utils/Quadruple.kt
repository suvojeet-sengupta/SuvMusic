package com.suvojeet.suvmusic.utils

data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
) : java.io.Serializable {
    override fun toString(): String = "($first, $second, $third, $fourth)"
}
