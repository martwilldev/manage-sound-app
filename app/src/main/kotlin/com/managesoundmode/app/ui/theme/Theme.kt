package com.managesoundmode.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
  primary = SoundNormalEmerald,
  onPrimary = Color(0xFF022C22),
  primaryContainer = SoundNormalContainerDark,
  onPrimaryContainer = Color(0xFFD1FAE5),
  secondary = SoundTimerBlue,
  onSecondary = Color.White,
  secondaryContainer = SoundTimerContainerDark,
  onSecondaryContainer = Color(0xFFDBEAFE),
  tertiary = SoundVibrateAmber,
  error = SoundSilentRose,
  background = BackgroundDark,
  onBackground = OnSurfaceDark,
  surface = SurfaceDark,
  onSurface = OnSurfaceDark,
  surfaceVariant = SurfaceContainerDark,
  onSurfaceVariant = OnSurfaceMutedDark,
  outline = SurfaceVariantDark,
)

private val LightColorScheme = lightColorScheme(
  primary = SoundNormalEmerald,
  onPrimary = Color.White,
  secondary = SoundTimerBlue,
  tertiary = SoundVibrateAmber,
  error = SoundSilentRose,
  background = BackgroundLight,
  onBackground = OnSurfaceLight,
  surface = SurfaceLight,
  onSurface = OnSurfaceLight,
  surfaceVariant = SurfaceContainerLight,
  onSurfaceVariant = OnSurfaceMutedLight,
)

@Composable
fun ManageSoundModeTheme(
  // Dark-First by default as defined in docs/DESIGN_SYSTEM.md
  darkTheme: Boolean = true,
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content,
  )
}
