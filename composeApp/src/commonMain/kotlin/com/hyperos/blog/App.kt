package com.hyperos.blog

import androidx.compose.runtime.*
import com.hyperos.blog.data.createDefaultApiClient
import com.hyperos.blog.navigation.AppRoute
import com.hyperos.blog.theme.AppTheme
import com.hyperos.blog.ui.AboutScreen
import com.hyperos.blog.ui.ArchiveScreen
import com.hyperos.blog.ui.FriendsScreen
import com.hyperos.blog.ui.HomeScreen
import com.hyperos.blog.ui.MessagesScreen
import com.hyperos.blog.ui.PostDetailScreen
import com.hyperos.blog.ui.SearchScreen
import com.hyperos.blog.ui.SettingsScreen
import com.hyperos.blog.ui.TagsScreen
import com.hyperos.blog.ui.admin.AdminEditorScreen
import com.hyperos.blog.ui.admin.AdminHomeScreen
import com.hyperos.blog.ui.admin.AdminLoginScreen

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
            AppRoute.Archive -> ArchiveScreen(appState, onNavigate = { currentRoute = it }, api = api)
            AppRoute.Messages -> MessagesScreen(appState, onNavigate = { currentRoute = it }, api = api)
            AppRoute.Friends -> FriendsScreen(appState, onNavigate = { currentRoute = it }, api = api)
            AppRoute.About -> AboutScreen(appState, onNavigate = { currentRoute = it })
            AppRoute.Settings -> SettingsScreen(appState, onNavigate = { currentRoute = it })
            AppRoute.Search -> SearchScreen(
                appState,
                onBack = { currentRoute = AppRoute.Home },
                onNavigate = { currentRoute = it },
                api = api,
            )
            AppRoute.Tags -> TagsScreen(appState, onNavigate = { currentRoute = it }, api = api)
            AppRoute.Admin -> {
                if (!appState.isAdmin) {
                    AdminLoginScreen(
                        appState = appState,
                        onLoggedIn = { currentRoute = AppRoute.Admin },
                        api = api,
                    )
                } else {
                    AdminHomeScreen(
                        appState = appState,
                        onBack = { currentRoute = AppRoute.Home },
                        onEditPost = { post ->
                            appState.editingPost = post
                            currentRoute = AppRoute.AdminEditor
                        },
                        onLogout = {
                            appState.adminToken = null
                            api.setToken(null)
                            currentRoute = AppRoute.Home
                        },
                        api = api,
                    )
                }
            }
            AppRoute.AdminEditor -> AdminEditorScreen(
                appState = appState,
                post = appState.editingPost,
                onBack = { currentRoute = AppRoute.Admin },
                onSaved = { currentRoute = AppRoute.Admin },
                api = api,
            )
            else -> HomeScreen(appState, api, onNavigate = { currentRoute = it })
        }
    }
}