package com.managesoundmode.app.di

import android.content.Context
import com.managesoundmode.app.data.local.DataStoreSoundPreferencesDataSource
import com.managesoundmode.app.data.local.SoundPreferencesDataSource
import com.managesoundmode.app.data.repository.DefaultSoundModeRepository
import com.managesoundmode.app.data.repository.SoundModeRepository
import com.managesoundmode.app.data.system.AndroidSoundSystemController
import com.managesoundmode.app.data.system.SoundSystemController
import com.managesoundmode.app.work.RestoreSoundModeWorker

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
    )
  }
}
