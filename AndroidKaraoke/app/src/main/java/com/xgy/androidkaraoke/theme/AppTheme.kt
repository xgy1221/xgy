package com.xgy.androidkaraoke.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

object KaraokeColors {
    val background = Color(0xFF000000)
    val backgroundElevated = Color(0xFF121212)
    val surface = Color.White.copy(alpha = 0.08f)
    val surfaceStrong = Color.White.copy(alpha = 0.12f)
    val textPrimary = Color.White
    val textSecondary = Color.White.copy(alpha = 0.55f)
    val textTertiary = Color.White.copy(alpha = 0.35f)
    val accent = Color.White
    val accentSoft = Color(0xFF6FA8FF)
    val danger = Color(0xFFD96B6B)
    val focusGlow = Color.White.copy(alpha = 0.35f)
}

object KaraokeDimens {
    val cardRadius = 18.dp
    val contentPadding = 48.dp
    val shelfSpacing = 28.dp
    val focusScale = 1.06f
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun KaraokeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = KaraokeColors.accent,
            onPrimary = Color.Black,
            secondary = KaraokeColors.accentSoft,
            background = KaraokeColors.background,
            onBackground = KaraokeColors.textPrimary,
            surface = KaraokeColors.backgroundElevated,
            onSurface = KaraokeColors.textPrimary,
            border = Color.White.copy(alpha = 0.18f),
        ),
        content = content,
    )
}

@Composable
fun AmbientBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(KaraokeColors.background)
            .background(
                Brush.radialGradient(
                    colors = listOf(Color(0xFF242424), Color.Black),
                    radius = 1200f,
                ),
            ),
    )
}
