package com.hyperos.blog.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.hyperos.blog.AppState
import com.hyperos.blog.data.ApiClient
import com.hyperos.blog.data.Post
import com.hyperos.blog.data.PostInput
import com.hyperos.blog.i18n.Strings
import com.hyperos.blog.ui.components.AppButton
import com.hyperos.blog.ui.components.MiuixScaffold
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back

@Composable
fun AdminEditorScreen(
    appState: AppState,
    post: Post?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
    api: ApiClient,
) {
    var titleZh by remember { mutableStateOf(post?.titleZh ?: "") }
    var titleEn by remember { mutableStateOf(post?.titleEn ?: "") }
    var summary by remember { mutableStateOf(post?.summary ?: "") }
    var content by remember { mutableStateOf(post?.content ?: "") }
    var category by remember { mutableStateOf(post?.category ?: "") }
    var tagsText by remember { mutableStateOf(post?.tags?.joinToString(",") ?: "") }
    var published by remember { mutableStateOf((post?.status ?: "published") == "published") }
    var saving by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    MiuixScaffold(
        title = if (post == null) Strings.get(appState.language, "publish") else Strings.get(appState.language, "edit"),
        appState = appState,
        currentRoute = null,
        onNavigate = { },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(MiuixIcons.Back, null)
            }
        },
        topBarActions = {
            AppButton(onClick = {
                saving = true
                scope.launch {
                    val input = PostInput(
                        slug = post?.slug ?: "",
                        titleZh = titleZh,
                        titleEn = titleEn,
                        summary = summary,
                        content = content,
                        category = category,
                        tags = tagsText.split(',').map { it.trim() }.filter { it.isNotEmpty() },
                        status = if (published) "published" else "draft",
                        pinned = post?.pinned ?: false,
                        featured = post?.featured ?: false,
                    )
                    val resp = if (post == null) {
                        api.post<Post>("/api/admin/posts", input)
                    } else {
                        api.put<Post>("/api/admin/posts/${post.id}", input)
                    }
                    saving = false
                    if (resp.ok) onSaved()
                }
            }, enabled = !saving) {
                Text(Strings.get(appState.language, "save"))
            }
        },
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextField(value = titleZh, onValueChange = { titleZh = it }, label = if (appState.language == "zh") "标题 (中文)" else "Title (ZH)")
            TextField(value = titleEn, onValueChange = { titleEn = it }, label = if (appState.language == "zh") "标题 (English)" else "Title (EN)")
            TextField(value = summary, onValueChange = { summary = it }, label = Strings.get(appState.language, "postSummary"), minLines = 2)
            TextField(value = content, onValueChange = { content = it }, label = Strings.get(appState.language, "postContent") + " (Markdown)", minLines = 12)
            TextField(value = category, onValueChange = { category = it }, label = Strings.get(appState.language, "category"))
            TextField(
                value = tagsText,
                onValueChange = { tagsText = it },
                label = if (appState.language == "zh") "标签，逗号分隔" else "Tags, comma separated",
            )
            Row {
                Switch(checked = published, onCheckedChange = { published = it })
                Spacer(Modifier.width(8.dp))
                Text(if (published) Strings.get(appState.language, "publish") else Strings.get(appState.language, "draft"))
            }
            if (post != null) {
                AppButton(
                    onClick = {
                        scope.launch {
                            api.delete<Any>("/api/admin/posts/${post.id}")
                            onSaved()
                        }
                    },
                ) {
                    Text(Strings.get(appState.language, "delete"))
                }
            }
        }
    }
}