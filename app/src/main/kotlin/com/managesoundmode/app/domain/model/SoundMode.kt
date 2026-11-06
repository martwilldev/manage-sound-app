package com.managesoundmode.app.domain.model

import com.managesoundmode.app.ui.common.AppLabels
import kotlinx.serialization.Serializable

@Serializable
enum class SoundMode(
  val restrictivenessRank: Int,
) {
  NORMAL(restrictivenessRank = 1),
  VIBRATE(restrictivenessRank = 2),
  SILENT(restrictivenessRank = 3),
  ;

  val title: String
    get() = AppLabels.Domain.soundModeTitle(this)

  val subtitle: String
    get() = AppLabels.Domain.soundModeSubtitle(this)
}
