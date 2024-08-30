package io.keeppro.krop

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform