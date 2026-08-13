package com.hyperos.blog

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.hyperos.blog.data.Post
import com.hyperos.blog.theme.ThemeState

class AppState {
    val themeState = ThemeState()
    var language by mutableStateOf("zh")
    var adminToken by mutableStateOf<String?>(null)
    var siteTitle by mutableStateOf("HyperOS 博客")
    var siteBio by mutableStateOf("欢迎来到我的博客")
    var avatarUrl by mutableStateOf("")
    var faviconUrl by mutableStateOf("")
    var currentSlug by mutableStateOf<String?>(null)
    var editingPost by mutableStateOf<Post?>(null)

    val isAdmin: Boolean get() = adminToken != null
}
