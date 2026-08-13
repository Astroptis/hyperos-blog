package com.hyperos.blog.navigation

import kotlinx.browser.document

actual object FaviconManager {
    actual fun update(url: String) {
        if (url.isBlank()) return
        val existing = document.getElementById("site-favicon")
        if (existing != null) {
            existing.setAttribute("href", url)
            return
        }
        val link = document.createElement("link").apply {
            setAttribute("rel", "icon")
            setAttribute("id", "site-favicon")
            setAttribute("href", url)
        }
        document.head!!.appendChild(link)
    }
}