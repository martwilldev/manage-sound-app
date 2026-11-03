package com.managesoundmode.app.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.managesoundmode.app.ManageSoundApplication
import com.managesoundmode.app.data.local.PersistedSoundPrefs
import com.managesoundmode.app.data.repository.SoundModeRepository
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
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

  fun onRefreshSystemState() {
    repository.refreshSystemState()
    viewModelScope.launch {
      repository.evaluateAndApplyRules()
    }
  }

  fun onSelectMode(mode: SoundMode) {
    viewModelScope.launch {
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
      repository.setStreamVolume(type, percent)
        .onFailure { error ->
          feedbackMessage.value = error.message ?: "Falha ao ajustar volume de ${type.label}"
        }
    }
  }

  fun onActivateProfile(profile: SoundProfile) {
    viewModelScope.launch {
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
      repository.extendTimedSession(additionalMinutes = additionalMinutes)
        .onSuccess {
          feedbackMessage.value = "Timer estendido em +$additionalMinutes min"
        }
    }
  }

  fun onCancelTimer() {
    viewModelScope.launch {
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
      repository.saveCurrentAsProfile(name, description)
        .onSuccess { profile ->
          feedbackMessage.value = "Perfil '${profile.name}' salvo com sucesso"
        }
        .onFailure { error ->
          feedbackMessage.value = error.message ?: "Falha ao salvar perfil"
        }
    }
  }

  fun onUpsertProfile(profile: SoundProfile, applyImmediately: Boolean) {
    viewModelScope.launch {
      repository.upsertProfile(profile, applyImmediately = applyImmediately)
        .onSuccess { saved ->
          feedbackMessage.value = if (applyImmediately) {
            "Perfil '${saved.name}' salvo e ativado"
          } else {
            "Perfil '${saved.name}' atualizado"
          }
        }
        .onFailure { error ->
          feedbackMessage.value = error.message ?: "Falha ao salvar perfil"
        }
    }
  }

  fun onDeleteCustomProfile(profileId: String) {
    viewModelScope.launch {
      repository.deleteCustomProfile(profileId)
        .onSuccess {
          feedbackMessage.value = "Perfil removido"
        }
    }
  }

  fun onUpsertScheduleRule(rule: ScheduleRule) {
    viewModelScope.launch {
      repository.upsertScheduleRule(rule)
        .onSuccess {
          feedbackMessage.value = "Horário '${rule.name}' (${rule.formattedWindow()}) salvo"
        }
        .onFailure { error ->
          feedbackMessage.value = error.message ?: "Erro ao salvar horário"
        }
    }
  }

  fun onToggleScheduleRule(rule: ScheduleRule, enabled: Boolean) {
    viewModelScope.launch {
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
      repository.upsertScheduleRule(rule.copy(daysOfWeek = updatedDays))
    }
  }

  fun onUpdateScheduleRuleProfile(rule: ScheduleRule, profileId: String) {
    viewModelScope.launch {
      repository.upsertScheduleRule(rule.copy(targetProfileId = profileId))
    }
  }

  fun onToggleScheduleRuleHighPriority(rule: ScheduleRule, highPriority: Boolean) {
    viewModelScope.launch {
      repository.upsertScheduleRule(rule.copy(highPriority = highPriority))
    }
  }

  fun onDeleteScheduleRule(ruleId: String) {
    viewModelScope.launch {
      repository.deleteScheduleRule(ruleId)
        .onSuccess {
          feedbackMessage.value = "Horário removido"
        }
    }
  }

  fun onUpsertLocationRule(rule: LocationRule) {
    viewModelScope.launch {
      repository.upsertLocationRule(rule)
        .onSuccess {
          feedbackMessage.value = "Local '${rule.name}' salvo"
        }
    }
  }

  fun onToggleLocationRule(rule: LocationRule, enabled: Boolean) {
    viewModelScope.launch {
      repository.upsertLocationRule(rule.copy(enabled = enabled))
    }
  }

  fun onToggleLocationPresence(ruleId: String, inside: Boolean) {
    viewModelScope.launch {
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

  fun onDeleteLocationRule(ruleId: String) {
    viewModelScope.launch {
      repository.deleteLocationRule(ruleId)
        .onSuccess {
          feedbackMessage.value = "Local removido"
        }
    }
  }

  fun onSetRestorePolicy(policy: RestorePolicy) {
    viewModelScope.launch {
      repository.setRestorePolicy(policy)
        .onSuccess {
          feedbackMessage.value = "Política de restauração: ${policy.title}"
        }
    }
  }

  fun onSetDefaultDayProfileId(profileId: String) {
    viewModelScope.launch {
      repository.setDefaultDayProfileId(profileId)
    }
  }

  fun onSetUseDynamicColor(enabled: Boolean) {
    viewModelScope.launch {
      repository.setUseDynamicColor(enabled)
    }
  }

  fun onSetThemeMode(mode: ThemeMode) {
    viewModelScope.launch {
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
      repository.setMuteMediaOnSilent(enabled)
    }
  }

  fun onSetPreserveMediaOnBluetooth(enabled: Boolean) {
    viewModelScope.launch {
      repository.setPreserveMediaOnBluetooth(enabled)
    }
  }

  fun onSetSmartVolumeTriggerEnabled(enabled: Boolean) {
    viewModelScope.launch {
      repository.setSmartVolumeTriggerEnabled(enabled)
    }
  }

  fun onSetPersistentQuickBarEnabled(enabled: Boolean) {
    viewModelScope.launch {
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
      repository.clearTransitionLogs()
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
