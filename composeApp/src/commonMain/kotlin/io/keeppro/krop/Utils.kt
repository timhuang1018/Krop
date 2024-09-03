package io.keeppro.krop

import coil3.PlatformContext
import coil3.request.ImageRequest

internal expect fun println(message: String)

internal expect fun getImageRequest(context: PlatformContext, url: String): ImageRequest
