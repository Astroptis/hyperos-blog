package com.hyperos.blog.navigation

expect object UrlRouter {
    fun currentPath(): String
    fun push(path: String)
    fun listen(onChange: (String) -> Unit)
}