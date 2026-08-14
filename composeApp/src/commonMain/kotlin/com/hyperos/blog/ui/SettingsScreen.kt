package com.hyperos.blog.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.hyperos.blog.data.AuthResponse
import com.hyperos.blog.i18n.Strings
import com.hyperos.blog.navigation.AppRoute
import com.hyperos.blog.theme.ThemeMode
import com.hyperos.blog.ui.components.MiuixScaffold
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.ColorPicker
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.preference.SliderPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun SettingsScreen(
    appState: AppState,
    onNavigate: (AppRoute) -> Unit,
    api: com.hyperos.blog.data.ApiClient,
) {
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    if (!appState.isAdmin) {
        MiuixScaffold(
            title = Strings.get(appState.language, "settings"),
            appState = appState,
            currentRoute = AppRoute.Settings,
            onNavigate = onNavigate,
        ) {
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    label = Strings.get(appState.language, "password"),
                )
                if (error != null) {
                    Text(error!!, color = MiuixTheme.colorScheme.error)
                }
                Button(
                    onClick = {
                        loading = true
                        error = null
                        scope.launch {
                            val resp = api.post<AuthResponse>(
                                "/api/auth/login",
                                mapOf("username" to "admin", "password" to password),
                            )
                            if (resp.ok && resp.data != null) {
                                appState.adminToken = resp.data.token
                                api.setToken(resp.data.token)
                            } else {
                                error = resp.error
                            }
                            loading = false
                        }
                    },
                    enabled = !loading,
                ) {
                    Text(if (loading) Strings.get(appState.language, "loading") else Strings.get(appState.language, "login"))
                }
            }
        }
        return
    }

    var showThemeDialog by remember { mutableStateOf(false) }
    var showColorDialog by remember { mutableStateOf(false) }
    var showLanguageDialog by remember { mutableStateOf(false) }

    MiuixScaffold(
        title = Strings.get(appState.language, "settings"),
        appState = appState,
        currentRoute = AppRoute.Settings,
        onNavigate = onNavigate,
    ) {
        Column(Modifier.fillMaxSize()) {
            Card(Modifier.fillMaxWidth()) {
                Column {
                    RadioButtonPreference(
                        title = Strings.get(appState.language, "themeColor"),
                        selected = false,
                        onClick = { showColorDialog = true },
                    )
                    HorizontalDivider()
                    ArrowPreference(
                        title = Strings.get(appState.language, "language"),
                        summary = if (appState.language == "zh") "简体中文" else "English",
                        onClick = { showLanguageDialog = true },
                    )
                    HorizontalDivider()
                    RadioButtonPreference(
                        title = Strings.get(appState.language, "darkMode"),
                        selected = false,
                        onClick = { showThemeDialog = true },
                    )
                    HorizontalDivider()
                    SwitchPreference(
                        title = Strings.get(appState.language, "darkMode"),
                        checked = appState.themeState.mode == ThemeMode.Dark,
                        onCheckedChange = { checked ->
                            appState.themeState.mode = if (checked) ThemeMode.Dark else ThemeMode.Light
                        },
                    )
                    HorizontalDivider()
                    SliderPreference(
                        value = 1f,
                        onValueChange = { },
                        title = if (appState.language == "zh") "字号（暂不可调）" else "Font size (coming soon)",
                        valueText = "1.0x",
                    )
                    HorizontalDivider()
                    ArrowPreference(
                        title = Strings.get(appState.language, "admin"),
                        onClick = { onNavigate(AppRoute.Admin) },
                    )
                }
            }
        }
    }

    if (showThemeDialog) {
        OverlayDialog(
            show = showThemeDialog,
            onDismissRequest = { showThemeDialog = false },
            title = if (appState.language == "zh") "主题模式" else "Theme mode",
        ) {
            Column(Modifier.padding(16.dp)) {
                ThemeMode.entries.forEach { mode ->
                    val label = when (mode) {
                        ThemeMode.System -> Strings.get(appState.language, "followSystem")
                        ThemeMode.Light -> Strings.get(appState.language, "light")
                        ThemeMode.Dark -> Strings.get(appState.language, "dark")
                        else -> mode.name
                    }
                    RadioButtonPreference(
                        title = label,
                        selected = appState.themeState.mode == mode,
                        onClick = {
                            appState.themeState.mode = mode
                            showThemeDialog = false
                        },
                    )
                }
            }
        }
    }

    if (showLanguageDialog) {
        OverlayDialog(
            show = showLanguageDialog,
            onDismissRequest = { showLanguageDialog = false },
            title = Strings.get(appState.language, "language"),
        ) {
            Column(Modifier.padding(16.dp)) {
                RadioButtonPreference(
                    title = "简体中文",
                    selected = appState.language == "zh",
                    onClick = { appState.language = "zh"; showLanguageDialog = false },
                )
                RadioButtonPreference(
                    title = "English",
                    selected = appState.language == "en",
                    onClick = { appState.language = "en"; showLanguageDialog = false },
                )
            }
        }
    }

    if (showColorDialog) {
        OverlayDialog(
            show = showColorDialog,
            onDismissRequest = { showColorDialog = false },
            title = Strings.get(appState.language, "themeColor"),
        ) {
            Column(Modifier.padding(16.dp)) {
                ColorPicker(
                    color = appState.themeState.keyColor,
                    onColorChanged = { appState.themeState.keyColor = it },
                )
            }
        }
    }
}