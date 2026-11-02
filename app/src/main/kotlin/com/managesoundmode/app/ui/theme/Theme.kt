package com.managesoundmode.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.managesoundmode.app.domain.model.ThemeMode

private val PixelExpressiveDarkScheme = darkColorScheme(
  primary = Color(0xFFA8C7FA),
  onPrimary = Color(0xFF062E6F),
  primaryContainer = Color(0xFF1B3A6B),
  onPrimaryContainer = Color(0xFFD6E3FF),
  secondary = Color(0xFFBEC6DC),
  onSecondary = Color(0xFF283041),
  secondaryContainer = Color(0xFF343D52),
  onSecondaryContainer = Color(0xFFDAE2F9),
  tertiary = Color(0xFFE5B8E8),
  onTertiary = Color(0xFF44244A),
  tertiaryContainer = Color(0xFF533359),
  onTertiaryContainer = Color(0xFFFAD7FF),
  error = Color(0xFFFFB4AB),
  errorContainer = Color(0xFF93000A),
  onErrorContainer = Color(0xFFFFDAD6),
  background = Color(0xFF0E131A),
  onBackground = Color(0xFFE2E2E9),
  surface = Color(0xFF0E131A),
  onSurface = Color(0xFFE2E2E9),
  surfaceVariant = Color(0xFF44474E),
  onSurfaceVariant = Color(0xFFC4C6D0),
  surfaceContainerLowest = Color(0xFF090E14),
  surfaceContainerLow = Color(0xFF161B22),
  surfaceContainer = Color(0xFF1A2028),
  surfaceContainerHigh = Color(0xFF252A33),
  surfaceContainerHighest = Color(0xFF30353E),
  outline = Color(0xFF8E9099),
)

private val PixelExpressiveLightScheme = lightColorScheme(
  primary = Color(0xFF1B5E91),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFD6E3FF),
  onPrimaryContainer = Color(0xFF001B3E),
  secondary = Color(0xFF565E71),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFDAE2F9),
  onSecondaryContainer = Color(0xFF131C2B),
  tertiary = Color(0xFF705575),
  onTertiary = Color.White,
  tertiaryContainer = Color(0xFFFAD8FD),
  onTertiaryContainer = Color(0xFF28132E),
  error = Color(0xFFBA1A1A),
  errorContainer = Color(0xFFFFDAD6),
  onErrorContainer = Color(0xFF410002),
  background = Color(0xFFF7F9FF),
  onBackground = Color(0xFF191C20),
  surface = Color(0xFFF7F9FF),
  onSurface = Color(0xFF191C20),
  surfaceVariant = Color(0xFFE0E2EC),
  onSurfaceVariant = Color(0xFF44474E),
  surfaceContainerLowest = Color(0xFFFFFFFF),
  surfaceContainerLow = Color(0xFFF1F4FA),
  surfaceContainer = Color(0xFFEBEEF4),
  surfaceContainerHigh = Color(0xFFE6E8EE),
  surfaceContainerHighest = Color(0xFFE0E2E9),
  outline = Color(0xFF74777F),
)

@Composable
fun ManageSoundModeTheme(
  themeMode: ThemeMode = ThemeMode.DARK,
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val systemDark = isSystemInDarkTheme()
  val isDark = when (themeMode) {
    ThemeMode.SYSTEM -> systemDark
    ThemeMode.DARK -> true
    ThemeMode.LIGHT -> false
  }

  val context = LocalContext.current
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    isDark -> PixelExpressiveDarkScheme
    else -> PixelExpressiveLightScheme
  }

  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window ?: return@SideEffect
      val insetsController = WindowCompat.getInsetsController(window, view)
      insetsController.isAppearanceLightStatusBars = !isDark
      insetsController.isAppearanceLightNavigationBars = !isDark
    }
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content,
  )
}
