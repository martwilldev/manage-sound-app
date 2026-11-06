package com.managesoundmode.app.domain.model

import com.managesoundmode.app.ui.common.AppLabels
import java.time.DayOfWeek
import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable
enum class Weekday(
  val isoDayNumber: Int,
) {
  SUN(isoDayNumber = 7),
  MON(isoDayNumber = 1),
  TUE(isoDayNumber = 2),
  WED(isoDayNumber = 3),
  THU(isoDayNumber = 4),
  FRI(isoDayNumber = 5),
  SAT(isoDayNumber = 6),
  ;

  val shortLabel: String
    get() = AppLabels.Domain.weekdayShortLabel(this)

  val fullLabel: String
    get() = AppLabels.Domain.weekdayFullLabel(this)

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

  fun formattedDaysSummary(): String = AppLabels.Domain.daysSummary(daysOfWeek)

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

  val hasCoordinates: Boolean
    get() = !latitude.isNaN() && !longitude.isNaN() && (latitude != 0.0 || longitude != 0.0)

  fun formattedCoordinates(): String =
    String.format(Locale.ROOT, "%.4f, %.4f", latitude, longitude)

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
enum class ThemeMode {
  SYSTEM,
  LIGHT,
  DARK,
  ;

  val label: String
    get() = AppLabels.Domain.themeModeLabel(this)
}

@Serializable
enum class RestorePolicy {
  DEFAULT_PROFILE,
  SNAPSHOT,
  ;

  val title: String
    get() = AppLabels.Domain.restorePolicyTitle(this)

  val subtitle: String
    get() = AppLabels.Domain.restorePolicySubtitle(this)
}

@Serializable
data class SavedStateSnapshot(
  val mode: SoundMode,
  val streams: VolumeStreams,
  val capturedAtEpochMillis: Long,
)

@Serializable
enum class TransitionTriggerSource {
  MANUAL,
  TIMER,
  SCHEDULE,
  LOCATION,
  RESTORE,
  BOOT,
  QUICK_TILE,
  NOTIFICATION,
  WIDGET,
  HARDWARE_BUTTON,
  ;

  val badgeLabel: String
    get() = AppLabels.Domain.triggerSourceBadge(this)
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
