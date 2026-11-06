package com.managesoundmode.app.domain.model

import com.managesoundmode.app.ui.common.AppLabels
import kotlinx.serialization.Serializable

@Serializable
enum class VolumeStreamType {
  RING,
  NOTIFICATION,
  MEDIA,
  ALARM,
  ;

  val label: String
    get() = AppLabels.Domain.streamTypeLabel(this)
}

@Serializable
data class VolumeStreams(
  val ringPercent: Int = 80,
  val notificationPercent: Int = 75,
  val mediaPercent: Int = 50,
  val alarmPercent: Int = 90,
) {
  init {
    require(ringPercent in 0..100) { "ringPercent must be between 0 and 100" }
    require(notificationPercent in 0..100) { "notificationPercent must be between 0 and 100" }
    require(mediaPercent in 0..100) { "mediaPercent must be between 0 and 100" }
    require(alarmPercent in 0..100) { "alarmPercent must be between 0 and 100" }
  }

  fun withUpdatedStream(type: VolumeStreamType, percent: Int): VolumeStreams {
    val clamped = percent.coerceIn(0, 100)
    return when (type) {
      VolumeStreamType.RING -> copy(ringPercent = clamped)
      VolumeStreamType.NOTIFICATION -> copy(notificationPercent = clamped)
      VolumeStreamType.MEDIA -> copy(mediaPercent = clamped)
      VolumeStreamType.ALARM -> copy(alarmPercent = clamped)
    }
  }
}
