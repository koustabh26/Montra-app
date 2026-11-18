package com.montra.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object CColors {
    val Bg = Color(0xFF06070D)
    val Surface = Color(0xFF0D0F1C)
    val CardTop = Color(0xFF1C2040)
    val CardBottom = Color(0xFF0F1124)
    val Cyan = Color(0xFF00E5FF)
    val Violet = Color(0xFF8B5CF6)
    val Magenta = Color(0xFFFF3D9A)
    val Lime = Color(0xFF39FFB0)
    val Amber = Color(0xFFFFC53D)
    val Red = Color(0xFFFF4D6D)
    val TextPrimary = Color(0xFFF2F4FF)
    val TextSecondary = Color(0xFF9AA0C3)
    val Divider = Color(0x1AFFFFFF)
}

private val scheme = darkColorScheme(
    primary = CColors.Cyan,
    onPrimary = Color(0xFF00131A),
    secondary = CColors.Violet,
    onSecondary = Color.White,
    tertiary = CColors.Magenta,
    background = CColors.Bg,
    onBackground = CColors.TextPrimary,
    surface = CColors.Surface,
    onSurface = CColors.TextPrimary,
    surfaceVariant = CColors.CardTop,
    onSurfaceVariant = CColors.TextSecondary,
    surfaceContainer = CColors.Surface,
    surfaceContainerHigh = CColors.CardBottom,
    surfaceContainerHighest = CColors.CardTop,
    outline = Color(0x55FFFFFF),
    outlineVariant = CColors.Divider,
    error = CColors.Red
)

@Composable
fun MontraTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = scheme, content = content)
}
