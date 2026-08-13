package com.hyperos.blog.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperos.blog.AppState
import com.hyperos.blog.data.ApiClient
import com.hyperos.blog.data.ArchiveGroup
import com.hyperos.blog.i18n.Strings
import com.hyperos.blog.navigation.AppRoute
import com.hyperos.blog.ui.components.MiuixScaffold
import com.hyperos.blog.ui.components.PostCard
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ArchiveScreen(
    appState: AppState,
    onNavigate: (AppRoute) -> Unit,
    api: ApiClient,
) {
    var archives by remember { mutableStateOf<List<ArchiveGroup>>(emptyList()) }

    LaunchedEffect(Unit) {
        val resp = api.get<List<ArchiveGroup>>("/api/archives")
        if (resp.ok) archives = resp.data ?: emptyList()
    }

    MiuixScaffold(
        title = Strings.get(appState.language, "archive"),
        appState = appState,
        currentRoute = AppRoute.Archive,
        onNavigate = onNavigate,
    ) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            items(archives, key = { "${it.year}-${it.month}" }) { group ->
                SmallTitle(
                    text = if (appState.language == "zh") {
                        "${group.year} 年 ${group.month} 月 (${group.posts.size})"
                    } else {
                        "${group.year}-${group.month} (${group.posts.size})"
                    },
                    textColor = MiuixTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(8.dp))
                group.posts.forEach { post ->
                    PostCard(post, appState.language, onClick = {
                        appState.currentSlug = post.slug
                        onNavigate(AppRoute.PostDetail)
                    })
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}