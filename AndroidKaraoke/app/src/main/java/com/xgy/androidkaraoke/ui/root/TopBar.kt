package com.xgy.androidkaraoke.ui.root

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.QueueMusic
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.xgy.androidkaraoke.BrandConfig
import com.xgy.androidkaraoke.theme.KaraokeColors
import com.xgy.androidkaraoke.ui.components.FocusSurface

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TopBar(
    queueCount: Int,
    onSearch: () -> Unit,
    onQueue: () -> Unit,
    onWechat: () -> Unit,
    onSettings: () -> Unit,
    topFocus: FocusRequester,
    navigateDown: FocusRequester,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = BrandConfig.logoTitle,
            color = KaraokeColors.textPrimary,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = BrandConfig.tagline,
            color = KaraokeColors.textTertiary,
            fontSize = 14.sp,
        )
        Spacer(Modifier.weight(1f))
        val queueFocus = remember { FocusRequester() }
        val wechatFocus = remember { FocusRequester() }
        val settingsFocus = remember { FocusRequester() }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TopAction("搜索", Icons.Outlined.Search, onSearch, topFocus, null, queueFocus, navigateDown)
            TopAction(
                "已点${if (queueCount > 0) " · $queueCount" else ""}",
                Icons.Outlined.QueueMusic,
                onQueue,
                queueFocus,
                topFocus,
                wechatFocus,
                navigateDown,
            )
            TopAction("扫码点歌", Icons.Outlined.PhoneAndroid, onWechat, wechatFocus, queueFocus, settingsFocus, navigateDown)
            TopAction("设置", Icons.Outlined.Settings, onSettings, settingsFocus, wechatFocus, null, navigateDown)
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun TopAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    focusRequester: FocusRequester,
    navigateLeft: FocusRequester?,
    navigateRight: FocusRequester?,
    navigateDown: FocusRequester,
) {
    FocusSurface(
        onClick = onClick,
        corner = 12.dp,
        focusRequester = focusRequester,
        navigateLeft = navigateLeft,
        navigateRight = navigateRight,
        navigateDown = navigateDown,
    ) { focused ->
        Row(
            modifier = Modifier
                .height(44.dp)
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (focused) KaraokeColors.textPrimary else KaraokeColors.textSecondary,
                modifier = Modifier.size(20.dp),
            )
            Text(
                text = label,
                color = if (focused) KaraokeColors.textPrimary else KaraokeColors.textSecondary,
                fontSize = 15.sp,
            )
        }
    }
}
