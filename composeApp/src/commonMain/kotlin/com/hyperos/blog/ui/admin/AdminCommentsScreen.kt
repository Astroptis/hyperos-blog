package com.hyperos.blog.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
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
import com.hyperos.blog.data.AdminComment
import com.hyperos.blog.i18n.Strings
import com.hyperos.blog.ui.components.AppButton
import com.hyperos.blog.ui.components.MiuixScaffold
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AdminCommentsScreen(
    appState: AppState,
    onBack: () -> Unit,
    api: ApiClient,
) {
    var comments by remember { mutableStateOf<List<AdminComment>>(emptyList()) }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        val resp = api.get<List<AdminComment>>("/api/admin/comments")
        if (resp.ok) comments = resp.data ?: emptyList()
    }

    LaunchedEffect(Unit) { load() }

    MiuixScaffold(
        title = Strings.get(appState.language, "commentManage"),
        appState = appState,
        currentRoute = null,
        onNavigate = { },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(MiuixIcons.Back, null)
            }
        },
    ) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (comments.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(Strings.get(appState.language, "empty"), color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
                    }
                }
            }
            items(comments, key = { it.id }) { comment ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(
                            "${comment.nickname} · ${comment.postTitle}",
                            fontWeight = FontWeight.Bold,
                            style = MiuixTheme.textStyles.body1,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(comment.content, style = MiuixTheme.textStyles.body2)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            comment.createdAt.take(10),
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                        Spacer(Modifier.height(8.dp))
                        AppButton(
                            onClick = {
                                scope.launch {
                                    api.delete<Any>("/api/comments/${comment.id}")
                                    load()
                                }
                            },
                        ) {
                            Text(Strings.get(appState.language, "delete"))
                        }
                    }
                }
            }
        }
    }
}