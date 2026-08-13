package com.hyperos.blog

import androidx.compose.runtime.*
import com.hyperos.blog.data.createDefaultApiClient
import com.hyperos.blog.navigation.AppRoute
import com.hyperos.blog.theme.AppTheme
import com.hyperos.blog.ui.HomeScreen
import com.hyperos.blog.ui.PostDetailScreen

@Composable
fun App() {
    val appState = remember { AppState() }
    val api = remember { createDefaultApiClient() }
    var currentRoute by remember { mutableStateOf(AppRoute.Home) }

    AppTheme(state = appState.themeState) {
        when (currentRoute) {
            AppRoute.PostDetail -> PostDetailScreen(
                appState = appState,
                slug = appState.currentSlug ?: "",
                onBack = { currentRoute = AppRoute.Home },
                api = api,
            )
            else -> HomeScreen(appState, api, onNavigate = { currentRoute = it })
        }
    }
}