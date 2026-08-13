package com.hyperos.blog

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hyperos.blog.theme.ThemeState

class AppState {
    val themeState = ThemeState()
    var language by mutableStateOf("zh")
    var adminToken by mutableStateOf<String?>(null)
    var siteTitle by mutableStateOf("HyperOS 博客")

    val isAdmin: Boolean get() = adminToken != null
}