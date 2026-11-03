package com.managesoundmode.app.di

import android.content.ComponentName
import android.content.Context
import android.service.quicksettings.TileService
import com.managesoundmode.app.data.local.DataStoreSoundPreferencesDataSource
import com.managesoundmode.app.data.local.SoundPreferencesDataSource
import com.managesoundmode.app.data.repository.DefaultSoundModeRepository
import com.managesoundmode.app.data.repository.SoundModeRepository
import com.managesoundmode.app.data.system.AndroidSoundSystemController
import com.managesoundmode.app.data.system.SoundSystemController
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.system.ExactRuleAlarmScheduler
import com.managesoundmode.app.system.SmartNotificationManager
import com.managesoundmode.app.tile.SoundModeTileService
import com.managesoundmode.app.widget.SoundModeGlanceWidget
import com.managesoundmode.app.work.RestoreSoundModeWorker
import kotlinx.coroutines.flow.first

interface AppContainer {
  val soundSystemController: SoundSystemController
  val preferencesDataSource: SoundPreferencesDataSource
  val soundModeRepository: SoundModeRepository
}

class DefaultAppContainer(
  private val appContext: Context,
) : AppContainer {

  override val soundSystemController: SoundSystemController by lazy {
    AndroidSoundSystemController(appContext)
  }

  override val preferencesDataSource: SoundPreferencesDataSource by lazy {
    DataStoreSoundPreferencesDataSource(appContext)
  }

  override val soundModeRepository: SoundModeRepository by lazy {
    DefaultSoundModeRepository(
      systemController = soundSystemController,
      preferencesDataSource = preferencesDataSource,
      onScheduleAutoRestore = { minutes -> RestoreSoundModeWorker.schedule(appContext, minutes) },
      onCancelAutoRestore = { RestoreSoundModeWorker.cancel(appContext) },
      onRescheduleRuleAlarms = { rules ->
        ExactRuleAlarmScheduler.scheduleNextTransition(appContext, rules)
      },
      onSmartRestorePrompt = { mode, mediaAutoMuted ->
        SmartNotificationManager.showSmartRestorePrompt(
          context = appContext,
          mode = mode,
          mediaAutoMuted = mediaAutoMuted,
        )
      },
      onDismissSmartPrompt = {
        SmartNotificationManager.dismissSmartPrompt(appContext)
      },
      onSyncActiveTimerNotification = { session ->
        SmartNotificationManager.syncActiveTimerNotification(appContext, session)
      },
      onRuleOverrideAlert = { ruleName ->
        SmartNotificationManager.showRuleOverrideAlert(appContext, ruleName)
      },
      onRefreshExternalSurfaces = {
        SoundModeGlanceWidget.refreshAllWidgets(appContext)
        runCatching {
          val prefs = preferencesDataSource.prefsFlow.first()
          val allProfiles = SoundProfile.defaultPresets + prefs.customProfiles
          val activeProfileName = prefs.activeProfileId?.let { activeId ->
            allProfiles.lastOrNull { it.id == activeId }?.name
          }
          SmartNotificationManager.syncPersistentQuickBarNotification(
            context = appContext,
            enabled = prefs.persistentQuickBarEnabled,
            currentMode = soundSystemController.readCurrentMode(),
            streams = soundSystemController.readVolumeStreams(),
            activeProfileName = activeProfileName,
            activeTimer = prefs.activeTimer,
          )
        }
        runCatching {
          TileService.requestListeningState(
            appContext,
            ComponentName(appContext, SoundModeTileService::class.java),
          )
        }
      },
    )
  }
}
