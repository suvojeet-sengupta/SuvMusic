package com.suvojeet.suvmusic.ui.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

object LyricsImageGenerator {

    /**
     * Generates a sharable image with selected lyrics and background.
     * Returns the URI of the saved image.
     */
    suspend fun generateAndShareImage(
        context: Context,
        lyricsLines: List<String>,
        songTitle: String,
        artistName: String,
        artworkUrl: String?
    ): android.net.Uri? = withContext(Dispatchers.IO) {
        try {
            // 1. Prepare Content
            val width = 1080
            val height = 1920 // 9:16 aspect ratio (Instagram story style)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            // 2. Draw Background
            drawBackground(context, canvas, width, height, artworkUrl)

            // 3. Draw Lyrics (Centered)
            drawLyrics(canvas, lyricsLines, width, height)

            // 4. Draw Footer (Song Info + Branding)
            drawFooter(canvas, songTitle, artistName, width, height)

            // 5. Save to Cache
            saveToCache(context, bitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun drawBackground(
        context: Context,
        canvas: Canvas,
        width: Int,
        height: Int,
        artworkUrl: String?
    ) {
        val paint = Paint()
        
        // Default gradient if artwork fails or is null
        // Dark gradient
        val gradient = android.graphics.LinearGradient(
            0f, 0f, 0f, height.toFloat(),
            intArrayOf(0xFF1A1A1A.toInt(), 0xFF000000.toInt()),
            null,
            android.graphics.Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        if (artworkUrl != null) {
            try {
                val loader = ImageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(artworkUrl)
                    .allowHardware(false) // Software bitmap needed for Canvas
                    .size(width, height) 
                    .build()

                val result = (loader.execute(request) as? SuccessResult)?.drawable
                if (result != null) {
                    val artBitmap = result.toBitmap(width, height, Bitmap.Config.ARGB_8888)
                    
                    // Draw blurred/dimmed artwork
                    // Simple dimming overlay
                    canvas.drawBitmap(artBitmap, 0f, 0f, null)
                    
                    // Dark overlay for readability
                    canvas.drawColor(0x99000000.toInt()) // ~60% black overlay
                }
            } catch (e: Exception) {
                // Fallback to gradient
            }
        }
    }

    private fun drawLyrics(
        canvas: Canvas,
        lines: List<String>,
        width: Int,
        height: Int
    ) {
        val textPaint = TextPaint().apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 64f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        // Calculate total height to vertically center
        val combinedText = lines.joinToString("\n\n") // Double newline for spacing
        
        // Leave padding
        val contentWidth = (width * 0.8f).toInt()
        
        val staticLayout = StaticLayout.Builder.obtain(
            combinedText,
            0,
            combinedText.length,
            textPaint,
            contentWidth
        )
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setLineSpacing(0f, 1.2f)
            .build()
            
        val textHeight = staticLayout.height
        
        // Center text vertically
        val startY = (height - textHeight) / 2f
        
        canvas.save()
        canvas.translate(width / 2f, startY) // Translate to center X, calculated Y
        staticLayout.draw(canvas)
        canvas.restore()
        
        // Draw quota marks just for style (Optional)
    }

    private fun drawFooter(
        canvas: Canvas,
        title: String,
        artist: String,
        width: Int,
        height: Int
    ) {
        val titlePaint = TextPaint().apply {
            color = 0xFFFFFFFF.toInt()
            textSize = 42f
            typeface = Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val artistPaint = TextPaint().apply {
            color = 0xCCFFFFFF.toInt() // 80% opacity
            textSize = 36f
            typeface = Typeface.DEFAULT
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }
        
        val brandingPaint = TextPaint().apply {
            color = 0x80FFFFFF.toInt() // 50% opacity
            textSize = 30f
            typeface = Typeface.MONOSPACE
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
        }

        val footerY = height - 150f
        
        // Song Title
        canvas.drawText(title, width / 2f, footerY - 60f, titlePaint)
        
        // Artist
        canvas.drawText(artist, width / 2f, footerY, artistPaint)
        
        // Branding
        canvas.drawText("Shared via SuvMusic", width / 2f, height - 50f, brandingPaint)
    }

    private fun saveToCache(context: Context, bitmap: Bitmap): android.net.Uri? {
        val imagesFolder = File(context.cacheDir, "images")
        if (!imagesFolder.exists()) imagesFolder.mkdirs()

        val file = File(imagesFolder, "lyrics_share_${System.currentTimeMillis()}.png")
        
        return try {
            val stream = FileOutputStream(file)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.flush()
            stream.close()
            
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
