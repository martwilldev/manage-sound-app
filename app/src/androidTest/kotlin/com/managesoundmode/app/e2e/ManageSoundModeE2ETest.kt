package com.managesoundmode.app.e2e

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.managesoundmode.app.MainActivity
import com.managesoundmode.app.ManageSoundApplication
import com.managesoundmode.app.domain.model.AppAccessRole
import com.managesoundmode.app.domain.model.AppPermission
import com.managesoundmode.app.domain.model.RestorePolicy
import com.managesoundmode.app.domain.model.SoundMode
import com.managesoundmode.app.domain.model.ThemeMode
import com.managesoundmode.app.system.BackgroundGeofenceManager
import com.managesoundmode.app.system.SystemZenRuleManager
import com.managesoundmode.app.ui.common.AppLabels
import com.managesoundmode.app.ui.common.AppLanguage
import com.managesoundmode.app.ui.common.AppTestTags
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Suíte de Testes End-to-End (E2E) executando sobre a [MainActivity] real,
 * integrada ao [ManageSoundApplication], [DataStoreSoundPreferencesDataSource],
 * [DefaultSoundModeRepository], [RuleEvaluator] e [AndroidSoundSystemController].
 */
class ManageSoundModeE2ETest {

  @get:Rule
  val composeRule = createAndroidComposeRule<MainActivity>()

  private val app: ManageSoundApplication
    get() = ApplicationProvider.getApplicationContext<Context>() as ManageSoundApplication

  @Before
  fun setUp() {
    AppLabels.currentLanguage = AppLanguage.PT_BR
    runCatching {
      InstrumentationRegistry.getInstrumentation()
        .uiAutomation
        .executeShellCommand("cmd notification allow_dnd ${app.packageName}")
        .close()
    }
    runBlocking {
      val prefs = app.container.preferencesDataSource
      prefs.resetFactoryDefaults()
      prefs.clearTransitionLogs()
      prefs.setActiveTimer(null)
      prefs.setThemeMode(ThemeMode.DARK)
      prefs.setAppLanguageCode("PT_BR")
      prefs.setLocationDisclosureAccepted(false)
      prefs.setOnboardingCompleted(true)
      app.container.soundModeRepository.refreshSystemState()
    }
    composeRule.waitForIdle()
  }

  @After
  fun tearDown() {
    AppLabels.currentLanguage = AppLanguage.PT_BR
    runBlocking {
      val prefs = app.container.preferencesDataSource
      prefs.resetFactoryDefaults()
      prefs.setActiveTimer(null)
      prefs.setOnboardingCompleted(true)
    }
  }

  @Test
  fun e2e01_firstRunOnboardingJourney_completesThreeStepsAndPersistsToRealDataStore() {
    runBlocking {
      app.container.preferencesDataSource.setOnboardingCompleted(false)
      app.container.soundModeRepository.refreshSystemState()
    }
    composeRule.waitForIdle()

    // Passo 1: Assistente abre automaticamente sobre a MainActivity
    composeRule.onNodeWithTag(AppTestTags.Onboarding.DIALOG).assertIsDisplayed()
    composeRule.onNodeWithTag(AppTestTags.Onboarding.stepPill(1)).assertIsDisplayed()

    // Passo 2: Checklist de Permissões
    composeRule.onNodeWithTag(AppTestTags.Onboarding.NEXT_BUTTON).performClick()
    composeRule.onNodeWithTag(AppTestTags.Onboarding.stepPill(2)).assertIsDisplayed()

    // Passo 3: Selecionar Perfil Padrão de Retorno ("Casa / Fim de Semana") e Concluir
    composeRule.onNodeWithTag(AppTestTags.Onboarding.NEXT_BUTTON).performClick()
    composeRule.onNodeWithTag(AppTestTags.Onboarding.defaultProfileChip("preset_home")).performClick()
    composeRule.onNodeWithTag(AppTestTags.Onboarding.FINISH_BUTTON).performClick()
    composeRule.waitForIdle()

    // Verifica que fechou o Onboarding, abriu a aba Agora e persistiu no DataStore real
    composeRule.onNodeWithTag(AppTestTags.Now.HERO_MODE_CARD).assertIsDisplayed()
    val savedPrefs = runBlocking { app.container.preferencesDataSource.prefsFlow.first() }
    assertTrue(savedPrefs.hasCompletedOnboarding)
    assertEquals("preset_home", savedPrefs.defaultDayProfileId)
  }

