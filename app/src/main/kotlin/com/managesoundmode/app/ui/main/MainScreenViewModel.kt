package com.managesoundmode.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.managesoundmode.app.ManageSoundApplication
import com.managesoundmode.app.data.local.PersistedSoundPrefs
import com.managesoundmode.app.data.repository.SoundModeRepository
import com.managesoundmode.app.domain.model.AccessControlConfig
import com.managesoundmode.app.domain.model.AppAccessRole
import com.managesoundmode.app.domain.model.AppPermission
import com.managesoundmode.app.domain.model.DeviceSoundSnapshot
import com.managesoundmode.app.domain.model.LocationRule
import com.managesoundmode.app.domain.model.RestorePolicy
import com.managesoundmode.app.domain.model.ScheduleRule
import com.managesoundmode.app.domain.model.SoundMode
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.domain.model.ThemeMode
import com.managesoundmode.app.domain.model.TimedModeSession
import com.managesoundmode.app.domain.model.TransitionLogEntry
import com.managesoundmode.app.domain.model.VolumeStreamType
import com.managesoundmode.app.domain.model.VolumeStreams
import com.managesoundmode.app.domain.model.Weekday
import com.managesoundmode.app.ui.common.AppLabels
import com.managesoundmode.app.ui.common.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface MainScreenUiState {
  data object Loading : MainScreenUiState

  data class Ready(
    val currentMode: SoundMode,
    val streams: VolumeStreams,
    val hasDndPermission: Boolean,
    val profiles: List<SoundProfile>,
    val activeProfileId: String? = null,
    val activeTimer: TimedModeSession? = null,
    val statusMessage: String? = null,
    val canScheduleExactAlarms: Boolean = true,
    val hasLocationPermission: Boolean = false,
    val hasBackgroundLocationPermission: Boolean = false,
    val hasNotificationPermission: Boolean = true,
    val isMusicActive: Boolean = false,
    val isBluetoothHeadsetConnected: Boolean = false,
    val activeRuleTitle: String? = null,
    val activeRuleSubtitle: String? = null,
    val isManuallyOverridden: Boolean = false,
    val scheduleRules: List<ScheduleRule> = emptyList(),
    val locationRules: List<LocationRule> = emptyList(),
    val restorePolicy: RestorePolicy = RestorePolicy.DEFAULT_PROFILE,
    val defaultDayProfileId: String = SoundProfile.DEFAULT_DAY_PROFILE_ID,
    val transitionLogs: List<TransitionLogEntry> = emptyList(),
    val useDynamicColor: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val hasAcceptedLocationDisclosure: Boolean = false,
    val muteMediaOnSilent: Boolean = true,
    val preserveMediaOnBluetooth: Boolean = true,
    val smartVolumeTriggerEnabled: Boolean = true,
    val persistentQuickBarEnabled: Boolean = true,
    val quickTileSlot1ProfileId: String = "preset_work_high",
    val quickTileSlot2ProfileId: String = "preset_sleep",
    val appLanguageCode: String = "PT_BR",
    val automationsPaused: Boolean = false,
    val hapticFeedbackEnabled: Boolean = true,
    val notifyOnAutoTransition: Boolean = true,
    val defaultTimerMinutes: Int = 30,
    val hasCompletedOnboarding: Boolean = true,
    val accessControlConfig: AccessControlConfig = AccessControlConfig(),
    val systemZenRulesEnabled: Boolean = true,
  ) : MainScreenUiState

  data class Error(val message: String) : MainScreenUiState
}

