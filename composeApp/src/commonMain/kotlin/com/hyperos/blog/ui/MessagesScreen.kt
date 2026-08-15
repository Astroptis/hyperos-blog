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
import com.hyperos.blog.data.Message
import com.hyperos.blog.i18n.Strings
import com.hyperos.blog.navigation.AppRoute
import com.hyperos.blog.ui.components.AppButton
import com.hyperos.blog.ui.components.MiuixScaffold
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun MessagesScreen(
    appState: AppState,
    onNavigate: (AppRoute) -> Unit,
    api: ApiClient,
) {
    var messages by remember { mutableStateOf<List<Message>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var nickname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        val resp = api.get<List<Message>>("/api/messages")
        if (resp.ok) messages = resp.data ?: emptyList()
    }

    LaunchedEffect(Unit) { load() }

    MiuixScaffold(
        title = Strings.get(appState.language, "messages"),
        appState = appState,
        currentRoute = AppRoute.Messages,
        onNavigate = onNavigate,
    ) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            item {
                AppButton(onClick = { showDialog = true }) {
                    Text(Strings.get(appState.language, "leaveMessage"))
                }
                Spacer(Modifier.height(16.dp))
            }
            if (messages.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text(
                            Strings.get(appState.language, "empty"),
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            }
            items(messages, key = { it.id }) { msg ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(msg.nickname, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(msg.content)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            msg.createdAt.take(16),
                            style = MiuixTheme.textStyles.footnote2,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }

    if (showDialog) {
        OverlayDialog(
            show = showDialog,
            onDismissRequest = { showDialog = false },
            title = Strings.get(appState.language, "leaveMessage"),
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
                            val resp = api.post<Message>(
                                "/api/messages",
                                mapOf("nickname" to nickname.trim(), "email" to email.trim(), "content" to content.trim()),
                            )
                            if (resp.ok) {
                                showDialog = false
                                load()
                            }
                        }
                    }) { Text(Strings.get(appState.language, "submit")) }
                }
            }
        }
    }
}