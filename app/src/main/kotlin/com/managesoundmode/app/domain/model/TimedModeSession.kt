package com.managesoundmode.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class TimedModeSession(
  val targetMode: SoundMode,
  val previousMode: SoundMode,
  val previousStreams: VolumeStreams,
  val durationMinutes: Int,
  val startedAtEpochMillis: Long,
  val endsAtEpochMillis: Long,
) {
  fun remainingMinutes(nowEpochMillis: Long): Int {
    val diffMillis = (endsAtEpochMillis - nowEpochMillis).coerceAtLeast(0L)
    return ((diffMillis + 59_999L) / 60_000L).toInt()
  }
}

data class DeviceSoundSnapshot(
  val currentMode: SoundMode,
  val streams: VolumeStreams,
  val hasDndPermission: Boolean,
  val canScheduleExactAlarms: Boolean = true,
  val hasLocationPermission: Boolean = false,
  val hasBackgroundLocationPermission: Boolean = false,
  val hasNotificationPermission: Boolean = true,
  val isMusicActive: Boolean = false,
  val isBluetoothHeadsetConnected: Boolean = false,
  val activeProfileId: String? = null,
  val activeTimer: TimedModeSession? = null,
  val activeRuleTitle: String? = null,
  val activeRuleSubtitle: String? = null,
  val activeRuleSessionKey: String? = null,
  val isManuallyOverridden: Boolean = false,
)
