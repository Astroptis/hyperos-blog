package com.hyperos.blog.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyperos.blog.AppState
import com.hyperos.blog.data.ApiClient
import com.hyperos.blog.data.Comment
import com.hyperos.blog.data.CommentInput
import com.hyperos.blog.data.Post
import com.hyperos.blog.i18n.Strings
import com.hyperos.blog.ui.components.AppButton
import com.hyperos.blog.ui.components.MiuixScaffold
import com.hyperos.blog.ui.components.blogMarkdownColors
import com.hyperos.blog.ui.components.blogMarkdownTypography
import kotlinx.coroutines.launch
import com.mikepenz.markdown.compose.Markdown
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Answer
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun PostDetailScreen(
    appState: AppState,
    slug: String,
    onBack: () -> Unit,
    api: ApiClient,
) {
    var post by remember { mutableStateOf<Post?>(null) }
    var comments by remember { mutableStateOf<List<Comment>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var showCommentDialog by remember { mutableStateOf(false) }
    var nickname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(slug) {
        val resp = api.get<Post>("/api/posts/$slug")
        if (resp.ok && resp.data != null) {
            post = resp.data
            api.post<Map<String, Long>>("/api/posts/${resp.data.id}/view")
            val cResp = api.get<List<Comment>>("/api/posts/${resp.data.id}/comments")
            if (cResp.ok) comments = cResp.data ?: emptyList()
        }
        loading = false
    }

    MiuixScaffold(
        title = post?.let { if (appState.language == "zh") it.titleZh else it.titleEn.ifBlank { it.titleZh } } ?: "",
        appState = appState,
        currentRoute = null,
        onNavigate = { },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(MiuixIcons.Back, null)
            }
        },
    ) {
        when {
            loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            post != null -> {
                LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
                    item {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SmallTitle(
                                text = post!!.category.ifBlank { Strings.get(appState.language, "category") },
                                textColor = MiuixTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "${post!!.createdAt.take(10)} · ${post!!.viewCount} ${Strings.get(appState.language, "views")}",
                                style = MiuixTheme.textStyles.footnote2,
                                color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            )
                        }
                    }
                    item {
                        Spacer(Modifier.height(8.dp))
                        Markdown(
                            content = post!!.content,
                            modifier = Modifier.fillMaxWidth(),
                            colors = blogMarkdownColors(),
                            typography = blogMarkdownTypography(),
                        )
                    }
                    item {
                        Spacer(Modifier.height(16.dp))
                        Row {
                            AppButton(onClick = {
                                scope.launch {
                                    api.post<Map<String, Any>>("/api/posts/${post!!.id}/like")
                                    post = post!!.copy(likeCount = post!!.likeCount + 1)
                                }
                            }) {
                                Text("${Strings.get(appState.language, "likes")} ${post!!.likeCount}")
                            }
                        }
                    }
                    item {
                        Spacer(Modifier.height(24.dp))
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SmallTitle(text = Strings.get(appState.language, "comments"))
                            Spacer(Modifier.weight(1f))
                            AppButton(onClick = { showCommentDialog = true }) {
                                Text(Strings.get(appState.language, "writeComment"))
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                    items(comments, key = { it.id }) { comment ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(14.dp)) {
                                Text(comment.nickname, fontWeight = FontWeight.Bold, style = MiuixTheme.textStyles.body1)
                                Spacer(Modifier.height(4.dp))
                                Text(comment.content, style = MiuixTheme.textStyles.body1)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    comment.createdAt.take(16),
                                    style = MiuixTheme.textStyles.footnote2,
                                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }
        }
    }

    if (showCommentDialog) {
        OverlayDialog(
            show = showCommentDialog,
            onDismissRequest = { showCommentDialog = false },
            title = Strings.get(appState.language, "writeComment"),
        ) {
            Column(Modifier.padding(16.dp)) {
                TextField(value = nickname, onValueChange = { nickname = it }, label = Strings.get(appState.language, "nickname"))
                Spacer(Modifier.height(8.dp))
                TextField(value = email, onValueChange = { email = it }, label = Strings.get(appState.language, "email"))
                Spacer(Modifier.height(8.dp))
                TextField(
                    value = content,
                    onValueChange = { content = it },
                    label = Strings.get(appState.language, "content"),
                    minLines = 3,
                )
                Spacer(Modifier.height(12.dp))
                Row {
                    Spacer(Modifier.weight(1f))
                    AppButton(onClick = {
                        scope.launch {
                            val p = post
                            if (p != null) {
                                val resp = api.post<Comment>(
                                    "/api/posts/${p.id}/comments",
                                    CommentInput(nickname.trim(), email.trim(), content.trim()),
                                )
                                if (resp.ok && resp.data != null) {
                                    val c = resp.data
                                    comments = comments + Comment(c.id, c.nickname, c.content, c.createdAt)
                                    showCommentDialog = false
                                }
                            }
                        }
                    }) {
                        Text(Strings.get(appState.language, "submit"))
                    }
                }
            }
        }
    }
}