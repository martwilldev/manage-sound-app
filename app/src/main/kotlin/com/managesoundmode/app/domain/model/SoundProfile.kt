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
  val preserveMediaIfPlaying: Boolean = true,
  val skipMediaOnBluetooth: Boolean = true,
) {
  companion object {
    const val DEFAULT_DAY_PROFILE_ID = "preset_default_day"

    val defaultPresets: List<SoundProfile> = listOf(
      SoundProfile(
        id = DEFAULT_DAY_PROFILE_ID,
        name = "Padrão (Dia)",
        description = "Perfil base fora de períodos agendados (Som ativo e equilibrado)",
        mode = SoundMode.NORMAL,
        streams = VolumeStreams(
          ringPercent = 80,
          notificationPercent = 75,
          mediaPercent = 50,
          alarmPercent = 90,
        ),
      ),
      SoundProfile(
        id = "preset_work_high",
        name = "Trabalho (Volume Alto)",
        description = "Garante toque em 100% para chamadas e zera mídia acidental",
        mode = SoundMode.NORMAL,
        streams = VolumeStreams(
          ringPercent = 100,
          notificationPercent = 45,
          mediaPercent = 0,
          alarmPercent = 90,
        ),
      ),
      SoundProfile(
        id = "preset_work",
        name = "Reunião / Vibrar",
        description = "Apenas vibração tátil para chamadas e alertas discretos",
        mode = SoundMode.VIBRATE,
        streams = VolumeStreams(
          ringPercent = 0,
          notificationPercent = 0,
          mediaPercent = 0,
          alarmPercent = 85,
        ),
      ),
      SoundProfile(
        id = "preset_sleep",
        name = "Dormir / Madrugada",
        description = "Silencioso total preservando apenas o despertador em 100%",
        mode = SoundMode.SILENT,
        streams = VolumeStreams(
          ringPercent = 0,
          notificationPercent = 0,
          mediaPercent = 0,
          alarmPercent = 100,
        ),
      ),
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
        id = "preset_meeting",
        name = "Foco Absoluto",
        description = "Silêncio absoluto sem vibração ou mídia",
        mode = SoundMode.SILENT,
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
