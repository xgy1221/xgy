package com.xgy.androidkaraoke.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.zIndex
import com.xgy.androidkaraoke.theme.KaraokeColors

/**
 * 全屏遮罩：吞掉空白区域的鼠标点击；配合 RootScreen 对背景 `canFocus=false` 防 DPAD 穿透。
 * 仅通过显式关闭 / Back 退出（点空白不关）。
 */
@Composable
fun BlockingOverlay(
    modifier: Modifier = Modifier,
    scrimColor: Color = KaraokeColors.background.copy(alpha = 0.82f),
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    val gate = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        runCatching { gate.requestFocus() }
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .zIndex(10f)
            .background(scrimColor)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { /* 吞掉点穿到首页的点击 */ },
            )
            .focusRequester(gate)
            .focusProperties { canFocus = true }
            .focusable(),
        contentAlignment = contentAlignment,
        content = content,
    )
}

/** 弹层面板：阻止点击落到遮罩空白逻辑，面板内控件正常可点。 */
@Composable
fun ModalPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { /* 面板空白区域不穿透 */ },
            )
            .focusProperties { canFocus = true },
    ) {
        content()
    }
}