class MainScreenViewModel(
  private val repository: SoundModeRepository,
) : ViewModel() {

  private val feedbackMessage = MutableStateFlow<String?>(null)

  val uiState: StateFlow<MainScreenUiState> = combine(
    repository.snapshotStream,
    repository.profilesStream,
    repository.preferencesStream,
    feedbackMessage,
  ) { snapshot: DeviceSoundSnapshot, profiles: List<SoundProfile>, prefs: PersistedSoundPrefs, message: String? ->
    val resolvedLanguage = runCatching { AppLanguage.valueOf(prefs.appLanguageCode) }
      .getOrDefault(AppLanguage.PT_BR)
    if (AppLabels.currentLanguage != resolvedLanguage) {
      AppLabels.currentLanguage = resolvedLanguage
    }

    MainScreenUiState.Ready(
      currentMode = snapshot.currentMode,
      streams = snapshot.streams,
      hasDndPermission = snapshot.hasDndPermission,
      profiles = profiles,
      activeProfileId = snapshot.activeProfileId,
      activeTimer = snapshot.activeTimer,
      statusMessage = message,
      canScheduleExactAlarms = snapshot.canScheduleExactAlarms,
      hasLocationPermission = snapshot.hasLocationPermission,
      hasBackgroundLocationPermission = snapshot.hasBackgroundLocationPermission,
      hasNotificationPermission = snapshot.hasNotificationPermission,
      isMusicActive = snapshot.isMusicActive,
      isBluetoothHeadsetConnected = snapshot.isBluetoothHeadsetConnected,
      activeRuleTitle = snapshot.activeRuleTitle,
      activeRuleSubtitle = snapshot.activeRuleSubtitle,
      isManuallyOverridden = snapshot.isManuallyOverridden,
      scheduleRules = prefs.scheduleRules,
      locationRules = prefs.locationRules,
      restorePolicy = prefs.restorePolicy,
      defaultDayProfileId = prefs.defaultDayProfileId,
      transitionLogs = prefs.transitionLogs,
      useDynamicColor = prefs.useDynamicColor,
      themeMode = prefs.themeMode,
      hasAcceptedLocationDisclosure = prefs.hasAcceptedLocationDisclosure,
      muteMediaOnSilent = prefs.muteMediaOnSilent,
      preserveMediaOnBluetooth = prefs.preserveMediaOnBluetooth,
      smartVolumeTriggerEnabled = prefs.smartVolumeTriggerEnabled,
      persistentQuickBarEnabled = prefs.persistentQuickBarEnabled,
      quickTileSlot1ProfileId = prefs.quickTileSlot1ProfileId,
      quickTileSlot2ProfileId = prefs.quickTileSlot2ProfileId,
      appLanguageCode = prefs.appLanguageCode,
      automationsPaused = prefs.automationsPaused,
      hapticFeedbackEnabled = prefs.hapticFeedbackEnabled,
      notifyOnAutoTransition = prefs.notifyOnAutoTransition,
      defaultTimerMinutes = prefs.defaultTimerMinutes,
      hasCompletedOnboarding = prefs.hasCompletedOnboarding,
      accessControlConfig = prefs.accessControlConfig,
      systemZenRulesEnabled = prefs.systemZenRulesEnabled,
    ) as MainScreenUiState
  }
    .catch { throwable ->
      emit(MainScreenUiState.Error(throwable.message ?: "Erro inesperado ao carregar estado de som"))
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5_000),
      initialValue = MainScreenUiState.Loading,
    )

  private suspend fun currentAccessControl(): AccessControlConfig =
    (uiState.value as? MainScreenUiState.Ready)?.accessControlConfig
      ?: repository.preferencesStream.first().accessControlConfig

  private suspend fun checkAccessPermission(permission: AppPermission): Boolean {
    val config = currentAccessControl()
    if (!config.hasPermission(permission)) {
      feedbackMessage.value = AppLabels.AccessControl.permissionDeniedMessage(permission)
      return false
    }
    return true
  }

  fun onRefreshSystemState() {
    repository.refreshSystemState()
    viewModelScope.launch {
      repository.evaluateAndApplyRules()
    }
  }

  fun onSelectMode(mode: SoundMode) {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.CHANGE_SOUND_AND_TIMER)) return@launch
      repository.setSoundMode(mode)
        .onSuccess {
          feedbackMessage.value = "Modo alterado para ${mode.title}"
        }
        .onFailure { error ->
          feedbackMessage.value = error.message ?: "Não foi possível alterar o modo de som"
        }
    }
  }

  fun onStreamVolumeChange(type: VolumeStreamType, percent: Int) {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.CHANGE_SOUND_AND_TIMER)) return@launch
      repository.setStreamVolume(type, percent)
        .onFailure { error ->
          feedbackMessage.value = error.message ?: "Falha ao ajustar volume de ${type.label}"
        }
    }
  }

  fun onActivateProfile(profile: SoundProfile) {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.CHANGE_SOUND_AND_TIMER)) return@launch
      repository.activateProfile(profile)
        .onSuccess {
          feedbackMessage.value = "Perfil '${profile.name}' ativado"
        }
        .onFailure { error ->
          feedbackMessage.value = error.message ?: "Não foi possível ativar o perfil '${profile.name}'"
        }
    }
  }

  fun onStartTimer(targetMode: SoundMode, durationMinutes: Int) {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.CHANGE_SOUND_AND_TIMER)) return@launch
      repository.startTimedSession(targetMode = targetMode, durationMinutes = durationMinutes)
        .onSuccess {
          feedbackMessage.value = "Modo ${targetMode.title} ativado por $durationMinutes min"
        }
        .onFailure { error ->
          feedbackMessage.value = error.message ?: "Falha ao iniciar temporizador"
        }
    }
  }

  fun onExtendTimer(additionalMinutes: Int) {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.CHANGE_SOUND_AND_TIMER)) return@launch
      repository.extendTimedSession(additionalMinutes = additionalMinutes)
        .onSuccess {
          feedbackMessage.value = "Timer estendido em +$additionalMinutes min"
        }
    }
  }

  fun onCancelTimer() {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.CHANGE_SOUND_AND_TIMER)) return@launch
      repository.cancelOrRestoreTimedSession()
        .onSuccess {
          feedbackMessage.value = "Temporizador encerrado e modo restaurado"
        }
        .onFailure { error ->
          feedbackMessage.value = error.message ?: "Falha ao restaurar modo anterior"
        }
    }
  }

  fun onSaveCurrentAsProfile(name: String, description: String) {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.MANAGE_PROFILES)) return@launch
      repository.saveCurrentAsProfile(name, description)
        .onSuccess { profile ->
          feedbackMessage.value = "Perfil '${profile.name}' salvo com sucesso"
        }
        .onFailure { error ->
          feedbackMessage.value = error.message ?: "Falha ao salvar perfil"
        }
    }
  }

  fun onUpsertProfile(
    profile: SoundProfile,
    applyImmediately: Boolean,
    showFeedback: Boolean = true,
  ) {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.MANAGE_PROFILES)) return@launch
      repository.upsertProfile(profile, applyImmediately = applyImmediately)
        .onSuccess { saved ->
          if (showFeedback) {
            feedbackMessage.value = if (applyImmediately) {
              "Perfil '${saved.name}' salvo e ativado"
            } else {
              "Perfil '${saved.name}' atualizado"
            }
          }
        }
        .onFailure { error ->
          feedbackMessage.value = error.message ?: "Falha ao salvar perfil"
        }
    }
  }

  fun onDeleteCustomProfile(profileId: String) {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.MANAGE_PROFILES)) return@launch
      repository.deleteCustomProfile(profileId)
        .onSuccess {
          feedbackMessage.value = "Perfil removido"
        }
    }
  }

  fun onUpsertScheduleRule(rule: ScheduleRule, showFeedback: Boolean = true) {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.MANAGE_SCHEDULES)) return@launch
      repository.upsertScheduleRule(rule)
        .onSuccess {
          if (showFeedback) {
            feedbackMessage.value = "Horário '${rule.name}' (${rule.formattedWindow()}) salvo"
          }
        }
        .onFailure { error ->
          feedbackMessage.value = error.message ?: "Erro ao salvar horário"
        }
    }
  }

  fun onToggleScheduleRule(rule: ScheduleRule, enabled: Boolean) {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.MANAGE_SCHEDULES)) return@launch
      repository.upsertScheduleRule(rule.copy(enabled = enabled))
        .onSuccess {
          val stateLabel = if (enabled) "ativado" else "desativado"
          feedbackMessage.value = "Horário '${rule.name}' $stateLabel"
        }
    }
  }

  fun onToggleScheduleRuleWeekday(rule: ScheduleRule, weekday: Weekday) {
    val updatedDays = if (weekday in rule.daysOfWeek) {
      if (rule.daysOfWeek.size > 1) rule.daysOfWeek - weekday else rule.daysOfWeek
    } else {
      rule.daysOfWeek + weekday
    }
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.MANAGE_SCHEDULES)) return@launch
      repository.upsertScheduleRule(rule.copy(daysOfWeek = updatedDays))
    }
  }

  fun onUpdateScheduleRuleProfile(rule: ScheduleRule, profileId: String) {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.MANAGE_SCHEDULES)) return@launch
      repository.upsertScheduleRule(rule.copy(targetProfileId = profileId))
    }
  }

  fun onToggleScheduleRuleHighPriority(rule: ScheduleRule, highPriority: Boolean) {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.MANAGE_SCHEDULES)) return@launch
      repository.upsertScheduleRule(rule.copy(highPriority = highPriority))
    }
  }

  fun onDeleteScheduleRule(ruleId: String) {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.MANAGE_SCHEDULES)) return@launch
      repository.deleteScheduleRule(ruleId)
        .onSuccess {
          feedbackMessage.value = "Horário removido"
        }
    }
  }

  fun onUpsertLocationRule(rule: LocationRule, showFeedback: Boolean = true) {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.MANAGE_LOCATIONS)) return@launch
      repository.upsertLocationRule(rule)
        .onSuccess {
          if (showFeedback) {
            feedbackMessage.value = "Local '${rule.name}' salvo"
          }
        }
    }
  }

  fun onToggleLocationRule(rule: LocationRule, enabled: Boolean) {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.MANAGE_LOCATIONS)) return@launch
      repository.upsertLocationRule(rule.copy(enabled = enabled))
    }
  }

  fun onToggleLocationPresence(ruleId: String, inside: Boolean) {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.MANAGE_LOCATIONS)) return@launch
      repository.toggleLocationPresence(ruleId, inside)
        .onSuccess {
          feedbackMessage.value = if (inside) {
            "Presença no local simulada • Regra avaliada"
          } else {
            "Saída do local simulada • Modo reavaliado"
          }
        }
    }
  }

  fun onSyncLocationsWithGps(latitude: Double, longitude: Double) {
    viewModelScope.launch {
      repository.syncLocationsWithDeviceCoordinates(latitude, longitude)
        .onSuccess { matchedCount ->
          feedbackMessage.value = if (matchedCount > 0) {
            "GPS sincronizado • Você está dentro de $matchedCount cerca(s) ativa(s)"
          } else {
            "GPS sincronizado (${String.format(java.util.Locale.ROOT, "%.4f, %.4f", latitude, longitude)})"
          }
        }
    }
  }

  fun onDeleteLocationRule(ruleId: String) {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.MANAGE_LOCATIONS)) return@launch
      repository.deleteLocationRule(ruleId)
        .onSuccess {
          feedbackMessage.value = "Local removido"
        }
    }
  }

  fun onSetRestorePolicy(policy: RestorePolicy) {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.ACCESS_SETTINGS)) return@launch
      repository.setRestorePolicy(policy)
        .onSuccess {
          feedbackMessage.value = "Política de restauração: ${policy.title}"
        }
    }
  }

  fun onSetDefaultDayProfileId(profileId: String) {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.MANAGE_PROFILES)) return@launch
      repository.setDefaultDayProfileId(profileId)
    }
  }

  fun onSetUseDynamicColor(enabled: Boolean) {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.ACCESS_SETTINGS)) return@launch
      repository.setUseDynamicColor(enabled)
    }
  }

  fun onSetThemeMode(mode: ThemeMode) {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.ACCESS_SETTINGS)) return@launch
      repository.setThemeMode(mode)
    }
  }

  fun onAcceptLocationDisclosure() {
    viewModelScope.launch {
      repository.setLocationDisclosureAccepted(true)
    }
  }

  fun onSetMuteMediaOnSilent(enabled: Boolean) {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.ACCESS_SETTINGS)) return@launch
      repository.setMuteMediaOnSilent(enabled)
    }
  }

  fun onSetPreserveMediaOnBluetooth(enabled: Boolean) {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.ACCESS_SETTINGS)) return@launch
      repository.setPreserveMediaOnBluetooth(enabled)
    }
  }

  fun onSetSmartVolumeTriggerEnabled(enabled: Boolean) {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.ACCESS_SETTINGS)) return@launch
      repository.setSmartVolumeTriggerEnabled(enabled)
    }
  }

  fun onSetPersistentQuickBarEnabled(enabled: Boolean) {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.ACCESS_SETTINGS)) return@launch
      repository.setPersistentQuickBarEnabled(enabled)
        .onSuccess {
          feedbackMessage.value = if (enabled) {
            "Gadget fixado na barra de notificações"
          } else {
            "Gadget removido da barra de notificações"
          }
        }
    }
  }

  fun onSetQuickTileSlotProfileId(slotIndex: Int, profileId: String) {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.ACCESS_SETTINGS)) return@launch
      repository.setQuickTileSlotProfileId(slotIndex, profileId)
        .onSuccess {
          val profiles = (uiState.value as? MainScreenUiState.Ready)?.profiles
          val name = profiles?.firstOrNull { it.id == profileId }?.name ?: "Perfil"
          feedbackMessage.value = "Bloco de Perfil $slotIndex vinculado a '$name'"
        }
    }
  }

  fun onShowStatusMessage(message: String) {
    feedbackMessage.value = message
  }

  fun onReapplyActiveRule() {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.CHANGE_SOUND_AND_TIMER)) return@launch
      repository.reapplyActiveRule()
        .onSuccess {
          feedbackMessage.value = "Regra ativa reaplicada com sucesso"
        }
        .onFailure { error ->
          feedbackMessage.value = error.message ?: "Falha ao reaplicar regra"
        }
    }
  }

  fun onClearTransitionLogs() {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.ACCESS_SETTINGS)) return@launch
      repository.clearTransitionLogs()
    }
  }

  fun onSetAppLanguage(language: AppLanguage) {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.ACCESS_SETTINGS)) return@launch
      AppLabels.currentLanguage = language
      repository.setAppLanguageCode(language.name)
    }
  }

  fun onSetAutomationsPaused(paused: Boolean) {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.PAUSE_AUTOMATIONS)) return@launch
      repository.setAutomationsPaused(paused)
        .onSuccess {
          feedbackMessage.value = if (paused) {
            "Automações pausadas (Modo Férias ativado)"
          } else {
            "Automações retomadas e reavaliadas"
          }
        }
    }
  }

  fun onSetHapticFeedbackEnabled(enabled: Boolean) {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.ACCESS_SETTINGS)) return@launch
      repository.setHapticFeedbackEnabled(enabled)
    }
  }

  fun onSetNotifyOnAutoTransition(enabled: Boolean) {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.ACCESS_SETTINGS)) return@launch
      repository.setNotifyOnAutoTransition(enabled)
    }
  }

  fun onSetDefaultTimerMinutes(minutes: Int) {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.ACCESS_SETTINGS)) return@launch
      repository.setDefaultTimerMinutes(minutes)
        .onSuccess {
          feedbackMessage.value = "Timer padrão definido para ${minutes}m"
        }
    }
  }

  fun onCompleteOnboarding() {
    viewModelScope.launch {
      repository.setOnboardingCompleted(true)
    }
  }

  fun onReopenOnboarding() {
    viewModelScope.launch {
      repository.setOnboardingCompleted(false)
    }
  }

  fun onSetAccessRole(role: AppAccessRole) {
    viewModelScope.launch {
      val current = currentAccessControl()
      val updated = current.withRole(role)
      repository.setAccessControlConfig(updated)
    }
  }

  fun onToggleAccessPermission(permission: AppPermission, enabled: Boolean) {
    viewModelScope.launch {
      val current = currentAccessControl()
      val updated = current.withPermissionEnabled(permission, enabled)
      repository.setAccessControlConfig(updated)
    }
  }

  fun onSetAccessPin(pinCode: String?) {
    viewModelScope.launch {
      val current = currentAccessControl()
      val cleanPin = pinCode?.trim()?.takeIf { it.isNotEmpty() }
      if (cleanPin != null && (cleanPin.length != 4 || !cleanPin.all { it.isDigit() })) {
        feedbackMessage.value = AppLabels.AccessControl.pinInvalidLengthMessage
        return@launch
      }
      repository.setAccessControlConfig(current.copy(pinCode = cleanPin))
        .onSuccess {
          feedbackMessage.value = if (cleanPin != null) {
            AppLabels.AccessControl.pinSavedMessage
          } else {
            AppLabels.AccessControl.pinRemovedMessage
          }
        }
    }
  }

  fun onUnlockAdminWithPin(inputPin: String, onSuccess: () -> Unit = {}) {
    viewModelScope.launch {
      val current = currentAccessControl()
      if (current.verifyPin(inputPin)) {
        repository.setAccessControlConfig(current.withRole(AppAccessRole.ADMIN))
          .onSuccess {
            feedbackMessage.value = AppLabels.AccessControl.unlockedAsAdminMessage
            onSuccess()
          }
      } else {
        feedbackMessage.value = AppLabels.AccessControl.pinChallengeWrongError
      }
    }
  }

  fun onSetSystemZenRulesEnabled(enabled: Boolean) {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.ACCESS_SETTINGS)) return@launch
      repository.setSystemZenRulesEnabled(enabled)
        .onSuccess {
          feedbackMessage.value = if (enabled) {
            "Modos do Sistema Android (AutomaticZenRule) ativados"
          } else {
            "Modos do Sistema Android removidos"
          }
        }
    }
  }

  fun onSyncSystemZenRulesNow() {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.ACCESS_SETTINGS)) return@launch
      repository.syncSystemZenRulesNow()
        .onSuccess { count ->
          feedbackMessage.value = AppLabels.Settings.zenRulesSyncedMessage(count)
        }
    }
  }

  fun onSyncBackgroundGeofencesNow() {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.MANAGE_LOCATIONS)) return@launch
      repository.syncBackgroundGeofencesNow()
        .onSuccess { count ->
          feedbackMessage.value = AppLabels.Locations.geofencesSyncedMessage(count)
        }
    }
  }

  fun onResetFactoryDefaults() {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.BACKUP_AND_FACTORY_RESET)) return@launch
      repository.resetFactoryDefaults()
        .onSuccess {
          feedbackMessage.value = "Padrões de fábrica restaurados com sucesso"
        }
        .onFailure { error ->
          feedbackMessage.value = error.message ?: "Falha ao restaurar padrões"
        }
    }
  }

  fun onDuplicateProfile(profile: SoundProfile) {
    val duplicated = profile.copy(
      id = "custom_${System.currentTimeMillis()}",
      name = "${profile.name} (Cópia)",
      isPreset = false,
    )
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.MANAGE_PROFILES)) return@launch
      repository.upsertProfile(duplicated, applyImmediately = false)
        .onSuccess {
          feedbackMessage.value = "Perfil '${duplicated.name}' criado"
        }
    }
  }

  fun onDuplicateScheduleRule(rule: ScheduleRule) {
    val duplicated = rule.copy(
      id = "sched_${System.currentTimeMillis()}",
      name = "${rule.name} (Cópia)",
    )
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.MANAGE_SCHEDULES)) return@launch
      repository.upsertScheduleRule(duplicated)
        .onSuccess {
          feedbackMessage.value = "Horário '${duplicated.name}' duplicado"
        }
    }
  }

  fun onExportBackupJson(onExported: (String) -> Unit) {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.BACKUP_AND_FACTORY_RESET)) return@launch
      repository.exportBackupJson()
        .onSuccess { json ->
          onExported(json)
          feedbackMessage.value = "Backup exportado com sucesso"
        }
        .onFailure { error ->
          feedbackMessage.value = error.message ?: "Erro ao exportar backup"
        }
    }
  }

  fun onImportBackupJson(rawJson: String) {
    viewModelScope.launch {
      if (!checkAccessPermission(AppPermission.BACKUP_AND_FACTORY_RESET)) return@launch
      repository.importBackupJson(rawJson)
        .onSuccess {
          feedbackMessage.value = "Backup importado e aplicado com sucesso"
        }
        .onFailure {
          feedbackMessage.value = "JSON de backup inválido. Verifique o conteúdo colado."
        }
    }
  }

  fun onDismissMessage() {
    feedbackMessage.value = null
  }

  companion object {
    val Factory: ViewModelProvider.Factory = viewModelFactory {
      initializer {
        val application =
          this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as ManageSoundApplication
        MainScreenViewModel(repository = application.container.soundModeRepository)
      }
    }
  }
}
