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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.managesoundmode.app.domain.model.ThemeMode

private val ZenithBorealDarkScheme = darkColorScheme(
  primary = ZenithNormalTealDark,
  onPrimary = Color(0xFF042F2E),
  primaryContainer = ZenithNormalContainerDark,
  onPrimaryContainer = ZenithNormalOnContainerDark,
  secondary = ZenithSilentVioletDark,
  onSecondary = Color(0xFF1E1B4B),
  secondaryContainer = ZenithSilentContainerDark,
  onSecondaryContainer = ZenithSilentOnContainerDark,
  tertiary = ZenithVibrateAmberDark,
  onTertiary = Color(0xFF451A03),
  tertiaryContainer = ZenithVibrateContainerDark,
  onTertiaryContainer = ZenithVibrateOnContainerDark,
  error = Color(0xFFF87171),
  onError = Color(0xFF450A0A),
  errorContainer = Color(0xFF3B1219),
  onErrorContainer = Color(0xFFFECACA),
  background = ZenithBorealBg,
  onBackground = ZenithBorealTextPrimary,
  surface = ZenithBorealBg,
  onSurface = ZenithBorealTextPrimary,
  surfaceVariant = ZenithBorealElevated,
  onSurfaceVariant = ZenithBorealTextSecondary,
  surfaceContainerLowest = ZenithBorealBg,
  surfaceContainerLow = ZenithBorealCard,
  surfaceContainer = ZenithBorealNav,
  surfaceContainerHigh = ZenithBorealElevated,
  surfaceContainerHighest = ZenithBorealPillInactive,
  outline = Color(0xFF334155),
  outlineVariant = ZenithBorealBorder,
)

private val ZenithNordicLightScheme = lightColorScheme(
  primary = ZenithNormalTealLight,
  onPrimary = Color.White,
  primaryContainer = ZenithNormalContainerLight,
  onPrimaryContainer = ZenithNormalOnContainerLight,
  secondary = ZenithSilentVioletLight,
  onSecondary = Color.White,
  secondaryContainer = ZenithSilentContainerLight,
  onSecondaryContainer = ZenithSilentOnContainerLight,
  tertiary = ZenithVibrateAmberLight,
  onTertiary = Color.White,
  tertiaryContainer = ZenithVibrateContainerLight,
  onTertiaryContainer = ZenithVibrateOnContainerLight,
  error = Color(0xFFDC2626),
  onError = Color.White,
  errorContainer = Color(0xFFFEE2E2),
  onErrorContainer = Color(0xFF7F1D1D),
  background = ZenithNordicBg,
  onBackground = ZenithNordicTextPrimary,
  surface = ZenithNordicBg,
  onSurface = ZenithNordicTextPrimary,
  surfaceVariant = ZenithNordicElevated,
  onSurfaceVariant = ZenithNordicTextSecondary,
  surfaceContainerLowest = ZenithNordicBg,
  surfaceContainerLow = ZenithNordicCard,
  surfaceContainer = ZenithNordicNav,
  surfaceContainerHigh = ZenithNordicElevated,
  surfaceContainerHighest = ZenithNordicPillInactive,
  outline = Color(0xFFCBD5E1),
  outlineVariant = ZenithNordicBorder,
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
  val baseZenithScheme = if (isDark) ZenithBorealDarkScheme else ZenithNordicLightScheme

  // Mantém a identidade atmosférica ZENITH ("Noite Boreal" / "Alvorada Nórdica") mesmo quando
  // Dynamic Color está ativo, ancorando todas as superfícies e acentos semânticos do Orbe.
  val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
    val dynamic = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    baseZenithScheme.copy(
      inversePrimary = dynamic.inversePrimary,
    )
  } else {
    baseZenithScheme
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

  CompositionLocalProvider(LocalZenithIsDark provides isDark) {
    MaterialTheme(
      colorScheme = colorScheme,
      typography = Typography,
      content = content,
    )
  }
}
