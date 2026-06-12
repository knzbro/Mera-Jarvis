package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = Color(0xFF00E5FF),     // Electric Cyan Glow
    secondary = Color(0xFF00FF87),   // Cyber Lime Green
    tertiary = Color(0xFFD500F9),    // Laser Purple
    background = Color(0xFF050505),  // Cosmic Dark Black
    surface = Color(0xFF0D0D11),     // Clean Dark Gray/Slate Card
    onPrimary = Color(0xFF000000),   // High Contrast Black Typography on primary
    onSecondary = Color(0xFF000000), // High Contrast Black Typography on secondary
    onTertiary = Color(0xFFFFFFFF),
    onBackground = Color(0xFFECEFF1), // Soft White/Gray text
    onSurface = Color(0xFFECEFF1)
  )

private val LightColorScheme = DarkColorScheme // Keep Jarvis dark cyber theme unified always to prevent eye strain and preserve premium neon branding

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark mode aesthetic by default
  dynamicColor: Boolean = false, // Disable dynamic wallpaper colors to protect our premium cyber styling
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
