package com.managesoundmode.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.managesoundmode.app.domain.model.SoundMode

/**
 * CompositionLocal que informa se o tema ZENITH ativo está em modo escuro (`Noite Boreal`)
 * ou claro (`Alvorada Nórdica`), respeitando a preferência do aplicativo.
 */
val LocalZenithIsDark = staticCompositionLocalOf { true }

// ============================================================================
// Paleta Semântica ZENITH — Dark Mode ("Noite Boreal")
// ============================================================================
val ZenithBorealBg = Color(0xFF070A10)
val ZenithBorealCard = Color(0xFF101624)
val ZenithBorealNav = Color(0xFF131B2E)
val ZenithBorealElevated = Color(0xFF161F33)
val ZenithBorealPillInactive = Color(0xFF1E293B)
val ZenithBorealBorder = Color(0xFF1E293B)
val ZenithBorealTextPrimary = Color(0xFFF8FAFC)
val ZenithBorealTextSecondary = Color(0xFF94A3B8)

val ZenithNormalTealDark = Color(0xFF2DD4BF)
val ZenithNormalContainerDark = Color(0xFF0D2E2B)
val ZenithNormalOnContainerDark = Color(0xFF99F6E4)
val ZenithNormalGlowDark = Color(0xFF14B8A6)

val ZenithVibrateAmberDark = Color(0xFFFBBF24)
val ZenithVibrateContainerDark = Color(0xFF36260A)
val ZenithVibrateOnContainerDark = Color(0xFFFDE68A)
val ZenithVibrateGlowDark = Color(0xFFF59E0B)

val ZenithSilentVioletDark = Color(0xFFA78BFA)
val ZenithSilentContainerDark = Color(0xFF231B42)
val ZenithSilentOnContainerDark = Color(0xFFDDD6FE)
val ZenithSilentGlowDark = Color(0xFF8B5CF6)

// ============================================================================
// Paleta Semântica ZENITH — Light Mode ("Alvorada Nórdica")
// ============================================================================
val ZenithNordicBg = Color(0xFFF6F5F0)
val ZenithNordicCard = Color(0xFFFFFFFF)
val ZenithNordicNav = Color(0xFFF0EDE6)
val ZenithNordicElevated = Color(0xFFEAE6DF)
val ZenithNordicPillInactive = Color(0xFFE2DDD5)
val ZenithNordicBorder = Color(0xFFE6E3DC)
val ZenithNordicTextPrimary = Color(0xFF0F172A)
val ZenithNordicTextSecondary = Color(0xFF475569)

val ZenithNormalTealLight = Color(0xFF0F766E)
val ZenithNormalContainerLight = Color(0xFFCCFBF1)
val ZenithNormalOnContainerLight = Color(0xFF115E59)
val ZenithNormalGlowLight = Color(0xFF14B8A6)

val ZenithVibrateAmberLight = Color(0xFFB45309)
val ZenithVibrateContainerLight = Color(0xFFFEF3C7)
val ZenithVibrateOnContainerLight = Color(0xFF92400E)
val ZenithVibrateGlowLight = Color(0xFFD97706)

val ZenithSilentVioletLight = Color(0xFF6D28D9)
val ZenithSilentContainerLight = Color(0xFFEDE9FE)
val ZenithSilentOnContainerLight = Color(0xFF5B21B6)
val ZenithSilentGlowLight = Color(0xFF7C3AED)

// Tokens de compatibilidade
val SoundNormalEmerald = ZenithNormalTealDark
val SoundNormalContainerDark = ZenithNormalContainerDark
val SoundVibrateAmber = ZenithVibrateAmberDark
val SoundVibrateContainerDark = ZenithVibrateContainerDark
val SoundSilentRose = Color(0xFFF87171)
val SoundSilentIndigo = ZenithSilentVioletDark
val SoundSilentContainerDark = ZenithSilentContainerDark
val SoundTimerBlue = Color(0xFF38BDF8)
val SoundTimerContainerDark = Color(0xFF0C2D48)

/**
 * Cor de container semântico ZENITH para cada [SoundMode] (Dark "Noite Boreal" & Light "Alvorada Nórdica").
 */
@Composable
fun SoundMode.tonalContainerColor(): Color {
  val isDark = LocalZenithIsDark.current
  return when (this) {
    SoundMode.NORMAL -> if (isDark) ZenithNormalContainerDark else ZenithNormalContainerLight
    SoundMode.VIBRATE -> if (isDark) ZenithVibrateContainerDark else ZenithVibrateContainerLight
    SoundMode.SILENT -> if (isDark) ZenithSilentContainerDark else ZenithSilentContainerLight
  }
}

/**
 * Cor de texto/ícone sobre o container semântico ZENITH para cada [SoundMode].
 */
@Composable
fun SoundMode.onTonalContainerColor(): Color {
  val isDark = LocalZenithIsDark.current
  return when (this) {
    SoundMode.NORMAL -> if (isDark) ZenithNormalOnContainerDark else ZenithNormalOnContainerLight
    SoundMode.VIBRATE -> if (isDark) ZenithVibrateOnContainerDark else ZenithVibrateOnContainerLight
    SoundMode.SILENT -> if (isDark) ZenithSilentOnContainerDark else ZenithSilentOnContainerLight
  }
}

/**
 * Cor de acento principal ZENITH para cada [SoundMode] (Orbe, pílulas ativas e barras de canal).
 */
@Composable
fun SoundMode.tonalAccentColor(): Color {
  val isDark = LocalZenithIsDark.current
  return when (this) {
    SoundMode.NORMAL -> if (isDark) ZenithNormalTealDark else ZenithNormalTealLight
    SoundMode.VIBRATE -> if (isDark) ZenithVibrateAmberDark else ZenithVibrateAmberLight
    SoundMode.SILENT -> if (isDark) ZenithSilentVioletDark else ZenithSilentVioletLight
  }
}

/**
 * Cor do halo luminoso do Orbe Acústico ZENITH.
 */
@Composable
fun SoundMode.orbGlowColor(): Color {
  val isDark = LocalZenithIsDark.current
  return when (this) {
    SoundMode.NORMAL -> if (isDark) ZenithNormalGlowDark else ZenithNormalGlowLight
    SoundMode.VIBRATE -> if (isDark) ZenithVibrateGlowDark else ZenithVibrateGlowLight
    SoundMode.SILENT -> if (isDark) ZenithSilentGlowDark else ZenithSilentGlowLight
  }
}

/**
 * Cor de contraste sobre o botão/pílula ativa do modo.
 */
@Composable
fun SoundMode.onActivePillColor(): Color {
  val isDark = LocalZenithIsDark.current
  return if (isDark) Color(0xFF070A10) else Color.White
}

fun SoundMode.accentColor(): Color = when (this) {
  SoundMode.NORMAL -> ZenithNormalTealDark
  SoundMode.VIBRATE -> ZenithVibrateAmberDark
  SoundMode.SILENT -> ZenithSilentVioletDark
}

fun SoundMode.containerColor(): Color = when (this) {
  SoundMode.NORMAL -> ZenithNormalContainerDark
  SoundMode.VIBRATE -> ZenithVibrateContainerDark
  SoundMode.SILENT -> ZenithSilentContainerDark
}
