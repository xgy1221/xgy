package com.xgy.androidkaraoke.ui.songs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.xgy.androidkaraoke.model.Song
import com.xgy.androidkaraoke.theme.KaraokeColors
import com.xgy.androidkaraoke.ui.components.FocusSurface

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SongRow(
    index: Int,
    song: Song,
    onSingNow: () -> Unit,
    onEnqueue: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "%02d".format(index),
            color = KaraokeColors.textTertiary,
            fontSize = 18.sp,
            modifier = Modifier.padding(end = 4.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = KaraokeColors.textPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val tags = buildList {
                add(song.artist)
                if (song.hasMv) add("MV")
                if (song.hasOriginal) add("原唱")
                if (song.hasAccompaniment) add("伴唱")
            }
            Text(
                text = tags.joinToString("  ·  "),
                color = KaraokeColors.textSecondary,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        SongAction("立即唱", emphasized = true, onClick = onSingNow)
        SongAction("点歌", emphasized = false, onClick = onEnqueue)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun SongAction(label: String, emphasized: Boolean, onClick: () -> Unit) {
    FocusSurface(onClick = onClick, corner = 20.dp) { focused ->
        Text(
            text = label,
            color = if (emphasized || focused) KaraokeColors.textPrimary else KaraokeColors.textSecondary,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .background(
                    when {
                        focused -> KaraokeColors.surfaceStrong
                        emphasized -> KaraokeColors.surface
                        else -> KaraokeColors.surface.copy(alpha = 0.4f)
                    },
                    RoundedCornerShape(20.dp),
                )
                .padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}
