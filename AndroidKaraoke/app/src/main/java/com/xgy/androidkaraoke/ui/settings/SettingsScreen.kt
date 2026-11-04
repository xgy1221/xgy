package com.xgy.androidkaraoke.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
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
import androidx.tv.material3.Icon
import androidx.tv.material3.Text
import com.xgy.androidkaraoke.BrandConfig
import com.xgy.androidkaraoke.library.LibrarySource
import com.xgy.androidkaraoke.library.LibrarySourceKind
import com.xgy.androidkaraoke.playback.AppViewModel
import com.xgy.androidkaraoke.theme.KaraokeColors
import com.xgy.androidkaraoke.ui.components.BlockingOverlay
import com.xgy.androidkaraoke.ui.components.FocusSurface
import com.xgy.androidkaraoke.ui.components.ModalPanel
import com.xgy.androidkaraoke.ui.components.ShelfHeader

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(vm: AppViewModel) {
    BlockingOverlay {
        ModalPanel(
            modifier = Modifier
                .width(820.dp)
                .height(560.dp)
                .background(KaraokeColors.backgroundElevated, RoundedCornerShape(24.dp))
                .padding(28.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (vm.settingsEditorKind) {
                    null -> SourceListPane(vm)
                    LibrarySourceKind.Nas -> when (vm.nasWizardStep) {
                        AppViewModel.NasWizardStep.Connect -> NasConnectPane(vm)
                        AppViewModel.NasWizardStep.PickDirectory -> NasDirPickerPane(vm)
                    }
                    LibrarySourceKind.Usb -> UsbEditorPane(vm)
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SourceListPane(vm: AppViewModel) {
    ShelfHeader("设置", "添加曲库 · NAS / U 盘 · ${BrandConfig.appDisplayName}")
    Text(
        "先连接 NAS，再浏览目录选择曲库根；也可扫描已插入的 U 盘。",
        color = KaraokeColors.textTertiary,
        fontSize = 14.sp,
    )

    vm.librarySources.forEach { source ->
        SourceRow(vm, source)
    }

    if (vm.librarySources.isEmpty()) {
        Text("暂无曲库，请添加 NAS 或扫描 U 盘。", color = KaraokeColors.textSecondary, fontSize = 16.sp)
    }

    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        FocusSurface(onClick = { vm.beginAddNas() }, corner = 12.dp) {
            Text(
                "添加曲库（NAS）",
                color = KaraokeColors.textPrimary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                fontSize = 16.sp,
            )
        }
        FocusSurface(onClick = { vm.scanUsbNow() }, corner = 12.dp) {
            Text(
                "扫描 U 盘",
                color = KaraokeColors.textPrimary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                fontSize = 16.sp,
            )
        }
        FocusSurface(onClick = { vm.dismissOverlay() }, corner = 12.dp) {
            Text(
                "关闭",
                color = KaraokeColors.textSecondary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                fontSize = 16.sp,
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SourceRow(vm: AppViewModel, source: LibrarySource) {
    val kindLabel = when (source.kind) {
        LibrarySourceKind.Nas -> "NAS"
        LibrarySourceKind.Usb -> "U盘"
    }
    val status = when {
        source.kind == LibrarySourceKind.Usb && source.localRootPath == null -> "已拔出"
        !source.enabled -> "已禁用"
        else -> "已启用"
    }
    val detail = when (source.kind) {
        LibrarySourceKind.Nas -> "${source.nasBaseUrl.orEmpty()}  ${source.nasLibraryPath.orEmpty()}"
        LibrarySourceKind.Usb -> {
            val root = source.localRootPath ?: "（未挂载）"
            val rel = source.relativeMusicPath.ifBlank { "根目录" }
            "$root · 曲库子路径：$rel"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(KaraokeColors.surface, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("[$kindLabel]", color = KaraokeColors.accentSoft, fontSize = 13.sp)
            Text(source.displayName, color = KaraokeColors.textPrimary, fontSize = 18.sp)
            Text(status, color = KaraokeColors.textTertiary, fontSize = 13.sp)
        }
        Text(detail, color = KaraokeColors.textSecondary, fontSize = 13.sp, maxLines = 2)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FocusSurface(onClick = { vm.openLibrary(source) }, corner = 10.dp) {
                Text(
                    "浏览",
                    color = KaraokeColors.textPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    fontSize = 15.sp,
                )
            }
            FocusSurface(onClick = { vm.beginEditSource(source) }, corner = 10.dp) {
                Text(
                    "编辑",
                    color = KaraokeColors.textPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    fontSize = 15.sp,
                )
            }
            FocusSurface(
                onClick = { vm.setSourceEnabled(source.id, !source.enabled) },
                corner = 10.dp,
            ) {
                Text(
                    if (source.enabled) "禁用" else "启用",
                    color = KaraokeColors.textPrimary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    fontSize = 15.sp,
                )
            }
            if (source.kind == LibrarySourceKind.Nas || source.localRootPath == null) {
                FocusSurface(onClick = { vm.removeSource(source.id) }, corner = 10.dp) {
                    Text(
                        "删除",
                        color = KaraokeColors.danger,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        fontSize = 15.sp,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NasConnectPane(vm: AppViewModel) {
    ShelfHeader(
        if (vm.editingSourceId == null) "添加曲库 · 连接 NAS" else "编辑 NAS · 重新连接",
        "群晖 DSM（如 :9527）请填账号密码；连接后选 File Station 目录（如 /volume1/music）",
    )
    SettingField("显示名称", vm.settingsDisplayName, singleLine = true) { vm.settingsDisplayName = it }
    SettingField("Base URL", vm.settingsBaseUrl, singleLine = true) { vm.settingsBaseUrl = it }
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
            onClick = { if (!vm.settingsBusy) vm.connectNasThenPickDirectory() },
            corner = 12.dp,
        ) {
            Text(
                if (vm.settingsBusy) "连接中…" else "连接并选择目录",
                color = KaraokeColors.textPrimary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                fontSize = 16.sp,
            )
        }
        FocusSurface(onClick = { vm.cancelSourceEditor() }, corner = 12.dp) {
            Text(
                "返回列表",
                color = KaraokeColors.textSecondary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                fontSize = 16.sp,
            )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun NasDirPickerPane(vm: AppViewModel) {
    ShelfHeader("选择曲库目录", "当前：${vm.dirPickPath}")
    Text(
        "进入文件夹浏览，确认后将该路径设为曲库根（MV / 原唱 / 伴唱）。",
        color = KaraokeColors.textTertiary,
        fontSize = 14.sp,
    )

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        FocusSurface(onClick = { vm.dirPickerGoUp() }, corner = 10.dp) {
            Text(
                "上级",
                color = KaraokeColors.textPrimary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                fontSize = 15.sp,
            )
        }
        FocusSurface(onClick = { vm.refreshDirPicker() }, corner = 10.dp) {
            Text(
                "刷新",
                color = KaraokeColors.textPrimary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                fontSize = 15.sp,
            )
        }
        FocusSurface(
            onClick = { if (!vm.settingsBusy) vm.confirmNasLibraryDirectory() },
            corner = 10.dp,
        ) {
            Text(
                if (vm.settingsBusy) "保存中…" else "选为曲库根目录",
                color = KaraokeColors.textPrimary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                fontSize = 15.sp,
            )
        }
        FocusSurface(onClick = { vm.backToNasConnect() }, corner = 10.dp) {
            Text(
                "返回连接",
                color = KaraokeColors.textSecondary,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                fontSize = 15.sp,
            )
        }
    }

    when {
        vm.dirPickLoading -> Text("正在列出目录…", color = KaraokeColors.textSecondary, fontSize = 16.sp)
        vm.dirPickError != null -> Text(vm.dirPickError.orEmpty(), color = KaraokeColors.danger, fontSize = 16.sp)
        vm.dirPickFolders.isEmpty() -> Text(
            "此路径下没有子文件夹。可直接「选为曲库根目录」。",
            color = KaraokeColors.textSecondary,
            fontSize = 15.sp,
        )
        else -> {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                vm.dirPickFolders.forEach { folder ->
                    FocusSurface(onClick = { vm.dirPickerEnter(folder.path) }, corner = 10.dp) { focused ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (focused) KaraokeColors.surfaceStrong else KaraokeColors.surface,
                                    RoundedCornerShape(10.dp),
                                )
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Icon(
                                Icons.Outlined.Folder,
                                contentDescription = null,
                                tint = KaraokeColors.textSecondary,
                            )
                            Text(folder.name, color = KaraokeColors.textPrimary, fontSize = 16.sp)
                        }
                    }
                }
            }
        }
    }

    vm.settingsMessage?.let { msg ->
        Text(
            text = msg,
            color = if (vm.settingsIsError) KaraokeColors.danger else KaraokeColors.accentSoft,
            fontSize = 15.sp,
        )
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun UsbEditorPane(vm: AppViewModel) {
    ShelfHeader("编辑 U 盘来源", "可指定曲库子目录（如 music / Karaoke）")
    SettingField("显示名称", vm.settingsDisplayName, singleLine = true) { vm.settingsDisplayName = it }
    SettingField(
        "曲库子路径（相对 U 盘根）",
        vm.settingsRelativeMusicPath,
        singleLine = true,
        placeholder = "留空 = U 盘根目录",
    ) { vm.settingsRelativeMusicPath = it }

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
                "保存",
                color = KaraokeColors.textPrimary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                fontSize = 16.sp,
            )
        }
        FocusSurface(onClick = { vm.cancelSourceEditor() }, corner = 12.dp) {
            Text(
                "返回列表",
                color = KaraokeColors.textSecondary,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                fontSize = 16.sp,
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
