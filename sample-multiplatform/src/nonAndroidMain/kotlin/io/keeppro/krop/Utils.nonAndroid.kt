package io.keeppro.krop

import coil3.PlatformContext
import coil3.request.ImageRequest

internal actual fun println(message: String) {
    kotlin.io.println(message)
}

internal actual fun getImageRequest(context: PlatformContext, url: String): ImageRequest {
    return ImageRequest.Builder(context)
        .data(url)
        .build()
}