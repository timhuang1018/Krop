package io.keeppro

import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.IRect
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo


//1. bytearray
actual fun cropBitmap(bitmap: Bitmap, x: Int, y: Int, width: Int, height: Int): ByteArray {
    val croppedBitmap = cropIntoBitmap(bitmap, x, y, width, height)

    // Convert the cropped bitmap to an Image
    val image = Image.makeFromBitmap(croppedBitmap)

    // Encode the image to PNG and get the ByteArray
    val imageData = image.encodeToData(EncodedImageFormat.PNG)

    return imageData?.bytes ?: ByteArray(0)
}

fun cropIntoBitmap(bitmap: Bitmap, x: Int, y: Int, width: Int, height: Int): Bitmap {
    // Define the rectangle area to crop
    val rect = IRect.makeXYWH(x, y, width, height)

    // Create a new Bitmap for the cropped area
    val croppedBitmap = Bitmap()
    val imageInfo = ImageInfo.makeN32Premul(width, height)
    croppedBitmap.allocPixels(imageInfo)

    // Extract the subset (cropped area) and copy it to the new bitmap
    bitmap.extractSubset(croppedBitmap, rect)

    return croppedBitmap
}