package com.hyperos.blog.data

import io.ktor.client.engine.js.Js
import kotlinx.browser.window

actual fun createDefaultApiClient(): ApiClient {
    val baseUrl = window.location.origin
    return ApiClient(baseUrl, Js.create())
}