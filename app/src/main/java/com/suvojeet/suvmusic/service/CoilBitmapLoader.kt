package com.suvojeet.suvmusic.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import coil.ImageLoader
import coil.request.ImageRequest
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@UnstableApi
class CoilBitmapLoader(private val context: Context) : BitmapLoader {

    private val imageLoader = ImageLoader(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    // Define minimum size for Bitmap to avoid Palette crash
    // 512px is sufficient for quality notification and color analysis
    private val DEFAULT_BITMAP_SIZE = 512

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()

        scope.launch {
            try {
                // Try loading the original URI
                val bitmap = loadBitmapInternal(uri)
                if (bitmap != null) {
                    future.set(bitmap)
                    return@launch
                }
            } catch (e: Exception) {
                // Initial load failed, try fallback
            }

            // Fallback logic
            try {
                val fallbackUris = getFallbackUris(uri)
                for (fallbackUri in fallbackUris) {
                    if (fallbackUri != uri) {
                        try {
                            val fallbackBitmap = loadBitmapInternal(fallbackUri)
                            if (fallbackBitmap != null) {
                                future.set(fallbackBitmap)
                                return@launch
                            }
                        } catch (e: Exception) {
                            // Try next fallback
                        }
                    }
                }
            } catch (e: Exception) {
                // Fallback failed
            }
            
            // If all else fails, return a default placeholder
            try {
                val placeholder = Bitmap.createBitmap(DEFAULT_BITMAP_SIZE, DEFAULT_BITMAP_SIZE, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(placeholder)
                canvas.drawColor(android.graphics.Color.DKGRAY) 
                future.set(placeholder)
            } catch (e: Exception) {
                future.setException(e)
            }
        }

        return future
    }

    /**
     * Creates a safe Bitmap from a Drawable.
     * If the Drawable has no dimensions (e.g., ColorDrawable), a 512x512 canvas is created.
     * This prevents the "Region must intersect" error in SystemUI.
     */
    private fun createFallbackBitmap(drawable: Drawable): Bitmap {
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else DEFAULT_BITMAP_SIZE
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else DEFAULT_BITMAP_SIZE

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }

    override fun supportsMimeType(mimeType: String): Boolean {
        return true
    }

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        scope.launch {
            try {
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(data, 0, data.size)
                if (bitmap != null) {
                    future.set(bitmap)
                } else {
                    future.setException(Exception("Failed to decode bitmap from byte array"))
                }
            } catch (e: Exception) {
                future.setException(e)
            }
        }
        return future
    }


    private suspend fun loadBitmapInternal(uri: Uri): Bitmap? {
        return try {
            val request = ImageRequest.Builder(context)
                .data(uri)
                .allowHardware(false)
                .build()

            val result = imageLoader.execute(request)
            val drawable = result.drawable ?: return null

            if (drawable is BitmapDrawable && drawable.bitmap != null && !drawable.bitmap.isRecycled && drawable.bitmap.width > 0 && drawable.bitmap.height > 0) {
                // IMPORTANT: Create a copy because Coil manages the original bitmap's lifecycle.
                // MediaSession may use this bitmap after Coil recycles the original, causing crash.
                drawable.bitmap.copy(drawable.bitmap.config ?: Bitmap.Config.ARGB_8888, false)
            } else {
                createFallbackBitmap(drawable)
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun getFallbackUris(uri: Uri): List<Uri> {
        val uriString = uri.toString()
        val fallbacks = mutableListOf<Uri>()
        
        // Check for Google/YouTube thumbnail pattern
        if (uriString.contains("googleusercontent.com") || uriString.contains("ggpht.com") || uriString.contains("ytimg.com")) {
            // If it's maxresdefault, try sddefault then hqdefault
            if (uriString.contains("maxresdefault")) {
                fallbacks.add(Uri.parse(uriString.replace("maxresdefault", "sddefault")))
                fallbacks.add(Uri.parse(uriString.replace("maxresdefault", "hqdefault")))
            } 
            // If it's sddefault, try hqdefault
            else if (uriString.contains("sddefault")) {
                fallbacks.add(Uri.parse(uriString.replace("sddefault", "hqdefault")))
            }
            
            // Retry with resizing if it's a googleusercontent/ggpht image
            if (uriString.contains("=w")) {
                // Try a standard size
                fallbacks.add(Uri.parse(uriString.replace(Regex("=w\\d+-h\\d+"), "=w544-h544")))
            }
        }
        return fallbacks
    }
}