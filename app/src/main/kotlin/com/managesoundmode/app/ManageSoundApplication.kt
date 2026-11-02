package com.managesoundmode.app

import android.app.Application
import com.managesoundmode.app.di.AppContainer
import com.managesoundmode.app.di.DefaultAppContainer
import com.managesoundmode.app.system.AppShortcutsManager
import com.managesoundmode.app.system.SmartNotificationManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ManageSoundApplication : Application() {
  lateinit var container: AppContainer
    private set

  private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  override fun onCreate() {
    super.onCreate()
    container = DefaultAppContainer(applicationContext)
    SmartNotificationManager.ensureChannels(applicationContext)
    AppShortcutsManager.publishDynamicShortcuts(applicationContext)

    applicationScope.launch {
      container.soundSystemController.systemChanges.collectLatest {
        if (container.soundSystemController.lastBroadcastWasExternal) {
          container.soundModeRepository.handleExternalAudioChange()
        }
      }
    }
  }
}
