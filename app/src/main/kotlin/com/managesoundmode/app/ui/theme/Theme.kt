package com.managesoundmode.app.ui.theme

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

private val SystemExpressiveDarkScheme = darkColorScheme(
  primary = Color(0xFFA8C7FA),
  onPrimary = Color(0xFF062E6F),
  primaryContainer = Color(0xFF0842A0),
  onPrimaryContainer = Color(0xFFD3E3FD),
  secondary = Color(0xFFC2C9BD),
  onSecondary = Color(0xFF2C332A),
  secondaryContainer = Color(0xFF3E4A3D),
  onSecondaryContainer = Color(0xFFDEE5D8),
  tertiary = SoundVibrateAmber,
  onTertiary = Color(0xFF422C00),
  tertiaryContainer = Color(0xFF5E4100),
  onTertiaryContainer = Color(0xFFFFDEA6),
  error = SoundSilentRose,
  errorContainer = Color(0xFF93000A),
  onErrorContainer = Color(0xFFFFDAD6),
  background = Color(0xFF0E1318),
  onBackground = Color(0xFFE1E2E8),
  surface = Color(0xFF111418),
  onSurface = Color(0xFFE1E2E8),
  surfaceVariant = Color(0xFF43474E),
  onSurfaceVariant = Color(0xFFC3C7CF),
  surfaceContainerLowest = Color(0xFF0B0E13),
  surfaceContainerLow = Color(0xFF171C22),
  surfaceContainer = Color(0xFF1D2228),
  surfaceContainerHigh = Color(0xFF272C33),
  surfaceContainerHighest = Color(0xFF32373E),
  outline = Color(0xFF8D9199),
)

private val SystemExpressiveLightScheme = lightColorScheme(
  primary = Color(0xFF0B57D0),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFD3E3FD),
  onPrimaryContainer = Color(0xFF041E49),
  secondary = Color(0xFF535F70),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFD7E3F7),
  onSecondaryContainer = Color(0xFF101C2B),
  tertiary = Color(0xFF7B5800),
  onTertiary = Color.White,
  tertiaryContainer = Color(0xFFFFDEA6),
  onTertiaryContainer = Color(0xFF271900),
  error = Color(0xFFBA1A1A),
  background = Color(0xFFF8F9FA),
  onBackground = Color(0xFF191C20),
  surface = Color(0xFFF8F9FA),
  onSurface = Color(0xFF191C20),
  surfaceVariant = Color(0xFFDFE2EB),
  onSurfaceVariant = Color(0xFF43474E),
  surfaceContainerLowest = Color(0xFFFFFFFF),
  surfaceContainerLow = Color(0xFFF2F4F7),
  surfaceContainer = Color(0xFFECEEF1),
  surfaceContainerHigh = Color(0xFFE6E8EC),
  surfaceContainerHighest = Color(0xFFE1E2E6),
)

@Composable
fun ManageSoundModeTheme(
  darkTheme: Boolean = isSystemInDarkTheme().let { true }, // Dark-First por padrão (estilo Google Clock)
  dynamicColor: Boolean = true,
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current
  val colorScheme = when {
    dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }
    darkTheme -> SystemExpressiveDarkScheme
    else -> SystemExpressiveLightScheme
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content,
  )
}
