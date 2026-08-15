package com.hyperos.blog.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import io.ktor.client.HttpClient
import io.ktor.client.engine.js.Js
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsBytes
import kotlinx.browser.window
import org.jetbrains.skia.Image

@Composable
actual fun RemoteImage(
    url: String,
    contentDescription: String?,
    modifier: Modifier,
) {
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    var failed by remember(url) { mutableStateOf(false) }

    LaunchedEffect(url) {
        bitmap = null
        failed = false
        if (url.isBlank()) {
            failed = true
            return@LaunchedEffect
        }
        try {
            val proxied = proxyUrl(url)
            val client = HttpClient(Js.create())
            val bytes = client.get(proxied).bodyAsBytes()
            client.close()
            val img = Image.makeFromEncoded(bytes)
            bitmap = img.toComposeImageBitmap()
        } catch (e: Exception) {
            failed = true
        }
    }

    val bmp = bitmap
    if (bmp != null) {
        Image(
            bitmap = bmp,
            contentDescription = contentDescription,
            modifier = modifier,
        )
    } else {
        Box(
            modifier = modifier.background(if (failed) Color.Transparent else Color.LightGray.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center,
        ) {
        }
    }
}

private fun proxyUrl(url: String): String {
    if (!url.startsWith("http://") && !url.startsWith("https://")) return url
    val origin = window.location.origin
    if (url.startsWith(origin)) return url
    return "${origin}/api/img-proxy?url=${encodeURIComponent(url)}"
}

private fun encodeURIComponent(s: String): String {
    val hex = "0123456789ABCDEF"
    val sb = StringBuilder()
    for (c in s) {
        if (c in 'A'..'Z' || c in 'a'..'z' || c in '0'..'9' || c == '-' || c == '_' || c == '.' || c == '~') {
            sb.append(c)
        } else {
            val bytes = c.toString().encodeToByteArray()
            for (b in bytes) {
                val v = 0xFF and b.toInt()
                sb.append('%').append(hex[v shr 4]).append(hex[v and 0x0F])
            }
        }
    }
    return sb.toString()
}