package com.hyperos.blog.data

import io.ktor.client.engine.js.Js
import kotlinx.browser.window

actual fun createDefaultApiClient(): ApiClient {
    val origin = window.location.origin
    return ApiClient(
        baseUrls = listOf(origin),
        engine = Js.create(),
    )
}