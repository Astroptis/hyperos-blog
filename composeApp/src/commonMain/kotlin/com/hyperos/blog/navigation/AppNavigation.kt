package com.hyperos.blog.navigation

enum class AppRoute {
    Home, Archive, Messages, Friends, About, Settings, Admin,
    PostDetail, Search, Tags, AdminEditor, AdminSiteSettings, AdminComments,
}

data class RouteResult(val route: AppRoute, val param: String? = null)

object AppRoutes {
    fun parse(path: String): RouteResult {
        val clean = path.trim().trimEnd('/').ifEmpty { "/" }
        return when {
            clean == "/" -> RouteResult(AppRoute.Home)
            clean == "/archive" -> RouteResult(AppRoute.Archive)
            clean == "/messages" -> RouteResult(AppRoute.Messages)
            clean == "/friends" -> RouteResult(AppRoute.Friends)
            clean == "/about" -> RouteResult(AppRoute.About)
            clean == "/settings" -> RouteResult(AppRoute.Settings)
            clean == "/search" -> RouteResult(AppRoute.Search)
            clean == "/tags" -> RouteResult(AppRoute.Tags)
            clean == "/admin" -> RouteResult(AppRoute.Admin)
            clean == "/admin/editor" -> RouteResult(AppRoute.AdminEditor)
            clean == "/admin/settings" -> RouteResult(AppRoute.AdminSiteSettings)
            clean == "/admin/comments" -> RouteResult(AppRoute.AdminComments)
            clean.startsWith("/post/") -> RouteResult(AppRoute.PostDetail, clean.removePrefix("/post/"))
            else -> RouteResult(AppRoute.Home)
        }
    }

    fun pathOf(route: AppRoute, param: String? = null): String {
        return when (route) {
            AppRoute.Home -> "/"
            AppRoute.Archive -> "/archive"
            AppRoute.Messages -> "/messages"
            AppRoute.Friends -> "/friends"
            AppRoute.About -> "/about"
            AppRoute.Settings -> "/settings"
            AppRoute.Search -> "/search"
            AppRoute.Tags -> "/tags"
            AppRoute.Admin -> "/admin"
            AppRoute.AdminEditor -> "/admin/editor"
            AppRoute.AdminSiteSettings -> "/admin/settings"
            AppRoute.AdminComments -> "/admin/comments"
            AppRoute.PostDetail -> if (param.isNullOrBlank()) "/" else "/post/$param"
        }
    }
}