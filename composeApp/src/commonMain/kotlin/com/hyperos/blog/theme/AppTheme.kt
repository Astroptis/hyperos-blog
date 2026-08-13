package com.hyperos.blog.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.TextStyles
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.defaultTextStyles
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

private fun TextStyle.withMiSans(fontFamily: FontFamily): TextStyle = copy(fontFamily = fontFamily)

@Composable
fun AppTheme(
    state: ThemeState,
    content: @Composable () -> Unit,
) {
    val controller = remember(state.mode, state.keyColor) { state.controller() }
    val miSans = MiSansFontFamily()
    val textStyles = remember(miSans) {
        val base = defaultTextStyles()
        TextStyles(
            main = base.main.withMiSans(miSans),
            paragraph = base.paragraph.withMiSans(miSans),
            body1 = base.body1.withMiSans(miSans),
            body2 = base.body2.withMiSans(miSans),
            button = base.button.withMiSans(miSans),
            footnote1 = base.footnote1.withMiSans(miSans),
            footnote2 = base.footnote2.withMiSans(miSans),
            headline1 = base.headline1.withMiSans(miSans),
            headline2 = base.headline2.withMiSans(miSans),
            subtitle = base.subtitle.withMiSans(miSans),
            title1 = base.title1.withMiSans(miSans),
            title2 = base.title2.withMiSans(miSans),
            title3 = base.title3.withMiSans(miSans),
            title4 = base.title4.withMiSans(miSans),
        )
    }
    MiuixTheme(controller = controller, textStyles = textStyles) {
        content()
    }
}