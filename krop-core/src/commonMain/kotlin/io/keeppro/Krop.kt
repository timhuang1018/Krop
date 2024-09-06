package io.keeppro

import coil3.Bitmap
import coil3.Image
import coil3.toBitmap

class Krop {

    private var image: Image? = null

    fun prepareImage(image: Image) {
        this.image = image
    }

    fun crop(startX: Int, startY: Int, width: Int, height: Int): ByteArray {
        val current = image ?: throw IllegalStateException("Image is not loaded")
        if (current.width == -1 || current.height == -1) throw IllegalStateException("Image width or height is not available")
        return cropBitmap(current.toBitmap(), startX, startY, width, height)
    }
}

expect fun cropBitmap(bitmap: Bitmap, x: Int, y: Int, width: Int, height: Int): ByteArray