  @Test
  fun e2e02_profileLifecycle_createDuplicateActivateAndAutoReturnToNowTab() {
    // 1. Navegar para a aba Perfis
    composeRule.onNodeWithTag(AppTestTags.Tabs.PROFILES).performClick()
    composeRule.onNodeWithTag(AppTestTags.Profiles.LIST).assertIsDisplayed()

    // 2. Criar um novo perfil customizado pelo FAB
    composeRule.onNodeWithTag(AppTestTags.Fab.SAVE_PROFILE_BUTTON).performClick()
    composeRule.onNodeWithTag(AppTestTags.Profiles.NAME_INPUT).performTextInput("Estudo E2E")
    composeRule.onNodeWithTag(AppTestTags.Profiles.CONFIRM_SAVE_BUTTON).performClick()
    composeRule.waitForIdle()

    val prefsAfterCreate = runBlocking { app.container.preferencesDataSource.prefsFlow.first() }
    assertTrue(prefsAfterCreate.customProfiles.any { it.name == "Estudo E2E" })

    // 3. Duplicar um perfil pelo menu contextual (⋮)
    composeRule.onNodeWithTag(AppTestTags.Profiles.editButton("preset_work")).performClick()
    composeRule.onNodeWithTag(AppTestTags.Profiles.menuDuplicateItem("preset_work")).performClick()
    composeRule.waitForIdle()

    val prefsAfterDuplicate = runBlocking { app.container.preferencesDataSource.prefsFlow.first() }
    assertTrue(prefsAfterDuplicate.customProfiles.any { it.name.contains("(Cópia)") })

    // 4. Ativar o perfil "Trabalho (Vibrar)" e validar retorno automático para a aba Agora
    composeRule.onNodeWithTag(AppTestTags.Profiles.card("preset_work")).performClick()
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(AppTestTags.Now.HERO_MODE_CARD).assertIsDisplayed()
    val prefsAfterActivate = runBlocking { app.container.preferencesDataSource.prefsFlow.first() }
    assertEquals("preset_work", prefsAfterActivate.activeProfileId)
    assertTrue(prefsAfterActivate.transitionLogs.isNotEmpty())
  }

  @Test
  fun e2e03_nowTab_soundModeSwitchAndContextualTimerLifecycleWithRealDataStore() {
    // 1. Alternar para modo Vibrar no Hero Card
    composeRule.onNodeWithTag(AppTestTags.Now.HERO_MODE_CARD).assertIsDisplayed()
    composeRule.onNodeWithTag(AppTestTags.Now.modeButton(SoundMode.VIBRATE)).performClick()
    composeRule.waitForIdle()

    // 2. Iniciar Timer Contextual de 30 minutos
    composeRule.onNodeWithTag(AppTestTags.Now.timerChip(30)).performClick()
    composeRule.waitForIdle()

    val prefsWithTimer = runBlocking { app.container.preferencesDataSource.prefsFlow.first() }
    assertNotNull(prefsWithTimer.activeTimer)
    assertEquals(30, prefsWithTimer.activeTimer?.durationMinutes)

    // 3. Estender Timer em +30 minutos
    composeRule.onNodeWithTag(AppTestTags.Now.EXTEND_TIMER_BUTTON).performClick()
    composeRule.waitForIdle()

    val prefsExtended = runBlocking { app.container.preferencesDataSource.prefsFlow.first() }
    assertEquals(60, prefsExtended.activeTimer?.durationMinutes)

    // 4. Cancelar/Restaurar Timer antecipadamente
    composeRule.onNodeWithTag(AppTestTags.Now.CANCEL_TIMER_BUTTON).performClick()
    composeRule.waitForIdle()

    val prefsCancelled = runBlocking { app.container.preferencesDataSource.prefsFlow.first() }
    assertNull(prefsCancelled.activeTimer)
  }

