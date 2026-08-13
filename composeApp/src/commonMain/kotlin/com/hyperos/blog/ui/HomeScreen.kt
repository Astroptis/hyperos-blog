package com.hyperos.blog.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperos.blog.AppState
import com.hyperos.blog.data.ApiClient
import com.hyperos.blog.data.SiteStats
import com.hyperos.blog.i18n.Strings
import com.hyperos.blog.navigation.AppRoute
import com.hyperos.blog.ui.components.MiuixScaffold
import com.hyperos.blog.ui.components.RemoteImage
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Search
import top.yukonga.miuix.kmp.theme.MiuixTheme
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun HomeScreen(
    appState: AppState,
    api: ApiClient,
    onNavigate: (AppRoute) -> Unit,
) {
    var stats by remember { mutableStateOf<SiteStats?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        val resp = api.get<SiteStats>("/api/stats")
        if (resp.ok) stats = resp.data
    }

    MiuixScaffold(
        title = appState.siteTitle,
        appState = appState,
        currentRoute = AppRoute.Home,
        onNavigate = onNavigate,
        topBarActions = {
            IconButton(onClick = { onNavigate(AppRoute.Search) }) {
                Icon(MiuixIcons.Search, null)
            }
        },
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
                    .widthIn(max = 600.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Spacer(Modifier.height(12.dp))
                RemoteImage(
                    url = appState.avatarUrl,
                    contentDescription = "avatar",
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape),
                )
                Text(
                    text = appState.siteTitle,
                    style = MiuixTheme.textStyles.title1,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = appState.siteBio,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatBox(Strings.get(appState.language, "postCount"), stats?.postCount?.toString() ?: "-", Modifier.weight(1f))
                    StatBox(Strings.get(appState.language, "commentCount"), stats?.commentCount?.toString() ?: "-", Modifier.weight(1f))
                    StatBox(Strings.get(appState.language, "viewCount"), stats?.viewCount?.toString() ?: "-", Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun StatBox(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(value, style = MiuixTheme.textStyles.title1, fontWeight = FontWeight.Bold)
            Text(label, style = MiuixTheme.textStyles.footnote2, color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
    }
}
