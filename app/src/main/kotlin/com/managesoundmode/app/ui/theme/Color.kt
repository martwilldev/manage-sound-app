package com.managesoundmode.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.managesoundmode.app.domain.model.SoundMode

val SoundNormalEmerald = Color(0xFF10B981)
val SoundNormalContainerDark = Color(0xFF064E3B)

val SoundVibrateAmber = Color(0xFFF59E0B)
val SoundVibrateContainerDark = Color(0xFF78350F)

val SoundSilentRose = Color(0xFFEF4444)
val SoundSilentIndigo = Color(0xFF6366F1)
val SoundSilentContainerDark = Color(0xFF312E81)

val SoundTimerBlue = Color(0xFF3B82F6)
val SoundTimerContainerDark = Color(0xFF1E3A8A)

/**
 * Papéis tonais 100% integrados ao Material 3 / Material You (Monet) — tanto em Dark quanto em Light Mode!
 */
@Composable
fun SoundMode.tonalContainerColor(): Color = when (this) {
  SoundMode.NORMAL -> MaterialTheme.colorScheme.primaryContainer
  SoundMode.VIBRATE -> MaterialTheme.colorScheme.tertiaryContainer
  SoundMode.SILENT -> MaterialTheme.colorScheme.secondaryContainer
}

@Composable
fun SoundMode.onTonalContainerColor(): Color = when (this) {
  SoundMode.NORMAL -> MaterialTheme.colorScheme.onPrimaryContainer
  SoundMode.VIBRATE -> MaterialTheme.colorScheme.onTertiaryContainer
  SoundMode.SILENT -> MaterialTheme.colorScheme.onSecondaryContainer
}

@Composable
fun SoundMode.tonalAccentColor(): Color = when (this) {
  SoundMode.NORMAL -> MaterialTheme.colorScheme.primary
  SoundMode.VIBRATE -> MaterialTheme.colorScheme.tertiary
  SoundMode.SILENT -> MaterialTheme.colorScheme.secondary
}

fun SoundMode.accentColor(): Color = when (this) {
  SoundMode.NORMAL -> SoundNormalEmerald
  SoundMode.VIBRATE -> SoundVibrateAmber
  SoundMode.SILENT -> SoundSilentIndigo
}

fun SoundMode.containerColor(): Color = when (this) {
  SoundMode.NORMAL -> SoundNormalContainerDark
  SoundMode.VIBRATE -> SoundVibrateContainerDark
  SoundMode.SILENT -> SoundSilentContainerDark
}
