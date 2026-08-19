package com.suvojeet.suvmusic.data.repository.remote

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Applies [HqAudioUrlProvider]'s routing decision to every HQ Audio request and feeds the
 * outcome back into its circuit breaker.
 *
 * Rewriting the host here — rather than picking a base URL when Retrofit is built — is
 * what makes the diversion possible at all: the DI graph is created once, so a base URL
 * chosen there would outlive any outage it was meant to route around.
 *
 * Requests to any other host pass through untouched, so this is safe on a shared client.
 */
class HqAudioRouteInterceptor : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (!HqAudioUrlProvider.isManagedHost(request.url.host)) {
            return chain.proceed(request)
        }

        val targetHost = HqAudioUrlProvider.activeHost()
        val routed = if (request.url.host.equals(targetHost, ignoreCase = true)) {
            request
        } else {
            request.newBuilder()
                .url(request.url.newBuilder().host(targetHost).build())
                .build()
        }

        return try {
            val response = chain.proceed(routed)
            if (isHealthy(response.code)) {
                HqAudioUrlProvider.recordSuccess(targetHost)
            } else {
                HqAudioUrlProvider.recordFailure(targetHost)
                com.suvojeet.suvmusic.util.HqDiagnostics.log("route", "HTTP ${response.code} from $targetHost ${routed.url.encodedPath}")
            }
            response
        } catch (e: IOException) {
            HqAudioUrlProvider.recordFailure(targetHost)
            com.suvojeet.suvmusic.util.HqDiagnostics.log("route", "transport error to $targetHost: ${e.javaClass.simpleName}: ${e.message}")
            throw e
        }
    }

    /**
     * Only server-side trouble counts against the host. A 404 means the edge answered
     * correctly about a thing that does not exist — diverting on that would be wrong.
     */
    private fun isHealthy(code: Int): Boolean = code < 500 && code != 429
}
