package com.suvojeet.suvmusic.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Turns a pasted cookie export into the `Cookie` header form SessionManager stores. Accepts a
 * raw header, a Netscape cookies.txt, or a browser extension's JSON export. Returns null when
 * the input holds no YouTube session cookie.
 */
object YouTubeCookieParser {
    private val AUTH_COOKIES = setOf("SAPISID", "__Secure-3PAPISID", "__Secure-1PAPISID")
    private const val YOUTUBE_DOMAIN = "youtube.com"
    private const val NETSCAPE_FIELDS = 7
    private const val HTTP_ONLY_PREFIX = "#HttpOnly_"

    private data class Entry(val name: String, val value: String, val domain: String?)

    fun parse(input: String): String? {
        val text = input.trim()
        if (text.isEmpty()) return null
        val entries = when {
            text.startsWith("[") -> parseJson(text)
            text.lineSequence().any { it.split('\t').size >= NETSCAPE_FIELDS } -> parseNetscape(text)
            else -> parseHeader(text)
        }
        val youtube = entries.filter { it.domain?.contains(YOUTUBE_DOMAIN) == true }
        val usable = (youtube.ifEmpty { entries })
            .filter { it.name.isNotEmpty() }
            .associate { it.name to it.value }
        if (usable.keys.none { it in AUTH_COOKIES }) return null
        return usable.entries.joinToString("; ") { "${it.key}=${it.value}" }
    }

    private fun parseHeader(text: String): List<Entry> =
        text.removePrefix("Cookie:").removePrefix("cookie:")
            .replace("\n", "")
            .split(';')
            .mapNotNull { part ->
                val pair = part.trim().split('=', limit = 2)
                if (pair.size == 2) Entry(pair[0].trim(), pair[1].trim(), null) else null
            }

    private fun parseNetscape(text: String): List<Entry> =
        text.lineSequence()
            .map { it.trim().removePrefix(HTTP_ONLY_PREFIX) }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { it.split('\t') }
            .filter { it.size >= NETSCAPE_FIELDS }
            .map { Entry(it[5], it[6], it[0]) }
            .toList()

    private fun parseJson(text: String): List<Entry> = try {
        (Json.parseToJsonElement(text) as JsonArray).mapNotNull { element ->
            val cookie = element.jsonObject
            val name = cookie["name"]?.jsonPrimitive?.content
            val value = cookie["value"]?.jsonPrimitive?.content
            if (name != null && value != null) {
                Entry(name, value, cookie["domain"]?.jsonPrimitive?.content)
            } else {
                null
            }
        }
    } catch (e: IllegalArgumentException) {
        emptyList()
    }
}
