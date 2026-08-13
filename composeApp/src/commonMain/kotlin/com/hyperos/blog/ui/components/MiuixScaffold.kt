package com.hyperos.blog.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import com.hyperos.blog.AppState
import com.hyperos.blog.i18n.Strings
import com.hyperos.blog.navigation.AppRoute
import top.yukonga.miuix.kmp.basic.NavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Album
import top.yukonga.miuix.kmp.icon.extended.Community
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.Filter
import top.yukonga.miuix.kmp.icon.extended.Home

@Composable
fun MiuixScaffold(
    title: String,
    appState: AppState,
    currentRoute: AppRoute?,
    onNavigate: (AppRoute) -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    topBarActions: @Composable RowScope.() -> Unit = {},
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = title,
                navigationIcon = navigationIcon,
                actions = topBarActions,
            )
        },
        bottomBar = {
            if (currentRoute != null) {
                NavigationBar {
                    val items = listOf(
                        Triple(AppRoute.Home, Strings.get(appState.language, "home"), MiuixIcons.Home),
                        Triple(AppRoute.Archive, Strings.get(appState.language, "archive"), MiuixIcons.Album),
                        Triple(AppRoute.Messages, Strings.get(appState.language, "messages"), MiuixIcons.Community),
                        Triple(AppRoute.Friends, Strings.get(appState.language, "friends"), MiuixIcons.Contacts),
                        Triple(AppRoute.Settings, Strings.get(appState.language, "settings"), MiuixIcons.Filter),
                    )
                    val selectedIndex = items.indexOfFirst { it.first == currentRoute }.coerceAtLeast(0)
                    items.forEachIndexed { index, item ->
                        NavigationBarItem(
                            selected = selectedIndex == index,
                            onClick = { onNavigate(item.first) },
                            icon = item.third,
                            label = item.second,
                        )
                    }
                }
            }
        },
        content = content,
    )
}