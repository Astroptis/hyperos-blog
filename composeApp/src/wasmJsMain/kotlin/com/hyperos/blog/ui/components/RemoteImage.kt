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
import io.ktor.client.call.body
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
            val client = HttpClient(Js.create())
            val bytes = client.get(url).bodyAsBytes()
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