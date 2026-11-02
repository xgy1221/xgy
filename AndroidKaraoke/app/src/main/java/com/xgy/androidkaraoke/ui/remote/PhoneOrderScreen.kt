package com.xgy.androidkaraoke.ui.remote

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.xgy.androidkaraoke.playback.AppViewModel
import com.xgy.androidkaraoke.remote.QrBitmap
import com.xgy.androidkaraoke.theme.KaraokeColors
import com.xgy.androidkaraoke.ui.components.FocusSurface
import com.xgy.androidkaraoke.ui.components.ShelfHeader

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun PhoneOrderScreen(vm: AppViewModel) {
    val url = vm.phoneOrderUrl
    val qr: Bitmap? = remember(url) {
        url?.let { QrBitmap.encode(it, 640) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KaraokeColors.background.copy(alpha = 0.82f)),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .width(900.dp)
                .background(KaraokeColors.backgroundElevated, RoundedCornerShape(24.dp))
                .padding(36.dp),
            horizontalArrangement = Arrangement.spacedBy(36.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(320.dp)
                    .background(KaraokeColors.textPrimary, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (qr != null) {
                    Image(
                        bitmap = qr.asImageBitmap(),
                        contentDescription = "手机点歌二维码",
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Text("等待局域网地址…", color = KaraokeColors.background, fontSize = 18.sp)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                ShelfHeader("手机点歌", "微信扫一扫或系统相机打开链接")
                Text(
                    "手机与电视连同一 Wi‑Fi，打开页面即可搜索点歌，加入电视「已点」队列。无需登录、无需微信开放平台。",
                    color = KaraokeColors.textSecondary,
                    fontSize = 16.sp,
                )
                Text(
                    text = url ?: "未获取到 IP（请检查 Wi‑Fi）",
                    color = KaraokeColors.accentSoft,
                    fontSize = 18.sp,
                )
                Text(
                    "服务状态：${if (vm.phoneServerRunning) "已开启" else "未开启"} · 曲库 ${vm.catalog.size} 首",
                    color = KaraokeColors.textTertiary,
                    fontSize = 14.sp,
                )
                FocusSurface(onClick = { vm.openPhoneOrder() }, corner = 12.dp) {
                    Text(
                        "刷新二维码",
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
    }
}
