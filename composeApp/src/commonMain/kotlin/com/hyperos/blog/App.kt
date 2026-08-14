package com.hyperos.blog

import androidx.compose.runtime.*
import com.hyperos.blog.data.createDefaultApiClient
import com.hyperos.blog.navigation.AppRoute
import com.hyperos.blog.navigation.AppRoutes
import com.hyperos.blog.navigation.FaviconManager
import com.hyperos.blog.navigation.RouteResult
import com.hyperos.blog.navigation.UrlRouter
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
import com.hyperos.blog.ui.admin.AdminCommentsScreen
import com.hyperos.blog.ui.admin.AdminEditorScreen
import com.hyperos.blog.ui.admin.AdminHomeScreen
import com.hyperos.blog.ui.admin.AdminLoginScreen
import com.hyperos.blog.ui.admin.AdminSiteSettingsScreen
import kotlinx.coroutines.launch

@Composable
fun App() {
    val appState = remember { AppState() }
    val api = remember { createDefaultApiClient() }
    val scope = rememberCoroutineScope()
    var routeResult by remember { mutableStateOf(AppRoutes.parse(UrlRouter.currentPath())) }

    fun applyPath(path: String) {
        val parsed = AppRoutes.parse(path)
        if (parsed.route == AppRoute.PostDetail) appState.currentSlug = parsed.param
        routeResult = parsed
    }

    fun navigate(route: AppRoute, param: String? = null) {
        if (route == AppRoute.PostDetail) {
            if (param != null) appState.currentSlug = param
        }
        val effectiveParam = if (route == AppRoute.PostDetail) appState.currentSlug else param
        val path = AppRoutes.pathOf(route, effectiveParam)
        UrlRouter.push(path)
        scope.launch {
            routeResult = RouteResult(route, effectiveParam)
        }
    }

    fun navigateSlug(slug: String) {
        appState.currentSlug = slug
        navigate(AppRoute.PostDetail, slug)
    }

    LaunchedEffect(Unit) {
        val resp = api.get<Map<String, String>>("/api/settings")
        if (resp.ok && resp.data != null) {
            resp.data["title"]?.let { appState.siteTitle = it }
            resp.data["bio"]?.let { appState.siteBio = it } ?: resp.data["description"]?.let { appState.siteBio = it }
            resp.data["avatar"]?.let { appState.avatarUrl = it }
            resp.data["favicon"]?.let { appState.faviconUrl = it }
            FaviconManager.update(appState.faviconUrl)
        }
        UrlRouter.listen { path ->
            applyPath(path)
        }
    }

    DisposableEffect(Unit) {
        onDispose { }
    }

    AppTheme(state = appState.themeState) {
        when (routeResult.route) {
            AppRoute.PostDetail -> PostDetailScreen(
                appState = appState,
                slug = appState.currentSlug ?: routeResult.param ?: "",
                onBack = { navigate(AppRoute.Home) },
                api = api,
            )
            AppRoute.Archive -> ArchiveScreen(appState, onNavigate = { route -> navigate(route) }, api = api)
            AppRoute.Messages -> MessagesScreen(appState, onNavigate = { route -> navigate(route) }, api = api)
            AppRoute.Friends -> FriendsScreen(appState, onNavigate = { route -> navigate(route) }, api = api)
            AppRoute.About -> AboutScreen(appState, onNavigate = { route -> navigate(route) })
            AppRoute.Settings -> SettingsScreen(appState, onNavigate = { route -> navigate(route) }, api = api)
            AppRoute.Search -> SearchScreen(
                appState,
                onBack = { navigate(AppRoute.Home) },
                onNavigate = { route -> navigate(route) },
                onOpenPost = { slug -> navigateSlug(slug) },
                api = api,
            )
            AppRoute.Tags -> TagsScreen(appState, onNavigate = { route -> navigate(route) }, api = api)
            AppRoute.Admin -> {
                if (!appState.isAdmin) {
                    AdminLoginScreen(
                        appState = appState,
                        onLoggedIn = { navigate(AppRoute.Admin) },
                        api = api,
                    )
                } else {
                    AdminHomeScreen(
                        appState = appState,
                        onBack = { navigate(AppRoute.Home) },
                        onOpenSiteSettings = { navigate(AppRoute.AdminSiteSettings) },
                        onOpenComments = { navigate(AppRoute.AdminComments) },
                        onEditPost = { post ->
                            appState.editingPost = post
                            navigate(AppRoute.AdminEditor)
                        },
                        onLogout = {
                            appState.adminToken = null
                            api.setToken(null)
                            navigate(AppRoute.Home)
                        },
                        api = api,
                    )
                }
            }
            AppRoute.AdminEditor -> AdminEditorScreen(
                appState = appState,
                post = appState.editingPost,
                onBack = { navigate(AppRoute.Admin) },
                onSaved = { navigate(AppRoute.Admin) },
                api = api,
            )
            AppRoute.AdminSiteSettings -> AdminSiteSettingsScreen(
                appState = appState,
                onBack = { navigate(AppRoute.Admin) },
                api = api,
            )
            AppRoute.AdminComments -> AdminCommentsScreen(
                appState = appState,
                onBack = { navigate(AppRoute.Admin) },
                api = api,
            )
            else -> HomeScreen(appState, api, onNavigate = { route -> navigate(route) })
        }
    }
}