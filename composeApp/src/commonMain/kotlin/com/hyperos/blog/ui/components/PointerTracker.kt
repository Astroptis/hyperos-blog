package com.hyperos.blog.ui.components

expect object PointerTracker {
    fun onMove(listener: (x: Double, y: Double) -> Unit): () -> Unit
}