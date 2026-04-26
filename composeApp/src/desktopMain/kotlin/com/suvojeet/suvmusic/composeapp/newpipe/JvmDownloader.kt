package com.suvojeet.suvmusic.composeapp.newpipe

import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Pure JDK implementation of NewPipe's [Downloader] — no OkHttp /
 * Ktor / Apache HTTP needed. Uses [HttpURLConnection] directly.
 *
 * Sufficient for YouTube search + stream URL extraction. If we ever
 * hit a quirky endpoint that needs more sophisticated handling
 * (cookies, automatic redirects with body, multipart uploads), swap
 * this for a Ktor-backed implementation — :core:domain already has
 * Ktor on the classpath via :core:db's transitive deps.
 */
class JvmDownloader : Downloader() {
    override fun execute(request: Request): Response {
        val connection = (URL(request.url()).openConnection() as HttpURLConnection).apply {
            requestMethod = request.httpMethod()
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            instanceFollowRedirects = true
            // Default UA so YouTube doesn't bounce us; NewPipe sets a more
            // specific one in request headers when needed.
            setRequestProperty("User-Agent", DEFAULT_USER_AGENT)
        }

        // Apply NewPipe-supplied headers (may override the defaults).
        for ((name, values) in request.headers()) {
            for (value in values) {
                connection.addRequestProperty(name, value)
            }
        }

        // Optional request body (POST with form data).
        request.dataToSend()?.let { body ->
            connection.doOutput = true
            connection.outputStream.use { it.write(body) }
        }

        val responseCode = connection.responseCode
        val responseMessage = connection.responseMessage.orEmpty()
        val latestUrl = connection.url.toString()

        // Read body — successful streams come from inputStream, errors from
        // errorStream. Read whichever is non-null.
        val rawBody = (connection.inputStream ?: connection.errorStream)
            ?.use { it.readBytes() }
            ?: ByteArray(0)
        val body = String(rawBody, StandardCharsets.UTF_8)

        // Filter out the pseudo-header `null` Java's HttpURLConnection adds
        // (which holds the HTTP/1.1 status line). NewPipe doesn't expect it.
        val responseHeaders: Map<String, List<String>> = connection.headerFields
            .filterKeys { it != null }

        return Response(responseCode, responseMessage, responseHeaders, body, latestUrl)
    }

    private companion object {
        const val TIMEOUT_MS = 30_000
        const val DEFAULT_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                "(KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }
}
