package com.hyperos.blog.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun AppButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    cornerRadius: Dp = 16.dp,
    buttonHeight: Dp = 48.dp,
    expandWidth: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val containerColor = if (enabled) MiuixTheme.colorScheme.secondaryVariant else MiuixTheme.colorScheme.disabledSecondaryVariant
    val contentColor = if (enabled) MiuixTheme.colorScheme.onSecondaryVariant else MiuixTheme.colorScheme.disabledOnSecondaryVariant
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Row(
            modifier = modifier
                .height(buttonHeight)
                .then(if (expandWidth) Modifier.fillMaxWidth() else Modifier)
                .padding(horizontal = 20.dp)
                .squircleBackground(color = containerColor, cornerRadius = cornerRadius)
                .clickable(enabled = enabled, onClick = onClick),
            horizontalArrangement = if (expandWidth) Arrangement.Center else Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}