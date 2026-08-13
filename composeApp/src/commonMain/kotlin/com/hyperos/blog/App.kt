package com.hyperos.blog

import androidx.compose.runtime.Composable
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

@Composable
fun App() {
    MiuixTheme(colors = lightColorScheme()) {
        // 占位：后续替换为真实界面
        Text("HyperOS Blog")
    }
}