package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TheiaColorScheme = darkColorScheme(
    primary = PurpleNeon,
    onPrimary = Color.White,
    secondary = TealNeon,
    onSecondary = Color.Black,
    tertiary = OrangeNeon,
    onTertiary = Color.Black,
    background = ThemeBg,
    onBackground = TextPrimary,
    surface = ThemeBg,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceColor,
    onSurfaceVariant = TextMuted,
    outline = BorderColor
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TheiaColorScheme,
        typography = Typography,
        content = content
    )
}
