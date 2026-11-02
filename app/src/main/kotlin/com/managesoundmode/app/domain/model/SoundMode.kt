package com.managesoundmode.app.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class SoundMode(
  val title: String,
  val subtitle: String,
) {
  NORMAL(
    title = "Normal",
    subtitle = "Toque e notificações audíveis",
  ),
  VIBRATE(
    title = "Vibrar",
    subtitle = "Silêncio sonoro, alerta tátil",
  ),
  SILENT(
    title = "Silencioso",
    subtitle = "Foco total, zero interrupções",
  ),
}
