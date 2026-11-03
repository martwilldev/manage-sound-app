package com.managesoundmode.app.tile

import android.app.PendingIntent
import android.app.StatusBarManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.provider.Settings
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import com.managesoundmode.app.ManageSoundApplication
import com.managesoundmode.app.R
import com.managesoundmode.app.domain.model.SoundMode
import com.managesoundmode.app.domain.model.TransitionTriggerSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

enum class QuickTileKind(
  val serviceClass: Class<out TileService>,
  val title: String,
  val subtitle: String,
  @param:DrawableRes val iconRes: Int,
) {
  CYCLE(
    serviceClass = SoundModeTileService::class.java,
    title = "Modo de Som (Ciclo)",
    subtitle = "1 único bloco que alterna Som → Vibrar → Silêncio",
    iconRes = R.drawable.ic_qs_sound_mode,
  ),
  MODE_NORMAL(
    serviceClass = NormalModeTileService::class.java,
    title = "Modo: Som",
    subtitle = "Fica aceso em Som Normal • 1 toque ativa o som",
    iconRes = R.drawable.ic_widget_volume_up,
  ),
  MODE_VIBRATE(
    serviceClass = VibrateModeTileService::class.java,
    title = "Modo: Vibrar",
    subtitle = "Fica aceso em Vibrar • Toque ativa ou volta para Som",
    iconRes = R.drawable.ic_widget_vibration,
  ),
  MODE_SILENT(
    serviceClass = SilentModeTileService::class.java,
    title = "Modo: Silêncio",
    subtitle = "Fica aceso em Silencioso • Toque ativa ou volta para Som",
    iconRes = R.drawable.ic_widget_silent,
  ),
  PROFILE_SLOT_1(
    serviceClass = ProfileSlot1TileService::class.java,
    title = "Perfil Rápido 1",
    subtitle = "Ativa os 4 volumes e modo do Perfil 1 em 1 toque",
    iconRes = R.drawable.ic_widget_style,
  ),
  PROFILE_SLOT_2(
    serviceClass = ProfileSlot2TileService::class.java,
    title = "Perfil Rápido 2",
    subtitle = "Ativa os 4 volumes e modo do Perfil 2 em 1 toque",
    iconRes = R.drawable.ic_widget_style,
  ),
  QUICK_TIMER(
    serviceClass = QuickTimerTileService::class.java,
    title = "Timer Silêncio (30m/1h/2h)",
    subtitle = "Inicia timer de vibração com auto-restore ou cancela em 1 toque",
    iconRes = R.drawable.ic_qs_timer,
  ),
  MEDIA_MUTE(
    serviceClass = MediaMuteTileService::class.java,
    title = "Zero Mídia (Mudo Rápido)",
    subtitle = "Fica aceso quando a Mídia está em 0% • Zera ou restaura",
    iconRes = R.drawable.ic_qs_media_mute,
  ),
}

abstract class BaseSoundTileService : TileService() {

  protected var serviceScope: CoroutineScope? = null

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
      handleTileClick(app)
      refreshTileState()
      SoundModeTileService.refreshAllTiles(applicationContext)
    }
  }

  protected fun openDndPermissionSettings() {
    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
      addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    runCatching {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        val pendingIntent = PendingIntent.getActivity(
          this,
          910,
          intent,
          PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        startActivityAndCollapse(pendingIntent)
      } else {
        @Suppress("DEPRECATION")
        startActivityAndCollapse(intent)
      }
    }
  }

  protected abstract suspend fun handleTileClick(app: ManageSoundApplication)
  protected abstract suspend fun refreshTileState()
}

/**
 * 0. Bloco Alternador Único (Ciclo: Som -> Vibrar -> Silêncio -> Som)
 */
class SoundModeTileService : BaseSoundTileService() {

  override suspend fun handleTileClick(app: ManageSoundApplication) {
    app.container.soundModeRepository.cycleSoundModeFromQuickTile()
  }

