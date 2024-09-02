package io.keeppro.krop

import android.util.Log
import coil3.Image
import coil3.PlatformContext
import coil3.request.ImageRequest
import coil3.request.allowHardware
import java.io.ByteArrayOutputStream

internal actual fun println(message: String) {
    Log.d("Krop", message)
}

typealias Bitmap = android.graphics.Bitmap

internal actual fun cropBitmap(bitmap: Bitmap, x: Int, y: Int, width: Int, height: Int): ByteArray {
    println("cropBitmap, x: $x, y: $y, width: $width, height: $height")
    val cropBitmap = Bitmap.createBitmap(bitmap, x, y, width, height)

    val format = android.graphics.Bitmap.CompressFormat.PNG
    val quality = 100
    val byteArrayOutputStream = ByteArrayOutputStream()
    cropBitmap.compress(format, quality, byteArrayOutputStream)
    return byteArrayOutputStream.toByteArray()
}

internal actual fun getImageRequest(context: PlatformContext, url: String): ImageRequest{
    return ImageRequest.Builder(context)
        .data(url)
        .allowHardware(false)
        .build()
}


