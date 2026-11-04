package com.xgy.androidkaraoke.ui.root

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.xgy.androidkaraoke.model.MainTab
import com.xgy.androidkaraoke.theme.KaraokeColors
import com.xgy.androidkaraoke.ui.components.FocusSurface

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CategoryTabs(
    selected: MainTab,
    onSelect: (MainTab) -> Unit,
) {
    // 仅保留：随心唱 / 歌星
    val tabs = listOf(MainTab.FreeSing, MainTab.Singers)
    LazyRow(
        contentPadding = PaddingValues(horizontal = 40.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(tabs) { tab ->
            val active = tab == selected
            FocusSurface(onClick = { onSelect(tab) }, corner = 10.dp) { focused ->
                Box(
                    modifier = Modifier
                        .height(44.dp)
                        .padding(horizontal = 18.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = tab.title,
                        color = when {
                            focused || active -> KaraokeColors.textPrimary
                            else -> KaraokeColors.textSecondary
                        },
                        fontSize = if (active) 22.sp else 18.sp,
                        fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}
