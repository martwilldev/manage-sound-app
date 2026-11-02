package com.managesoundmode.app.ui.theme

import androidx.compose.ui.graphics.Color
import com.managesoundmode.app.domain.model.SoundMode

// Semantic tokens from docs/DESIGN_SYSTEM.md
val SoundNormalEmerald = Color(0xFF10B981)
val SoundNormalContainerDark = Color(0xFF064E3B)

val SoundVibrateAmber = Color(0xFFF59E0B)
val SoundVibrateContainerDark = Color(0xFF78350F)

val SoundSilentRose = Color(0xFFEF4444)
val SoundSilentIndigo = Color(0xFF6366F1)
val SoundSilentContainerDark = Color(0xFF312E81)

val SoundTimerBlue = Color(0xFF3B82F6)
val SoundTimerContainerDark = Color(0xFF1E3A8A)

// Dark-First Surface Palette
val BackgroundDark = Color(0xFF0B0F17)
val SurfaceDark = Color(0xFF111827)
val SurfaceContainerDark = Color(0xFF1F2937)
val SurfaceVariantDark = Color(0xFF374151)
val OnSurfaceDark = Color(0xFFF9FAFB)
val OnSurfaceMutedDark = Color(0xFF9CA3AF)

// Light Surface Palette
val BackgroundLight = Color(0xFFF8FAFC)
val SurfaceLight = Color(0xFFFFFFFF)
val SurfaceContainerLight = Color(0xFFF1F5F9)
val OnSurfaceLight = Color(0xFF0F172A)
val OnSurfaceMutedLight = Color(0xFF475569)

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
