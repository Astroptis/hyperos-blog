package com.hyperos.blog.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import hyperos_blog.composeapp.generated.resources.MiSans_Regular
import hyperos_blog.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.Font as ResourcesFont

@OptIn(ExperimentalResourceApi::class)
@Composable
fun MiSansFontFamily(): FontFamily {
    return FontFamily(
        ResourcesFont(
            resource = Res.font.MiSans_Regular,
            weight = FontWeight.Normal,
            style = FontStyle.Normal,
            variationSettings = FontVariation.Settings(FontWeight.Normal, FontStyle.Normal),
        ),
    )
}