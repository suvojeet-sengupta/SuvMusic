package com.suvojeet.suvmusic.composeapp.image

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade

/**
 * Builds a Coil 3 [ImageLoader] for both targets — Android + Desktop —
 * using the Ktor network engine. Same factory call from commonMain so
 * neither target needs platform-specific Coil glue.
 *
 * Crossfade enabled by default for nicer cover-art transitions.
 */
fun buildAppImageLoader(context: PlatformContext): ImageLoader =
    ImageLoader.Builder(context)
        .components { add(KtorNetworkFetcherFactory()) }
        .crossfade(true)
        .build()
