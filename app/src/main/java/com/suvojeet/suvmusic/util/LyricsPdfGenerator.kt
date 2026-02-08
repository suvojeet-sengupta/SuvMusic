package com.suvojeet.suvmusic.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object LyricsPdfGenerator {

    /**
     * Generates a sharable PDF with all lyrics.
     * Returns the URI of the saved PDF.
     */
    suspend fun generateAndSharePdf(
        context: Context,
        lyricsLines: List<String>,
        songTitle: String,
        artistName: String
    ): android.net.Uri? = withContext(Dispatchers.IO) {
        try {
            // A4 size in points (approx 595 x 842)
            val pageWidth = 595
            val pageHeight = 842
            
            // Branding Colors (Matching SuvMusic Theme)
            val primaryColor = Color.parseColor("#5A189A") // Purple40 - Deep Electric Purple
            val secondaryColor = Color.parseColor("#D43A9C") // Magenta60 - Hot Magenta
            val accentColor = Color.parseColor("#00BDD6") // Cyan70 - Neon Cyan
            val pageBackgroundColor = Color.rgb(252, 252, 255) // Very light blue-ish white
            val textColor = Color.rgb(33, 33, 33) // Soft Black
            
            val pdfDocument = PdfDocument()
            
            // Paints
            val backgroundPaint = Paint().apply { color = pageBackgroundColor }
            val headerPaint = Paint().apply { color = primaryColor }
            
            val titlePaint = TextPaint().apply {
                color = Color.WHITE
                textSize = 28f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
                textAlign = Paint.Align.LEFT
            }
            
            val artistPaint = TextPaint().apply {
                color = Color.parseColor("#E8BFFF") // Purple90 - Light Lavender
                textSize = 18f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                isAntiAlias = true
                textAlign = Paint.Align.LEFT
            }
            
            val brandingPaint = TextPaint().apply {
                color = accentColor
                textSize = 14f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                isAntiAlias = true
                textAlign = Paint.Align.RIGHT
            }
            
            val bodyPaint = TextPaint().apply {
                color = textColor
                textSize = 16f
                typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                isAntiAlias = true
            }

            val footerPaint = TextPaint().apply {
                color = Color.GRAY
                textSize = 10f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.ITALIC)
                isAntiAlias = true
                textAlign = Paint.Align.CENTER
            }

            // Layout setup
            val contentWidth = pageWidth - 100 // 50 padding each side
            val fullText = lyricsLines.joinToString("\n\n") 
            
            val staticLayout = StaticLayout.Builder.obtain(
                fullText, 0, fullText.length, bodyPaint, contentWidth
            )
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(10f, 1.3f)
                .build()

            val headerHeight = 140
            val footerHeight = 60
            val contentHeightPerPage = pageHeight - headerHeight - footerHeight
            
            val totalLines = staticLayout.lineCount
            var currentLine = 0
            var pageNumber = 1

            while (currentLine < totalLines) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // 1. Background
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), backgroundPaint)
                
                // 2. Header Background
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), 120f, headerPaint)
                
                // 3. Header Content
                val safeTitle = if (songTitle.length > 30) songTitle.take(27) + "..." else songTitle
                canvas.drawText(safeTitle, 50f, 60f, titlePaint)
                
                // Artist
                canvas.drawText(artistName, 50f, 90f, artistPaint)
                
                // App Branding (Top Right)
                canvas.drawText("SuvMusic", pageWidth - 50f, 60f, brandingPaint)
                
                // Decorative accent line
                val linePaint = Paint().apply { 
                    color = secondaryColor
                    strokeWidth = 3f 
                }
                canvas.drawLine(50f, 105f, 180f, 105f, linePaint) 

                // 4. Content
                canvas.save()
                canvas.translate(50f, headerHeight.toFloat())
                
                // Determine lines for this page
                var pageLinesHeight = 0
                val startLine = currentLine
                var endLine = currentLine
                
                while (endLine < totalLines) {
                    val lineHeight = staticLayout.getLineBottom(endLine) - staticLayout.getLineTop(endLine)
                    if (pageLinesHeight + lineHeight > contentHeightPerPage) {
                        break
                    }
                    pageLinesHeight += lineHeight
                    endLine++
                }
                
                // If a single line is too huge (unlikely) or we made no progress, force at least one line to avoid infinite loop
                if (endLine == startLine && endLine < totalLines) {
                    endLine++
                }

                // Draw the specific range of lines
                // We need to translate the canvas upwards so that the 'startLine' is at y=0
                val scrollY = staticLayout.getLineTop(startLine)
                canvas.translate(0f, -scrollY.toFloat())
                
                // Clip the canvas to show only the height we calculated
                // The clip rectangle must be in the current coordinate system (which is shifted by -scrollY)
                // Actually, clipRect is applied relative to current matrix.
                // We want to clip from y=scrollY to y=scrollY + pageLinesHeight
                canvas.clipRect(0, scrollY, contentWidth, scrollY + pageLinesHeight)
                
                staticLayout.draw(canvas)
                
                canvas.restore()

                // 5. Footer
                canvas.drawLine(50f, pageHeight - 50f, pageWidth - 50f, pageHeight - 50f, Paint().apply { color = Color.LTGRAY })
                canvas.drawText("Page $pageNumber", pageWidth / 2f, pageHeight - 30f, footerPaint)
                canvas.drawText("Exported from SuvMusic", pageWidth / 2f, pageHeight - 15f, footerPaint.apply { textSize = 8f })

                pdfDocument.finishPage(page)
                
                currentLine = endLine
                pageNumber++
            }
            
            val uri = savePdfToDocuments(context, pdfDocument, songTitle)
            pdfDocument.close()
            uri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun savePdfToDocuments(context: Context, document: PdfDocument, title: String): android.net.Uri? {
        val safeTitle = title.replace(Regex("[^a-zA-Z0-9.-]"), "_")
        val fileName = "Lyrics_${safeTitle}_${System.currentTimeMillis()}.pdf"
        
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOCUMENTS + "/SuvMusic")
        }

        val resolver = context.contentResolver
        val uri = resolver.insert(android.provider.MediaStore.Files.getContentUri("external"), contentValues)

        return if (uri != null) {
            try {
                resolver.openOutputStream(uri)?.use { stream ->
                    document.writeTo(stream)
                }
                uri
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        } else {
            null
        }
    }
}
