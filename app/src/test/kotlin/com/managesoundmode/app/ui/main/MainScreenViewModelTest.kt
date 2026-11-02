package com.managesoundmode.app.ui.main

import com.managesoundmode.app.data.local.PersistedSoundPrefs
import com.managesoundmode.app.data.local.SoundPreferencesDataSource
import com.managesoundmode.app.data.repository.DefaultSoundModeRepository
import com.managesoundmode.app.data.system.DndPermissionRequiredException
import com.managesoundmode.app.data.system.SoundSystemController
import com.managesoundmode.app.domain.model.SoundMode
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.domain.model.TimedModeSession
import com.managesoundmode.app.domain.model.VolumeStreamType
import com.managesoundmode.app.domain.model.VolumeStreams
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainScreenViewModelTest {

  private val testDispatcher = UnconfinedTestDispatcher()
  private lateinit var fakeSystemController: FakeSoundSystemController
  private lateinit var fakePreferences: FakeSoundPreferencesDataSource
  private lateinit var repository: DefaultSoundModeRepository
  private lateinit var viewModel: MainScreenViewModel

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    fakeSystemController = FakeSoundSystemController()
    fakePreferences = FakeSoundPreferencesDataSource()
    repository = DefaultSoundModeRepository(
      systemController = fakeSystemController,
      preferencesDataSource = fakePreferences,
    )
    viewModel = MainScreenViewModel(repository)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
  }

  @Test
  fun uiState_whenSubscribed_emitsReadyWithSystemDefaults() = runTest {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    val state = viewModel.uiState.value
    assertTrue(state is MainScreenUiState.Ready)
    val ready = state as MainScreenUiState.Ready
    assertEquals(SoundMode.NORMAL, ready.currentMode)
    assertEquals(SoundProfile.defaultPresets.size, ready.profiles.size)
  }

  @Test
  fun onSelectMode_vibrate_updatesModeToVibrate() = runTest {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    viewModel.onSelectMode(SoundMode.VIBRATE)

    val ready = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(SoundMode.VIBRATE, ready.currentMode)
    assertEquals("Modo alterado para Vibrar", ready.statusMessage)
  }

  @Test
  fun onSelectMode_silentWithoutDndPermission_keepsPreviousModeAndShowsErrorMessage() = runTest {
    fakeSystemController.dndGranted = false
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    viewModel.onSelectMode(SoundMode.SILENT)

    val ready = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(SoundMode.NORMAL, ready.currentMode)
    assertNotNull(ready.statusMessage)
    assertTrue(ready.statusMessage!!.contains("Não Perturbe"))
  }

  @Test
  fun onStartTimer_andCancelTimer_restoresPreviousMode() = runTest {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    viewModel.onStartTimer(targetMode = SoundMode.VIBRATE, durationMinutes = 30)
    val duringTimer = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(SoundMode.VIBRATE, duringTimer.currentMode)
    assertNotNull(duringTimer.activeTimer)
    assertEquals(30, duringTimer.activeTimer?.durationMinutes)

    viewModel.onCancelTimer()
    val afterCancel = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(SoundMode.NORMAL, afterCancel.currentMode)
    assertNull(afterCancel.activeTimer)
  }

  @Test
  fun onSaveCurrentAsProfile_addsCustomProfileAndActivatesIt() = runTest {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    viewModel.onSaveCurrentAsProfile("Estudo Profundo", "Foco para leitura")
    val ready = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(SoundProfile.defaultPresets.size + 1, ready.profiles.size)
    assertTrue(ready.profiles.any { it.name == "Estudo Profundo" && !it.isPreset })
    assertNotNull(ready.activeProfileId)
  }
}

private class FakeSoundSystemController : SoundSystemController {
  private val changes = MutableStateFlow(Unit)
  var mode: SoundMode = SoundMode.NORMAL
  var streams: VolumeStreams = VolumeStreams()
  var dndGranted: Boolean = true

  override val systemChanges: Flow<Unit> = changes

  override fun readCurrentMode(): SoundMode = mode

  override fun readVolumeStreams(): VolumeStreams = streams

  override fun hasDndPermission(): Boolean = dndGranted

  override fun applySoundMode(mode: SoundMode): Result<SoundMode> = runCatching {
    if (mode == SoundMode.SILENT && !dndGranted) {
      throw DndPermissionRequiredException()
    }
    this.mode = mode
    this.mode
  }

  override fun applyStreamVolume(type: VolumeStreamType, percent: Int): Result<VolumeStreams> = runCatching {
    streams = streams.withUpdatedStream(type, percent)
    streams
  }

  override fun applyAllStreams(streams: VolumeStreams): Result<VolumeStreams> = runCatching {
    this.streams = streams
    this.streams
  }
}

private class FakeSoundPreferencesDataSource : SoundPreferencesDataSource {
  private val state = MutableStateFlow(PersistedSoundPrefs())
  override val prefsFlow: Flow<PersistedSoundPrefs> = state

  override suspend fun setActiveProfileId(profileId: String?) {
    state.value = state.value.copy(activeProfileId = profileId)
  }

  override suspend fun setActiveTimer(session: TimedModeSession?) {
    state.value = state.value.copy(activeTimer = session)
  }

  override suspend fun upsertCustomProfile(profile: SoundProfile) {
    val updated = state.value.customProfiles.filterNot { it.id == profile.id } + profile
    state.value = state.value.copy(customProfiles = updated)
  }

  override suspend fun deleteCustomProfile(profileId: String) {
    val updated = state.value.customProfiles.filterNot { it.id == profileId }
    state.value = state.value.copy(customProfiles = updated)
  }
}
