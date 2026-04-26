package com.suvojeet.suvmusic.composeapp.image

import android.graphics.Bitmap
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Android dominant-color extractor. Loads bytes via Ktor (already in
 * commonMain deps), decodes with [android.graphics.BitmapFactory], and
 * samples pixels via [android.graphics.Bitmap.getPixel].
 */
internal actual class PlatformDominantColorExtractor actual constructor() {

    private val httpClient = HttpClient(CIO)

    actual suspend fun extract(imageUrl: String, isDarkTheme: Boolean): DominantColors? =
        withContext(Dispatchers.IO) {
            try {
                val bytes = httpClient.get(imageUrl).bodyAsBytes()
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    ?: return@withContext null
                val (r, g, b) = sampleAverageRgb(bitmap)
                buildDominantColors(r, g, b, isDarkTheme)
            } catch (t: Throwable) {
                null
            }
        }

    private fun sampleAverageRgb(bitmap: Bitmap): Triple<Int, Int, Int> {
        val width = bitmap.width
        val height = bitmap.height
        val step = maxOf(1, minOf(width, height) / 10)
        var totalR = 0L
        var totalG = 0L
        var totalB = 0L
        var count = 0L
        for (x in 0 until width step step) {
            for (y in 0 until height step step) {
                val pixel = bitmap.getPixel(x, y)
                totalR += android.graphics.Color.red(pixel)
                totalG += android.graphics.Color.green(pixel)
                totalB += android.graphics.Color.blue(pixel)
                count++
            }
        }
        if (count == 0L) return Triple(0, 0, 0)
        return Triple((totalR / count).toInt(), (totalG / count).toInt(), (totalB / count).toInt())
    }
}
