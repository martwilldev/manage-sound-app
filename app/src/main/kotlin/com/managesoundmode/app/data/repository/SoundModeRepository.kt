package com.managesoundmode.app.data.repository

import com.managesoundmode.app.data.local.SoundPreferencesDataSource
import com.managesoundmode.app.data.system.SoundSystemController
import com.managesoundmode.app.domain.model.DeviceSoundSnapshot
import com.managesoundmode.app.domain.model.SoundMode
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.domain.model.TimedModeSession
import com.managesoundmode.app.domain.model.VolumeStreamType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

interface SoundModeRepository {
  val snapshotStream: Flow<DeviceSoundSnapshot>
  val profilesStream: Flow<List<SoundProfile>>

  fun refreshSystemState()
  suspend fun setSoundMode(mode: SoundMode): Result<Unit>
  suspend fun setStreamVolume(type: VolumeStreamType, percent: Int): Result<Unit>
  suspend fun activateProfile(profile: SoundProfile): Result<Unit>
  suspend fun startTimedSession(targetMode: SoundMode, durationMinutes: Int, nowEpochMillis: Long = System.currentTimeMillis()): Result<TimedModeSession>
  suspend fun cancelOrRestoreTimedSession(): Result<Unit>
  suspend fun saveCurrentAsProfile(name: String, description: String): Result<SoundProfile>
}

class DefaultSoundModeRepository(
  private val systemController: SoundSystemController,
  private val preferencesDataSource: SoundPreferencesDataSource,
  private val onScheduleAutoRestore: (durationMinutes: Int) -> Unit = {},
  private val onCancelAutoRestore: () -> Unit = {},
) : SoundModeRepository {

  private val manualRefreshTrigger = MutableStateFlow(0L)

  override val snapshotStream: Flow<DeviceSoundSnapshot> = combine(
    systemController.systemChanges,
    manualRefreshTrigger,
    preferencesDataSource.prefsFlow,
  ) { _, _, prefs ->
    DeviceSoundSnapshot(
      currentMode = systemController.readCurrentMode(),
      streams = systemController.readVolumeStreams(),
      hasDndPermission = systemController.hasDndPermission(),
      activeProfileId = prefs.activeProfileId,
      activeTimer = prefs.activeTimer,
    )
  }

  override val profilesStream: Flow<List<SoundProfile>> =
    preferencesDataSource.prefsFlow.combine(manualRefreshTrigger) { prefs, _ ->
      SoundProfile.defaultPresets + prefs.customProfiles
    }

  override fun refreshSystemState() {
    manualRefreshTrigger.value += 1L
  }

  override suspend fun setSoundMode(mode: SoundMode): Result<Unit> {
    return systemController.applySoundMode(mode).map {
      preferencesDataSource.setActiveProfileId(null)
      refreshSystemState()
    }
  }

  override suspend fun setStreamVolume(type: VolumeStreamType, percent: Int): Result<Unit> {
    return systemController.applyStreamVolume(type, percent).map {
      preferencesDataSource.setActiveProfileId(null)
      refreshSystemState()
    }
  }

  override suspend fun activateProfile(profile: SoundProfile): Result<Unit> {
    return systemController.applySoundMode(profile.mode).mapCatching {
      if (profile.mode == SoundMode.NORMAL) {
        systemController.applyAllStreams(profile.streams).getOrThrow()
      } else {
        // Still apply media and alarm streams even in Vibrate/Silent modes
        systemController.applyStreamVolume(VolumeStreamType.MEDIA, profile.streams.mediaPercent).getOrThrow()
        systemController.applyStreamVolume(VolumeStreamType.ALARM, profile.streams.alarmPercent).getOrThrow()
      }
      preferencesDataSource.setActiveProfileId(profile.id)
      refreshSystemState()
    }
  }

  override suspend fun startTimedSession(
    targetMode: SoundMode,
    durationMinutes: Int,
    nowEpochMillis: Long,
  ): Result<TimedModeSession> {
    val previousMode = systemController.readCurrentMode()
    val previousStreams = systemController.readVolumeStreams()

    return systemController.applySoundMode(targetMode).map {
      val session = TimedModeSession(
        targetMode = targetMode,
        previousMode = previousMode,
        previousStreams = previousStreams,
        durationMinutes = durationMinutes,
        startedAtEpochMillis = nowEpochMillis,
        endsAtEpochMillis = nowEpochMillis + durationMinutes * 60_000L,
      )
      preferencesDataSource.setActiveTimer(session)
      onScheduleAutoRestore(durationMinutes)
      refreshSystemState()
      session
    }
  }

  override suspend fun cancelOrRestoreTimedSession(): Result<Unit> = runCatching {
    onCancelAutoRestore()
    val savedTimer = preferencesDataSource.prefsFlow.first().activeTimer
    preferencesDataSource.setActiveTimer(null)
    val restoreMode = savedTimer?.previousMode ?: SoundMode.NORMAL
    systemController.applySoundMode(restoreMode).getOrThrow()
    if (savedTimer != null && restoreMode == SoundMode.NORMAL) {
      systemController.applyAllStreams(savedTimer.previousStreams)
    }
    refreshSystemState()
  }

  override suspend fun saveCurrentAsProfile(name: String, description: String): Result<SoundProfile> = runCatching {
    val trimmedName = name.trim()
    require(trimmedName.isNotEmpty()) { "O nome do perfil não pode ser vazio" }
    val profile = SoundProfile(
      id = "custom_${System.currentTimeMillis()}",
      name = trimmedName,
      description = description.trim().ifEmpty { "Perfil customizado pelo usuário" },
      mode = systemController.readCurrentMode(),
      streams = systemController.readVolumeStreams(),
      isPreset = false,
    )
    preferencesDataSource.upsertCustomProfile(profile)
    preferencesDataSource.setActiveProfileId(profile.id)
    refreshSystemState()
    profile
  }
}
