package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF003644),
    onPrimaryContainer = Color(0xFFA6EEFF),
    secondary = EmeraldGreen,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF003924),
    onSecondaryContainer = Color(0xFF8CF8C4),
    tertiary = GoldAccent,
    onTertiary = Color.Black,
    background = ObsidianBlack,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    outline = BorderDark,
    error = CoralRed,
    onError = Color.White
)

private val LightColorScheme = DarkColorScheme // KeySight 3D is crafted as an immersive dark concert workstation

@Composable
fun KeySightTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
