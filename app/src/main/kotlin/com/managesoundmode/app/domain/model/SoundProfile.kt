package com.managesoundmode.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class SoundProfile(
  val id: String,
  val name: String,
  val description: String,
  val mode: SoundMode,
  val streams: VolumeStreams,
  val isPreset: Boolean = true,
  val iconKey: String = "tune",
  val preserveMediaIfPlaying: Boolean = true,
  val skipMediaOnBluetooth: Boolean = true,
) {
  fun summaryLabel(): String = when (mode) {
    SoundMode.NORMAL -> "Toque ${streams.ringPercent}% • Mídia ${streams.mediaPercent}%"
    SoundMode.VIBRATE -> "Vibrar • Mídia ${streams.mediaPercent}%"
    SoundMode.SILENT -> "Silencioso • Alarme ${streams.alarmPercent}%"
  }

  companion object {
    const val DEFAULT_DAY_PROFILE_ID = "preset_default_day"

    val defaultPresets: List<SoundProfile> = listOf(
      SoundProfile(
        id = DEFAULT_DAY_PROFILE_ID,
        name = "Padrão (Dia)",
        description = "Equilibrado para o dia a dia",
        mode = SoundMode.NORMAL,
        iconKey = "sunny",
        streams = VolumeStreams(
          ringPercent = 80,
          notificationPercent = 75,
          mediaPercent = 50,
          alarmPercent = 90,
        ),
      ),
      SoundProfile(
        id = "preset_work_high",
        name = "Trabalho",
        description = "Toque alto e mídia zerada",
        mode = SoundMode.NORMAL,
        iconKey = "work",
        streams = VolumeStreams(
          ringPercent = 100,
          notificationPercent = 45,
          mediaPercent = 0,
          alarmPercent = 90,
        ),
      ),
      SoundProfile(
        id = "preset_work",
        name = "Reunião",
        description = "Apenas vibração discreta",
        mode = SoundMode.VIBRATE,
        iconKey = "vibration",
        streams = VolumeStreams(
          ringPercent = 0,
          notificationPercent = 0,
          mediaPercent = 0,
          alarmPercent = 85,
        ),
      ),
      SoundProfile(
        id = "preset_sleep",
        name = "Dormir",
        description = "Silêncio com alarme em 100%",
        mode = SoundMode.SILENT,
        iconKey = "bedtime",
        streams = VolumeStreams(
          ringPercent = 0,
          notificationPercent = 0,
          mediaPercent = 0,
          alarmPercent = 100,
        ),
      ),
      SoundProfile(
        id = "preset_home",
        name = "Casa",
        description = "Som ativo para chamadas e mídia",
        mode = SoundMode.NORMAL,
        iconKey = "home",
        streams = VolumeStreams(
          ringPercent = 85,
          notificationPercent = 80,
          mediaPercent = 60,
          alarmPercent = 90,
        ),
      ),
      SoundProfile(
        id = "preset_meeting",
        name = "Foco Total",
        description = "Zero interrupções sonoras",
        mode = SoundMode.SILENT,
        iconKey = "shield",
        streams = VolumeStreams(
          ringPercent = 0,
          notificationPercent = 0,
          mediaPercent = 0,
          alarmPercent = 75,
        ),
      ),
    )
  }
}
