package com.suvojeet.suvmusic.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import androidx.media3.common.util.BitmapLoader
import androidx.media3.common.util.UnstableApi
import coil3.imageLoader
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.asDrawable
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@UnstableApi
class CoilBitmapLoader(private val context: Context) : BitmapLoader {

    private val imageLoader = context.imageLoader
    private val scope = CoroutineScope(Dispatchers.IO)

    // Define standard size for Bitmap to avoid Palette crash and system scaling issues.
    // 320px was the standard maximum for MediaSession metadata on many Android versions.
    // Modern devices can handle higher resolutions. We use 720px for high quality,
    // which is safely under the 1MB Binder limit when using RGB_565 (720*720*2 = ~1MB).
    // Improvement (3): Further reduce to 256px for Android Auto to save memory on head units.
    private val DEFAULT_BITMAP_SIZE = if (isAndroidAuto()) 256 else 720

    /**
     * Detect if the current session is Android Auto.
     */
    private fun isAndroidAuto(): Boolean {
        return try {
            val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as android.app.UiModeManager
            uiModeManager.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_CAR
        } catch (e: Exception) {
            false
        }
    }

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()

        scope.launch {
            try {
                // Try loading the original URI
                val bitmap = loadBitmapInternal(uri)
                if (bitmap != null && !bitmap.isRecycled) {
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
                            if (fallbackBitmap != null && !fallbackBitmap.isRecycled) {
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
                future.set(createPlaceholderBitmap())
            } catch (e: Exception) {
                future.setException(e)
            }
        }

        return future
    }

    /**
     * Creates a safe Bitmap from a Drawable.
     * Uses RGB_565 for memory efficiency (no alpha needed for album art).
     */
    private fun createFallbackBitmap(drawable: Drawable): Bitmap {
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else DEFAULT_BITMAP_SIZE
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else DEFAULT_BITMAP_SIZE

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
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
                    // Wrap in BitmapDrawable and use our safe conversion to ensure sizing and ownership
                    val drawable = BitmapDrawable(context.resources, bitmap)
                    val safeBitmap = createSafeBitmapFromDrawable(drawable)
                    
                    // Recycle the temporary raw bitmap if it's different from safeBitmap
                    if (safeBitmap != null && safeBitmap != bitmap && !bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                    
                    if (safeBitmap != null) {
                        future.set(safeBitmap)
                    } else {
                        future.setException(Exception("Failed to create safe bitmap from decoded data"))
                    }
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
                .size(DEFAULT_BITMAP_SIZE) // Optimization: Ask Coil to downscale during decode
                .allowHardware(false) // Required for drawing to Canvas
                .build()

            val result = imageLoader.execute(request)
            if (result !is SuccessResult) return null
            val drawable = result.image.asDrawable(context.resources)

            // Always create a fresh bitmap by drawing to canvas.
            // This ensures we own the bitmap entirely and Coil won't recycle it from under us.
            createSafeBitmapFromDrawable(drawable)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Creates a safe, owned bitmap from any drawable by drawing it to a fresh canvas.
     * This avoids race conditions with Coil's bitmap recycling and ensures the size
     * is within safe limits for MediaSession metadata.
     * Uses RGB_565 to balance quality and Binder transaction limits.
     */
    private fun createSafeBitmapFromDrawable(drawable: Drawable): Bitmap? {
        return try {
            var width: Int
            var height: Int

            if (drawable is BitmapDrawable && drawable.bitmap != null && !drawable.bitmap.isRecycled) {
                width = drawable.bitmap.width
                height = drawable.bitmap.height
            } else {
                width = drawable.intrinsicWidth.takeIf { it > 0 } ?: DEFAULT_BITMAP_SIZE
                height = drawable.intrinsicHeight.takeIf { it > 0 } ?: DEFAULT_BITMAP_SIZE
            }

            // Limit maximum dimension to DEFAULT_BITMAP_SIZE (720px)
            if (width > DEFAULT_BITMAP_SIZE || height > DEFAULT_BITMAP_SIZE) {
                val ratio = width.toFloat() / height.toFloat()
                if (width > height) {
                    width = DEFAULT_BITMAP_SIZE
                    height = (DEFAULT_BITMAP_SIZE / ratio).toInt()
                } else {
                    height = DEFAULT_BITMAP_SIZE
                    width = (DEFAULT_BITMAP_SIZE * ratio).toInt()
                }
            }
            
            // Ensure positive dimensions
            width = width.coerceAtLeast(1)
            height = height.coerceAtLeast(1)

            // Create a completely fresh bitmap that we own
            // Use RGB_565 for album art to save 50% memory and stay under Binder 1MB limit
            val freshBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            val canvas = Canvas(freshBitmap)

            try {
                drawable.setBounds(0, 0, width, height)
                drawable.draw(canvas)
            } catch (e: Exception) {
                // Drawing failed (e.g., source bitmap recycled during draw), return placeholder
                if (!freshBitmap.isRecycled) freshBitmap.recycle()
                return createPlaceholderBitmap()
            }

            // Final safety check: if for some reason the fresh bitmap is invalid, return placeholder
            if (freshBitmap.isRecycled) {
                return createPlaceholderBitmap()
            }

            freshBitmap
        } catch (e: Exception) {
            createPlaceholderBitmap()
        }
    }

    /**
     * Creates a simple placeholder bitmap when all else fails.
     */
    private fun createPlaceholderBitmap(): Bitmap {
        return try {
            val bitmap = Bitmap.createBitmap(DEFAULT_BITMAP_SIZE, DEFAULT_BITMAP_SIZE, Bitmap.Config.RGB_565)
            val canvas = Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.DKGRAY)
            bitmap
        } catch (e: Exception) {
            // Absolute fallback - should not happen unless OOM
            Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565)
        }
    }

    private fun getFallbackUris(uri: Uri): List<Uri> {
        val uriString = uri.toString()
        val fallbacks = mutableListOf<Uri>()
        
        // Check for Google/YouTube thumbnail pattern
        if (uriString.contains("googleusercontent.com") || uriString.contains("ggpht.com") || 
            uriString.contains("ytimg.com") || uriString.contains("youtube.com")) {
            
            // If it's maxresdefault, try sddefault then hqdefault
            if (uriString.contains("maxresdefault")) {
                fallbacks.add(Uri.parse(uriString.replace("maxresdefault", "sddefault")))
                fallbacks.add(Uri.parse(uriString.replace("maxresdefault", "hqdefault")))
            } 
            // If it's sddefault, try hqdefault
            else if (uriString.contains("sddefault")) {
                fallbacks.add(Uri.parse(uriString.replace("sddefault", "hqdefault")))
            }
            
            // Handle googleusercontent/ggpht image resizing fallbacks
            if (uriString.contains("=w") || uriString.contains("=s")) {
                // Try smaller standard sizes
                if (uriString.contains("=w")) {
                    fallbacks.add(Uri.parse(uriString.replace(Regex("=w\\d+-h\\d+"), "=w544-h544")))
                    fallbacks.add(Uri.parse(uriString.replace(Regex("=w\\d+-h\\d+"), "=w256-h256")))
                }
                if (uriString.contains("=s")) {
                    fallbacks.add(Uri.parse(uriString.replace(Regex("=s\\d+"), "=s544")))
                    fallbacks.add(Uri.parse(uriString.replace(Regex("=s\\d+"), "=s256")))
                }
            }
        }
        return fallbacks
    }
}