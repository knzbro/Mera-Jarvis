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

private val LightColorScheme =
  lightColorScheme(
    primary = Color(0xFF00BCD4),     // Bright Cyan Pro
    secondary = Color(0xFF00C853),   // Vivid Green
    tertiary = Color(0xFFAA00FF),    // Vivid Purple
    background = Color(0xFFF8F9FA),  // Crisp White/Gray
    surface = Color(0xFFFFFFFF),     // Pure White Card
    onPrimary = Color(0xFFFFFFFF),   // White text on primary
    onSecondary = Color(0xFFFFFFFF), // White text on secondary
    onTertiary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1E1E24), // High Contrast Dark text
    onSurface = Color(0xFF1E1E24)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false, // Disable dynamic colors to keep pro neon styling
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
