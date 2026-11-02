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
) {
  companion object {
    val defaultPresets: List<SoundProfile> = listOf(
      SoundProfile(
        id = "preset_home",
        name = "Casa / Disponível",
        description = "Som ativo para chamadas, mensagens e mídia",
        mode = SoundMode.NORMAL,
        streams = VolumeStreams(
          ringPercent = 85,
          notificationPercent = 80,
          mediaPercent = 60,
          alarmPercent = 90,
        ),
      ),
      SoundProfile(
        id = "preset_work",
        name = "Trabalho / Escritório",
        description = "Apenas vibração para chamadas e alertas discretos",
        mode = SoundMode.VIBRATE,
        streams = VolumeStreams(
          ringPercent = 0,
          notificationPercent = 0,
          mediaPercent = 30,
          alarmPercent = 85,
        ),
      ),
      SoundProfile(
        id = "preset_meeting",
        name = "Reunião / Foco",
        description = "Silêncio absoluto sem vibração ou mídia",
        mode = SoundMode.SILENT,
        streams = VolumeStreams(
          ringPercent = 0,
          notificationPercent = 0,
          mediaPercent = 0,
          alarmPercent = 75,
        ),
      ),
      SoundProfile(
        id = "preset_sleep",
        name = "Sono / Madrugada",
        description = "Silencioso total preservando apenas o despertador",
        mode = SoundMode.SILENT,
        streams = VolumeStreams(
          ringPercent = 0,
          notificationPercent = 0,
          mediaPercent = 0,
          alarmPercent = 100,
        ),
      ),
    )
  }
}
