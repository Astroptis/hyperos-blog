package com.hyperos.blog.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hyperos.blog.AppState
import com.hyperos.blog.i18n.Strings
import com.hyperos.blog.navigation.AppRoute
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Album
import top.yukonga.miuix.kmp.icon.extended.Community
import top.yukonga.miuix.kmp.icon.extended.Contacts
import top.yukonga.miuix.kmp.icon.extended.Filter
import top.yukonga.miuix.kmp.icon.extended.Home
import top.yukonga.miuix.kmp.squircle.squircleBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme

private data class SideNavItem(val route: AppRoute, val label: String, val icon: ImageVector)

@Composable
fun MiuixScaffold(
    title: String,
    appState: AppState,
    currentRoute: AppRoute?,
    onNavigate: (AppRoute) -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    topBarActions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = title,
                navigationIcon = navigationIcon,
                actions = topBarActions,
            )
        },
        content = { innerPadding ->
            Box(
                Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
            ) {
                content(innerPadding)
                if (currentRoute != null) {
                    HoverSideNavigationBar(
                        appState = appState,
                        currentRoute = currentRoute,
                        onNavigate = onNavigate,
                    )
                }
            }
        },
    )
}

@Composable
private fun BoxScope.HoverSideNavigationBar(
    appState: AppState,
    currentRoute: AppRoute,
    onNavigate: (AppRoute) -> Unit,
) {
    val items = listOf(
        SideNavItem(AppRoute.Home, Strings.get(appState.language, "home"), MiuixIcons.Home),
        SideNavItem(AppRoute.Archive, Strings.get(appState.language, "archive"), MiuixIcons.Album),
        SideNavItem(AppRoute.Messages, Strings.get(appState.language, "messages"), MiuixIcons.Community),
        SideNavItem(AppRoute.Friends, Strings.get(appState.language, "friends"), MiuixIcons.Contacts),
        SideNavItem(AppRoute.Settings, Strings.get(appState.language, "settings"), MiuixIcons.Filter),
    )
    val selectedIndex = items.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

    var isHovered by remember { mutableStateOf(false) }
    val expandedWidth = 132.dp
    val collapsedWidth = 52.dp
    val barWidth by animateDpAsState(
        targetValue = if (isHovered) expandedWidth else collapsedWidth,
        animationSpec = tween(durationMillis = 200),
        label = "sideNavBarWidth",
    )
    val cornerRadius by animateDpAsState(
        targetValue = if (isHovered) 28.dp else 26.dp,
        animationSpec = tween(durationMillis = 200),
        label = "sideNavCornerRadius",
    )

    Column(
        modifier = Modifier
            .align(Alignment.CenterStart)
            .padding(start = 16.dp)
            .width(barWidth)
            .dropShadow(
                    shape = RoundedCornerShape(cornerRadius),
                    shadow = Shadow(
                        radius = 16.dp,
                        color = Color.Black,
                        alpha = 0.18f,
                    ),
                )
                .squircleBackground(
                    color = MiuixTheme.colorScheme.surfaceContainer,
                    cornerRadius = cornerRadius,
                )
                .pointerInput(Unit) {
                    var inside = false
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            val pos = event.changes.firstOrNull()?.position
                            if (pos != null) {
                                val now = pos.x >= 0f && pos.y >= 0f && pos.x <= size.width && pos.y <= size.height
                                if (now != inside) {
                                    inside = now
                                    isHovered = now
                                }
                            }
                        }
                    }
                }
                .padding(vertical = 10.dp),
        ) {
            items.forEachIndexed { index, item ->
                SideNavItemRow(
                    item = item,
                    selected = index == selectedIndex,
                    expanded = isHovered,
                    onClick = { onNavigate(item.route) },
                )
            }
        }
}

@Composable
private fun SideNavItemRow(
    item: SideNavItem,
    selected: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (selected) {
        MiuixTheme.colorScheme.onSurfaceContainer
    } else {
        MiuixTheme.colorScheme.onSurfaceContainer.copy(alpha = 0.4f)
    }
    val itemCornerRadius = 22.dp
    Row(
        modifier = Modifier
            .height(48.dp)
            .padding(horizontal = 6.dp)
            .squircleBackground(
                color = if (selected) {
                    MiuixTheme.colorScheme.surfaceContainerHighest
                } else {
                    Color.Transparent
                },
                cornerRadius = itemCornerRadius,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (expanded) Arrangement.Start else Arrangement.Center,
    ) {
        Image(
            modifier = Modifier.size(26.dp),
            imageVector = item.icon,
            contentDescription = item.label,
            colorFilter = ColorFilter.tint(tint),
        )
        AnimatedVisibility(
            visible = expanded,
            enter = expandHorizontally() + fadeIn(),
            exit = shrinkHorizontally() + fadeOut(),
        ) {
            Text(
                text = item.label,
                modifier = Modifier
                    .padding(start = 8.dp),
                fontSize = 14.sp,
                color = tint,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            )
        }
        Spacer(Modifier.weight(1f))
    }
}