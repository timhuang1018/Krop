package io.keeppro

import android.os.Build
import java.io.ByteArrayOutputStream


typealias Bitmap = android.graphics.Bitmap

actual fun cropBitmap(bitmap: Bitmap, x: Int, y: Int, width: Int, height: Int): ByteArray {
    val safeBitmap = if (Build.VERSION.SDK_INT >= 26 && bitmap.config == android.graphics.Bitmap.Config.HARDWARE) {
        bitmap.copy(android.graphics.Bitmap.Config.ARGB_8888, false)
    } else {
        bitmap
    }
    val cropBitmap = Bitmap.createBitmap(safeBitmap, x, y, width, height)

    val format = android.graphics.Bitmap.CompressFormat.PNG
    val quality = 100
    val byteArrayOutputStream = ByteArrayOutputStream()
    cropBitmap.compress(format, quality, byteArrayOutputStream)
    return byteArrayOutputStream.toByteArray()
}


