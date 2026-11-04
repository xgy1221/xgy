package com.xgy.androidkaraoke.ui.playback

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.xgy.androidkaraoke.lyrics.LyricsDocument
import com.xgy.androidkaraoke.theme.KaraokeColors

/**
 * 主流 K 歌式滚动歌词：当前行放大高亮 + 行内进度条（唱到哪）。
 * 无歌词时不渲染（由调用方控制），避免假歌词。
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun LyricsOverlay(
    doc: LyricsDocument,
    currentIndex: Int,
    lineProgress: Float,
    modifier: Modifier = Modifier,
) {
    val window = remember(doc, currentIndex) {
        val start = (currentIndex - 2).coerceAtLeast(0)
        val end = (currentIndex + 3).coerceAtMost(doc.lines.lastIndex)
        if (doc.lines.isEmpty()) emptyList()
        else (start..end).map { it to doc.lines[it] }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 72.dp)
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.35f),
                        Color.Black.copy(alpha = 0.45f),
                        Color.Transparent,
                    ),
                ),
                RoundedCornerShape(16.dp),
            )
            .padding(vertical = 18.dp, horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.widthIn(max = 980.dp),
        ) {
            if (currentIndex < 0 && doc.lines.isNotEmpty()) {
                Text(
                    text = doc.lines.first().text,
                    color = KaraokeColors.textTertiary,
                    fontSize = 22.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            window.forEach { (index, line) ->
                val isCurrent = index == currentIndex
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = line.text,
                        color = when {
                            isCurrent -> Color.White
                            index < currentIndex -> KaraokeColors.textTertiary
                            else -> KaraokeColors.textSecondary
                        },
                        fontSize = if (isCurrent) 34.sp else 22.sp,
                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (isCurrent) {
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .fillMaxWidth(0.55f)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White.copy(alpha = 0.18f)),
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(lineProgress.coerceIn(0f, 1f))
                                    .height(3.dp)
                                    .background(KaraokeColors.accentSoft.copy(alpha = 0.95f)),
                            )
                        }
                    }
                }
            }
        }
    }
}
