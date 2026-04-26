package com.suvojeet.suvmusic.composeapp.image

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

/**
 * Desktop dominant-color extractor. Loads bytes via Ktor and decodes
 * with javax.imageio (JDK built-in — handles JPEG/PNG/BMP/WebP-via-
 * plugin). Pixel sampling uses [BufferedImage.getRGB].
 *
 * Skipped Skia here because ImageIO + BufferedImage is JDK-native, no
 * extra deps, and the per-pixel ARGB extraction is identical math to
 * the Android side.
 */
internal actual class PlatformDominantColorExtractor actual constructor() {

    private val httpClient = HttpClient(CIO)

    actual suspend fun extract(imageUrl: String, isDarkTheme: Boolean): DominantColors? =
        withContext(Dispatchers.IO) {
            try {
                val bytes = httpClient.get(imageUrl).bodyAsBytes()
                val image = ImageIO.read(ByteArrayInputStream(bytes)) ?: return@withContext null
                val (r, g, b) = sampleAverageRgb(image)
                buildDominantColors(r, g, b, isDarkTheme)
            } catch (t: Throwable) {
                null
            }
        }

    private fun sampleAverageRgb(image: BufferedImage): Triple<Int, Int, Int> {
        val width = image.width
        val height = image.height
        val step = maxOf(1, minOf(width, height) / 10)
        var totalR = 0L
        var totalG = 0L
        var totalB = 0L
        var count = 0L
        for (x in 0 until width step step) {
            for (y in 0 until height step step) {
                val argb = image.getRGB(x, y)
                totalR += (argb shr 16) and 0xFF
                totalG += (argb shr 8) and 0xFF
                totalB += argb and 0xFF
                count++
            }
        }
        if (count == 0L) return Triple(0, 0, 0)
        return Triple((totalR / count).toInt(), (totalG / count).toInt(), (totalB / count).toInt())
    }
}
