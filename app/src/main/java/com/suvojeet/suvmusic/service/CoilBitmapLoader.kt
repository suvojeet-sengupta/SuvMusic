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

/**
 * A custom BitmapLoader for Media3 that uses Coil for high-performance image loading.
 * Features:
 * 1. Proactive Quality Upgrading: Automatically tries to load High-Res (MaxRes) before falling back.
 * 2. Memory Efficiency: Uses RGB_565 for album art to stay within Binder transaction limits.
 * 3. Thread Safety: Creates owned Bitmaps to prevent "recycled source" crashes in System UI.
 */
@UnstableApi
class CoilBitmapLoader(private val context: Context) : BitmapLoader {

    private val imageLoader = context.imageLoader
    private val scope = CoroutineScope(Dispatchers.IO)

    // Standard high-quality size for modern notification shades.
    // 720px is crisp on 1440p screens and safe for IPC when using RGB_565.
    private val DEFAULT_BITMAP_SIZE = if (isAndroidAuto()) 256 else 720

    // --- Media3 API Implementation ---

    override fun loadBitmap(uri: Uri): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()

        scope.launch {
            // Priority Strategy: Try High -> Medium -> Low
            val prioritizedUris = getPrioritizedUris(uri)
            
            for (targetUri in prioritizedUris) {
                try {
                    val bitmap = loadBitmapInternal(targetUri)
                    if (bitmap != null && !bitmap.isRecycled) {
                        future.set(bitmap)
                        return@launch
                    }
                } catch (e: Exception) {
                    // Failures here are common (e.g. 404 for maxresdefault), so we continue to next
                }
            }
            
            // Absolute fallback - return a default placeholder
            try {
                future.set(createPlaceholderBitmap())
            } catch (e: Exception) {
                future.setException(e)
            }
        }

        return future
    }

    override fun decodeBitmap(data: ByteArray): ListenableFuture<Bitmap> {
        val future = SettableFuture.create<Bitmap>()
        scope.launch {
            try {
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(data, 0, data.size)
                if (bitmap != null) {
                    val drawable = BitmapDrawable(context.resources, bitmap)
                    val safeBitmap = createSafeBitmapFromDrawable(drawable)
                    
                    if (safeBitmap != null && safeBitmap != bitmap && !bitmap.isRecycled) {
                        bitmap.recycle()
                    }
                    
                    if (safeBitmap != null) future.set(safeBitmap)
                    else future.setException(Exception("Safe conversion failed"))
                } else {
                    future.setException(Exception("Decode failed"))
                }
            } catch (e: Exception) {
                future.setException(e)
            }
        }
        return future
    }

    override fun supportsMimeType(mimeType: String): Boolean = true

    // --- Private Business Logic ---

    /**
     * Identifies image providers and provides a descending list of resolution variants.
     */
    private fun getPrioritizedUris(originalUri: Uri): List<Uri> {
        val uriString = originalUri.toString()
        val uris = mutableListOf<Uri>()

        when {
            // YouTube: Upgrade to maxresdefault proactively
            uriString.contains("ytimg.com") || uriString.contains("youtube.com") -> {
                val base = uriString
                    .replace(Regex("(mq|hq|sd|maxres|)default"), "maxresdefault")
                
                uris.add(Uri.parse(base))                                   // Max (1280x720)
                uris.add(Uri.parse(base.replace("maxresdefault", "sddefault"))) // Mid (640x480)
                uris.add(Uri.parse(base.replace("maxresdefault", "hqdefault"))) // Low-Mid (480x360)
                uris.add(Uri.parse(base.replace("maxresdefault", "mqdefault"))) // Low (320x180)
            }

            // Google User Content (YT Music): Proactively try high-res sizing
            uriString.contains("googleusercontent.com") || uriString.contains("ggpht.com") -> {
                uris.add(upgradeGoogleUri(uriString, 720)) // High
                uris.add(upgradeGoogleUri(uriString, 544)) // Standard
                uris.add(upgradeGoogleUri(uriString, 400)) // Low
            }

            else -> uris.add(originalUri)
        }

        if (!uris.contains(originalUri)) uris.add(originalUri)
        return uris.distinct()
    }

    private fun upgradeGoogleUri(uriString: String, size: Int): Uri {
        val upgraded = when {
            uriString.contains("=w") -> uriString.replace(Regex("=w\\d+-h\\d+"), "=w$size-h$size")
            uriString.contains("=s") -> uriString.replace(Regex("=s\\d+"), "=s$size")
            else -> uriString
        }
        return Uri.parse(upgraded)
    }

    private suspend fun loadBitmapInternal(uri: Uri): Bitmap? {
        return try {
            val request = ImageRequest.Builder(context)
                .data(uri)
                .size(DEFAULT_BITMAP_SIZE)
                .allowHardware(false)
                .build()

            val result = imageLoader.execute(request)
            if (result !is SuccessResult) return null
            val drawable = result.image.asDrawable(context.resources)

            createSafeBitmapFromDrawable(drawable)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Converts any Drawable into a safe, software-backed Bitmap that we own.
     * Prevents "recycled source" crashes and manages IPC payload size.
     */
    private fun createSafeBitmapFromDrawable(drawable: Drawable): Bitmap? {
        return try {
            var width = drawable.intrinsicWidth.takeIf { it > 0 } ?: DEFAULT_BITMAP_SIZE
            var height = drawable.intrinsicHeight.takeIf { it > 0 } ?: DEFAULT_BITMAP_SIZE

            // Enforce size limits while maintaining aspect ratio
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

            val bitmap = Bitmap.createBitmap(width.coerceAtLeast(1), height.coerceAtLeast(1), Bitmap.Config.RGB_565)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun createPlaceholderBitmap(): Bitmap {
        val bitmap = Bitmap.createBitmap(DEFAULT_BITMAP_SIZE, DEFAULT_BITMAP_SIZE, Bitmap.Config.RGB_565)
        val canvas = Canvas(bitmap)
        canvas.drawColor(android.graphics.Color.DKGRAY)
        return bitmap
    }

    private fun isAndroidAuto(): Boolean {
        return try {
            val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as android.app.UiModeManager
            uiModeManager.currentModeType == android.content.res.Configuration.UI_MODE_TYPE_CAR
        } catch (e: Exception) {
            false
        }
    }
}
