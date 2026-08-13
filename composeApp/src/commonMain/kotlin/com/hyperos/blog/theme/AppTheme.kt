package com.hyperos.blog.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme

enum class ThemeMode(val schemeMode: ColorSchemeMode) {
    System(ColorSchemeMode.System),
    Light(ColorSchemeMode.Light),
    Dark(ColorSchemeMode.Dark),
    MonetLight(ColorSchemeMode.MonetLight),
    MonetDark(ColorSchemeMode.MonetDark),
    MonetSystem(ColorSchemeMode.MonetSystem),
}

class ThemeState {
    var mode by mutableStateOf(ThemeMode.MonetLight)
    var keyColor by mutableStateOf(Color(0xFF3482FF))

    fun controller(): ThemeController {
        return ThemeController(
            colorSchemeMode = mode.schemeMode,
            lightColors = lightColorScheme(),
            darkColors = darkColorScheme(),
            keyColor = keyColor,
            paletteStyle = ThemePaletteStyle.TonalSpot,
        )
    }
}

@Composable
fun AppTheme(
    state: ThemeState,
    content: @Composable () -> Unit,
) {
    val controller = remember(state.mode, state.keyColor) { state.controller() }
    MiuixTheme(controller = controller) {
        content()
    }
}