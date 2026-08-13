package com.hyperos.blog

import androidx.compose.runtime.*
import com.hyperos.blog.data.createDefaultApiClient
import com.hyperos.blog.navigation.AppRoute
import com.hyperos.blog.theme.AppTheme
import com.hyperos.blog.ui.HomeScreen

@Composable
fun App() {
    val appState = remember { AppState() }
    val api = remember { createDefaultApiClient() }
    var currentRoute by remember { mutableStateOf(AppRoute.Home) }

    AppTheme(state = appState.themeState) {
        when (currentRoute) {
            AppRoute.Home -> HomeScreen(appState, api, onNavigate = { currentRoute = it })
            else -> HomeScreen(appState, api, onNavigate = { currentRoute = it })
        }
    }
}