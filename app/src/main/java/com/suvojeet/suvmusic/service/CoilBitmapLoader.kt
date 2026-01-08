package com.suvojeet.suvmusic.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
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

                if (drawable is BitmapDrawable) {
                    future.set(drawable.bitmap)
                } else {
                    // Try to convert to bitmap if it's not a BitmapDrawable (e.g. ColorDrawable)
                    val bitmap = (drawable as? BitmapDrawable)?.bitmap 
                        ?: Bitmap.createBitmap(
                            drawable?.intrinsicWidth?.takeIf { it > 0 } ?: 1, 
                            drawable?.intrinsicHeight?.takeIf { it > 0 } ?: 1, 
                            Bitmap.Config.ARGB_8888
                        ).apply {
                            drawable?.setBounds(0, 0, width, height)
                            drawable?.draw(android.graphics.Canvas(this))
                        }
                    future.set(bitmap)
                }
            } catch (e: Exception) {
                future.setException(e)
            }
        }

        return future
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