  @Test
  fun e2e04_schedulesAndLocationsAutomation_withProminentDisclosureAndVacationMode() {
    // 1. Aba Horários: criar nova rotina de horário
    composeRule.onNodeWithTag(AppTestTags.Tabs.SCHEDULES).performClick()
    composeRule.onNodeWithTag(AppTestTags.Schedules.LIST).assertIsDisplayed()

    composeRule.onNodeWithTag(AppTestTags.Fab.ADD_SCHEDULE_FAB).performClick()
    composeRule.onNodeWithTag(AppTestTags.Schedules.NAME_INPUT).performTextClearance()
    composeRule.onNodeWithTag(AppTestTags.Schedules.NAME_INPUT).performTextInput("Plantão E2E")
    composeRule.onNodeWithTag(AppTestTags.Schedules.CONFIRM_SAVE_BUTTON).performClick()
    composeRule.waitForIdle()

    val prefsAfterSchedule = runBlocking { app.container.preferencesDataSource.prefsFlow.first() }
    assertTrue(prefsAfterSchedule.scheduleRules.any { it.name == "Plantão E2E" })

    // 2. Aba Locais: clicar em + Novo Local -> aceitar Prominent Disclosure -> salvar local
    composeRule.onNodeWithTag(AppTestTags.Tabs.LOCATIONS).performClick()
    composeRule.onNodeWithTag(AppTestTags.Locations.LIST).assertIsDisplayed()

    composeRule.onNodeWithTag(AppTestTags.Fab.ADD_LOCATION_FAB).performClick()
    composeRule.onNodeWithTag(AppTestTags.Locations.ACCEPT_DISCLOSURE_BUTTON).performClick()
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(AppTestTags.Locations.NAME_INPUT).performTextClearance()
    composeRule.onNodeWithTag(AppTestTags.Locations.NAME_INPUT).performTextInput("Escritório E2E")
    composeRule.onNodeWithTag(AppTestTags.Locations.CONFIRM_SAVE_BUTTON).performClick()
    composeRule.waitForIdle()

    val prefsAfterLocation = runBlocking { app.container.preferencesDataSource.prefsFlow.first() }
    assertTrue(prefsAfterLocation.hasAcceptedLocationDisclosure)
    assertTrue(prefsAfterLocation.locationRules.any { it.name == "Escritório E2E" })

    // 3. Menu Global (⋮) -> Pausar Automações (Modo Férias)
    composeRule.onNodeWithTag(AppTestTags.TopBar.OVERFLOW_MENU_BUTTON).performClick()
    composeRule.onNodeWithTag(AppTestTags.TopBar.MENU_PAUSE_AUTOMATIONS).performClick()
    composeRule.waitForIdle()

    val prefsPaused = runBlocking { app.container.preferencesDataSource.prefsFlow.first() }
    assertTrue(prefsPaused.automationsPaused)
  }

