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

  @Test
  fun onUpsertProfile_whenEditingActiveProfileWithoutApplyNow_stillSyncsHardwareStreams() = runTest {
    fakePreferences.clearSchedules()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    val initialReady = viewModel.uiState.value as MainScreenUiState.Ready
    val workPreset = initialReady.profiles.first { it.id == "preset_work_high" }
    viewModel.onActivateProfile(workPreset)

    val editedWork = workPreset.copy(
      streams = workPreset.streams.copy(ringPercent = 64, mediaPercent = 22),
    )
    viewModel.onUpsertProfile(editedWork, applyImmediately = false)

    val afterEdit = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals("preset_work_high", afterEdit.activeProfileId)
    assertEquals(64, afterEdit.streams.ringPercent)
    assertEquals(22, afterEdit.streams.mediaPercent)
  }

  @Test
  fun onSetMuteMediaOnSilent_whileInVibrate_immediatelyMutesAndRestoresMedia() = runTest {
    fakePreferences.clearSchedules()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    // Start in NORMAL with muteMediaOnSilent disabled and media at 60%
    viewModel.onSetMuteMediaOnSilent(false)
    viewModel.onStreamVolumeChange(VolumeStreamType.MEDIA, 60)
    viewModel.onSelectMode(SoundMode.VIBRATE)

    val beforeToggle = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(SoundMode.VIBRATE, beforeToggle.currentMode)
    assertEquals(60, beforeToggle.streams.mediaPercent)

    // Enable muteMediaOnSilent while already in VIBRATE -> media should immediately drop to 0%
    viewModel.onSetMuteMediaOnSilent(true)
    val afterEnable = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(0, afterEnable.streams.mediaPercent)

    // Disable muteMediaOnSilent while still in VIBRATE -> media should restore to 60%
    viewModel.onSetMuteMediaOnSilent(false)
    val afterDisable = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(60, afterDisable.streams.mediaPercent)
  }

  @Test
  fun onStreamVolumeChange_ringAboveZeroInVibrate_switchesModeToNormal() = runTest {
    fakePreferences.clearSchedules()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    viewModel.onSelectMode(SoundMode.VIBRATE)
    assertEquals(SoundMode.VIBRATE, (viewModel.uiState.value as MainScreenUiState.Ready).currentMode)

    viewModel.onStreamVolumeChange(VolumeStreamType.RING, 70)
    val afterRing = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(SoundMode.NORMAL, afterRing.currentMode)
    assertEquals(70, afterRing.streams.ringPercent)
  }

  @Test
  fun handleExternalAudioChange_whenRaisingMediaVolumeWhileAlreadyInVibrate_neverRevertsToZero() = runTest {
    fakePreferences.clearSchedules()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    // Enter VIBRATE -> media is auto-muted to 0%
    viewModel.onSelectMode(SoundMode.VIBRATE)
    assertEquals(0, (viewModel.uiState.value as MainScreenUiState.Ready).streams.mediaPercent)

    // User presses physical Volume Up button while in VIBRATE to raise media to 40%
    fakeSystemController.streams = fakeSystemController.streams.copy(mediaPercent = 40)
    fakeSystemController.externalFlag = true
    repository.handleExternalAudioChange()

    // Volume MUST stay at 40% and never revert to 0%
    val afterHardwareVolumeUp = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(SoundMode.VIBRATE, afterHardwareVolumeUp.currentMode)
    assertEquals(40, afterHardwareVolumeUp.streams.mediaPercent)
  }

  @Test
  fun onRefreshSystemState_afterIncreasingVolumeDuringActiveRule_neverRevertsToRuleVolume() = runTest {
    fakePreferences.clearSchedules()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    val collegeRule = LocationRule.defaultLocations.first { it.id == "loc_college" }
    viewModel.onToggleLocationRule(collegeRule, enabled = true)
    viewModel.onToggleLocationPresence("loc_college", inside = true)
    assertEquals(0, (viewModel.uiState.value as MainScreenUiState.Ready).streams.mediaPercent)

    // User increases media volume to 65%
    viewModel.onStreamVolumeChange(VolumeStreamType.MEDIA, 65)
    assertEquals(65, (viewModel.uiState.value as MainScreenUiState.Ready).streams.mediaPercent)

    // Simulate onResume calling onRefreshSystemState() -> evaluateAndApplyRules()
    viewModel.onRefreshSystemState()

    val afterResume = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(65, afterResume.streams.mediaPercent)
  }

  @Test
  fun onSetPersistentQuickBarEnabled_updatesPreferenceAndTriggersRefresh() = runTest {
    var surfaceRefreshCount = 0
    val repoWithSurfaceRefresh = DefaultSoundModeRepository(
      systemController = fakeSystemController,
      preferencesDataSource = fakePreferences,
      onRefreshExternalSurfaces = { surfaceRefreshCount++ },
    )
    val vm = MainScreenViewModel(repoWithSurfaceRefresh)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      vm.uiState.collect()
    }

    assertTrue((vm.uiState.value as MainScreenUiState.Ready).persistentQuickBarEnabled)

    vm.onSetPersistentQuickBarEnabled(false)
    val afterDisable = vm.uiState.value as MainScreenUiState.Ready
    assertEquals(false, afterDisable.persistentQuickBarEnabled)
    assertEquals("Gadget removido da barra de notificações", afterDisable.statusMessage)
    assertEquals(1, surfaceRefreshCount)

    vm.onSetPersistentQuickBarEnabled(true)
    val afterEnable = vm.uiState.value as MainScreenUiState.Ready
    assertEquals(true, afterEnable.persistentQuickBarEnabled)
    assertEquals("Gadget fixado na barra de notificações", afterEnable.statusMessage)
    assertEquals(2, surfaceRefreshCount)
  }

  @Test
  fun onSetQuickTileSlotProfileId_andToggleQuickMediaMute_workCorrectly() = runTest {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    viewModel.onSetQuickTileSlotProfileId(1, "preset_home")
    viewModel.onSetQuickTileSlotProfileId(2, "preset_meeting")

    val readyAfterSlots = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals("preset_home", readyAfterSlots.quickTileSlot1ProfileId)
    assertEquals("preset_meeting", readyAfterSlots.quickTileSlot2ProfileId)

    // Media starts at 50%; toggleQuickMediaMute should mute to 0% and save 50%
    viewModel.onStreamVolumeChange(VolumeStreamType.MEDIA, 64)
    val mutedPercent = repository.toggleQuickMediaMute().getOrThrow()
    assertEquals(0, mutedPercent)
    assertEquals(0, (viewModel.uiState.value as MainScreenUiState.Ready).streams.mediaPercent)

    // Second toggle should restore back to 64%
    val restoredPercent = repository.toggleQuickMediaMute().getOrThrow()
    assertEquals(64, restoredPercent)
    assertEquals(64, (viewModel.uiState.value as MainScreenUiState.Ready).streams.mediaPercent)
  }
}

