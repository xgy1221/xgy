package com.xgy.androidkaraoke.ui.remote

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.xgy.androidkaraoke.playback.AppViewModel
import com.xgy.androidkaraoke.remote.QrImages
import com.xgy.androidkaraoke.theme.KaraokeColors
import com.xgy.androidkaraoke.ui.components.FocusSurface

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun RemoteScreen(vm: AppViewModel) {
    val url = vm.remoteUrl
    val qr = remember(url) { url?.let { QrImages.create(it, 520) } }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(KaraokeColors.background.copy(alpha = 0.92f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .background(KaraokeColors.backgroundElevated, RoundedCornerShape(28.dp))
                .padding(36.dp),
        ) {
            Text("微信扫码点歌", color = KaraokeColors.textPrimary, fontSize = 32.sp)
            Text(
                "手机和电视连同一个 Wi-Fi。微信扫一扫即可点歌，不用登录、不用关注公众号。",
                color = KaraokeColors.textSecondary,
                fontSize = 16.sp,
                textAlign = TextAlign.Center,
            )
            if (qr != null) {
                Image(
                    bitmap = qr.asImageBitmap(),
                    contentDescription = "点歌二维码",
                    modifier = Modifier
                        .size(280.dp)
                        .background(KaraokeColors.textPrimary, RoundedCornerShape(12.dp))
                        .padding(10.dp),
                )
                Text(url ?: "", color = KaraokeColors.textTertiary, fontSize = 14.sp)
            } else {
                Text("没拿到电视的局域网地址，二维码出不来。", color = KaraokeColors.danger, fontSize = 18.sp)
            }
            FocusSurface(onClick = { vm.dismissOverlay() }, corner = 12.dp) {
                Text(
                    "关闭",
                    color = KaraokeColors.textPrimary,
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp),
                )
            }
        }
    }
}