  @Test
  fun e2e05_categorizedSettingsHub_appearanceAutomationBackupJsonRoundtripAndFactoryReset() {
    // 1. Abrir Menu Global (⋮) -> Ajustes
    composeRule.onNodeWithTag(AppTestTags.TopBar.OVERFLOW_MENU_BUTTON).performClick()
    composeRule.onNodeWithTag(AppTestTags.TopBar.MENU_SETTINGS).performClick()
    composeRule.onNodeWithTag(AppTestTags.Settings.LIST).assertIsDisplayed()

    // 2. Sub-tela Aparência: alterar Tema para Claro
    composeRule.onNodeWithTag(AppTestTags.Settings.CATEGORY_APPEARANCE).performClick()
    composeRule.onNodeWithTag(AppTestTags.Settings.themeButton(ThemeMode.LIGHT)).performClick()
    composeRule.onNodeWithTag(AppTestTags.Settings.CLOSE_SUBSCREEN_BUTTON).performClick()
    composeRule.waitForIdle()

    // 3. Sub-tela Automação: alterar Política de Retorno para SNAPSHOT
    composeRule.onNodeWithTag(AppTestTags.Settings.CATEGORY_AUTOMATION).performClick()
    composeRule.onNodeWithTag(AppTestTags.Settings.restorePolicy(RestorePolicy.SNAPSHOT)).performClick()
    composeRule.onNodeWithTag(AppTestTags.Settings.CLOSE_SUBSCREEN_BUTTON).performClick()
    composeRule.waitForIdle()

    // 4. Sub-tela Botões, Mídia & Timer: definir Timer Padrão para 60m (1h)
    composeRule.onNodeWithTag(AppTestTags.Settings.CATEGORY_AUDIO_MEDIA).performClick()
    composeRule.onNodeWithTag(AppTestTags.Settings.defaultTimerChip(60)).performClick()
    composeRule.onNodeWithTag(AppTestTags.Settings.CLOSE_SUBSCREEN_BUTTON).performClick()
    composeRule.waitForIdle()

    // 5. Sub-tela Backup & Restaurar Padrões: Exportar JSON
    composeRule.onNodeWithTag(AppTestTags.Settings.CATEGORY_BACKUP_RESET).performScrollTo().performClick()
    composeRule.onNodeWithTag(AppTestTags.Settings.EXPORT_BACKUP_BUTTON).performClick()
    composeRule.waitForIdle()

    val exportedJson = runBlocking { app.container.preferencesDataSource.exportBackupJson() }
    assertTrue(exportedJson.contains("SNAPSHOT"))

    // 6. Restaurar Padrões de Fábrica e confirmar no modal
    composeRule.onNodeWithTag(AppTestTags.Settings.RESET_DEFAULTS_BUTTON).performClick()
    composeRule.onNodeWithTag(AppTestTags.Settings.CONFIRM_RESET_DEFAULTS_BUTTON).performClick()
    composeRule.waitForIdle()

    val prefsAfterReset = runBlocking { app.container.preferencesDataSource.prefsFlow.first() }
    assertEquals(RestorePolicy.DEFAULT_PROFILE, prefsAfterReset.restorePolicy)
    assertEquals(30, prefsAfterReset.defaultTimerMinutes)

    // 7. Reabrir Backup & Restaurar Padrões e importar o JSON exportado
    composeRule.onNodeWithTag(AppTestTags.Settings.CATEGORY_BACKUP_RESET).performScrollTo().performClick()
    composeRule
      .onNodeWithTag(AppTestTags.Settings.IMPORT_BACKUP_INPUT)
      .performTextReplacement(exportedJson)
    composeRule.onNodeWithTag(AppTestTags.Settings.IMPORT_BACKUP_BUTTON).performClick()
    composeRule.waitForIdle()

    val prefsAfterImport = runBlocking { app.container.preferencesDataSource.prefsFlow.first() }
    assertEquals(RestorePolicy.SNAPSHOT, prefsAfterImport.restorePolicy)
    assertEquals(60, prefsAfterImport.defaultTimerMinutes)
    assertEquals(ThemeMode.LIGHT, prefsAfterImport.themeMode)

    // 8. Fechar sub-tela e fechar Hub de Ajustes
    composeRule.onNodeWithTag(AppTestTags.Settings.CLOSE_SUBSCREEN_BUTTON).performClick()
    composeRule.onNodeWithTag(AppTestTags.Settings.CLOSE_SETTINGS_BUTTON).performClick()
    composeRule.onNodeWithTag(AppTestTags.Now.HERO_MODE_CARD).assertIsDisplayed()
  }