  override suspend fun refreshTileState() {
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
    fun refreshAllTiles(context: Context) {
      QuickTileKind.entries.forEach { kind ->
        runCatching {
          requestListeningState(context, ComponentName(context, kind.serviceClass))
        }
      }
    }

    fun requestAddTile(
      context: Context,
      kind: QuickTileKind = QuickTileKind.CYCLE,
      customLabel: String? = null,
      onResultMessage: (String) -> Unit,
    ) {
      val displayTitle = customLabel ?: kind.title
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val statusBarManager = context.getSystemService(StatusBarManager::class.java)
        if (statusBarManager == null) {
          onResultMessage("Abra o lápis (✏️) no topo das notificações para adicionar '$displayTitle'.")
          return
        }
        val componentName = ComponentName(context, kind.serviceClass)
        val icon = Icon.createWithResource(context, kind.iconRes)
        statusBarManager.requestAddTileService(
          componentName,
          displayTitle,
          icon,
          ContextCompat.getMainExecutor(context),
        ) { resultCode ->
          val msg = when (resultCode) {
            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ADDED ->
              "Bloco '$displayTitle' adicionado ao topo das notificações!"
            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_ALREADY_ADDED ->
              "O bloco '$displayTitle' já está ativo no topo das notificações."
            StatusBarManager.TILE_ADD_REQUEST_RESULT_TILE_NOT_ADDED ->
              "Adição de '$displayTitle' cancelada."
            else ->
              "Puxe o topo das notificações e toque no lápis (✏️) para posicionar '$displayTitle'."
          }
          onResultMessage(msg)
        }
      } else {
        onResultMessage(
          "Puxe a barra de notificações e toque no lápis (✏️) para adicionar o bloco '$displayTitle'.",
        )
      }
    }
  }
}

/**
 * Opção A.1 — Bloco Direto: Modo Som Normal
 */
class NormalModeTileService : BaseSoundTileService() {

  override suspend fun handleTileClick(app: ManageSoundApplication) {
    app.container.soundModeRepository.setSoundMode(
      mode = SoundMode.NORMAL,
      source = TransitionTriggerSource.QUICK_TILE,
    )
  }

  override suspend fun refreshTileState() {
    val tile = qsTile ?: return
    val app = applicationContext as? ManageSoundApplication ?: return
    val mode = app.container.soundSystemController.readCurrentMode()
    val streams = app.container.soundSystemController.readVolumeStreams()
    val isActive = mode == SoundMode.NORMAL

    tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    tile.icon = Icon.createWithResource(this, R.drawable.ic_widget_volume_up)
    tile.label = "Som Normal"
    tile.subtitle = if (isActive) {
      "Ativo • Toque ${streams.ringPercent}%"
    } else {
      "Toque p/ ativar"
    }
    tile.updateTile()
  }
}

/**
 * Opção A.2 — Bloco Direto: Modo Vibrar
 */
class VibrateModeTileService : BaseSoundTileService() {

  override suspend fun handleTileClick(app: ManageSoundApplication) {
    val current = app.container.soundSystemController.readCurrentMode()
    val target = if (current == SoundMode.VIBRATE) SoundMode.NORMAL else SoundMode.VIBRATE
    app.container.soundModeRepository.setSoundMode(
      mode = target,
      source = TransitionTriggerSource.QUICK_TILE,
    )
  }

  override suspend fun refreshTileState() {
    val tile = qsTile ?: return
    val app = applicationContext as? ManageSoundApplication ?: return
    val mode = app.container.soundSystemController.readCurrentMode()
    val streams = app.container.soundSystemController.readVolumeStreams()
    val isActive = mode == SoundMode.VIBRATE

    tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    tile.icon = Icon.createWithResource(this, R.drawable.ic_widget_vibration)
    tile.label = "Vibrar"
    tile.subtitle = if (isActive) {
      "Ativo • Mídia ${streams.mediaPercent}%"
    } else {
      "Toque p/ vibrar"
    }
    tile.updateTile()
  }
}

/**
 * Opção A.3 — Bloco Direto: Modo Silencioso
 */
