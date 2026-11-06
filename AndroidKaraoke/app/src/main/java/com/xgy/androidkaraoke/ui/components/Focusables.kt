package com.xgy.androidkaraoke.ui.components

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.xgy.androidkaraoke.theme.KaraokeColors
import com.xgy.androidkaraoke.theme.KaraokeDimens

/** 弹层打开时，背后的首页按钮设为 false，方向键就不能穿过去。 */
val LocalTvFocusEnabled = compositionLocalOf { true }

@Composable
fun FocusSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    corner: Dp = KaraokeDimens.cardRadius,
    requestFocus: Boolean = false,
    focusRequester: FocusRequester? = null,
    navigateUp: FocusRequester? = null,
    navigateDown: FocusRequester? = null,
    navigateLeft: FocusRequester? = null,
    navigateRight: FocusRequester? = null,
    content: @Composable (focused: Boolean) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val fallback = remember { FocusRequester() }
    val requester = focusRequester ?: fallback
    val tvFocusEnabled = LocalTvFocusEnabled.current
    val shape = RoundedCornerShape(corner)
    LaunchedEffect(requestFocus, tvFocusEnabled) {
        if (requestFocus && tvFocusEnabled) {
            runCatching { requester.requestFocus() }
        }
    }
    Box(
        modifier = modifier
            .focusRequester(requester)
            .onFocusChanged { focused = it.isFocused }
            // 只保留一个焦点。再套 focusable() 时，索尼遥控要按两次才动一格。
            .onKeyEvent { event ->
                if (!event.isConfirmKey()) return@onKeyEvent false
                if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount == 0) {
                    onClick()
                }
                // 吃掉按下和抬起，避免 clickable 再触发一次。
                true
            }
            // focusProperties 必须写在 clickable 前面，否则 canFocus 不生效，焦点会掉进空白。
            .focusProperties {
                canFocus = tvFocusEnabled
                navigateUp?.let { up = it }
                navigateDown?.let { down = it }
                navigateLeft?.let { left = it }
                navigateRight?.let { right = it }
            }
            .clickable(onClick = onClick)
            .clip(shape)
            .border(
                width = if (focused) 3.dp else 0.dp,
                color = if (focused) Color.White else Color.Transparent,
                shape = shape,
            ),
    ) {
        content(focused)
    }
}

private fun androidx.compose.ui.input.key.KeyEvent.isConfirmKey(): Boolean {
    val code = nativeKeyEvent.keyCode
    return code == AndroidKeyEvent.KEYCODE_DPAD_CENTER ||
        code == AndroidKeyEvent.KEYCODE_ENTER ||
        code == AndroidKeyEvent.KEYCODE_NUMPAD_ENTER ||
        code == AndroidKeyEvent.KEYCODE_BUTTON_SELECT ||
        code == AndroidKeyEvent.KEYCODE_BUTTON_A ||
        code == AndroidKeyEvent.KEYCODE_SPACE
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PosterCard(
    title: String,
    subtitle: String? = null,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.MusicNote,
    badge: String? = null,
    focusRequester: FocusRequester? = null,
    navigateUp: FocusRequester? = null,
    navigateDown: FocusRequester? = null,
    navigateLeft: FocusRequester? = null,
    navigateRight: FocusRequester? = null,
) {
    FocusSurface(
        onClick = onClick,
        modifier = modifier,
        focusRequester = focusRequester,
        navigateUp = navigateUp,
        navigateDown = navigateDown,
        navigateLeft = navigateLeft,
        navigateRight = navigateRight,
    ) { focused ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        listOf(
                            tint.copy(alpha = if (focused) 0.95f else 0.75f),
                            tint.copy(alpha = 0.35f).compositeTowardBlack(),
                        ),
                    ),
                ),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.28f),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(12.dp),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
            ) {
                if (badge != null) {
                    Text(
                        text = badge,
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Text(
                    text = title,
                    color = KaraokeColors.textPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = KaraokeColors.textSecondary,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun ShelfHeader(title: String, subtitle: String? = null) {
    Column {
        Text(
            text = title,
            color = KaraokeColors.textPrimary,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )
        if (subtitle != null) {
            Text(
                text = subtitle,
                color = KaraokeColors.textSecondary,
                fontSize = 16.sp,
            )
        }
    }
}

private fun Color.compositeTowardBlack(): Color =
    Color(
        red = red * 0.35f,
        green = green * 0.35f,
        blue = blue * 0.35f,
        alpha = 1f,
    )
