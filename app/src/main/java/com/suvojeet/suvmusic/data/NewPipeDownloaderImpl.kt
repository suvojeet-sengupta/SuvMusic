package com.suvojeet.suvmusic.data

import okhttp3.OkHttpClient
import okhttp3.Request
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
    private val sessionManager: SessionManager? = null
) : Downloader() {

    override fun execute(request: ExtractorRequest): Response {
        val httpMethod = request.httpMethod()
        val url = request.url()
        val headers = request.headers()
        val dataToSend = request.dataToSend()

        val requestBuilder = Request.Builder()
            .url(url)
            .method(httpMethod, if (dataToSend != null) {
                okhttp3.RequestBody.create(null, dataToSend)
            } else if (httpMethod == "POST" || httpMethod == "PUT") {
                okhttp3.RequestBody.create(null, ByteArray(0))
            } else {
                null
            })

        // Add headers
        headers.forEach { (key, values) ->
            values.forEach { value ->
                requestBuilder.addHeader(key, value)
            }
        }

        // Add cookies from SessionManager if available
        sessionManager?.getCookies()?.let { cookies ->
            if (cookies.isNotEmpty()) {
                requestBuilder.addHeader("Cookie", cookies)
            }
        }

        // Add default user agent if not present
        if (!headers.containsKey("User-Agent")) {
            requestBuilder.addHeader(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
            )
        }

        try {
            val response = client.newCall(requestBuilder.build()).execute()
            
            if (response.code == 429) {
                response.close()
                throw ReCaptchaException("Rate limited", url)
            }

            val responseBodyString = response.body?.string() ?: ""
            
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
        } catch (e: IOException) {
            throw e
        }
    }
}
