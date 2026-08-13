package com.hyperos.blog.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyperos.blog.AppState
import com.hyperos.blog.data.ApiClient
import com.hyperos.blog.data.Post
import com.hyperos.blog.data.PostListData
import com.hyperos.blog.data.SiteStats
import com.hyperos.blog.i18n.Strings
import com.hyperos.blog.ui.components.MiuixScaffold
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.icon.extended.Filter
import top.yukonga.miuix.kmp.icon.extended.Settings
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AdminHomeScreen(
    appState: AppState,
    onBack: () -> Unit,
    onOpenSiteSettings: () -> Unit,
    onOpenComments: () -> Unit,
    onEditPost: (Post?) -> Unit,
    onLogout: () -> Unit,
    api: ApiClient,
) {
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var stats by remember { mutableStateOf<SiteStats?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        val pResp = api.get<PostListData>("/api/admin/posts?page=1&pageSize=50")
        if (pResp.ok) posts = pResp.data?.posts ?: emptyList()
        val sResp = api.get<SiteStats>("/api/stats")
        if (sResp.ok) stats = sResp.data
    }

    LaunchedEffect(Unit) { load() }

    MiuixScaffold(
        title = Strings.get(appState.language, "admin"),
        appState = appState,
        currentRoute = null,
        onNavigate = { },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(MiuixIcons.Back, null)
            }
        },
        topBarActions = {
            IconButton(onClick = { onEditPost(null) }) {
                Icon(MiuixIcons.Add, null)
            }
        },
    ) {
        Column(Modifier.fillMaxSize()) {
            LazyColumn(Modifier.weight(1f).fillMaxWidth(), contentPadding = PaddingValues(16.dp)) {
                stats?.let { s ->
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatsCard(Strings.get(appState.language, "postCount"), s.postCount.toString(), Modifier.weight(1f))
                            StatsCard(Strings.get(appState.language, "commentCount"), s.commentCount.toString(), Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatsCard(Strings.get(appState.language, "viewCount"), s.viewCount.toString(), Modifier.weight(1f))
                            StatsCard(Strings.get(appState.language, "totalVisits"), s.totalVisits.toString(), Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(16.dp))
                    }
                }
                item {
                    Card(Modifier.fillMaxWidth(), onClick = onOpenSiteSettings) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(MiuixIcons.Settings, null, tint = MiuixTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(Strings.get(appState.language, "siteSettings"), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
                item {
                    Card(Modifier.fillMaxWidth(), onClick = onOpenComments) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(MiuixIcons.Filter, null, tint = MiuixTheme.colorScheme.primary)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(Strings.get(appState.language, "commentManage"), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                items(posts, key = { it.id }) { post ->
                    Card(Modifier.fillMaxWidth(), onClick = { onEditPost(post) }) {
                        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    if (appState.language == "zh") post.titleZh else post.titleEn.ifBlank { post.titleZh },
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    "${post.createdAt.take(10)} · ${post.viewCount} ${Strings.get(appState.language, "views")}",
                                    style = MiuixTheme.textStyles.footnote2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
            Button(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                onClick = onLogout,
            ) {
                Text(Strings.get(appState.language, "logout"))
            }
        }
    }
}

@Composable
private fun StatsCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MiuixTheme.textStyles.title1, fontWeight = FontWeight.Bold)
            Text(label, style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
    }
}