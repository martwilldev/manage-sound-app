package com.managesoundmode.app.ui.main

import com.managesoundmode.app.data.local.PersistedSoundPrefs
import com.managesoundmode.app.data.local.SoundPreferencesDataSource
import com.managesoundmode.app.data.local.SoundPrefsBackupPayload
import com.managesoundmode.app.data.repository.DefaultSoundModeRepository
import com.managesoundmode.app.data.system.DndPermissionRequiredException
import com.managesoundmode.app.data.system.SoundSystemController
import com.managesoundmode.app.domain.model.AccessControlConfig
import com.managesoundmode.app.domain.model.AppAccessRole
import com.managesoundmode.app.domain.model.AppPermission
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
import com.managesoundmode.app.system.BackgroundGeofenceManager
import com.managesoundmode.app.system.SystemZenRuleManager
import com.managesoundmode.app.ui.common.AppLabels
import com.managesoundmode.app.ui.common.AppLanguage
import com.managesoundmode.app.ui.common.AppTestTags
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
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

  @Test
  fun onDeleteCustomProfile_canDeletePresetProfile_andAutoSaveUpdatesWithoutStatusMessage() = runTest {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    val initialProfiles = (viewModel.uiState.value as MainScreenUiState.Ready).profiles
    assertTrue(initialProfiles.any { it.id == "preset_meeting" })

    viewModel.onDeleteCustomProfile("preset_meeting")
    val afterDelete = viewModel.uiState.value as MainScreenUiState.Ready
    assertTrue(afterDelete.profiles.none { it.id == "preset_meeting" })
    assertEquals("Perfil removido", afterDelete.statusMessage)
    viewModel.onDismissMessage()

    val sleepProfile = afterDelete.profiles.first { it.id == "preset_sleep" }
    viewModel.onUpsertProfile(
      sleepProfile.copy(name = "Dormir Profundo"),
      applyImmediately = false,
      showFeedback = false,
    )
    val afterAutoSave = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals("Dormir Profundo", afterAutoSave.profiles.first { it.id == "preset_sleep" }.name)
    assertNull(afterAutoSave.statusMessage)
  }

  @Test
  fun onSyncLocationsWithGps_activatesGeofenceWhenInsideRadius_andDeactivatesWhenOutside() = runTest {
    fakePreferences.clearSchedules()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    val collegeRule = LocationRule.defaultLocations.first { it.id == "loc_college" }
    viewModel.onToggleLocationRule(collegeRule, enabled = true)

    // Coordinates ~30m away from loc_college (-23.5505, -46.6333, radius 200m)
    viewModel.onSyncLocationsWithGps(latitude = -23.5506, longitude = -46.6334)
    val insideState = viewModel.uiState.value as MainScreenUiState.Ready
    assertTrue(insideState.locationRules.first { it.id == "loc_college" }.isCurrentlyInside)
    assertEquals(SoundMode.VIBRATE, insideState.currentMode)

    // Coordinates ~5km away -> outside all geofences
    viewModel.onSyncLocationsWithGps(latitude = -23.6000, longitude = -46.7000)
    val outsideState = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(false, outsideState.locationRules.first { it.id == "loc_college" }.isCurrentlyInside)
    assertEquals(SoundMode.NORMAL, outsideState.currentMode)
  }

  @Test
  fun appLabelsAndAppTestTags_provideBilingualTranslationsAndDeterministicTestIds() {
    try {
      AppLabels.currentLanguage = AppLanguage.PT_BR
      assertEquals("Modos de Som", AppLabels.Tabs.nowTopBarTitle)
      assertEquals("Agora", AppLabels.Tabs.nowLabel)
      assertEquals("Som Ativo", SoundMode.NORMAL.title)
      assertEquals("Toque (Ring)", VolumeStreamType.RING.label)

      AppLabels.currentLanguage = AppLanguage.EN
      assertEquals("Sound Modes", AppLabels.Tabs.nowTopBarTitle)
      assertEquals("Now", AppLabels.Tabs.nowLabel)
      assertEquals("Active Sound", SoundMode.NORMAL.title)
      assertEquals("Ring", VolumeStreamType.RING.label)
      assertEquals("Locations", AppLabels.Tabs.locationsLabel)

      assertEquals("tab_now", AppTestTags.Tabs.NOW)
      assertEquals("mode_button_VIBRATE", AppTestTags.Now.modeButton(SoundMode.VIBRATE))
      assertEquals("profile_card_preset_sleep", AppTestTags.Profiles.card("preset_sleep"))
      assertEquals("schedule_switch_sched_work", AppTestTags.Schedules.switch("sched_work"))
      assertEquals("location_card_loc_office", AppTestTags.Locations.card("loc_office"))
      assertEquals("language_button_EN", AppTestTags.Settings.languageButton(AppLanguage.EN))
      assertEquals("topbar_overflow_menu_button", AppTestTags.TopBar.OVERFLOW_MENU_BUTTON)
      assertEquals("settings_category_appearance", AppTestTags.Settings.CATEGORY_APPEARANCE)
    } finally {
      AppLabels.currentLanguage = AppLanguage.PT_BR
    }
  }

  @Test
  fun onSetAutomationsPaused_pausesAndResumesActiveRuleEvaluation() = runTest {
    fakePreferences.clearSchedules()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    val collegeRule = LocationRule.defaultLocations.first { it.id == "loc_college" }
    viewModel.onToggleLocationRule(collegeRule, enabled = true)
    viewModel.onToggleLocationPresence("loc_college", inside = true)
    assertEquals(SoundMode.VIBRATE, (viewModel.uiState.value as MainScreenUiState.Ready).currentMode)

    // Enable Vacation Mode (Pause Automations) -> should restore to Default Day Profile (NORMAL)
    viewModel.onSetAutomationsPaused(true)
    val pausedState = viewModel.uiState.value as MainScreenUiState.Ready
    assertTrue(pausedState.automationsPaused)
    assertEquals(SoundMode.NORMAL, pausedState.currentMode)

    // Resume Automations -> should immediately re-evaluate and re-apply the active location rule (VIBRATE)
    viewModel.onSetAutomationsPaused(false)
    val resumedState = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(false, resumedState.automationsPaused)
    assertEquals(SoundMode.VIBRATE, resumedState.currentMode)
  }

  @Test
  fun onResetFactoryDefaults_andDuplicateItems_andPersistLanguage_workCorrectly() = runTest {
    try {
      backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
        viewModel.uiState.collect()
      }

      // Delete a preset profile and change language to EN
      viewModel.onDeleteCustomProfile("preset_meeting")
      viewModel.onSetAppLanguage(AppLanguage.EN)
      viewModel.onSetDefaultTimerMinutes(60)

      val modifiedState = viewModel.uiState.value as MainScreenUiState.Ready
      assertTrue(modifiedState.profiles.none { it.id == "preset_meeting" })
      assertEquals("EN", modifiedState.appLanguageCode)
      assertEquals(AppLanguage.EN, AppLabels.currentLanguage)
      assertEquals(60, modifiedState.defaultTimerMinutes)

      // Duplicate a profile and a schedule rule
      val sleepProfile = modifiedState.profiles.first { it.id == "preset_sleep" }
      viewModel.onDuplicateProfile(sleepProfile)
      val firstSchedule = modifiedState.scheduleRules.first()
      viewModel.onDuplicateScheduleRule(firstSchedule)

      val afterDuplicate = viewModel.uiState.value as MainScreenUiState.Ready
      assertTrue(afterDuplicate.profiles.any { it.name.contains("(Cópia)") })
      assertEquals(ScheduleRule.defaultSchedules.size + 1, afterDuplicate.scheduleRules.size)

      // Reset factory defaults -> recovers deleted preset_meeting and resets schedules & timer
      viewModel.onResetFactoryDefaults()
      val afterReset = viewModel.uiState.value as MainScreenUiState.Ready
      assertTrue(afterReset.profiles.any { it.id == "preset_meeting" })
      assertEquals(SoundProfile.defaultPresets.size, afterReset.profiles.size)
      assertEquals(ScheduleRule.defaultSchedules.size, afterReset.scheduleRules.size)
      assertEquals(30, afterReset.defaultTimerMinutes)
    } finally {
      AppLabels.currentLanguage = AppLanguage.PT_BR
    }
  }

  @Test
  fun onCompleteOnboarding_andReopenOnboarding_persistOnboardingFlag() = runTest {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    val initialState = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(false, initialState.hasCompletedOnboarding)

    viewModel.onCompleteOnboarding()
    val completedState = viewModel.uiState.value as MainScreenUiState.Ready
    assertTrue(completedState.hasCompletedOnboarding)

    viewModel.onReopenOnboarding()
    val reopenedState = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(false, reopenedState.hasCompletedOnboarding)
  }

  @Test
  fun backupExportAndImport_roundtripAndInvalidJsonHandling_workCorrectly() = runTest {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    // Customize preferences and rules before export
    viewModel.onSetThemeMode(ThemeMode.LIGHT)
    viewModel.onSetRestorePolicy(RestorePolicy.SNAPSHOT)
    viewModel.onSetDefaultDayProfileId("preset_home")
    viewModel.onSetDefaultTimerMinutes(120)
    viewModel.onSaveCurrentAsProfile("Foco Extremo", "Modo de concentração")

    var exportedJson = ""
    viewModel.onExportBackupJson { json -> exportedJson = json }
    assertTrue(exportedJson.contains("Foco Extremo"))
    assertEquals("Backup exportado com sucesso", (viewModel.uiState.value as MainScreenUiState.Ready).statusMessage)

    // Reset factory defaults to wipe custom profile and settings
    viewModel.onResetFactoryDefaults()
    val afterReset = viewModel.uiState.value as MainScreenUiState.Ready
    assertTrue(afterReset.profiles.none { it.name == "Foco Extremo" })
    assertEquals(RestorePolicy.DEFAULT_PROFILE, afterReset.restorePolicy)
    assertEquals(30, afterReset.defaultTimerMinutes)

    // Import valid JSON backup -> restores custom profile and preferences
    viewModel.onImportBackupJson(exportedJson)
    val afterImport = viewModel.uiState.value as MainScreenUiState.Ready
    assertTrue(afterImport.profiles.any { it.name == "Foco Extremo" })
    assertEquals(ThemeMode.LIGHT, afterImport.themeMode)
    assertEquals(RestorePolicy.SNAPSHOT, afterImport.restorePolicy)
    assertEquals("preset_home", afterImport.defaultDayProfileId)
    assertEquals(120, afterImport.defaultTimerMinutes)
    assertEquals("Backup importado e aplicado com sucesso", afterImport.statusMessage)

    // Import invalid JSON -> emits clear error message without crashing
    viewModel.onImportBackupJson("{invalid_json")
    val afterInvalid = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals("JSON de backup inválido. Verifique o conteúdo colado.", afterInvalid.statusMessage)
  }

  @Test
  fun restorePolicy_snapshotVsDefaultProfile_restoresExpectedStateWhenRuleEnds() = runTest {
    fakePreferences.clearSchedules()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    // Set custom volume 42% and SNAPSHOT restore policy before entering location geofence
    viewModel.onStreamVolumeChange(VolumeStreamType.RING, 42)
    viewModel.onSetRestorePolicy(RestorePolicy.SNAPSHOT)

    val collegeRule = LocationRule.defaultLocations.first { it.id == "loc_college" }
    viewModel.onToggleLocationRule(collegeRule, enabled = true)
    viewModel.onToggleLocationPresence("loc_college", inside = true)
    assertEquals(SoundMode.VIBRATE, (viewModel.uiState.value as MainScreenUiState.Ready).currentMode)

    // Leave geofence -> SNAPSHOT restores NORMAL mode and 42% ring volume
    viewModel.onToggleLocationPresence("loc_college", inside = false)
    val afterSnapshotRestore = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(SoundMode.NORMAL, afterSnapshotRestore.currentMode)
    assertEquals(42, afterSnapshotRestore.streams.ringPercent)
  }

  @Test
  fun timerSession_startExtendAndCancel_restoresPreviousModeAndVolumes() = runTest {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    viewModel.onStreamVolumeChange(VolumeStreamType.MEDIA, 55)
    viewModel.onStartTimer(SoundMode.VIBRATE, 30)

    val timerStarted = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(SoundMode.VIBRATE, timerStarted.currentMode)
    assertNotNull(timerStarted.activeTimer)
    assertEquals(30, timerStarted.activeTimer?.durationMinutes)

    viewModel.onExtendTimer(30)
    val timerExtended = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(60, timerExtended.activeTimer?.durationMinutes)

    viewModel.onCancelTimer()
    val timerCancelled = viewModel.uiState.value as MainScreenUiState.Ready
    assertNull(timerCancelled.activeTimer)
    assertEquals(SoundMode.NORMAL, timerCancelled.currentMode)
    assertEquals(55, timerCancelled.streams.mediaPercent)
  }

  @Test
  fun allSettingsPreferences_locationCrud_andQuickTileCycle_updateCorrectly() = runTest {
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    viewModel.onAcceptLocationDisclosure()
    viewModel.onSetUseDynamicColor(false)
    viewModel.onSetHapticFeedbackEnabled(false)
    viewModel.onSetNotifyOnAutoTransition(false)
    viewModel.onSetPreserveMediaOnBluetooth(false)
    viewModel.onSetSmartVolumeTriggerEnabled(false)

    val updatedPrefs = viewModel.uiState.value as MainScreenUiState.Ready
    assertTrue(updatedPrefs.hasAcceptedLocationDisclosure)
    assertEquals(false, updatedPrefs.useDynamicColor)
    assertEquals(false, updatedPrefs.hapticFeedbackEnabled)
    assertEquals(false, updatedPrefs.notifyOnAutoTransition)
    assertEquals(false, updatedPrefs.preserveMediaOnBluetooth)
    assertEquals(false, updatedPrefs.smartVolumeTriggerEnabled)

    // Upsert and delete location rule
    val customLocation = LocationRule(
      id = "loc_custom_gym",
      name = "Academia",
      latitude = -23.56,
      longitude = -46.65,
      radiusMeters = 150,
      targetProfileId = "preset_work",
      enabled = true,
    )
    viewModel.onUpsertLocationRule(customLocation, showFeedback = true)
    assertTrue((viewModel.uiState.value as MainScreenUiState.Ready).locationRules.any { it.id == "loc_custom_gym" })

    viewModel.onDeleteLocationRule("loc_custom_gym")
    assertTrue((viewModel.uiState.value as MainScreenUiState.Ready).locationRules.none { it.id == "loc_custom_gym" })

    // Cycle sound mode from Quick Tile: NORMAL -> VIBRATE -> SILENT -> NORMAL
    assertEquals(SoundMode.VIBRATE, repository.cycleSoundModeFromQuickTile().getOrThrow())
    assertEquals(SoundMode.SILENT, repository.cycleSoundModeFromQuickTile().getOrThrow())
    assertEquals(SoundMode.NORMAL, repository.cycleSoundModeFromQuickTile().getOrThrow())

    // Clear transition logs
    assertTrue((viewModel.uiState.value as MainScreenUiState.Ready).transitionLogs.isNotEmpty())
    viewModel.onClearTransitionLogs()
    assertTrue((viewModel.uiState.value as MainScreenUiState.Ready).transitionLogs.isEmpty())
  }

  @Test
  fun internalRbacAccessControl_enforcesRolesGranularPermissionsAndPinUnlock() = runTest {
    fakePreferences.clearSchedules()
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      viewModel.uiState.collect()
    }

    // 1. Default role is ADMIN with all 7 permissions
    val initialState = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(AppAccessRole.ADMIN, initialState.accessControlConfig.activeRole)
    assertEquals(AppPermission.entries.size, initialState.accessControlConfig.grantedPermissions.size)

    // 2. Configure 4-digit PIN ("2580")
    viewModel.onSetAccessPin("2580")
    val withPin = viewModel.uiState.value as MainScreenUiState.Ready
    assertTrue(withPin.accessControlConfig.isPinRequired)
    assertEquals("2580", withPin.accessControlConfig.pinCode)

    // 3. Switch to OPERATOR -> allows sound/timer changes, blocks profile/schedule/settings edits
    viewModel.onSetAccessRole(AppAccessRole.OPERATOR)
    val operatorState = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(AppAccessRole.OPERATOR, operatorState.accessControlConfig.activeRole)

    // Sound mode switch is allowed for OPERATOR
    viewModel.onSelectMode(SoundMode.VIBRATE)
    assertEquals(SoundMode.VIBRATE, (viewModel.uiState.value as MainScreenUiState.Ready).currentMode)

    // Profile creation and Settings changes are blocked for OPERATOR
    val profilesCountBefore = (viewModel.uiState.value as MainScreenUiState.Ready).profiles.size
    viewModel.onSaveCurrentAsProfile("Perfil Bloqueado", "Desc")
    val afterBlockedProfile = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(profilesCountBefore, afterBlockedProfile.profiles.size)
    assertEquals(
      AppLabels.AccessControl.permissionDeniedMessage(AppPermission.MANAGE_PROFILES),
      afterBlockedProfile.statusMessage,
    )

    viewModel.onSetThemeMode(ThemeMode.LIGHT)
    val afterBlockedSettings = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(ThemeMode.DARK, afterBlockedSettings.themeMode)
    assertEquals(
      AppLabels.AccessControl.permissionDeniedMessage(AppPermission.ACCESS_SETTINGS),
      afterBlockedSettings.statusMessage,
    )

    // 4. Switch to VIEWER -> even sound mode changes are blocked
    viewModel.onSetAccessRole(AppAccessRole.VIEWER)
    viewModel.onSelectMode(SoundMode.NORMAL)
    val afterBlockedSound = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(SoundMode.VIBRATE, afterBlockedSound.currentMode)
    assertEquals(
      AppLabels.AccessControl.permissionDeniedMessage(AppPermission.CHANGE_SOUND_AND_TIMER),
      afterBlockedSound.statusMessage,
    )

    // 5. Enable a single granular permission -> transitions role to CUSTOM
    viewModel.onToggleAccessPermission(AppPermission.PAUSE_AUTOMATIONS, enabled = true)
    val customState = viewModel.uiState.value as MainScreenUiState.Ready
    assertEquals(AppAccessRole.CUSTOM, customState.accessControlConfig.activeRole)
    viewModel.onSetAutomationsPaused(true)
    assertTrue((viewModel.uiState.value as MainScreenUiState.Ready).automationsPaused)

    // 6. Wrong PIN rejected, right PIN ("2580") unlocks ADMIN
    var unlockedCallbackInvoked = false
    viewModel.onUnlockAdminWithPin("0000") { unlockedCallbackInvoked = true }
    assertEquals(false, unlockedCallbackInvoked)
    assertEquals(AppAccessRole.CUSTOM, (viewModel.uiState.value as MainScreenUiState.Ready).accessControlConfig.activeRole)

    viewModel.onUnlockAdminWithPin("2580") { unlockedCallbackInvoked = true }
    assertTrue(unlockedCallbackInvoked)
    assertEquals(AppAccessRole.ADMIN, (viewModel.uiState.value as MainScreenUiState.Ready).accessControlConfig.activeRole)
  }

  @Test
  fun auto04SystemZenRules_andGeo01BackgroundGeofencingWithDwellAndWifiSsid_workCorrectly() = runTest {
    fakePreferences.clearSchedules()
    BackgroundGeofenceManager.clearDwellCandidates()
    var syncedZenRulesCount = 0
    var syncedGeofencesCount = 0
    var scheduledDwellDelayMillis: Long? = null

    val repoWithCallbacks = DefaultSoundModeRepository(
      systemController = fakeSystemController,
      preferencesDataSource = fakePreferences,
      onSyncSystemZenRules = { profiles, _, enabled ->
        if (enabled) {
          syncedZenRulesCount = profiles.size
          profiles.size
        } else {
          syncedZenRulesCount = 0
          0
        }
      },
      onSyncGeofenceRegistrations = { rules, paused ->
        syncedGeofencesCount = if (paused) 0 else rules.count { it.enabled && it.hasCoordinates }
        syncedGeofencesCount
      },
      onScheduleGeofenceDwellCheck = { delayMillis ->
        scheduledDwellDelayMillis = delayMillis
      },
    )
    val vm = MainScreenViewModel(repoWithCallbacks)
    backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
      vm.uiState.collect()
    }

    // 1. [AUTO-04] Condition URI round-trip & Zen Rules toggle + manual sync
    val conditionUriString = SystemZenRuleManager.buildConditionUriString("preset_work_high")
    assertEquals(
      "preset_work_high",
      SystemZenRuleManager.extractProfileIdFromConditionUri(conditionUriString),
    )

    vm.onSyncSystemZenRulesNow()
    assertEquals(SoundProfile.defaultPresets.size, syncedZenRulesCount)

    vm.onSetSystemZenRulesEnabled(false)
    assertEquals(false, (vm.uiState.value as MainScreenUiState.Ready).systemZenRulesEnabled)
    assertEquals(0, syncedZenRulesCount)

    vm.onSetSystemZenRulesEnabled(true)
    assertEquals(true, (vm.uiState.value as MainScreenUiState.Ready).systemZenRulesEnabled)
    assertEquals(SoundProfile.defaultPresets.size, syncedZenRulesCount)

    // 2. [GEO-01] Enable "Faculdade" location rule (-23.5505, -46.6333, Campus-WiFi)
    val collegeRule = LocationRule.defaultLocations.first { it.id == "loc_college" }
    vm.onToggleLocationRule(collegeRule, enabled = true)
    vm.onSyncBackgroundGeofencesNow()
    assertEquals(2, syncedGeofencesCount)

    // Step 2a: Enter geofence WITHOUT matching Wi-Fi -> Pending 2-minute Dwell (does NOT activate yet)
    val t0 = 1_700_000_000_000L
    scheduledDwellDelayMillis = null
    repoWithCallbacks.processBackgroundGeofenceSignal(
      latitude = collegeRule.latitude,
      longitude = collegeRule.longitude,
      connectedWifiSsid = "Rede-Visitante",
      nowEpochMillis = t0,
    )
    val afterPendingDwell = vm.uiState.value as MainScreenUiState.Ready
    assertEquals(BackgroundGeofenceManager.DEFAULT_DWELL_DURATION_MILLIS, scheduledDwellDelayMillis)
    assertEquals(false, afterPendingDwell.locationRules.first { it.id == "loc_college" }.isCurrentlyInside)
    assertEquals(SoundMode.NORMAL, afterPendingDwell.currentMode)

    // Step 2b: After 2 minutes (120_000 ms) inside the radius -> Dwell confirmed, switches to VIBRATE
    scheduledDwellDelayMillis = null
    repoWithCallbacks.processBackgroundGeofenceSignal(
      latitude = collegeRule.latitude,
      longitude = collegeRule.longitude,
      connectedWifiSsid = "Rede-Visitante",
      nowEpochMillis = t0 + BackgroundGeofenceManager.DEFAULT_DWELL_DURATION_MILLIS,
    )
    val afterConfirmedDwell = vm.uiState.value as MainScreenUiState.Ready
    assertNull(scheduledDwellDelayMillis)
    assertEquals(true, afterConfirmedDwell.locationRules.first { it.id == "loc_college" }.isCurrentlyInside)
    assertEquals(SoundMode.VIBRATE, afterConfirmedDwell.currentMode)

    // Step 2c: Exit geofence -> ConfirmedOutside, restores NORMAL mode
    repoWithCallbacks.processBackgroundGeofenceSignal(
      latitude = -23.6000,
      longitude = -46.7000,
      connectedWifiSsid = null,
      nowEpochMillis = t0 + 300_000L,
    )
    val afterExit = vm.uiState.value as MainScreenUiState.Ready
    assertEquals(false, afterExit.locationRules.first { it.id == "loc_college" }.isCurrentlyInside)
    assertEquals(SoundMode.NORMAL, afterExit.currentMode)

    // Step 2d: Re-enter with matching Wi-Fi SSID ("Campus-WiFi") -> Instant confirmation (0 dwell wait)
    repoWithCallbacks.processBackgroundGeofenceSignal(
      latitude = collegeRule.latitude,
      longitude = collegeRule.longitude,
      connectedWifiSsid = "\"Campus-WiFi\"",
      nowEpochMillis = t0 + 400_000L,
    )
    val afterInstantWifiConfirm = vm.uiState.value as MainScreenUiState.Ready
    assertEquals(true, afterInstantWifiConfirm.locationRules.first { it.id == "loc_college" }.isCurrentlyInside)
    assertEquals(SoundMode.VIBRATE, afterInstantWifiConfirm.currentMode)
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
    state.value = state.value.copy(
      customProfiles = updated,
      deletedProfileIds = state.value.deletedProfileIds - profile.id,
    )
  }

  override suspend fun deleteCustomProfile(profileId: String) {
    val updated = state.value.customProfiles.filterNot { it.id == profileId }
    state.value = state.value.copy(
      customProfiles = updated,
      deletedProfileIds = state.value.deletedProfileIds + profileId,
    )
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

  override suspend fun clearTransitionLogs() {
    state.value = state.value.copy(transitionLogs = emptyList())
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

  override suspend fun setAppLanguageCode(languageCode: String) {
    state.value = state.value.copy(appLanguageCode = languageCode)
  }

  override suspend fun setAutomationsPaused(paused: Boolean) {
    state.value = state.value.copy(automationsPaused = paused)
  }

  override suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
    state.value = state.value.copy(hapticFeedbackEnabled = enabled)
  }

  override suspend fun setNotifyOnAutoTransition(enabled: Boolean) {
    state.value = state.value.copy(notifyOnAutoTransition = enabled)
  }

  override suspend fun setDefaultTimerMinutes(minutes: Int) {
    state.value = state.value.copy(defaultTimerMinutes = minutes)
  }

  override suspend fun setOnboardingCompleted(completed: Boolean) {
    state.value = state.value.copy(hasCompletedOnboarding = completed)
  }

  override suspend fun setAccessControlConfig(config: AccessControlConfig) {
    state.value = state.value.copy(accessControlConfig = config)
  }

  override suspend fun setSystemZenRulesEnabled(enabled: Boolean) {
    state.value = state.value.copy(systemZenRulesEnabled = enabled)
  }

  override suspend fun resetFactoryDefaults() {
    state.value = PersistedSoundPrefs(
      themeMode = state.value.themeMode,
      appLanguageCode = state.value.appLanguageCode,
    )
  }

  override suspend fun exportBackupJson(): String {
    val current = state.value
    val payload = SoundPrefsBackupPayload(
      version = 1,
      customProfiles = current.customProfiles,
      deletedProfileIds = current.deletedProfileIds,
      scheduleRules = current.scheduleRules,
      locationRules = current.locationRules,
      restorePolicy = current.restorePolicy,
      defaultDayProfileId = current.defaultDayProfileId,
      useDynamicColor = current.useDynamicColor,
      themeMode = current.themeMode,
      appLanguageCode = current.appLanguageCode,
      automationsPaused = current.automationsPaused,
      hapticFeedbackEnabled = current.hapticFeedbackEnabled,
      notifyOnAutoTransition = current.notifyOnAutoTransition,
      defaultTimerMinutes = current.defaultTimerMinutes,
      muteMediaOnSilent = current.muteMediaOnSilent,
      preserveMediaOnBluetooth = current.preserveMediaOnBluetooth,
      smartVolumeTriggerEnabled = current.smartVolumeTriggerEnabled,
      persistentQuickBarEnabled = current.persistentQuickBarEnabled,
      quickTileSlot1ProfileId = current.quickTileSlot1ProfileId,
      quickTileSlot2ProfileId = current.quickTileSlot2ProfileId,
      accessControlConfig = current.accessControlConfig,
      systemZenRulesEnabled = current.systemZenRulesEnabled,
    )
    return Json.encodeToString(payload)
  }

  override suspend fun importBackupJson(rawJson: String) {
    val json = Json { ignoreUnknownKeys = true }
    val payload = json.decodeFromString<SoundPrefsBackupPayload>(rawJson.trim())
    state.value = state.value.copy(
      customProfiles = payload.customProfiles,
      deletedProfileIds = payload.deletedProfileIds,
      scheduleRules = payload.scheduleRules,
      locationRules = payload.locationRules,
      restorePolicy = payload.restorePolicy,
      defaultDayProfileId = payload.defaultDayProfileId,
      useDynamicColor = payload.useDynamicColor,
      themeMode = payload.themeMode,
      appLanguageCode = payload.appLanguageCode,
      automationsPaused = payload.automationsPaused,
      hapticFeedbackEnabled = payload.hapticFeedbackEnabled,
      notifyOnAutoTransition = payload.notifyOnAutoTransition,
      defaultTimerMinutes = payload.defaultTimerMinutes,
      muteMediaOnSilent = payload.muteMediaOnSilent,
      preserveMediaOnBluetooth = payload.preserveMediaOnBluetooth,
      smartVolumeTriggerEnabled = payload.smartVolumeTriggerEnabled,
      persistentQuickBarEnabled = payload.persistentQuickBarEnabled,
      quickTileSlot1ProfileId = payload.quickTileSlot1ProfileId,
      quickTileSlot2ProfileId = payload.quickTileSlot2ProfileId,
      accessControlConfig = payload.accessControlConfig,
      systemZenRulesEnabled = payload.systemZenRulesEnabled,
    )
  }
}
