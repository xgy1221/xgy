package com.xgy.androidkaraoke.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.xgy.androidkaraoke.playback.AppViewModel
import com.xgy.androidkaraoke.theme.KaraokeColors
import com.xgy.androidkaraoke.ui.components.FocusSurface

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(vm: AppViewModel) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KaraokeColors.background.copy(alpha = 0.88f))
            .padding(36.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(KaraokeColors.backgroundElevated, RoundedCornerShape(24.dp))
                .verticalScroll(rememberScrollState())
                .padding(28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("曲库设置", color = KaraokeColors.textPrimary, fontSize = 28.sp)
            Text(
                "点歌不用登录。这里只填 NAS 账号，用来读取家里的曲库。可以添加多个曲库。",
                color = KaraokeColors.textSecondary,
                fontSize = 15.sp,
            )
            Text(
                "每首歌一个文件夹：MV（mp4/mkv）、文件名带「原唱」的音源、文件名带「伴唱」或「伴奏」的音源。例如 周杰伦/青花瓷/青花瓷.mp4、青花瓷-原唱.mp3、青花瓷-伴唱.mp3。",
                color = KaraokeColors.textTertiary,
                fontSize = 14.sp,
            )
            Text(vm.statusMessage, color = KaraokeColors.accentSoft, fontSize = 15.sp)
            vm.libraries.forEach { library ->
                FocusSurface(onClick = { vm.selectLibrary(library) }, corner = 12.dp) { focused ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (focused || vm.editingId == library.id) KaraokeColors.surfaceStrong else KaraokeColors.surface, RoundedCornerShape(12.dp))
                            .padding(14.dp),
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(library.name, color = KaraokeColors.textPrimary, fontSize = 18.sp)
                            Text("${library.baseUrl}${library.path}", color = KaraokeColors.textTertiary, fontSize = 13.sp)
                        }
                        FocusSurface(onClick = { vm.removeLibrary(library.id) }, corner = 8.dp) {
                            Text("删除", color = KaraokeColors.textSecondary, modifier = Modifier.padding(8.dp), fontSize = 14.sp)
                        }
                    }
                }
            }
            SettingField("名称", vm.formName) { vm.formName = it }
            SettingField("NAS 地址", vm.formUrl) { vm.formUrl = it }
            SettingField("曲库路径", vm.formPath) { vm.formPath = it }
            SettingField("NAS 账号", vm.formUser) { vm.formUser = it }
            SettingField("NAS 密码", vm.formPass, password = true, placeholder = "留空则不改已保存的密码") { vm.formPass = it }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ActionButton(if (vm.settingsBusy) "扫描中…" else "保存并扫描") { if (!vm.settingsBusy) vm.saveLibrary() }
                ActionButton("重新扫描") { if (!vm.settingsBusy) vm.rescan() }
                ActionButton("再添加一个曲库") { if (!vm.settingsBusy) vm.addLibrary() }
                ActionButton("关闭") { vm.dismissOverlay() }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun ActionButton(label: String, onClick: () -> Unit) {
    FocusSurface(onClick = onClick, corner = 12.dp) {
        Text(label, color = KaraokeColors.textPrimary, modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp), fontSize = 16.sp)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SettingField(
    label: String,
    value: String,
    password: Boolean = false,
    placeholder: String? = null,
    onChange: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = KaraokeColors.textSecondary, fontSize = 14.sp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(KaraokeColors.surface, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            if (value.isEmpty() && placeholder != null) {
                Text(placeholder, color = KaraokeColors.textTertiary, fontSize = 16.sp)
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(color = KaraokeColors.textPrimary, fontSize = 18.sp),
                cursorBrush = SolidColor(KaraokeColors.textPrimary),
                visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
