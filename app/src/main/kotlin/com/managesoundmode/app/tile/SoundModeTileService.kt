package com.managesoundmode.app.tile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.managesoundmode.app.ManageSoundApplication
import com.managesoundmode.app.domain.model.SoundMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class SoundModeTileService : TileService() {

  private var serviceScope: CoroutineScope? = null

  override fun onStartListening() {
    super.onStartListening()
    serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    refreshTileState()
  }

  override fun onStopListening() {
    serviceScope?.cancel()
    serviceScope = null
    super.onStopListening()
  }

  override fun onClick() {
    super.onClick()
    val app = applicationContext as? ManageSoundApplication ?: return
    val scope = serviceScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    scope.launch {
      app.container.soundModeRepository.cycleSoundModeFromQuickTile()
      refreshTileState()
    }
  }

  private fun refreshTileState() {
    val tile = qsTile ?: return
    val app = applicationContext as? ManageSoundApplication ?: return
    val mode = app.container.soundSystemController.readCurrentMode()

    tile.state = Tile.STATE_ACTIVE
    tile.label = "Modo: ${mode.title}"
    tile.subtitle = when (mode) {
      SoundMode.NORMAL -> "Toque Audível"
      SoundMode.VIBRATE -> "Apenas Vibração"
      SoundMode.SILENT -> "Silencioso / Foco"
    }
    tile.updateTile()
  }
}
