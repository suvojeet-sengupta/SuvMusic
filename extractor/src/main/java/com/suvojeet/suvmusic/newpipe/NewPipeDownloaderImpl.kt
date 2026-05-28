package com.suvojeet.suvmusic.newpipe

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request as ExtractorRequest
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException

/**
 * A custom Downloader implementation for NewPipeExtractor using OkHttp.
 * This is required to initialize NewPipe and supports cookie injection for authenticated requests.
 */
class NewPipeDownloaderImpl(
    private val client: OkHttpClient,
    private val cookieProvider: (() -> String)? = null
) : Downloader() {

    override fun execute(request: ExtractorRequest): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder = Request.Builder()
            .url(url)
            .method(httpMethod, if (dataToSend != null) {
                dataToSend.toRequestBody(null)
            } else if (httpMethod == "POST" || httpMethod == "PUT") {
                ByteArray(0).toRequestBody(null)
            } else {
                null
            })

        // Add headers
        headers.forEach { (key, values) ->
            values.forEach { value ->
                requestBuilder.addHeader(key, value)
            }
        }

        // Add cookies from provider if available
        cookieProvider?.invoke()?.let { cookies ->
            if (cookies.isNotEmpty()) {
                requestBuilder.addHeader("Cookie", cookies)
            }
        }

        // Improved Header Handling:
        // Use a more modern and consistent User-Agent.
        // Some YouTube endpoints are very sensitive to User-Agent/Client mismatches.
        if (!headers.containsKey("User-Agent")) {
            val isAndroidClient = url.contains("android") || url.contains("googlevideo.com")
            val userAgent = if (isAndroidClient) {
                "com.google.android.youtube/19.05.36 (Linux; U; Android 14; en_US) gzip"
            } else {
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Safari/537.36"
            }
            requestBuilder.addHeader("User-Agent", userAgent)
        }
        
        // Add common headers to look more like a real browser/app
        if (!headers.containsKey("Accept-Language")) {
            requestBuilder.addHeader("Accept-Language", "en-US,en;q=0.9")
        }

        val reqStart = System.currentTimeMillis()
        val shortUrl = url.take(120)
        android.util.Log.d("NewPipeDL", "$httpMethod $shortUrl")
        try {
        val response = client.newCall(requestBuilder.build()).execute()
        response.use {
            val latency = System.currentTimeMillis() - reqStart
            if (response.code == 429) {
                android.util.Log.w("NewPipeDL", "429 ratelimit after ${latency}ms: $shortUrl")
                throw ReCaptchaException("Rate limited", url)
            }
            if (response.code >= 400) {
                android.util.Log.w("NewPipeDL", "HTTP ${response.code} ${response.message} after ${latency}ms: $shortUrl")
            } else {
                android.util.Log.d("NewPipeDL", "HTTP ${response.code} in ${latency}ms: $shortUrl")
            }

            // Prevent OOM by limiting response size (e.g., 10MB limit for metadata)
            val body = response.body
            val responseBodyString = if (body != null) {
                val limit = 10 * 1024 * 1024L // 10MB
                val contentLength = body.contentLength()
                if (contentLength > limit) {
                    android.util.Log.w("NewPipeDL", "body too large (${contentLength} > $limit), returning empty: $shortUrl")
                    "" // Return empty for too large files (likely streams not metadata)
                } else {
                    try {
                        // Peek or read conservatively
                        val source = body.source()
                        source.request(limit) // Request up to limit
                        if (source.buffer.size > limit) {
                            android.util.Log.w("NewPipeDL", "body exceeded limit during read, returning empty: $shortUrl")
                            ""
                        } else body.string()
                    } catch (e: Exception) {
                        android.util.Log.w("NewPipeDL", "body read threw ${e.javaClass.simpleName}: ${e.message}; $shortUrl")
                        ""
                    }
                }
            } else ""

            val responseHeaders = mutableMapOf<String, MutableList<String>>()
            response.headers.forEach { (name, value) ->
                responseHeaders.getOrPut(name) { mutableListOf() }.add(value)
            }

            return Response(
                response.code,
                response.message,
                responseHeaders,
                responseBodyString,
                url
            )
        }
        } catch (e: IOException) {
            android.util.Log.e("NewPipeDL", "IOException after ${System.currentTimeMillis() - reqStart}ms on $shortUrl: ${e.javaClass.simpleName}: ${e.message}")
            throw e
        }
    }
}