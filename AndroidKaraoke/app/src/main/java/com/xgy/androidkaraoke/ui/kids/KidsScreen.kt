package com.xgy.androidkaraoke.ui.kids

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.xgy.androidkaraoke.data.MockData
import com.xgy.androidkaraoke.playback.AppViewModel
import com.xgy.androidkaraoke.ui.components.PosterCard
import com.xgy.androidkaraoke.ui.components.ShelfHeader

@Composable
fun KidsScreen(vm: AppViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 40.dp)) {
            ShelfHeader("儿童", "适合亲子一起唱的内容")
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            items(MockData.kidsPosters, key = { it.id }) { card ->
                PosterCard(
                    title = card.title,
                    subtitle = card.subtitle,
                    tint = card.tint,
                    onClick = { vm.showToast(card.title) },
                    modifier = Modifier.width(160.dp).height(220.dp),
                )
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(MockData.kidsBrands, key = { it.id }) { card ->
                PosterCard(
                    title = card.title,
                    tint = card.tint,
                    onClick = { vm.showToast(card.title) },
                    modifier = Modifier.width(180.dp).height(100.dp),
                )
            }
        }
    }
}
