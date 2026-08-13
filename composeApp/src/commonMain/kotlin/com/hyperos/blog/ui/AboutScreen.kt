package com.hyperos.blog.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyperos.blog.AppState
import com.hyperos.blog.i18n.Strings
import com.hyperos.blog.navigation.AppRoute
import com.hyperos.blog.ui.components.MiuixScaffold
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AboutScreen(
    appState: AppState,
    onNavigate: (AppRoute) -> Unit,
) {
    MiuixScaffold(
        title = Strings.get(appState.language, "about"),
        appState = appState,
        currentRoute = AppRoute.About,
        onNavigate = onNavigate,
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        Strings.get(appState.language, "welcome"),
                        style = MiuixTheme.textStyles.title1,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        if (appState.language == "zh") "用 Compose Multiplatform + Miuix 构建的个人博客。" else "A personal blog built with Compose Multiplatform + Miuix.",
                        style = MiuixTheme.textStyles.body1,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        if (appState.language == "zh") "基于 Cloudflare Workers + D1 + KV，支持主题切换与多语言。" else "Powered by Cloudflare Workers + D1 + KV, with theming and i18n.",
                        style = MiuixTheme.textStyles.body1,
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(20.dp)) {
                    SmallTitle(text = if (appState.language == "zh") "技术栈" else "Tech Stack")
                    Spacer(Modifier.height(8.dp))
                    listOf(
                        "Kotlin + Compose Multiplatform (wasmJs)",
                        "Miuix UI (HyperOS 风格)",
                        "Cloudflare Workers + D1 + KV",
                    ).forEach { tech ->
                        Row {
                            Text("·  ", color = MiuixTheme.colorScheme.primary)
                            Text(tech, style = MiuixTheme.textStyles.body1)
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            }
        }
    }
}