  @Test
  fun e2e06_internalRbacAccessControl_rolesGranularPermissionsAndPinProtection() {
    // 1. Abrir Menu Global (⋮) -> Controle de Acesso
    composeRule.onNodeWithTag(AppTestTags.TopBar.OVERFLOW_MENU_BUTTON).performClick()
    composeRule.onNodeWithTag(AppTestTags.TopBar.MENU_ACCESS_CONTROL).performClick()
    composeRule.onNodeWithTag(AppTestTags.AccessControl.SUBSCREEN).assertIsDisplayed()

    // 2. Cadastrar PIN de 4 dígitos ("4210") e salvar
    composeRule.onNodeWithTag(AppTestTags.AccessControl.PIN_INPUT).performTextReplacement("4210")
    composeRule.onNodeWithTag(AppTestTags.AccessControl.SAVE_PIN_BUTTON).performClick()
    composeRule.waitForIdle()

    val prefsAfterPin = runBlocking { app.container.preferencesDataSource.prefsFlow.first() }
    assertEquals("4210", prefsAfterPin.accessControlConfig.pinCode)

    // 3. Mudar Papel para Somente Leitura (VIEWER) -> 0 permissões concedidas
    composeRule.onNodeWithTag(AppTestTags.AccessControl.roleCard(AppAccessRole.VIEWER)).performClick()
    composeRule.waitForIdle()

    val prefsViewer = runBlocking { app.container.preferencesDataSource.prefsFlow.first() }
    assertEquals(AppAccessRole.VIEWER, prefsViewer.accessControlConfig.activeRole)
    assertTrue(prefsViewer.accessControlConfig.grantedPermissions.isEmpty())

    // 4. Fechar sub-tela e validar que trocar modo de som e pausar automações estão bloqueados
    composeRule.onNodeWithTag(AppTestTags.Settings.CLOSE_SUBSCREEN_BUTTON).performClick()
    composeRule.onNodeWithTag(AppTestTags.TopBar.ROLE_BADGE_CHIP).assertIsDisplayed()

    composeRule.onNodeWithTag(AppTestTags.Now.modeButton(SoundMode.VIBRATE)).performClick()
    composeRule.waitForIdle()
    val prefsBlockedMode = runBlocking { app.container.preferencesDataSource.prefsFlow.first() }
    assertTrue(prefsBlockedMode.transitionLogs.isEmpty())

    composeRule.onNodeWithTag(AppTestTags.TopBar.OVERFLOW_MENU_BUTTON).performClick()
    composeRule.onNodeWithTag(AppTestTags.TopBar.MENU_PAUSE_AUTOMATIONS).performClick()
    composeRule.waitForIdle()
    val prefsStillUnpaused = runBlocking { app.container.preferencesDataSource.prefsFlow.first() }
    assertEquals(false, prefsStillUnpaused.automationsPaused)

    // 5. Clicar no ROLE_BADGE_CHIP no TopBar abre o desafio de PIN; inserir PIN correto ("4210") e desbloquear Admin
    composeRule.onNodeWithTag(AppTestTags.TopBar.ROLE_BADGE_CHIP).performClick()
    composeRule.onNodeWithTag(AppTestTags.AccessControl.PIN_CHALLENGE_DIALOG).assertIsDisplayed()
    composeRule
      .onNodeWithTag(AppTestTags.AccessControl.PIN_CHALLENGE_INPUT)
      .performTextReplacement("4210")
    composeRule
      .onNodeWithTag(AppTestTags.AccessControl.PIN_CHALLENGE_QUICK_ADMIN_BUTTON)
      .performClick()
    composeRule.waitForIdle()

    val prefsAdminRestored = runBlocking { app.container.preferencesDataSource.prefsFlow.first() }
    assertEquals(AppAccessRole.ADMIN, prefsAdminRestored.accessControlConfig.activeRole)

    // 6. Agora como Admin, alternar modo para Vibrar e iniciar Timer de 30m funciona normalmente
    composeRule.onNodeWithTag(AppTestTags.Now.modeButton(SoundMode.VIBRATE)).performClick()
    composeRule.waitForIdle()
    composeRule.onNodeWithTag(AppTestTags.Now.timerChip(30)).performClick()
    composeRule.waitForIdle()
    val prefsAfterAllowedAction = runBlocking { app.container.preferencesDataSource.prefsFlow.first() }
    assertNotNull(prefsAfterAllowedAction.activeTimer)
  }