class SilentModeTileService : BaseSoundTileService() {

  override suspend fun handleTileClick(app: ManageSoundApplication) {
    if (!app.container.soundSystemController.hasDndPermission()) {
      openDndPermissionSettings()
      return
    }
    val current = app.container.soundSystemController.readCurrentMode()
    val target = if (current == SoundMode.SILENT) SoundMode.NORMAL else SoundMode.SILENT
    app.container.soundModeRepository.setSoundMode(
      mode = target,
      source = TransitionTriggerSource.QUICK_TILE,
    )
  }

  override suspend fun refreshTileState() {
    val tile = qsTile ?: return
    val app = applicationContext as? ManageSoundApplication ?: return
    val hasDnd = app.container.soundSystemController.hasDndPermission()
    val mode = app.container.soundSystemController.readCurrentMode()
    val streams = app.container.soundSystemController.readVolumeStreams()
    val isActive = mode == SoundMode.SILENT

    tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    tile.icon = Icon.createWithResource(this, R.drawable.ic_widget_silent)
    tile.label = "Silêncio"
    tile.subtitle = when {
      !hasDnd -> "Autorizar DND"
      isActive -> "Ativo • Alarme ${streams.alarmPercent}%"
      else -> "Toque p/ silenciar"
    }
    tile.updateTile()
  }
}

/**
 * Opção B — Slots Dinâmicos de Perfis Favoritos (Slot 1 e Slot 2)
 */
abstract class BaseProfileSlotTileService(
  private val slotIndex: Int,
) : BaseSoundTileService() {

  override suspend fun handleTileClick(app: ManageSoundApplication) {
    val prefs = app.container.preferencesDataSource.prefsFlow.first()
    val profiles = app.container.soundModeRepository.profilesStream.first()
    val targetId = if (slotIndex == 1) prefs.quickTileSlot1ProfileId else prefs.quickTileSlot2ProfileId
    val targetProfile = profiles.firstOrNull { it.id == targetId } ?: profiles.firstOrNull() ?: return

    if (targetProfile.mode == SoundMode.SILENT && !app.container.soundSystemController.hasDndPermission()) {
      openDndPermissionSettings()
      return
    }

    if (prefs.activeProfileId == targetProfile.id) {
      val fallback = profiles.firstOrNull { it.id == prefs.defaultDayProfileId }
      if (fallback != null && fallback.id != targetProfile.id) {
        app.container.soundModeRepository.activateProfile(
          profile = fallback,
          source = TransitionTriggerSource.QUICK_TILE,
        )
      } else {
        app.container.soundModeRepository.setSoundMode(
          mode = SoundMode.NORMAL,
          source = TransitionTriggerSource.QUICK_TILE,
        )
      }
    } else {
      app.container.soundModeRepository.activateProfile(
        profile = targetProfile,
        source = TransitionTriggerSource.QUICK_TILE,
      )
    }
  }

  override suspend fun refreshTileState() {
    val tile = qsTile ?: return
    val app = applicationContext as? ManageSoundApplication ?: return
    val prefs = app.container.preferencesDataSource.prefsFlow.first()
    val profiles = app.container.soundModeRepository.profilesStream.first()
    val targetId = if (slotIndex == 1) prefs.quickTileSlot1ProfileId else prefs.quickTileSlot2ProfileId
    val targetProfile = profiles.firstOrNull { it.id == targetId } ?: profiles.firstOrNull()

    if (targetProfile == null) {
      tile.state = Tile.STATE_INACTIVE
      tile.label = "Perfil $slotIndex"
      tile.subtitle = "Configure no app"
      tile.updateTile()
      return
    }

    val isActive = prefs.activeProfileId == targetProfile.id
    val iconRes = when (targetProfile.mode) {
      SoundMode.NORMAL -> R.drawable.ic_widget_volume_up
      SoundMode.VIBRATE -> R.drawable.ic_widget_vibration
      SoundMode.SILENT -> R.drawable.ic_widget_silent
    }

    tile.state = if (isActive) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    tile.icon = Icon.createWithResource(this, iconRes)
    tile.label = targetProfile.name
    tile.subtitle = if (isActive) {
      "Ativo • ${targetProfile.mode.title}"
    } else {
      targetProfile.summaryLabel()
    }
    tile.updateTile()
  }
}

