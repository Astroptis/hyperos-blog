package com.hyperos.blog.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.hyperos.blog.data.Post
import com.hyperos.blog.i18n.Strings
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun PostCard(
    post: Post,
    language: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = if (language == "zh") post.titleZh else (post.titleEn.ifBlank { post.titleZh }),
                style = MiuixTheme.textStyles.title2,
                fontWeight = FontWeight.Bold,
            )
            if (post.summary.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = post.summary,
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 2,
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                SmallTitle(text = post.category.ifBlank { Strings.get(language, "category") })
                Spacer(Modifier.width(8.dp))
                post.tags.take(3).forEach { tag ->
                    SmallTitle(text = "#$tag")
                    Spacer(Modifier.width(6.dp))
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${post.createdAt.take(10)} · ${post.viewCount} ${Strings.get(language, "views")}",
                    style = MiuixTheme.textStyles.footnote2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                )
            }
        }
    }
}
