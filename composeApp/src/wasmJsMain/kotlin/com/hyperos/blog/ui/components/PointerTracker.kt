package com.hyperos.blog.ui.components

import kotlinx.browser.window
import org.w3c.dom.events.Event

actual object PointerTracker {
    actual fun onMove(listener: (x: Double, y: Double) -> Unit): () -> Unit {
        val handler: (Event) -> Unit = { e ->
            val mouse = e as org.w3c.dom.events.MouseEvent
            listener(mouse.clientX.toDouble(), mouse.clientY.toDouble())
        }
        window.addEventListener("mousemove", handler)
        return { window.removeEventListener("mousemove", handler) }
    }
}