class ProfileSlot1TileService : BaseProfileSlotTileService(slotIndex = 1)
class ProfileSlot2TileService : BaseProfileSlotTileService(slotIndex = 2)

/**
 * Opção C — Bloco de Timer Rápido (30m -> 1h -> 2h -> Cancelar)
 */
class QuickTimerTileService : BaseSoundTileService() {

  override suspend fun handleTileClick(app: ManageSoundApplication) {
    val now = System.currentTimeMillis()
    val prefs = app.container.preferencesDataSource.prefsFlow.first()
    val activeTimer = prefs.activeTimer

    if (activeTimer == null) {
      val currentMode = app.container.soundSystemController.readCurrentMode()
      val targetMode = if (currentMode == SoundMode.NORMAL) SoundMode.VIBRATE else currentMode
      lastTapTimestampMillis = now
      app.container.soundModeRepository.startTimedSession(
        targetMode = targetMode,
        durationMinutes = 30,
        nowEpochMillis = now,
      )
    } else if (now - lastTapTimestampMillis <= 8_000L && activeTimer.durationMinutes < 120) {
      // Toque consecutivo rápido escala de 30m -> 60m -> 120m
      lastTapTimestampMillis = now
      val addMinutes = if (activeTimer.durationMinutes < 60) 30 else 60
      app.container.soundModeRepository.extendTimedSession(
        additionalMinutes = addMinutes,
        nowEpochMillis = now,
      )
    } else {
      lastTapTimestampMillis = 0L
      app.container.soundModeRepository.cancelOrRestoreTimedSession()
    }
  }

  override suspend fun refreshTileState() {
    val tile = qsTile ?: return
    val app = applicationContext as? ManageSoundApplication ?: return
    val prefs = app.container.preferencesDataSource.prefsFlow.first()
    val activeTimer = prefs.activeTimer
    val remainingMin = activeTimer?.remainingMinutes(System.currentTimeMillis())

    tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_timer)
    if (activeTimer != null && remainingMin != null && remainingMin > 0) {
      tile.state = Tile.STATE_ACTIVE
      tile.label = "Timer • ${remainingMin}m"
      tile.subtitle = "${activeTimer.targetMode.title} • Toque: +30m / Fim"
    } else {
      tile.state = Tile.STATE_INACTIVE
      tile.label = "Timer Silêncio"
      tile.subtitle = "Toque: 30m • 1h • 2h"
    }
    tile.updateTile()
  }

  private companion object {
    @Volatile
    var lastTapTimestampMillis: Long = 0L
  }
}

/**
 * Opção D — Bloco Zero Mídia (Mudo Instantâneo de Mídia)
 */
class MediaMuteTileService : BaseSoundTileService() {

  override suspend fun handleTileClick(app: ManageSoundApplication) {
    app.container.soundModeRepository.toggleQuickMediaMute()
  }

  override suspend fun refreshTileState() {
    val tile = qsTile ?: return
    val app = applicationContext as? ManageSoundApplication ?: return
    val streams = app.container.soundSystemController.readVolumeStreams()
    val prefs = app.container.preferencesDataSource.prefsFlow.first()
    val isMediaMuted = streams.mediaPercent == 0

    tile.state = if (isMediaMuted) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
    tile.icon = Icon.createWithResource(this, R.drawable.ic_qs_media_mute)
    if (isMediaMuted) {
      val restoreTarget = prefs.savedMediaBeforeMute ?: 50
      tile.label = "Mídia Zerada"
      tile.subtitle = "0% • Toque restaura ${restoreTarget}%"
    } else {
      tile.label = "Zero Mídia"
      tile.subtitle = "Mídia ${streams.mediaPercent}% • Toque p/ 0%"
    }
    tile.updateTile()
  }
}
