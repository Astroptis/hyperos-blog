package com.hyperos.blog.navigation

import kotlinx.browser.window
import org.w3c.dom.events.Event

actual object UrlRouter {
    actual fun currentPath(): String {
        val path = window.location.pathname ?: "/"
        return path.ifEmpty { "/" }
    }

    actual fun push(path: String) {
        window.history.pushState(null, "", path)
    }

    actual fun listen(onChange: (String) -> Unit) {
        window.addEventListener("popstate", { _: Event -> onChange(currentPath()) })
    }
}