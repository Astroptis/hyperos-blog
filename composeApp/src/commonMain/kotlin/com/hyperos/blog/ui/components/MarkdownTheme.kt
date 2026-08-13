package com.hyperos.blog.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextLinkStyles
import com.mikepenz.markdown.model.DefaultMarkdownColors
import com.mikepenz.markdown.model.DefaultMarkdownTypography
import com.mikepenz.markdown.model.MarkdownColors
import com.mikepenz.markdown.model.MarkdownTypography
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun blogMarkdownColors(): MarkdownColors {
    val cs = MiuixTheme.colorScheme
    return DefaultMarkdownColors(
        text = cs.onBackground,
        codeBackground = cs.surfaceVariant,
        inlineCodeBackground = cs.surfaceVariant,
        dividerColor = cs.dividerLine,
        tableBackground = cs.surfaceVariant,
    )
}

@Composable
fun blogMarkdownTypography(): MarkdownTypography {
    val ts = MiuixTheme.textStyles
    val link = TextLinkStyles(
        style = ts.subtitle.copy(color = MiuixTheme.colorScheme.primary).toSpanStyle(),
    )
    return DefaultMarkdownTypography(
        h1 = ts.title1,
        h2 = ts.title2,
        h3 = ts.title3,
        h4 = ts.title4,
        h5 = ts.body1,
        h6 = ts.body1,
        text = ts.main,
        code = ts.body1,
        inlineCode = ts.body1,
        quote = ts.body1,
        paragraph = ts.body1,
        ordered = ts.body1,
        bullet = ts.body1,
        list = ts.body1,
        textLink = link,
        table = ts.body1,
    )
}