private class FakeSoundSystemController : SoundSystemController {
  private val changes = MutableStateFlow(Unit)
  var mode: SoundMode = SoundMode.NORMAL
  var streams: VolumeStreams = VolumeStreams()
  var dndGranted: Boolean = true
  var externalFlag: Boolean = false

  override val systemChanges: Flow<Unit> = changes
  override val lastBroadcastWasExternal: Boolean
    get() = externalFlag

  override fun clearExternalBroadcastFlag() {
    externalFlag = false
  }

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

  override suspend fun setMuteMediaOnSilent(enabled: Boolean) {
    state.value = state.value.copy(muteMediaOnSilent = enabled)
  }

  override suspend fun setPreserveMediaOnBluetooth(enabled: Boolean) {
    state.value = state.value.copy(preserveMediaOnBluetooth = enabled)
  }

  override suspend fun setSmartVolumeTriggerEnabled(enabled: Boolean) {
    state.value = state.value.copy(smartVolumeTriggerEnabled = enabled)
  }

  override suspend fun setPersistentQuickBarEnabled(enabled: Boolean) {
    state.value = state.value.copy(persistentQuickBarEnabled = enabled)
  }

  override suspend fun setQuickTileSlot1ProfileId(profileId: String) {
    state.value = state.value.copy(quickTileSlot1ProfileId = profileId)
  }

  override suspend fun setQuickTileSlot2ProfileId(profileId: String) {
    state.value = state.value.copy(quickTileSlot2ProfileId = profileId)
  }

  override suspend fun setSavedMediaBeforeMute(percent: Int?) {
    state.value = state.value.copy(savedMediaBeforeMute = percent)
  }
}
