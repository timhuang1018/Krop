package io.keeppro

import java.io.ByteArrayOutputStream


typealias Bitmap = android.graphics.Bitmap

actual fun cropBitmap(bitmap: Bitmap, x: Int, y: Int, width: Int, height: Int): ByteArray {
    println("cropBitmap, x: $x, y: $y, width: $width, height: $height")
    val cropBitmap = Bitmap.createBitmap(bitmap, x, y, width, height)

    val format = android.graphics.Bitmap.CompressFormat.PNG
    val quality = 100
    val byteArrayOutputStream = ByteArrayOutputStream()
    cropBitmap.compress(format, quality, byteArrayOutputStream)
    return byteArrayOutputStream.toByteArray()
}


