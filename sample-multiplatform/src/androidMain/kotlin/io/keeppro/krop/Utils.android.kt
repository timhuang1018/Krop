package io.keeppro.krop

import android.util.Log
import coil3.PlatformContext
import coil3.request.ImageRequest
import coil3.request.allowHardware

internal actual fun println(message: String) {
    Log.d("Krop", message)
}

internal actual fun getImageRequest(context: PlatformContext, url: String): ImageRequest {
    return ImageRequest.Builder(context)
        .data(url)
        .allowHardware(false)
        .build()
}