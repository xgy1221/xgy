package com.xgy.androidkaraoke.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.xgy.androidkaraoke.BrandConfig
import com.xgy.androidkaraoke.playback.AppViewModel
import com.xgy.androidkaraoke.theme.KaraokeColors
import com.xgy.androidkaraoke.ui.components.FocusSurface
import com.xgy.androidkaraoke.ui.components.ShelfHeader

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(vm: AppViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KaraokeColors.background.copy(alpha = 0.8f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .width(760.dp)
                .background(KaraokeColors.backgroundElevated, RoundedCornerShape(24.dp))
                .padding(32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            ShelfHeader("设置", "NAS 曲库 · 无需登录 · ${BrandConfig.appDisplayName}")
            Text(
                "账号密码仅保存在本机加密存储。曲库入口在此，不占用顶部分类。",
                color = KaraokeColors.textTertiary,
                fontSize = 14.sp,
            )
            SettingField("Base URL", vm.settingsBaseUrl, singleLine = true) { vm.settingsBaseUrl = it }
            SettingField("曲库路径", vm.settingsPath, singleLine = true) { vm.settingsPath = it }
            SettingField("用户名（可选）", vm.settingsUsername, singleLine = true) { vm.settingsUsername = it }
            SettingField(
                "密码（可选）",
                vm.settingsPassword,
                singleLine = true,
                isPassword = true,
                placeholder = if (vm.settingsUsername.isNotEmpty()) "••••••••（留空保留原密码）" else "如需 Basic 认证再填写",
            ) { vm.settingsPassword = it }

            vm.settingsMessage?.let { msg ->
                Text(
                    text = msg,
                    color = if (vm.settingsIsError) KaraokeColors.danger else KaraokeColors.accentSoft,
                    fontSize = 16.sp,
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FocusSurface(
                    onClick = { if (!vm.settingsBusy) vm.saveSettingsAndTest(test = false) },
                    corner = 12.dp,
                ) {
                    Text(
                        if (vm.settingsBusy) "处理中…" else "保存",
                        color = KaraokeColors.textPrimary,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                        fontSize = 17.sp,
                    )
                }
                FocusSurface(
                    onClick = { if (!vm.settingsBusy) vm.saveSettingsAndTest(test = true) },
                    corner = 12.dp,
                ) {
                    Text(
                        "测试连接",
                        color = KaraokeColors.textPrimary,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                        fontSize = 17.sp,
                    )
                }
                FocusSurface(onClick = { vm.openNasLibrary() }, corner = 12.dp) {
                    Text(
                        "浏览曲库",
                        color = KaraokeColors.textPrimary,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                        fontSize = 17.sp,
                    )
                }
                FocusSurface(onClick = { vm.dismissOverlay() }, corner = 12.dp) {
                    Text(
                        "关闭",
                        color = KaraokeColors.textSecondary,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
                        fontSize = 17.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text(
                "目标机：Sony KD-65A9F · Android 10。library.json 可写 mv / original / accompaniment。",
                color = KaraokeColors.textTertiary,
                fontSize = 13.sp,
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingField(
    label: String,
    value: String,
    singleLine: Boolean,
    isPassword: Boolean = false,
    placeholder: String? = null,
    onChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = KaraokeColors.textSecondary, fontSize = 14.sp)
        FocusSurface(onClick = {}, corner = 12.dp) { focused ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (focused) KaraokeColors.surfaceStrong else KaraokeColors.surface,
                        RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                if (value.isEmpty() && placeholder != null) {
                    Text(placeholder, color = KaraokeColors.textTertiary, fontSize = 17.sp)
                }
                BasicTextField(
                    value = value,
                    onValueChange = onChange,
                    singleLine = singleLine,
                    textStyle = TextStyle(color = KaraokeColors.textPrimary, fontSize = 17.sp),
                    cursorBrush = SolidColor(KaraokeColors.textPrimary),
                    visualTransformation = if (isPassword) {
                        PasswordVisualTransformation()
                    } else {
                        VisualTransformation.None
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
