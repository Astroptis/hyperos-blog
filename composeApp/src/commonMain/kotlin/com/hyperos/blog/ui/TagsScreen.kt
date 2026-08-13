package com.hyperos.blog.ui

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyperos.blog.AppState
import com.hyperos.blog.data.ApiClient
import com.hyperos.blog.data.TagCount
import com.hyperos.blog.i18n.Strings
import com.hyperos.blog.navigation.AppRoute
import com.hyperos.blog.ui.components.MiuixScaffold
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun TagsScreen(
    appState: AppState,
    onNavigate: (AppRoute) -> Unit,
    api: ApiClient,
) {
    var tags by remember { mutableStateOf<List<TagCount>>(emptyList()) }

    LaunchedEffect(Unit) {
        val resp = api.get<List<TagCount>>("/api/tags")
        if (resp.ok) tags = resp.data ?: emptyList()
    }

    MiuixScaffold(
        title = Strings.get(appState.language, "tags"),
        appState = appState,
        currentRoute = AppRoute.Home,
        onNavigate = onNavigate,
    ) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
            items(tags, key = { it.name }) { tag ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(14.dp)) {
                        Text(
                            "#${tag.name}",
                            style = MiuixTheme.textStyles.body1,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "${tag.count}",
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}