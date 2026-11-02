package com.managesoundmode.app.tile

import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import com.managesoundmode.app.ManageSoundApplication
import com.managesoundmode.app.domain.model.SoundMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SoundModeTileService : TileService() {

  private var serviceScope: CoroutineScope? = null

  override fun onStartListening() {
    super.onStartListening()
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    serviceScope = scope
    scope.launch {
      refreshTileState()
    }
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

  private suspend fun refreshTileState() {
    val tile = qsTile ?: return
    val app = applicationContext as? ManageSoundApplication ?: return
    val mode = app.container.soundSystemController.readCurrentMode()
    val prefs = app.container.preferencesDataSource.prefsFlow.first()
    val profiles = app.container.soundModeRepository.profilesStream.first()
    val activeProfile = profiles.firstOrNull { it.id == prefs.activeProfileId }
    val remainingMin = prefs.activeTimer?.remainingMinutes(System.currentTimeMillis())

    tile.state = Tile.STATE_ACTIVE
    tile.label = activeProfile?.name ?: "Som: ${mode.title}"
    tile.subtitle = when {
      remainingMin != null && remainingMin > 0 -> "${mode.title} • ${remainingMin}m"
      activeProfile != null -> mode.title
      mode == SoundMode.NORMAL -> "Toque Audível"
      mode == SoundMode.VIBRATE -> "Apenas Vibração"
      else -> "Silencioso / Foco"
    }
    tile.updateTile()
  }
}
