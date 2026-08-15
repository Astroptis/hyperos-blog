package com.hyperos.blog.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.hyperos.blog.data.Category
import com.hyperos.blog.data.Post
import com.hyperos.blog.data.PostListData
import com.hyperos.blog.i18n.Strings
import com.hyperos.blog.navigation.AppRoute
import com.hyperos.blog.ui.components.AppButton
import com.hyperos.blog.ui.components.MiuixScaffold
import com.hyperos.blog.ui.components.PostCard
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.TabRow
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun ArchiveScreen(
    appState: AppState,
    onNavigate: (AppRoute) -> Unit,
    api: ApiClient,
) {
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var total by remember { mutableStateOf(0L) }
    var page by remember { mutableStateOf(1) }
    var loading by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var categories by remember { mutableStateOf<List<Category>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    suspend fun loadPosts(reset: Boolean) {
        if (loading) return
        loading = true
        val targetPage = if (reset) 1 else page
        val resp = api.get<PostListData>(
            "/api/posts?page=$targetPage&pageSize=10&category=$selectedCategory"
        )
        if (resp.ok && resp.data != null) {
            posts = if (reset) resp.data.posts else posts + resp.data.posts
            total = resp.data.total
            page = targetPage + 1
        }
        loading = false
        refreshing = false
    }

    LaunchedEffect(Unit) {
        val catResp = api.get<List<Category>>("/api/categories")
        if (catResp.ok) categories = catResp.data ?: emptyList()
        loadPosts(true)
    }

    MiuixScaffold(
        title = Strings.get(appState.language, "archive"),
        appState = appState,
        currentRoute = AppRoute.Archive,
        onNavigate = onNavigate,
    ) {
        PullToRefresh(
            isRefreshing = refreshing,
            onRefresh = {
                refreshing = true
                scope.launch { loadPosts(true) }
            },
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (categories.isNotEmpty()) {
                    item {
                        TabRow(
                            tabs = listOf(Strings.get(appState.language, "all")) + categories.map {
                                if (appState.language == "zh") it.name_zh else it.name_en
                            },
                            selectedTabIndex = categories.indexOfFirst { it.name_zh == selectedCategory }.let {
                                if (selectedCategory.isEmpty()) 0 else it + 1
                            },
                            onTabSelected = { index ->
                                selectedCategory = if (index == 0) "" else categories[index - 1].name_zh
                                scope.launch { loadPosts(true) }
                            },
                        )
                    }
                }
                if (posts.isEmpty() && !loading) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(40.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                Strings.get(appState.language, "empty"),
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                }
                items(posts, key = { it.id }) { post ->
                    PostCard(post, appState.language, onClick = {
                        appState.currentSlug = post.slug
                        onNavigate(AppRoute.PostDetail)
                    })
                }
                if (posts.size < total) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().padding(8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            AppButton(onClick = { scope.launch { loadPosts(false) } }) {
                                Text(Strings.get(appState.language, "loadMore"))
                            }
                        }
                    }
                }
            }
        }
    }
}
