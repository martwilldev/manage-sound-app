package com.managesoundmode.app.domain.model

import java.time.DayOfWeek
import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable
enum class Weekday(
  val shortLabel: String,
  val fullLabel: String,
  val isoDayNumber: Int,
) {
  SUN(shortLabel = "D", fullLabel = "Dom", isoDayNumber = 7),
  MON(shortLabel = "S", fullLabel = "Seg", isoDayNumber = 1),
  TUE(shortLabel = "T", fullLabel = "Ter", isoDayNumber = 2),
  WED(shortLabel = "Q", fullLabel = "Qua", isoDayNumber = 3),
  THU(shortLabel = "Q", fullLabel = "Qui", isoDayNumber = 4),
  FRI(shortLabel = "S", fullLabel = "Sex", isoDayNumber = 5),
  SAT(shortLabel = "S", fullLabel = "Sáb", isoDayNumber = 6);

  companion object {
    fun fromDayOfWeek(dayOfWeek: DayOfWeek): Weekday =
      entries.first { it.isoDayNumber == dayOfWeek.value }

    val weekdays: Set<Weekday> = setOf(MON, TUE, WED, THU, FRI)
    val sleepWeeknights: Set<Weekday> = setOf(SUN, MON, TUE, WED, THU)
    val allDays: Set<Weekday> = entries.toSet()
  }
}

@Serializable
data class ScheduleRule(
  val id: String,
  val name: String,
  val startHour: Int,
  val startMinute: Int,
  val endHour: Int,
  val endMinute: Int,
  val daysOfWeek: Set<Weekday>,
  val targetProfileId: String,
  val enabled: Boolean = true,
  val highPriority: Boolean = false,
) {
  init {
    require(startHour in 0..23 && endHour in 0..23) { "Hora deve estar entre 0 e 23" }
    require(startMinute in 0..59 && endMinute in 0..59) { "Minuto deve estar entre 0 e 59" }
  }

  val startMinutesOfDay: Int
    get() = startHour * 60 + startMinute

  val endMinutesOfDay: Int
    get() = endHour * 60 + endMinute

  val crossesMidnight: Boolean
    get() = startMinutesOfDay > endMinutesOfDay

  fun formattedStart(): String = String.format(Locale.ROOT, "%02d:%02d", startHour, startMinute)

  fun formattedEnd(): String = String.format(Locale.ROOT, "%02d:%02d", endHour, endMinute)

  fun formattedWindow(): String = "${formattedStart()} – ${formattedEnd()}"

  fun formattedDaysSummary(): String {
    if (daysOfWeek.size == 7) return "Todos os dias"
    if (daysOfWeek == Weekday.weekdays) return "Seg a Sex"
    if (daysOfWeek == Weekday.sleepWeeknights) return "Dom a Qui"
    if (daysOfWeek == setOf(Weekday.SAT, Weekday.SUN)) return "Fim de semana"
    return Weekday.entries.filter { it in daysOfWeek }.joinToString(", ") { it.fullLabel }
  }

  companion object {
    val defaultSchedules: List<ScheduleRule> = listOf(
      ScheduleRule(
        id = "sched_sleep",
        name = "Dormir",
        startHour = 22,
        startMinute = 30,
        endHour = 7,
        endMinute = 0,
        daysOfWeek = Weekday.sleepWeeknights,
        targetProfileId = "preset_sleep",
        enabled = true,
        highPriority = false,
      ),
      ScheduleRule(
        id = "sched_work",
        name = "Trabalho (Comercial)",
        startHour = 8,
        startMinute = 30,
        endHour = 18,
        endMinute = 0,
        daysOfWeek = Weekday.weekdays,
        targetProfileId = "preset_work_high",
        enabled = true,
        highPriority = false,
      ),
    )
  }
}

@Serializable
data class LocationRule(
  val id: String,
  val name: String,
  val latitude: Double,
  val longitude: Double,
  val radiusMeters: Int = 150,
  val wifiSsid: String = "",
  val targetProfileId: String,
  val enabled: Boolean = true,
  val isCurrentlyInside: Boolean = false,
  val highPriority: Boolean = false,
) {
  init {
    require(radiusMeters in 100..2000) { "Raio deve estar entre 100m e 2000m" }
  }

  companion object {
    val defaultLocations: List<LocationRule> = listOf(
      LocationRule(
        id = "loc_office",
        name = "Escritório",
        latitude = -23.5614,
        longitude = -46.6559,
        radiusMeters = 150,
        wifiSsid = "Corp-5G",
        targetProfileId = "preset_work_high",
        enabled = true,
        isCurrentlyInside = false,
      ),
      LocationRule(
        id = "loc_college",
        name = "Faculdade / Reuniões",
        latitude = -23.5505,
        longitude = -46.6333,
        radiusMeters = 200,
        wifiSsid = "Campus-WiFi",
        targetProfileId = "preset_work",
        enabled = false,
        isCurrentlyInside = false,
      ),
    )
  }
}

@Serializable
enum class ThemeMode(
  val label: String,
) {
  SYSTEM("Sistema"),
  LIGHT("Claro"),
  DARK("Escuro"),
}

@Serializable
enum class RestorePolicy(
  val title: String,
  val subtitle: String,
) {
  DEFAULT_PROFILE(
    title = "Perfil Padrão do Dia",
    subtitle = "Garante retorno ao perfil base (Som Ativo) ao encerrar qualquer período",
  ),
  SNAPSHOT(
    title = "Último Estado Manual (Snapshot)",
    subtitle = "Restaura exatamente o modo e os volumes que estavam antes da regra iniciar",
  ),
}

@Serializable
data class SavedStateSnapshot(
  val mode: SoundMode,
  val streams: VolumeStreams,
  val capturedAtEpochMillis: Long,
)

@Serializable
enum class TransitionTriggerSource(
  val badgeLabel: String,
) {
  MANUAL("Manual"),
  TIMER("Timer"),
  SCHEDULE("Horário"),
  LOCATION("Local"),
  RESTORE("Auto-Restore"),
  BOOT("Sistema / Boot"),
  QUICK_TILE("Atalho Rápido"),
  NOTIFICATION("Notificação"),
  WIDGET("Widget"),
  HARDWARE_BUTTON("Botão de Volume"),
}

@Serializable
data class TransitionLogEntry(
  val id: String,
  val timestampEpochMillis: Long,
  val title: String,
  val detail: String,
  val source: TransitionTriggerSource,
  val appliedMode: SoundMode,
)
