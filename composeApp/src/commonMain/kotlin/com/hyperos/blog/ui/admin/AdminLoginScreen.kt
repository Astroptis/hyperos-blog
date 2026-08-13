package com.hyperos.blog.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import com.hyperos.blog.ui.components.MiuixScaffold
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AdminLoginScreen(
    appState: AppState,
    onLoggedIn: () -> Unit,
    api: ApiClient,
) {
    var password by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    MiuixScaffold(
        title = Strings.get(appState.language, "login"),
        appState = appState,
        currentRoute = null,
        onNavigate = { },
    ) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextField(value = password, onValueChange = { password = it }, label = Strings.get(appState.language, "password"))
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
                            onLoggedIn()
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
}