package com.suvojeet.suvmusic.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

object SuvCodeGenerator {

    /**
     * Generates a QR Code Bitmap for the given content.
     */
    fun generateCode(content: String, width: Int = 512, height: Int = 512): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height)
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bmp.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
            bmp
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Helper to create the standard format for SuvMusic codes.
     */
    fun createPlaylistUrl(id: String): String = "suvmusic://playlist/$id"
    fun createAlbumUrl(id: String): String = "suvmusic://album/$id"
}
