package com.hyperos.blog.data

import io.ktor.client.engine.js.Js
import kotlinx.browser.window

actual fun createDefaultApiClient(): ApiClient {
    val origin = window.location.origin
    val apiOrigin = "https://api.astroptis.dpdns.org"
    return ApiClient(
        baseUrls = listOf(apiOrigin, origin),
        engine = Js.create(),
    )
}