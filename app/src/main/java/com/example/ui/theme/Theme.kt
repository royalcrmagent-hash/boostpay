package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = BoostPrimary,
    secondary = BoostSecondary,
    tertiary = BoostAccent,
    background = DarkBackground,
    surface = SurfaceDark,
    surfaceVariant = Color(0xFF1E2838),
    onPrimary = TextWhite,
    onSecondary = TextWhite,
    onBackground = TextWhite,
    onSurface = TextWhite,
    onSurfaceVariant = TextLightGray,
    outline = Color(0xFF374151),
    outlineVariant = Color(0xFF1F2937)
  )

private val LightColorScheme =
  lightColorScheme(
    primary = BoostPrimary,
    secondary = BoostSecondary,
    tertiary = BoostAccent,
    background = LightBackground,
    surface = SurfaceLight,
    surfaceVariant = Color(0xFFF3F4F6),
    onPrimary = TextWhite,
    onSecondary = TextWhite,
    onBackground = TextDarkGray,
    onSurface = TextDarkGray,
    onSurfaceVariant = Color(0xFF6B7280),
    outline = Color(0xFFD1D5DB),
    outlineVariant = Color(0xFFE5E7EB)
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  // Dynamic color is available on Android 12+
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
