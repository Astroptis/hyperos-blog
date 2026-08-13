package com.hyperos.blog

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.hyperos.blog.navigation.AppRoute
import com.hyperos.blog.theme.AppTheme
import com.hyperos.blog.ui.AboutScreen
import com.hyperos.blog.ui.HomeScreen

@Composable
fun App() {
    val appState = remember { AppState() }
    var currentRoute by remember { mutableStateOf(AppRoute.Home) }

    AppTheme(state = appState.themeState) {
        when (currentRoute) {
            AppRoute.Home -> HomeScreen()
            AppRoute.About -> AboutScreen()
            else -> HomeScreen()
        }
    }
}