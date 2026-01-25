package com.suvojeet.suvmusic.ui.utils

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
            // Let's use a standard resolution, e.g., 72dpi -> 595x842
            // Or higher for better quality on screens -> 300dpi is standard for print but maybe large for canvas ops
            // Android PdfDocument uses points (1/72 inch).
            val pageWidth = 595
            val pageHeight = 842
            
            val pdfDocument = PdfDocument()
            val paint = Paint()
            val titlePaint = TextPaint().apply {
                color = Color.BLACK
                textSize = 24f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val artistPaint = TextPaint().apply {
                color = Color.DKGRAY
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }
            val bodyPaint = TextPaint().apply {
                color = Color.BLACK
                textSize = 14f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                isAntiAlias = true
            }

            // Layout the entire text to determine pages
            val contentWidth = pageWidth - 80 // 40 padding each side
            val fullText = lyricsLines.joinToString("\n")
            
            val staticLayout = StaticLayout.Builder.obtain(
                fullText, 0, fullText.length, bodyPaint, contentWidth
            )
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, 1.2f)
                .build()

            // Header height (Title + Artist + spacing)
            val headerHeight = 100
            val footerHeight = 50
            val contentHeightPerPage = pageHeight - headerHeight - footerHeight
            
            var currentLine = 0
            val totalLines = staticLayout.lineCount
            var pageNumber = 1

            while (currentLine < totalLines) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                val page = pdfDocument.startPage(pageInfo)
                val canvas = page.canvas

                // Draw Header (Title & Artist) - Only on first page? Or all? Let's do all for clarity
                canvas.drawText(songTitle, 40f, 50f, titlePaint)
                canvas.drawText(artistName, 40f, 75f, artistPaint)
                
                // Draw Page Number
                // canvas.drawText("Page $pageNumber", pageWidth - 80f, pageHeight - 30f, artistPaint)
                
                // Draw Footer branding
                paint.color = Color.LTGRAY
                paint.textSize = 10f
                canvas.drawText("Shared via SuvMusic", pageWidth / 2f - 40f, pageHeight - 30f, paint)
                
                // Draw Lyrics Content
                canvas.save()
                canvas.translate(40f, headerHeight.toFloat())
                
                // Calculate how many lines fit this page
                // We need to render the portion of StaticLayout relevant to this page
                // Since StaticLayout doesn't easily support drawing a range of lines without clipping or new layouts,
                // we'll use clipping.
                
                // Determine the height to draw
                // We need to scroll the layout
                val lineTop = staticLayout.getLineTop(currentLine)
                canvas.translate(0f, (-lineTop).toFloat())
                
                // Set clip to only show content area
                // Since we translated up by lineTop, the current line starts at y=0 relative to clip
                // But the clip rect needs to be in current canvas coordinates?
                // Actually, easier approach:
                // Draw the layout, but clip the canvas to the content area BEFORE translating?
                // No, translation affects where it draws.
                
                // Let's create a new StaticLayout for each page or complex clipping?
                // Simpler: Just extract text for this page.
                
                var pageLinesHeight = 0
                val startLine = currentLine
                while (currentLine < totalLines) {
                    val lineHeight = staticLayout.getLineBottom(currentLine) - staticLayout.getLineTop(currentLine)
                    if (pageLinesHeight + lineHeight > contentHeightPerPage) {
                        break
                    }
                    pageLinesHeight += lineHeight
                    currentLine++
                }
                
                // Extract text for this range
                val startChar = staticLayout.getLineStart(startLine)
                val endChar = staticLayout.getLineEnd(currentLine - 1)
                val pageText = fullText.substring(startChar, endChar)
                
                // Restore canvas to remove the previous complicate translation attempt
                canvas.restore() 
                
                // New save for actual drawing
                canvas.save()
                canvas.translate(40f, headerHeight.toFloat())
                
                val pageLayout = StaticLayout.Builder.obtain(
                    pageText, 0, pageText.length, bodyPaint, contentWidth
                ).build()
                
                pageLayout.draw(canvas)
                
                canvas.restore()
                pdfDocument.finishPage(page)
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
