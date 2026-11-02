package com.managesoundmode.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class SoundMode(
  val title: String,
  val subtitle: String,
  val restrictivenessRank: Int,
) {
  NORMAL(
    title = "Som Ativo",
    subtitle = "Toque e notificações audíveis",
    restrictivenessRank = 1,
  ),
  VIBRATE(
    title = "Vibrar",
    subtitle = "Silêncio sonoro, alerta tátil",
    restrictivenessRank = 2,
  ),
  SILENT(
    title = "Silencioso",
    subtitle = "Foco total, zero interrupções",
    restrictivenessRank = 3,
  ),
}
