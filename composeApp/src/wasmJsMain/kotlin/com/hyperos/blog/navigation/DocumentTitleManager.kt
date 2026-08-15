package com.hyperos.blog.navigation

import kotlinx.browser.document

actual object DocumentTitleManager {
    actual fun update(title: String) {
        document.title = title
    }
}
