package com.managesoundmode.app.ui.main

import com.managesoundmode.app.data.local.PersistedSoundPrefs
import com.managesoundmode.app.data.local.SoundPreferencesDataSource
import com.managesoundmode.app.data.repository.DefaultSoundModeRepository
import com.managesoundmode.app.data.system.DndPermissionRequiredException
import com.managesoundmode.app.data.system.SoundSystemController
import com.managesoundmode.app.domain.model.LocationRule
import com.managesoundmode.app.domain.model.RestorePolicy
import com.managesoundmode.app.domain.model.SavedStateSnapshot
import com.managesoundmode.app.domain.model.ScheduleRule
import com.managesoundmode.app.domain.model.SoundMode
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.domain.model.ThemeMode
import com.managesoundmode.app.domain.model.TimedModeSession
import com.managesoundmode.app.domain.model.TransitionLogEntry
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
    assertEquals(ScheduleRule.defaultSchedules.size, ready.scheduleRules.size)
    assertEquals(LocationRule.defaultLocations.size, ready.locationRules.size)
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
    // Desabilita horários padrão para testar isolamento do timer
    fakePreferences.clearSchedules()
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

  @Test
  fun onToggleLocationPresence_activatesLocationRuleProfile() = runTest {
    fakePreferences.clearSchedules()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    val collegeRule = LocationRule.defaultLocations.first { it.id == "loc_college" }
    viewModel.onToggleLocationRule(collegeRule, enabled = true)
    viewModel.onToggleLocationPresence("loc_college", inside = true)

    val ready = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(SoundMode.VIBRATE, ready.currentMode)
    assertTrue(ready.locationRules.first { it.id == "loc_college" }.isCurrentlyInside)
  }

  @Test
  fun onSetRestorePolicy_updatesPolicyInUiState() = runTest {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    viewModel.onSetRestorePolicy(RestorePolicy.SNAPSHOT)
    val ready = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(RestorePolicy.SNAPSHOT, ready.restorePolicy)
  }

  @Test
  fun onSetThemeMode_andUpsertProfile_updatesThemeAndEditsPresetProfile() = runTest {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    viewModel.onSetThemeMode(ThemeMode.LIGHT)
    val afterTheme = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(ThemeMode.LIGHT, afterTheme.themeMode)

    val workPreset = afterTheme.profiles.first { it.id == "preset_work_high" }
    val editedWork = workPreset.copy(
      streams = workPreset.streams.copy(ringPercent = 75, mediaPercent = 15),
    )
    viewModel.onUpsertProfile(editedWork, applyImmediately = true)

    val afterEdit = viewModel.uiState.value as MainScreenUiState.Ready
    val updatedPreset = afterEdit.profiles.first { it.id == "preset_work_high" }
    assertEquals(75, updatedPreset.streams.ringPercent)
    assertEquals(15, updatedPreset.streams.mediaPercent)
    assertEquals("preset_work_high", afterEdit.activeProfileId)
  }
}

private class FakeSoundSystemController : SoundSystemController {
  private val changes = MutableStateFlow(Unit)
  var mode: SoundMode = SoundMode.NORMAL
  var streams: VolumeStreams = VolumeStreams()
  var dndGranted: Boolean = true

  override val systemChanges: Flow<Unit> = changes
  override val lastBroadcastWasExternal: Boolean = false

  override fun clearExternalBroadcastFlag() {}

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

  fun clearSchedules() {
    state.value = state.value.copy(scheduleRules = emptyList())
  }

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

  override suspend fun upsertScheduleRule(rule: ScheduleRule) {
    val existing = state.value.scheduleRules.toMutableList()
    val idx = existing.indexOfFirst { it.id == rule.id }
    if (idx >= 0) existing[idx] = rule else existing.add(rule)
    state.value = state.value.copy(scheduleRules = existing)
  }

  override suspend fun deleteScheduleRule(ruleId: String) {
    state.value = state.value.copy(
      scheduleRules = state.value.scheduleRules.filterNot { it.id == ruleId },
    )
  }

  override suspend fun upsertLocationRule(rule: LocationRule) {
    val existing = state.value.locationRules.toMutableList()
    val idx = existing.indexOfFirst { it.id == rule.id }
    if (idx >= 0) existing[idx] = rule else existing.add(rule)
    state.value = state.value.copy(locationRules = existing)
  }

  override suspend fun deleteLocationRule(ruleId: String) {
    state.value = state.value.copy(
      locationRules = state.value.locationRules.filterNot { it.id == ruleId },
    )
  }

  override suspend fun setRestorePolicy(policy: RestorePolicy) {
    state.value = state.value.copy(restorePolicy = policy)
  }

  override suspend fun setDefaultDayProfileId(profileId: String) {
    state.value = state.value.copy(defaultDayProfileId = profileId)
  }

  override suspend fun setSavedSnapshot(snapshot: SavedStateSnapshot?) {
    state.value = state.value.copy(savedSnapshot = snapshot)
  }

  override suspend fun setLastAppliedRuleSessionKey(sessionKey: String?) {
    state.value = state.value.copy(lastAppliedRuleSessionKey = sessionKey)
  }

  override suspend fun setOverriddenSessionKey(sessionKey: String?) {
    state.value = state.value.copy(overriddenSessionKey = sessionKey)
  }

  override suspend fun appendTransitionLog(entry: TransitionLogEntry) {
    state.value = state.value.copy(
      transitionLogs = listOf(entry) + state.value.transitionLogs,
    )
  }

  override suspend fun setUseDynamicColor(enabled: Boolean) {
    state.value = state.value.copy(useDynamicColor = enabled)
  }

  override suspend fun setThemeMode(mode: ThemeMode) {
    state.value = state.value.copy(themeMode = mode)
  }

  override suspend fun setLocationDisclosureAccepted(accepted: Boolean) {
    state.value = state.value.copy(hasAcceptedLocationDisclosure = accepted)
  }
}
