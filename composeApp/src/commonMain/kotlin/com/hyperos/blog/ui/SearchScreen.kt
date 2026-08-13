package com.hyperos.blog.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperos.blog.AppState
import com.hyperos.blog.data.ApiClient
import com.hyperos.blog.data.Post
import com.hyperos.blog.data.SearchResult
import com.hyperos.blog.i18n.Strings
import com.hyperos.blog.navigation.AppRoute
import com.hyperos.blog.ui.components.MiuixScaffold
import com.hyperos.blog.ui.components.PostCard
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SearchScreen(
    appState: AppState,
    onBack: () -> Unit,
    onNavigate: (AppRoute) -> Unit,
    onOpenPost: (String) -> Unit,
    api: ApiClient,
) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<Post>>(emptyList()) }
    var searched by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    MiuixScaffold(
        title = Strings.get(appState.language, "search"),
        appState = appState,
        currentRoute = null,
        onNavigate = { },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(MiuixIcons.Back, null)
            }
        },
        topBarActions = {
            InputField(
                query = query,
                onQueryChange = { query = it },
                onSearch = { q ->
                    scope.launch {
                        if (q.isBlank()) return@launch
                        val resp = api.get<SearchResult>("/api/search?q=$q")
                        if (resp.ok) results = resp.data?.posts ?: emptyList()
                        searched = true
                    }
                },
                expanded = false,
                onExpandedChange = { },
            )
        },
    ) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            if (!searched) {
                item {
                    Text(
                        Strings.get(appState.language, "welcome"),
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            } else if (results.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text(Strings.get(appState.language, "empty"))
                    }
                }
            }
            items(results, key = { it.id }) { post ->
                PostCard(post, appState.language, onClick = { onOpenPost(post.slug) })
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}