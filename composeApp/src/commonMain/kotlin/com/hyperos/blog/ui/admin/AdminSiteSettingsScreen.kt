package com.hyperos.blog.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.hyperos.blog.i18n.Strings
import com.hyperos.blog.navigation.FaviconManager
import com.hyperos.blog.ui.components.AppButton
import com.hyperos.blog.ui.components.MiuixScaffold
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AdminSiteSettingsScreen(
    appState: AppState,
    onBack: () -> Unit,
    api: ApiClient,
) {
    var title by remember { mutableStateOf(appState.siteTitle) }
    var bio by remember { mutableStateOf(appState.siteBio) }
    var avatar by remember { mutableStateOf(appState.avatarUrl) }
    var favicon by remember { mutableStateOf(appState.faviconUrl) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    MiuixScaffold(
        title = Strings.get(appState.language, "siteSettings"),
        appState = appState,
        currentRoute = null,
        onNavigate = { },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(MiuixIcons.Back, null)
            }
        },
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextField(
                value = title,
                onValueChange = { title = it },
                label = Strings.get(appState.language, "siteName"),
            )
            TextField(
                value = bio,
                onValueChange = { bio = it },
                label = Strings.get(appState.language, "siteBio"),
            )
            TextField(
                value = avatar,
                onValueChange = { avatar = it },
                label = Strings.get(appState.language, "avatarUrl"),
            )
            TextField(
                value = favicon,
                onValueChange = { favicon = it },
                label = Strings.get(appState.language, "faviconUrl"),
            )
            if (message != null) {
                Text(message!!, color = if (message!!.startsWith("OK")) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.error)
            }
            AppButton(
                onClick = {
                    saving = true
                    message = null
                    scope.launch {
                        val resp = api.put<Map<String, Any>>(
                            "/api/settings",
                            mapOf(
                                "title" to title.trim(),
                                "bio" to bio.trim(),
                                "avatar" to avatar.trim(),
                                "favicon" to favicon.trim(),
                            ),
                        )
                        if (resp.ok) {
                            appState.siteTitle = title.trim().ifBlank { appState.siteTitle }
                            appState.siteBio = bio.trim()
                            appState.avatarUrl = avatar.trim()
                            appState.faviconUrl = favicon.trim()
                            FaviconManager.update(favicon.trim())
                            message = "OK saved"
                        } else {
                            message = resp.error ?: "Failed"
                        }
                        saving = false
                    }
                },
                enabled = !saving,
            ) {
                Text(if (saving) Strings.get(appState.language, "loading") else Strings.get(appState.language, "save"))
            }
        }
    }
}
