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
                val request = ImageRequest.Builder(context)
                    .data(uri)
                    .allowHardware(false) // Hardware bitmaps are not accessible by other processes/notifications
                    .build()

                val result = imageLoader.execute(request)
                val drawable = result.drawable

                if (drawable == null) {
                    future.setException(Exception("Drawable is null"))
                    return@launch
                }

                if (drawable is BitmapDrawable) {
                    // If image is already a valid Bitmap, return it
                    if (drawable.bitmap.width > 0 && drawable.bitmap.height > 0) {
                        future.set(drawable.bitmap)
                    } else {
                        // If BitmapDrawable is empty or broken
                        future.set(createFallbackBitmap(drawable))
                    }
                } else {
                    // Convert Drawable (Vector, Color, etc.) to Bitmap
                    future.set(createFallbackBitmap(drawable))
                }
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
}