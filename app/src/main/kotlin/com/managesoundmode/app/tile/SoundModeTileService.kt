package com.managesoundmode.app.tile

import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.graphics.drawable.Icon
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.core.content.ContextCompat
import com.managesoundmode.app.ManageSoundApplication
import com.managesoundmode.app.R
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
    val streams = app.container.soundSystemController.readVolumeStreams()
    val prefs = app.container.preferencesDataSource.prefsFlow.first()
    val profiles = app.container.soundModeRepository.profilesStream.first()
    val activeProfile = profiles.firstOrNull { it.id == prefs.activeProfileId }
    val remainingMin = prefs.activeTimer?.remainingMinutes(System.currentTimeMillis())

    val iconRes = when (mode) {
      SoundMode.NORMAL -> R.drawable.ic_widget_volume_up
      SoundMode.VIBRATE -> R.drawable.ic_widget_vibration
      SoundMode.SILENT -> R.drawable.ic_widget_silent
    }

    tile.state = Tile.STATE_ACTIVE
    tile.icon = Icon.createWithResource(this, iconRes)
    tile.label = activeProfile?.name ?: "Som: ${mode.title}"
    tile.subtitle = when {
      remainingMin != null && remainingMin > 0 -> "${mode.title} • ${remainingMin}m"
      mode == SoundMode.NORMAL -> "Toque ${streams.ringPercent}% • Mídia ${streams.mediaPercent}%"
      else -> "${mode.title} • Mídia ${streams.mediaPercent}%"
    }
    tile.updateTile()
  }

  companion object {
    fun requestAddTile(
      context: Context,
      onResultMessage: (String) -> Unit,
    ) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val statusBarManager = context.getSystemService(StatusBarManager::class.java)
        if (statusBarManager == null) {
          onResultMessage("Abra o lápis (✏️) no topo das notificações para adicionar o bloco.")
          return
        }
        val componentName = ComponentName(context, SoundModeTileService::class.java)
        val icon = Icon.createWithResource(context, R.drawable.ic_qs_sound_mode)
        statusBarManager.requestAddTileService(
          componentName,
          context.getString(R.string.app_name),
          icon,
          ContextCompat.getMainExecutor(context),
        ) { resultCode ->
          val msg = when (resultCode) {
            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ->
              "Bloco adicionado ao topo das notificações!"
            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ->
              "O bloco já está ativo no topo das notificações."
            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED ->
              "Adição cancelada."
            else ->
              "Puxe o topo das notificações e toque no lápis (✏️) para posicionar o bloco."
          }
          onResultMessage(msg)
        }
      } else {
        onResultMessage(
          "Puxe a barra de notificações e toque no lápis (✏️) para adicionar o bloco 'Manage Sound Mode'.",
        )
      }
    }
  }
}
