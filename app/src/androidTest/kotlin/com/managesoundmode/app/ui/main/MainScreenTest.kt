package com.managesoundmode.app.ui.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.managesoundmode.app.domain.model.AccessControlConfig
import com.managesoundmode.app.domain.model.AppAccessRole
import com.managesoundmode.app.domain.model.AppPermission
import com.managesoundmode.app.domain.model.LocationRule
import com.managesoundmode.app.domain.model.RestorePolicy
import com.managesoundmode.app.domain.model.ScheduleRule
import com.managesoundmode.app.domain.model.SoundMode
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.domain.model.ThemeMode
import com.managesoundmode.app.domain.model.TimedModeSession
import com.managesoundmode.app.domain.model.TransitionLogEntry
import com.managesoundmode.app.domain.model.TransitionTriggerSource
import com.managesoundmode.app.domain.model.VolumeStreamType
import com.managesoundmode.app.domain.model.VolumeStreams
import com.managesoundmode.app.tile.QuickTileKind
import com.managesoundmode.app.ui.common.AppLabels
import com.managesoundmode.app.ui.common.AppLanguage
import com.managesoundmode.app.ui.common.AppTestTags
import com.managesoundmode.app.ui.theme.ManageSoundModeTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Suíte completa de testes instrumentados de UI (Jetpack Compose) cobrindo 100% dos casos de uso
 * do aplicativo utilizando exclusivamente seletores centralizados em [AppTestTags].
 */
class MainScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  @After
  fun resetLanguage() {
    AppLabels.currentLanguage = AppLanguage.PT_BR
  }

  // =========================================================================
  // UC-01 a UC-04: Onboarding de 1ª Execução & Setup Rápido de Permissões
  // =========================================================================

  @Test
  fun uc01_to_uc04_onboardingWizard_coversAllThreeStepsPermissionsAndPreferences() {
    var onboardingCompleted = false
    var dndRequested = false
    var notificationsRequested = false
    var exactAlarmRequested = false
    var locationDisclosureAccepted = false
    var locationRequested = false
    var selectedDefaultProfileId = SoundProfile.DEFAULT_DAY_PROFILE_ID
    var muteMediaEnabled = true

    composeTestRule.setContent {
      var hasAcceptedDisclosure by mutableStateOf(false)
      ManageSoundModeTheme {
        MainDashboardContent(
          state = MainScreenUiState.Ready(
            currentMode = SoundMode.NORMAL,
            streams = VolumeStreams(),
            hasDndPermission = false,
            hasNotificationPermission = false,
            canScheduleExactAlarms = false,
            hasLocationPermission = false,
            hasAcceptedLocationDisclosure = hasAcceptedDisclosure,
            profiles = SoundProfile.defaultPresets,
            hasCompletedOnboarding = false,
          ),
          onSelectMode = {},
          onStreamVolumeChange = { _, _ -> },
          onActivateProfile = {},
          onStartTimer = { _, _ -> },
          onCancelTimer = {},
          onSaveProfile = { _, _ -> },
          onDismissMessage = {},
          onRequestDndPermission = { dndRequested = true },
          onRequestNotificationPermission = { notificationsRequested = true },
          onRequestExactAlarmPermission = { exactAlarmRequested = true },
          onAcceptLocationDisclosure = {
            locationDisclosureAccepted = true
            hasAcceptedDisclosure = true
          },
          onRequestLocationPermission = { locationRequested = true },
          onSetDefaultDayProfileId = { selectedDefaultProfileId = it },
          onSetMuteMediaOnSilent = { muteMediaEnabled = it },
          onCompleteOnboarding = { onboardingCompleted = true },
        )
      }
    }

    // Passo 1: Boas-vindas visível
    composeTestRule.onNodeWithTag(AppTestTags.Onboarding.DIALOG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AppTestTags.Onboarding.stepPill(1)).assertIsDisplayed()

    // Avançar para Passo 2 (Permissões)
    composeTestRule.onNodeWithTag(AppTestTags.Onboarding.NEXT_BUTTON).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.Onboarding.GRANT_DND_BUTTON).performClick()
    assertTrue(dndRequested)

    composeTestRule.onNodeWithTag(AppTestTags.Onboarding.GRANT_NOTIFICATIONS_BUTTON).performClick()
    assertTrue(notificationsRequested)

    composeTestRule.onNodeWithTag(AppTestTags.Onboarding.GRANT_EXACT_ALARM_BUTTON).performClick()
    assertTrue(exactAlarmRequested)

    // Localização abre Prominent Disclosure primeiro quando ainda não aceito
    composeTestRule.onNodeWithTag(AppTestTags.Onboarding.GRANT_LOCATION_BUTTON).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.Locations.ACCEPT_DISCLOSURE_BUTTON).performClick()
    assertTrue(locationDisclosureAccepted)
    assertTrue(locationRequested)

    // Avançar para Passo 3 (Perfil Padrão & Preferências)
    composeTestRule.onNodeWithTag(AppTestTags.Onboarding.NEXT_BUTTON).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.Onboarding.defaultProfileChip("preset_home")).performClick()
    assertEquals("preset_home", selectedDefaultProfileId)

    // Testar botão Voltar e avançar novamente para Concluir
    composeTestRule.onNodeWithTag(AppTestTags.Onboarding.BACK_BUTTON).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.Onboarding.NEXT_BUTTON).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.Onboarding.FINISH_BUTTON).performClick()
    assertTrue(onboardingCompleted)
  }

  // =========================================================================
  // UC-05 a UC-10: Aba Agora (Modos, Timer, Override Manual, DND e Volumes)
  // =========================================================================

  @Test
  fun uc05_to_uc10_nowTab_coversModesTimerOverrideBannerDndAndVolumeSliders() {
    var selectedMode: SoundMode? = null
    var dndClicked = false
    var reapplyClicked = false
    var startedTimerPair: Pair<SoundMode, Int>? = null
    var extendedMinutes: Int? = null
    var timerCancelled = false

    val now = System.currentTimeMillis()

    composeTestRule.setContent {
      var activeTimer by mutableStateOf<TimedModeSession?>(null)
      var currentMode by mutableStateOf(SoundMode.VIBRATE)

      ManageSoundModeTheme {
        MainDashboardContent(
          state = MainScreenUiState.Ready(
            currentMode = currentMode,
            streams = VolumeStreams(ringPercent = 70, notificationPercent = 60, mediaPercent = 40, alarmPercent = 90),
            hasDndPermission = false,
            isManuallyOverridden = true,
            activeTimer = activeTimer,
            profiles = SoundProfile.defaultPresets,
            activeProfileId = "preset_work_high",
          ),
          onSelectMode = {
            selectedMode = it
            currentMode = it
          },
          onStreamVolumeChange = { _, _ -> },
          onActivateProfile = {},
          onStartTimer = { mode, mins ->
            startedTimerPair = mode to mins
            activeTimer = TimedModeSession(
              targetMode = mode,
              previousMode = SoundMode.NORMAL,
              previousStreams = VolumeStreams(),
              durationMinutes = mins,
              startedAtEpochMillis = now,
              endsAtEpochMillis = now + mins * 60_000L,
            )
          },
          onExtendTimer = { addMins -> extendedMinutes = addMins },
          onCancelTimer = {
            timerCancelled = true
            activeTimer = null
          },
          onSaveProfile = { _, _ -> },
          onDismissMessage = {},
          onRequestDndPermission = { dndClicked = true },
          onReapplyActiveRule = { reapplyClicked = true },
        )
      }
    }

    // UC-06: Banner DND e botão Ativar
    composeTestRule.onNodeWithTag(AppTestTags.Now.DND_PERMISSION_BANNER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AppTestTags.Now.DND_ENABLE_BUTTON).performClick()
    assertTrue(dndClicked)

    // UC-08: Banner de Override Manual e botão Reaplicar
    composeTestRule.onNodeWithTag(AppTestTags.Now.MANUAL_OVERRIDE_BANNER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AppTestTags.Now.REAPPLY_RULE_BUTTON).performClick()
    assertTrue(reapplyClicked)

    // UC-05: Alternar modos pelo Hero Card
    composeTestRule.onNodeWithTag(AppTestTags.Now.HERO_MODE_CARD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AppTestTags.Now.modeButton(SoundMode.SILENT)).performClick()
    assertEquals(SoundMode.SILENT, selectedMode)

    // UC-07: Iniciar Timer Contextual de 30m, estender +30m e cancelar/restaurar
    composeTestRule.onNodeWithTag(AppTestTags.Now.timerChip(30)).performClick()
    assertEquals(SoundMode.SILENT to 30, startedTimerPair)

    composeTestRule.onNodeWithTag(AppTestTags.Now.EXTEND_TIMER_BUTTON).performClick()
    assertEquals(30, extendedMinutes)

    composeTestRule.onNodeWithTag(AppTestTags.Now.CANCEL_TIMER_BUTTON).performClick()
    assertTrue(timerCancelled)

    // UC-10: Verificar presença dos 4 Sliders de Volume
    VolumeStreamType.entries.forEach { streamType ->
      composeTestRule
        .onNodeWithTag(AppTestTags.Now.streamSlider(streamType))
        .performScrollTo()
        .assertIsDisplayed()
    }

    // UC-09: Clicar no Chip do Perfil Ativo navega para a aba Perfis
    composeTestRule.onNodeWithTag(AppTestTags.Now.ACTIVE_PROFILE_CHIP).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.Profiles.LIST).assertIsDisplayed()
  }

  // =========================================================================
  // UC-11 a UC-15: Aba Perfis (Ativar, Criar, Editar, Duplicar, Padrão e Excluir)
  // =========================================================================

  @Test
  fun uc11_to_uc15_profilesTab_coversActivateCreateEditDuplicateSetDefaultAndDelete() {
    var activatedProfileId: String? = null
    var upsertedProfile: SoundProfile? = null
    var duplicatedProfileId: String? = null
    var defaultProfileId: String? = null
    var deletedProfileId: String? = null

    composeTestRule.setContent {
      ManageSoundModeTheme {
        MainDashboardContent(
          state = MainScreenUiState.Ready(
            currentMode = SoundMode.NORMAL,
            streams = VolumeStreams(),
            hasDndPermission = true,
            profiles = SoundProfile.defaultPresets,
            defaultDayProfileId = SoundProfile.DEFAULT_DAY_PROFILE_ID,
          ),
          onSelectMode = {},
          onStreamVolumeChange = { _, _ -> },
          onActivateProfile = { activatedProfileId = it.id },
          onStartTimer = { _, _ -> },
          onCancelTimer = {},
          onSaveProfile = { _, _ -> },
          onUpsertProfile = { profile, _ -> upsertedProfile = profile },
          onDuplicateProfile = { duplicatedProfileId = it.id },
          onSetDefaultDayProfileId = { defaultProfileId = it },
          onDeleteCustomProfile = { deletedProfileId = it },
          onDismissMessage = {},
          onRequestDndPermission = {},
        )
      }
    }

    // Abrir aba Perfis
    composeTestRule.onNodeWithTag(AppTestTags.Tabs.PROFILES).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.Profiles.LIST).assertIsDisplayed()

    // UC-14: Menu contextual (Duplicar, Definir como padrão, Excluir)
    composeTestRule.onNodeWithTag(AppTestTags.Profiles.editButton("preset_sleep")).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.Profiles.menuDuplicateItem("preset_sleep")).performClick()
    assertEquals("preset_sleep", duplicatedProfileId)

    composeTestRule.onNodeWithTag(AppTestTags.Profiles.editButton("preset_sleep")).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.Profiles.menuSetDefaultItem("preset_sleep")).performClick()
    assertEquals("preset_sleep", defaultProfileId)

    composeTestRule.onNodeWithTag(AppTestTags.Profiles.editButton("preset_sleep")).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.Profiles.menuDeleteItem("preset_sleep")).performClick()
    assertEquals("preset_sleep", deletedProfileId)

    // UC-12: Criar Novo Perfil pelo FAB
    composeTestRule.onNodeWithTag(AppTestTags.Fab.SAVE_PROFILE_BUTTON).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.Profiles.NAME_INPUT).performTextInput("Cinema VIP")
    composeTestRule.onNodeWithTag(AppTestTags.Profiles.CONFIRM_SAVE_BUTTON).performClick()
    assertNotNull(upsertedProfile)
    assertEquals("Cinema VIP", upsertedProfile?.name)

    // UC-13: Editar perfil pelo menu e excluir pelo botão de topo do editor
    composeTestRule.onNodeWithTag(AppTestTags.Profiles.editButton("preset_work")).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.Profiles.menuEditItem("preset_work")).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.Profiles.DELETE_TOP_BUTTON).performClick()
    assertEquals("preset_work", deletedProfileId)

    // UC-11: Tocar no card ativa o perfil e retorna automaticamente para a aba Agora
    composeTestRule.onNodeWithTag(AppTestTags.Profiles.card("preset_work_high")).performClick()
    assertEquals("preset_work_high", activatedProfileId)
    composeTestRule.onNodeWithTag(AppTestTags.Now.HERO_MODE_CARD).assertIsDisplayed()
  }

  // =========================================================================
  // UC-16 a UC-19: Aba Horários (Toggle, Alarme Exato, Criar, Editar, Duplicar e Excluir)
  // =========================================================================

  @Test
  fun uc16_to_uc19_schedulesTab_coversToggleExactAlarmBannerCreateDuplicateAndDelete() {
    var toggledRulePair: Pair<String, Boolean>? = null
    var exactAlarmAuthorized = false
    var upsertedSchedule: ScheduleRule? = null
    var duplicatedScheduleId: String? = null
    var deletedScheduleId: String? = null

    composeTestRule.setContent {
      ManageSoundModeTheme {
        MainDashboardContent(
          state = MainScreenUiState.Ready(
            currentMode = SoundMode.NORMAL,
            streams = VolumeStreams(),
            hasDndPermission = true,
            canScheduleExactAlarms = false,
            profiles = SoundProfile.defaultPresets,
            scheduleRules = ScheduleRule.defaultSchedules,
          ),
          onSelectMode = {},
          onStreamVolumeChange = { _, _ -> },
          onActivateProfile = {},
          onStartTimer = { _, _ -> },
          onCancelTimer = {},
          onSaveProfile = { _, _ -> },
          onDismissMessage = {},
          onRequestDndPermission = {},
          onRequestExactAlarmPermission = { exactAlarmAuthorized = true },
          onToggleScheduleRule = { rule, enabled -> toggledRulePair = rule.id to enabled },
          onUpsertScheduleRule = { upsertedSchedule = it },
          onDuplicateScheduleRule = { duplicatedScheduleId = it.id },
          onDeleteScheduleRule = { deletedScheduleId = it },
        )
      }
    }

    // Abrir aba Horários
    composeTestRule.onNodeWithTag(AppTestTags.Tabs.SCHEDULES).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.Schedules.LIST).assertIsDisplayed()

    // UC-19: Banner de Alarme Exato e botão Autorizar
    composeTestRule.onNodeWithTag(AppTestTags.Schedules.EXACT_ALARM_BANNER).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AppTestTags.Schedules.AUTHORIZE_EXACT_ALARM_BUTTON).performClick()
    assertTrue(exactAlarmAuthorized)

    // UC-16: Alternar switch de horário
    val firstRule = ScheduleRule.defaultSchedules.first()
    composeTestRule.onNodeWithTag(AppTestTags.Schedules.switch(firstRule.id)).performClick()
    assertEquals(firstRule.id to !firstRule.enabled, toggledRulePair)

    // Menu contextual do Horário: Duplicar e Excluir
    composeTestRule.onNodeWithTag(AppTestTags.Schedules.optionsButton(firstRule.id)).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.Schedules.menuDuplicateItem(firstRule.id)).performClick()
    assertEquals(firstRule.id, duplicatedScheduleId)

    composeTestRule.onNodeWithTag(AppTestTags.Schedules.optionsButton(firstRule.id)).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.Schedules.menuDeleteItem(firstRule.id)).performClick()
    assertEquals(firstRule.id, deletedScheduleId)

    // UC-17: Criar novo horário pelo FAB
    composeTestRule.onNodeWithTag(AppTestTags.Fab.ADD_SCHEDULE_FAB).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.Schedules.NAME_INPUT).performTextClearance()
    composeTestRule.onNodeWithTag(AppTestTags.Schedules.NAME_INPUT).performTextInput("Plantão Noturno")
    composeTestRule.onNodeWithTag(AppTestTags.Schedules.CONFIRM_SAVE_BUTTON).performClick()
    assertNotNull(upsertedSchedule)
    assertEquals("Plantão Noturno", upsertedSchedule?.name)

    // UC-18: Abrir editor clicando no card e excluir pelo botão do topo
    composeTestRule.onNodeWithTag(AppTestTags.Schedules.card(firstRule.id)).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.Schedules.DELETE_TOP_BUTTON).performClick()
    assertEquals(firstRule.id, deletedScheduleId)
  }

  // =========================================================================
  // UC-20 a UC-23: Aba Locais (Disclosure, Cercas, Presença, Criar e Excluir)
  // =========================================================================

  @Test
  fun uc20_to_uc23_locationsTab_coversDisclosureTogglePresenceCreateAndDelete() {
    var disclosureAccepted = false
    var toggledLocationPair: Pair<String, Boolean>? = null
    var presenceToggledPair: Pair<String, Boolean>? = null
    var upsertedLocation: LocationRule? = null
    var deletedLocationId: String? = null

    composeTestRule.setContent {
      var hasAccepted by mutableStateOf(false)
      ManageSoundModeTheme {
        MainDashboardContent(
          state = MainScreenUiState.Ready(
            currentMode = SoundMode.NORMAL,
            streams = VolumeStreams(),
            hasDndPermission = true,
            hasAcceptedLocationDisclosure = hasAccepted,
            profiles = SoundProfile.defaultPresets,
            locationRules = LocationRule.defaultLocations,
          ),
          onSelectMode = {},
          onStreamVolumeChange = { _, _ -> },
          onActivateProfile = {},
          onStartTimer = { _, _ -> },
          onCancelTimer = {},
          onSaveProfile = { _, _ -> },
          onDismissMessage = {},
          onRequestDndPermission = {},
          onAcceptLocationDisclosure = {
            disclosureAccepted = true
            hasAccepted = true
          },
          onToggleLocationRule = { rule, enabled -> toggledLocationPair = rule.id to enabled },
          onToggleLocationPresence = { id, inside -> presenceToggledPair = id to inside },
          onUpsertLocationRule = { upsertedLocation = it },
          onDeleteLocationRule = { deletedLocationId = it },
        )
      }
    }

    // Abrir aba Locais
    composeTestRule.onNodeWithTag(AppTestTags.Tabs.LOCATIONS).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.Locations.LIST).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AppTestTags.Locations.SYNC_GPS_CARD).assertIsDisplayed()

    val firstLoc = LocationRule.defaultLocations.first()

    // UC-20: Alternar switch do local
    composeTestRule.onNodeWithTag(AppTestTags.Locations.switch(firstLoc.id)).performClick()
    assertEquals(firstLoc.id to !firstLoc.enabled, toggledLocationPair)

    // UC-23: Alternar presença (Entrar/Sair da cerca) pelo menu contextual e excluir
    composeTestRule.onNodeWithTag(AppTestTags.Locations.optionsButton(firstLoc.id)).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.Locations.menuPresenceItem(firstLoc.id)).performClick()
    assertEquals(firstLoc.id to !firstLoc.isCurrentlyInside, presenceToggledPair)

    composeTestRule.onNodeWithTag(AppTestTags.Locations.optionsButton(firstLoc.id)).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.Locations.menuDeleteItem(firstLoc.id)).performClick()
    assertEquals(firstLoc.id, deletedLocationId)

    // UC-21 & UC-22: Clicar em + Novo Local exibe Prominent Disclosure antes de abrir o editor
    composeTestRule.onNodeWithTag(AppTestTags.Fab.ADD_LOCATION_FAB).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.Locations.ACCEPT_DISCLOSURE_BUTTON).performClick()
    assertTrue(disclosureAccepted)

    // Editor de Local aberto após aceitar o Disclosure
    composeTestRule.onNodeWithTag(AppTestTags.Locations.NAME_INPUT).performTextClearance()
    composeTestRule.onNodeWithTag(AppTestTags.Locations.NAME_INPUT).performTextInput("Hospital Central")
    composeTestRule.onNodeWithTag(AppTestTags.Locations.CONFIRM_SAVE_BUTTON).performClick()
    assertNotNull(upsertedLocation)
    assertEquals("Hospital Central", upsertedLocation?.name)
  }

  // =========================================================================
  // UC-24 a UC-30: Menu Global (⋮) & Hub Categorizado de Ajustes (8 Sub-telas)
  // =========================================================================

  @Test
  fun uc24_to_uc30_globalMenuAndCategorizedSettingsHub_coversAllEightSubScreensAndActions() {
    var automationsPausedState = false
    var factoryResetCalled = false
    var reopenedOnboarding = false
    var selectedTheme = ThemeMode.DARK
    var selectedLanguage = AppLanguage.PT_BR
    var dynamicColorState = true
    var hapticState = true
    var selectedRestorePolicy = RestorePolicy.DEFAULT_PROFILE
    var selectedDefaultReturnProfile = SoundProfile.DEFAULT_DAY_PROFILE_ID
    var notifyAutoState = true
    var suggestTimerState = true
    var testedSmartNotification = false
    var muteMediaState = true
    var preserveBtState = true
    var defaultTimerMins = 30
    var quickBarState = true
    var requestedQuickTileKind: QuickTileKind? = null
    var pinnedWidget = false
    var clearedLogs = false
    var exportedBackup = false
    var importedJson: String? = null

    val sampleLogs = listOf(
      TransitionLogEntry(
        id = "log_1",
        timestampEpochMillis = System.currentTimeMillis(),
        title = "Modo alterado para Vibrar",
        detail = "Acionado manualmente",
        source = TransitionTriggerSource.MANUAL,
        appliedMode = SoundMode.VIBRATE,
      ),
    )

    composeTestRule.setContent {
      var paused by mutableStateOf(false)
      ManageSoundModeTheme {
        MainDashboardContent(
          state = MainScreenUiState.Ready(
            currentMode = SoundMode.NORMAL,
            streams = VolumeStreams(),
            hasDndPermission = true,
            profiles = SoundProfile.defaultPresets,
            transitionLogs = sampleLogs,
            automationsPaused = paused,
          ),
          onSelectMode = {},
          onStreamVolumeChange = { _, _ -> },
          onActivateProfile = {},
          onStartTimer = { _, _ -> },
          onCancelTimer = {},
          onSaveProfile = { _, _ -> },
          onDismissMessage = {},
          onRequestDndPermission = {},
          onSetAutomationsPaused = {
            paused = it
            automationsPausedState = it
          },
          onResetFactoryDefaults = { factoryResetCalled = true },
          onReopenOnboarding = { reopenedOnboarding = true },
          onSetThemeMode = { selectedTheme = it },
          onSetAppLanguage = {
            selectedLanguage = it
            AppLabels.currentLanguage = it
          },
          onSetUseDynamicColor = { dynamicColorState = it },
          onSetHapticFeedbackEnabled = { hapticState = it },
          onSetRestorePolicy = { selectedRestorePolicy = it },
          onSetDefaultDayProfileId = { selectedDefaultReturnProfile = it },
          onSetNotifyOnAutoTransition = { notifyAutoState = it },
          onSetSmartVolumeTriggerEnabled = { suggestTimerState = it },
          onTestSmartNotification = { testedSmartNotification = true },
          onSetMuteMediaOnSilent = { muteMediaState = it },
          onSetPreserveMediaOnBluetooth = { preserveBtState = it },
          onSetDefaultTimerMinutes = { defaultTimerMins = it },
          onSetPersistentQuickBarEnabled = { quickBarState = it },
          onRequestAddQuickTile = { kind, _ -> requestedQuickTileKind = kind },
          onPinHomeWidget = { pinnedWidget = true },
          onClearLogs = { clearedLogs = true },
          onExportBackupJson = { callback ->
            exportedBackup = true
            callback("{\"version\":1}")
          },
          onImportBackupJson = { importedJson = it },
        )
      }
    }

    // UC-24: Menu Global (⋮) -> Pausar Automações (Modo Férias)
    composeTestRule.onNodeWithTag(AppTestTags.TopBar.OVERFLOW_MENU_BUTTON).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.TopBar.MENU_PAUSE_AUTOMATIONS).performClick()
    assertTrue(automationsPausedState)

    // Menu Global (⋮) -> Restaurar Padrões de Fábrica + Confirmação
    composeTestRule.onNodeWithTag(AppTestTags.TopBar.OVERFLOW_MENU_BUTTON).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.TopBar.MENU_RESTORE_DEFAULTS).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.Settings.CONFIRM_RESET_DEFAULTS_BUTTON).performClick()
    assertTrue(factoryResetCalled)

    // Menu Global (⋮) -> Assistente de Setup & Permissões
    composeTestRule.onNodeWithTag(AppTestTags.TopBar.OVERFLOW_MENU_BUTTON).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.TopBar.MENU_SETUP_WIZARD).performClick()
    assertTrue(reopenedOnboarding)

    // UC-25: Menu Global (⋮) -> Abrir Hub de Ajustes Full-Screen
    composeTestRule.onNodeWithTag(AppTestTags.TopBar.OVERFLOW_MENU_BUTTON).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.TopBar.MENU_SETTINGS).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.Settings.LIST).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AppTestTags.Settings.MASTER_AUTOMATION_CARD).assertIsDisplayed()

    // UC-26: Sub-tela 1 — Aparência, Tema & Idioma
    composeTestRule.onNodeWithTag(AppTestTags.Settings.CATEGORY_APPEARANCE).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.Settings.themeButton(ThemeMode.LIGHT)).performClick()
    assertEquals(ThemeMode.LIGHT, selectedTheme)
    composeTestRule.onNodeWithTag(AppTestTags.Settings.languageButton(AppLanguage.EN)).performClick()
    assertEquals(AppLanguage.EN, selectedLanguage)
    composeTestRule.onNodeWithTag(AppTestTags.Settings. languageButton(AppLanguage.PT_BR)).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.Settings.DYNAMIC_COLOR_SWITCH).performClick()
    assertEquals(false, dynamicColorState)
    composeTestRule.onNodeWithTag(AppTestTags.Settings.HAPTIC_FEEDBACK_SWITCH).performClick()
    assertEquals(false, hapticState)
    composeTestRule.onNodeWithTag(AppTestTags.Settings.CLOSE_SUBSCREEN_BUTTON).performClick()

    // UC-27: Sub-tela 2 — Automação & Retorno de Rotina
    composeTestRule.onNodeWithTag(AppTestTags.Settings.CATEGORY_AUTOMATION).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.Settings.restorePolicy(RestorePolicy.SNAPSHOT)).performClick()
    assertEquals(RestorePolicy.SNAPSHOT, selectedRestorePolicy)
    composeTestRule.onNodeWithTag(AppTestTags.Settings.defaultReturnProfileChip("preset_home")).performClick()
    assertEquals("preset_home", selectedDefaultReturnProfile)
    composeTestRule.onNodeWithTag(AppTestTags.Settings.NOTIFY_AUTO_TRANSITION_SWITCH).performClick()
    assertEquals(false, notifyAutoState)
    composeTestRule.onNodeWithTag(AppTestTags.Settings.CLOSE_SUBSCREEN_BUTTON).performClick()

    // UC-28: Sub-tela 3 — Botões de Volume, Mídia & Timer
    composeTestRule.onNodeWithTag(AppTestTags.Settings.CATEGORY_AUDIO_MEDIA).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.Settings.SUGGEST_TIMER_SWITCH).performClick()
    assertEquals(false, suggestTimerState)
    composeTestRule.onNodeWithTag(AppTestTags.Settings.TEST_SMART_NOTIFICATION_BUTTON).performClick()
    assertTrue(testedSmartNotification)
    composeTestRule.onNodeWithTag(AppTestTags.Settings.MUTE_MEDIA_SWITCH).performClick()
    assertEquals(false, muteMediaState)
    composeTestRule.onNodeWithTag(AppTestTags.Settings.PRESERVE_BT_SWITCH).performClick()
    assertEquals(false, preserveBtState)
    composeTestRule.onNodeWithTag(AppTestTags.Settings.defaultTimerChip(60)).performClick()
    assertEquals(60, defaultTimerMins)
    composeTestRule.onNodeWithTag(AppTestTags.Settings.CLOSE_SUBSCREEN_BUTTON).performClick()

    // UC-29: Sub-tela 4 — Atalhos no Header & Widgets
    composeTestRule.onNodeWithTag(AppTestTags.Settings.CATEGORY_GADGETS_WIDGETS).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.Settings.PERSISTENT_QUICK_BAR_SWITCH).performClick()
    assertEquals(false, quickBarState)
    composeTestRule.onNodeWithTag(AppTestTags.Settings.ADD_TILE_VIBRATE).performClick()
    assertEquals(QuickTileKind.MODE_VIBRATE, requestedQuickTileKind)
    composeTestRule.onNodeWithTag(AppTestTags.Settings.PIN_HOME_WIDGET_BUTTON).performScrollTo().performClick()
    assertTrue(pinnedWidget)
    composeTestRule.onNodeWithTag(AppTestTags.Settings.CLOSE_SUBSCREEN_BUTTON).performClick()

    // UC-30a: Sub-tela 5 — Permissões & Diagnóstico
    composeTestRule.onNodeWithTag(AppTestTags.Settings.CATEGORY_PERMISSIONS).performScrollTo().performClick()
    reopenedOnboarding = false
    composeTestRule.onNodeWithTag(AppTestTags.Settings.REOPEN_ONBOARDING_BUTTON).performClick()
    assertTrue(reopenedOnboarding)

    // Reabrir Ajustes para testar sub-telas 6, 7 e 8
    composeTestRule.onNodeWithTag(AppTestTags.TopBar.OVERFLOW_MENU_BUTTON).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.TopBar.MENU_SETTINGS).performClick()

    // UC-30b: Sub-tela 6 — Histórico de Transições (Filtros + Limpar)
    composeTestRule.onNodeWithTag(AppTestTags.Settings.CATEGORY_HISTORY).performScrollTo().performClick()
    composeTestRule.onNodeWithTag(AppTestTags.Settings.historyFilterChip("MANUAL")).performClick()
    composeTestRule.onNodeWithText("Modo alterado para Vibrar").assertIsDisplayed()
    composeTestRule.onNodeWithTag(AppTestTags.Settings.CLEAR_HISTORY_BUTTON).performClick()
    assertTrue(clearedLogs)
    composeTestRule.onNodeWithTag(AppTestTags.Settings.CLOSE_SUBSCREEN_BUTTON).performClick()

    // UC-30c: Sub-tela 7 — Backup JSON & Restaurar Padrões
    composeTestRule.onNodeWithTag(AppTestTags.Settings.CATEGORY_BACKUP_RESET).performScrollTo().performClick()
    composeTestRule.onNodeWithTag(AppTestTags.Settings.EXPORT_BACKUP_BUTTON).performClick()
    assertTrue(exportedBackup)
    composeTestRule.onNodeWithTag(AppTestTags.Settings.IMPORT_BACKUP_BUTTON).performClick()
    assertEquals("{\"version\":1}", importedJson)
    composeTestRule.onNodeWithTag(AppTestTags.Settings.CLOSE_SUBSCREEN_BUTTON).performClick()

    // UC-30d: Sub-tela 8 — Sobre o Modos de Som & Privacidade
    composeTestRule.onNodeWithTag(AppTestTags.Settings.CATEGORY_ABOUT).performScrollTo().performClick()
    composeTestRule.onNodeWithText(AppLabels.Settings.privacyCommitmentTitle).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AppTestTags.Settings.CLOSE_SUBSCREEN_BUTTON).performClick()

    // Fechar Hub de Ajustes Full-Screen
    composeTestRule.onNodeWithTag(AppTestTags.Settings.CLOSE_SETTINGS_BUTTON).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.Now.HERO_MODE_CARD).assertIsDisplayed()
  }

  /**
   * UC-31: Controle de Acesso Interno (RBAC + Matriz Granular de 7 Permissões + Proteção por PIN de 4 dígitos).
   */
  @Test
  fun uc31_internalAccessControl_rolesGranularMatrixAndPinChallenge() {
    var accessConfig by mutableStateOf(AccessControlConfig())
    var statusMessageText by mutableStateOf<String?>(null)

    composeTestRule.setContent {
      ManageSoundModeTheme {
        MainDashboardContent(
          state = MainScreenUiState.Ready(
            currentMode = SoundMode.NORMAL,
            streams = VolumeStreams(),
            hasDndPermission = true,
            profiles = SoundProfile.defaultPresets,
            activeProfileId = "preset_home",
            scheduleRules = ScheduleRule.defaultSchedules,
            locationRules = LocationRule.defaultLocations,
            accessControlConfig = accessConfig,
            statusMessage = statusMessageText,
          ),
          onSelectMode = {},
          onStreamVolumeChange = { _, _ -> },
          onActivateProfile = {},
          onStartTimer = { _, _ -> },
          onCancelTimer = {},
          onSaveProfile = { _, _ -> },
          onDismissMessage = { statusMessageText = null },
          onRequestDndPermission = {},
          onSetAccessRole = { role -> accessConfig = accessConfig.withRole(role) },
          onToggleAccessPermission = { perm, enabled ->
            accessConfig = accessConfig.withPermissionEnabled(perm, enabled)
          },
          onSetAccessPin = { pin -> accessConfig = accessConfig.copy(pinCode = pin) },
          onUnlockAdminWithPin = { inputPin, onSuccess ->
            if (accessConfig.verifyPin(inputPin)) {
              accessConfig = accessConfig.withRole(AppAccessRole.ADMIN)
              onSuccess()
            }
          },
          onShowStatusMessage = { msg -> statusMessageText = msg },
        )
      }
    }

    // 1. Abrir Controle de Acesso pelo menu ⋮
    composeTestRule.onNodeWithTag(AppTestTags.TopBar.OVERFLOW_MENU_BUTTON).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.TopBar.MENU_ACCESS_CONTROL).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.AccessControl.SUBSCREEN).assertIsDisplayed()

    // 2. Definir PIN de 4 dígitos ("1357")
    composeTestRule.onNodeWithTag(AppTestTags.AccessControl.PIN_INPUT).performTextInput("1357")
    composeTestRule.onNodeWithTag(AppTestTags.AccessControl.SAVE_PIN_BUTTON).performClick()
    assertEquals("1357", accessConfig.pinCode)

    // 3. Trocar para papel OPERATOR e alternar permissão granular PAUSE_AUTOMATIONS -> vira CUSTOM
    composeTestRule.onNodeWithTag(AppTestTags.AccessControl.roleCard(AppAccessRole.OPERATOR)).performClick()
    assertEquals(AppAccessRole.OPERATOR, accessConfig.activeRole)

    composeTestRule
      .onNodeWithTag(AppTestTags.AccessControl.permissionSwitch(AppPermission.PAUSE_AUTOMATIONS))
      .performScrollTo()
      .performClick()
    assertEquals(AppAccessRole.CUSTOM, accessConfig.activeRole)
    assertTrue(accessConfig.hasPermission(AppPermission.PAUSE_AUTOMATIONS))

    // 4. Fechar sub-tela e verificar chip de papel restrito no TopBar + bloqueio de criação de Perfil
    composeTestRule.onNodeWithTag(AppTestTags.Settings.CLOSE_SUBSCREEN_BUTTON).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.TopBar.ROLE_BADGE_CHIP).assertIsDisplayed()

    composeTestRule.onNodeWithTag(AppTestTags.Tabs.PROFILES).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.Fab.SAVE_PROFILE_BUTTON).performClick()
    assertEquals(
      AppLabels.AccessControl.permissionDeniedMessage(AppPermission.MANAGE_PROFILES),
      statusMessageText,
    )

    // 5. Clicar no chip de papel restrito abre o desafio de PIN; desbloquear Admin com "1357"
    composeTestRule.onNodeWithTag(AppTestTags.TopBar.ROLE_BADGE_CHIP).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.AccessControl.PIN_CHALLENGE_DIALOG).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AppTestTags.AccessControl.PIN_CHALLENGE_INPUT).performTextInput("1357")
    composeTestRule.onNodeWithTag(AppTestTags.AccessControl.PIN_CHALLENGE_QUICK_ADMIN_BUTTON).performClick()
    assertEquals(AppAccessRole.ADMIN, accessConfig.activeRole)
  }

  /**
   * UC-32: [AUTO-04] Modos do Sistema Android 15+ (AutomaticZenRule) e [GEO-01] Geofencing Passivo em Background.
   */
  @Test
  fun uc32_auto04SystemZenRules_andGeo01BackgroundGeofencingStatusCards() {
    var zenRulesEnabled by mutableStateOf(true)
    var syncedZenRules by mutableStateOf(false)
    var syncedGeofences by mutableStateOf(false)

    composeTestRule.setContent {
      ManageSoundModeTheme {
        MainDashboardContent(
          state = MainScreenUiState.Ready(
            currentMode = SoundMode.NORMAL,
            streams = VolumeStreams(),
            hasDndPermission = true,
            profiles = SoundProfile.defaultPresets,
            activeProfileId = "preset_home",
            scheduleRules = ScheduleRule.defaultSchedules,
            locationRules = LocationRule.defaultLocations,
            systemZenRulesEnabled = zenRulesEnabled,
          ),
          onSelectMode = {},
          onStreamVolumeChange = { _, _ -> },
          onActivateProfile = {},
          onStartTimer = { _, _ -> },
          onCancelTimer = {},
          onSaveProfile = { _, _ -> },
          onDismissMessage = {},
          onRequestDndPermission = {},
          onSetSystemZenRulesEnabled = { enabled -> zenRulesEnabled = enabled },
          onSyncSystemZenRulesNow = { syncedZenRules = true },
          onSyncBackgroundGeofencesNow = { syncedGeofences = true },
        )
      }
    }

    // 1. [GEO-01] Aba Locais: Card de Geofencing Passivo + botão de atualizar cercas
    composeTestRule.onNodeWithTag(AppTestTags.Tabs.LOCATIONS).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.Locations.GEOFENCE_STATUS_CARD).assertIsDisplayed()
    composeTestRule.onNodeWithTag(AppTestTags.Locations.SYNC_GEOFENCES_BUTTON).performClick()
    assertTrue(syncedGeofences)

    // 2. [AUTO-04] Configurações -> Automação: Switch e botão de sincronização AutomaticZenRule
    composeTestRule.onNodeWithTag(AppTestTags.TopBar.OVERFLOW_MENU_BUTTON).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.TopBar.MENU_SETTINGS).performClick()
    composeTestRule.onNodeWithTag(AppTestTags.Settings.CATEGORY_AUTOMATION).performClick()

    composeTestRule
      .onNodeWithTag(AppTestTags.Settings.SYSTEM_ZEN_RULES_SWITCH)
      .performScrollTo()
      .performClick()
    assertEquals(false, zenRulesEnabled)

    composeTestRule
      .onNodeWithTag(AppTestTags.Settings.SYNC_ZEN_RULES_BUTTON)
      .performScrollTo()
      .performClick()
    assertTrue(syncedZenRules)
  }
}
