package com.hyperos.blog.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.hyperos.blog.AppState
import com.hyperos.blog.data.ApiClient
import com.hyperos.blog.data.Friend
import com.hyperos.blog.i18n.Strings
import com.hyperos.blog.navigation.AppRoute
import com.hyperos.blog.ui.components.MiuixScaffold
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Add
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun FriendsScreen(
    appState: AppState,
    onNavigate: (AppRoute) -> Unit,
    api: ApiClient,
) {
    var friends by remember { mutableStateOf<List<Friend>>(emptyList()) }
    var showDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    suspend fun load() {
        val resp = api.get<List<Friend>>("/api/friends")
        if (resp.ok) friends = resp.data ?: emptyList()
    }

    LaunchedEffect(Unit) { load() }

    MiuixScaffold(
        title = Strings.get(appState.language, "friends"),
        appState = appState,
        currentRoute = AppRoute.Friends,
        onNavigate = onNavigate,
        topBarActions = {
            IconButton(onClick = { showDialog = true }) {
                Icon(MiuixIcons.Add, null)
            }
        },
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(friends, key = { it.id }) { friend ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp)) {
                        Text(friend.name, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            friend.description.ifBlank { friend.url },
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            style = MiuixTheme.textStyles.body1,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        OverlayDialog(
            show = showDialog,
            onDismissRequest = { showDialog = false },
            title = Strings.get(appState.language, "addFriend"),
        ) {
            Column(Modifier.padding(16.dp)) {
                TextField(value = name, onValueChange = { name = it }, label = Strings.get(appState.language, "friendName"))
                Spacer(Modifier.height(8.dp))
                TextField(value = url, onValueChange = { url = it }, label = Strings.get(appState.language, "friendUrl"))
                Spacer(Modifier.height(8.dp))
                TextField(value = desc, onValueChange = { desc = it }, label = Strings.get(appState.language, "friendDesc"))
                Spacer(Modifier.height(12.dp))
                Row {
                    Spacer(Modifier.weight(1f))
                    Button(onClick = {
                        scope.launch {
                            val resp = api.post<Friend>(
                                "/api/friends",
                                mapOf("name" to name.trim(), "url" to url.trim(), "description" to desc.trim()),
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