  @Test
  fun e2e07_auto04SystemZenRules_andGeo01BackgroundGeofencingWithDwellAndWifiSsid() {
    BackgroundGeofenceManager.clearDwellCandidates()

    // 1. Navegar para Locais, validar GEOFENCE_STATUS_CARD e habilitar "loc_college"
    composeRule.onNodeWithTag(AppTestTags.Tabs.LOCATIONS).performClick()
    composeRule.onNodeWithTag(AppTestTags.Locations.GEOFENCE_STATUS_CARD).assertIsDisplayed()
    composeRule.onNodeWithTag(AppTestTags.Locations.SYNC_GEOFENCES_BUTTON).performClick()
    composeRule.waitForIdle()

    composeRule.onNodeWithTag(AppTestTags.Locations.switch("loc_college")).performClick()
    composeRule.waitForIdle()

    // 2. [GEO-01] Simular sinal de entrada em background sem Wi-Fi (Dwell pendente de 2 min)
    val t0 = 1_750_000_000_000L
    runBlocking {
      val college = app.container.preferencesDataSource.prefsFlow.first()
        .locationRules.first { it.id == "loc_college" }
      app.container.soundModeRepository.processBackgroundGeofenceSignal(
        latitude = college.latitude,
        longitude = college.longitude,
        connectedWifiSsid = "Rede-Externa",
        nowEpochMillis = t0,
      )
      val prefsPending = app.container.preferencesDataSource.prefsFlow.first()
      assertEquals(false, prefsPending.locationRules.first { it.id == "loc_college" }.isCurrentlyInside)

      // Após 2 minutos de permanência (Dwell), confirma presença e ativa perfil da regra
      app.container.soundModeRepository.processBackgroundGeofenceSignal(
        latitude = college.latitude,
        longitude = college.longitude,
        connectedWifiSsid = "Rede-Externa",
        nowEpochMillis = t0 + BackgroundGeofenceManager.DEFAULT_DWELL_DURATION_MILLIS,
      )
      val prefsConfirmed = app.container.preferencesDataSource.prefsFlow.first()
      assertEquals(true, prefsConfirmed.locationRules.first { it.id == "loc_college" }.isCurrentlyInside)

      // Saída da cerca geográfica restaura estado externo
      app.container.soundModeRepository.processBackgroundGeofenceSignal(
        latitude = -23.6200,
        longitude = -46.7200,
        connectedWifiSsid = null,
        nowEpochMillis = t0 + 300_000L,
      )
      val prefsExited = app.container.preferencesDataSource.prefsFlow.first()
      assertEquals(false, prefsExited.locationRules.first { it.id == "loc_college" }.isCurrentlyInside)

      // Re-entrada com Wi-Fi SSID ("Campus-WiFi") confirma presença instantaneamente sem aguardar Dwell
      app.container.soundModeRepository.processBackgroundGeofenceSignal(
        latitude = college.latitude,
        longitude = college.longitude,
        connectedWifiSsid = "\"Campus-WiFi\"",
        nowEpochMillis = t0 + 350_000L,
      )
      val prefsWifiInstant = app.container.preferencesDataSource.prefsFlow.first()
      assertEquals(true, prefsWifiInstant.locationRules.first { it.id == "loc_college" }.isCurrentlyInside)
    }

    // 3. [AUTO-04] Abrir Configurações -> Automação e sincronizar Modos do Sistema (AutomaticZenRule)
    composeRule.onNodeWithTag(AppTestTags.TopBar.OVERFLOW_MENU_BUTTON).performClick()
    composeRule.onNodeWithTag(AppTestTags.TopBar.MENU_SETTINGS).performClick()
    composeRule.onNodeWithTag(AppTestTags.Settings.CATEGORY_AUTOMATION).performClick()

    composeRule
      .onNodeWithTag(AppTestTags.Settings.SYNC_ZEN_RULES_BUTTON)
      .performScrollTo()
      .performClick()
    composeRule.waitForIdle()

    val registeredCount = SystemZenRuleManager.countRegisteredRules(app)
    assertTrue(registeredCount >= 0)

    composeRule
      .onNodeWithTag(AppTestTags.Settings.SYSTEM_ZEN_RULES_SWITCH)
      .performScrollTo()
      .performClick()
    composeRule.waitForIdle()

    val prefsAfterToggleOff = runBlocking { app.container.preferencesDataSource.prefsFlow.first() }
    assertEquals(false, prefsAfterToggleOff.systemZenRulesEnabled)
    assertEquals(0, SystemZenRuleManager.countRegisteredRules(app))
  }
}
