package com.example.safeko.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.util.UUID

data class CoAdminInvite(
    val qrBitmap: Bitmap,
    val token: String,
    val deepLink: String
)

object QRCodeUtils {
    fun generateQRCode(content: String, width: Int = 512, height: Int = 512): Bitmap? {
        return try {
            val bitMatrix: BitMatrix = MultiFormatWriter().encode(
                content,
                BarcodeFormat.QR_CODE,
                width,
                height
            )
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
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

    fun generateCoAdminInviteQR(lgcId: String, width: Int = 512, height: Int = 512): CoAdminInvite? {
        return try {
            val token = UUID.randomUUID().toString()
            // URL-encode parameters to handle special characters properly
            val encodedToken = java.net.URLEncoder.encode(token, "UTF-8")
            val encodedLgcId = java.net.URLEncoder.encode(lgcId, "UTF-8")
            val deepLink = "safeko://invite?token=$encodedToken&lgcId=$encodedLgcId"
            
            val qrBitmap = generateQRCode(deepLink, width, height)
            
            if (qrBitmap != null) {
                CoAdminInvite(
                    qrBitmap = qrBitmap,
                    token = token,
                    deepLink = deepLink
                )
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
