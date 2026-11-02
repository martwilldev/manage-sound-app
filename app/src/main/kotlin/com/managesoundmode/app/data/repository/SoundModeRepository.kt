package com.managesoundmode.app.data.repository

import com.managesoundmode.app.data.local.PersistedSoundPrefs
import com.managesoundmode.app.data.local.SoundPreferencesDataSource
import com.managesoundmode.app.data.system.SoundSystemController
import com.managesoundmode.app.domain.engine.RuleEvaluationDecision
import com.managesoundmode.app.domain.engine.RuleEvaluator
import com.managesoundmode.app.domain.model.DeviceSoundSnapshot
import com.managesoundmode.app.domain.model.LocationRule
import com.managesoundmode.app.domain.model.RestorePolicy
import com.managesoundmode.app.domain.model.SavedStateSnapshot
import com.managesoundmode.app.domain.model.ScheduleRule
import com.managesoundmode.app.domain.model.SoundMode
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.domain.model.ThemeMode
import com.managesoundmode.app.domain.model.TimedModeSession
import com.managesoundmode.app.domain.model.TransitionLogEntry
import com.managesoundmode.app.domain.model.TransitionTriggerSource
import com.managesoundmode.app.domain.model.VolumeStreamType
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

interface SoundModeRepository {
  val snapshotStream: Flow<DeviceSoundSnapshot>
  val profilesStream: Flow<List<SoundProfile>>
  val preferencesStream: Flow<PersistedSoundPrefs>

  fun refreshSystemState()
  suspend fun setSoundMode(
    mode: SoundMode,
    source: TransitionTriggerSource = TransitionTriggerSource.MANUAL,
  ): Result<Unit>
  suspend fun setStreamVolume(type: VolumeStreamType, percent: Int): Result<Unit>
  suspend fun activateProfile(
    profile: SoundProfile,
    source: TransitionTriggerSource = TransitionTriggerSource.MANUAL,
  ): Result<Unit>
  suspend fun startTimedSession(
    targetMode: SoundMode,
    durationMinutes: Int,
    nowEpochMillis: Long = System.currentTimeMillis(),
  ): Result<TimedModeSession>
  suspend fun extendTimedSession(
    additionalMinutes: Int,
    nowEpochMillis: Long = System.currentTimeMillis(),
  ): Result<TimedModeSession>
  suspend fun cancelOrRestoreTimedSession(): Result<Unit>
  suspend fun saveCurrentAsProfile(name: String, description: String): Result<SoundProfile>
  suspend fun upsertProfile(profile: SoundProfile, applyImmediately: Boolean = false): Result<SoundProfile>
  suspend fun deleteCustomProfile(profileId: String): Result<Unit>

  suspend fun upsertScheduleRule(rule: ScheduleRule): Result<Unit>
  suspend fun deleteScheduleRule(ruleId: String): Result<Unit>
  suspend fun upsertLocationRule(rule: LocationRule): Result<Unit>
  suspend fun deleteLocationRule(ruleId: String): Result<Unit>
  suspend fun toggleLocationPresence(ruleId: String, inside: Boolean): Result<Unit>

  suspend fun setRestorePolicy(policy: RestorePolicy): Result<Unit>
  suspend fun setDefaultDayProfileId(profileId: String): Result<Unit>
  suspend fun setUseDynamicColor(enabled: Boolean): Result<Unit>
  suspend fun setThemeMode(mode: ThemeMode): Result<Unit>
  suspend fun setLocationDisclosureAccepted(accepted: Boolean): Result<Unit>
  suspend fun setMuteMediaOnSilent(enabled: Boolean): Result<Unit>
  suspend fun setPreserveMediaOnBluetooth(enabled: Boolean): Result<Unit>
  suspend fun setSmartVolumeTriggerEnabled(enabled: Boolean): Result<Unit>
  suspend fun clearTransitionLogs(): Result<Unit>

  suspend fun reapplyActiveRule(): Result<Unit>
  suspend fun evaluateAndApplyRules(
    source: TransitionTriggerSource = TransitionTriggerSource.SCHEDULE,
    nowEpochMillis: Long = System.currentTimeMillis(),
  ): Result<RuleEvaluationDecision>
  suspend fun cycleSoundModeFromQuickTile(): Result<SoundMode>
  suspend fun handleExternalAudioChange(): Result<Unit>
}

class DefaultSoundModeRepository(
  private val systemController: SoundSystemController,
  private val preferencesDataSource: SoundPreferencesDataSource,
  private val onScheduleAutoRestore: (durationMinutes: Int) -> Unit = {},
  private val onCancelAutoRestore: () -> Unit = {},
  private val onRescheduleRuleAlarms: (scheduleRules: List<ScheduleRule>) -> Unit = {},
  private val onSmartRestorePrompt: (mode: SoundMode, mediaAutoMuted: Boolean) -> Unit = { _, _ -> },
  private val onDismissSmartPrompt: () -> Unit = {},
  private val onSyncActiveTimerNotification: (session: TimedModeSession?) -> Unit = {},
  private val onRuleOverrideAlert: (ruleName: String) -> Unit = {},
  private val onRefreshExternalSurfaces: suspend () -> Unit = {},
  private val clockNowMillis: () -> Long = { System.currentTimeMillis() },
) : SoundModeRepository {

  private val manualRefreshTrigger = MutableStateFlow(0L)

  override val preferencesStream: Flow<PersistedSoundPrefs> = preferencesDataSource.prefsFlow

  override val profilesStream: Flow<List<SoundProfile>> =
    preferencesDataSource.prefsFlow.combine(manualRefreshTrigger) { prefs, _ ->
      mergeProfiles(prefs.customProfiles)
    }

  override val snapshotStream: Flow<DeviceSoundSnapshot> = combine(
    systemController.systemChanges,
    manualRefreshTrigger,
    preferencesDataSource.prefsFlow,
  ) { _, _, prefs ->
    val nowMillis = clockNowMillis()
    val nowDateTime = LocalDateTime.ofInstant(
      Instant.ofEpochMilli(nowMillis),
      ZoneId.systemDefault(),
    )
    val allProfiles = mergeProfiles(prefs.customProfiles)

    val decision = RuleEvaluator.evaluate(
      nowDateTime = nowDateTime,
      nowEpochMillis = nowMillis,
      profiles = allProfiles,
      scheduleRules = prefs.scheduleRules,
      locationRules = prefs.locationRules,
      activeTimer = prefs.activeTimer,
      overriddenSessionKey = prefs.overriddenSessionKey,
      restorePolicy = prefs.restorePolicy,
      defaultDayProfileId = prefs.defaultDayProfileId,
      savedSnapshot = prefs.savedSnapshot,
    )

    val (ruleTitle, ruleSubtitle, ruleKey, overridden) = when (decision) {
      is RuleEvaluationDecision.TimerActive -> Quadruple(
        "Modo Temporizado (${decision.timer.targetMode.title})",
        "Auto-Restore ativo • Restam ${decision.remainingMinutes} min",
        null,
        false,
      )
      is RuleEvaluationDecision.ApplyRule -> Quadruple(
        "Rotina '${decision.winner.ruleName}' ativa",
        "${decision.winner.subtitle} • Perfil ${decision.winner.profile.name}",
        decision.winner.sessionKey,
        false,
      )
      is RuleEvaluationDecision.RuleOverriddenByUser -> Quadruple(
        "Rotina '${decision.winner.ruleName}' pausada manualmente",
        "Alterado pelos botões/painel • Retoma no próximo período",
        decision.winner.sessionKey,
        true,
      )
      is RuleEvaluationDecision.NoActiveRules -> Quadruple(
        null,
        "Padrão do Sistema • Nenhuma automação restritiva no momento",
        null,
        false,
      )
    }

    DeviceSoundSnapshot(
      currentMode = systemController.readCurrentMode(),
      streams = systemController.readVolumeStreams(),
      hasDndPermission = systemController.hasDndPermission(),
      canScheduleExactAlarms = systemController.canScheduleExactAlarms(),
      hasLocationPermission = systemController.hasLocationPermission(),
      hasBackgroundLocationPermission = systemController.hasBackgroundLocationPermission(),
      hasNotificationPermission = systemController.hasNotificationPermission(),
      isMusicActive = systemController.isMusicActive(),
      isBluetoothHeadsetConnected = systemController.isBluetoothHeadsetConnected(),
      activeProfileId = prefs.activeProfileId,
      activeTimer = prefs.activeTimer,
      activeRuleTitle = ruleTitle,
      activeRuleSubtitle = ruleSubtitle,
      activeRuleSessionKey = ruleKey,
      isManuallyOverridden = overridden,
    )
  }

  override fun refreshSystemState() {
    manualRefreshTrigger.value += 1L
  }

  override suspend fun setSoundMode(
    mode: SoundMode,
    source: TransitionTriggerSource,
  ): Result<Unit> {
    val prefs = preferencesDataSource.prefsFlow.first()
    val currentMedia = systemController.readVolumeStreams().mediaPercent

    return systemController.applySoundMode(mode).map {
      if (mode != SoundMode.NORMAL) {
        val skipBt = prefs.preserveMediaOnBluetooth && systemController.isBluetoothHeadsetConnected()
        if (prefs.muteMediaOnSilent && !skipBt && currentMedia > 0) {
          preferencesDataSource.setSavedMediaBeforeMute(currentMedia)
          systemController.applyStreamVolume(VolumeStreamType.MEDIA, 0)
        }
      } else {
        onDismissSmartPrompt()
        val savedMedia = prefs.savedMediaBeforeMute
        if (savedMedia != null && systemController.readVolumeStreams().mediaPercent == 0) {
          systemController.applyStreamVolume(VolumeStreamType.MEDIA, savedMedia)
          preferencesDataSource.setSavedMediaBeforeMute(null)
        }
      }

      markManualOverrideIfRuleActive()
      preferencesDataSource.setActiveProfileId(null)
      logTransition(
        title = "Modo alterado para ${mode.title}",
        detail = "Alternância direta de modo de toque",
        source = source,
        mode = mode,
      )
      onRefreshExternalSurfaces()
      refreshSystemState()
    }
  }

  override suspend fun setStreamVolume(type: VolumeStreamType, percent: Int): Result<Unit> {
    if ((type == VolumeStreamType.RING || type == VolumeStreamType.NOTIFICATION) &&
      percent > 0 &&
      systemController.readCurrentMode() != SoundMode.NORMAL
    ) {
      systemController.applySoundMode(SoundMode.NORMAL)
    }
    return systemController.applyStreamVolume(type, percent).map {
      markManualOverrideIfRuleActive()
      preferencesDataSource.setActiveProfileId(null)
      onRefreshExternalSurfaces()
      refreshSystemState()
    }
  }

  override suspend fun activateProfile(
    profile: SoundProfile,
    source: TransitionTriggerSource,
  ): Result<Unit> {
    return applyProfileToSystem(profile).map {
      if (source == TransitionTriggerSource.MANUAL) {
        markManualOverrideIfRuleActive()
      }
      preferencesDataSource.setActiveProfileId(profile.id)
      logTransition(
        title = "Perfil '${profile.name}' ativado",
        detail = "${profile.mode.title} • Toque ${profile.streams.ringPercent}%, Mídia ${profile.streams.mediaPercent}%",
        source = source,
        mode = profile.mode,
      )
      onRefreshExternalSurfaces()
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
    val prefs = preferencesDataSource.prefsFlow.first()

    return systemController.applySoundMode(targetMode).map {
      if (targetMode != SoundMode.NORMAL && prefs.muteMediaOnSilent) {
        val skipBt = prefs.preserveMediaOnBluetooth && systemController.isBluetoothHeadsetConnected()
        if (!skipBt && previousStreams.mediaPercent > 0) {
          systemController.applyStreamVolume(VolumeStreamType.MEDIA, 0)
        }
      }
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
      onSyncActiveTimerNotification(session)
      logTransition(
        title = "Timer iniciado: ${targetMode.title} por ${durationMinutes}m",
        detail = "Restaurará automaticamente para ${previousMode.title}",
        source = TransitionTriggerSource.TIMER,
        mode = targetMode,
      )
      onRefreshExternalSurfaces()
      refreshSystemState()
      session
    }
  }

  override suspend fun extendTimedSession(
    additionalMinutes: Int,
    nowEpochMillis: Long,
  ): Result<TimedModeSession> = runCatching {
    val prefs = preferencesDataSource.prefsFlow.first()
    val current = prefs.activeTimer
      ?: return startTimedSession(
        targetMode = systemController.readCurrentMode().let {
          if (it == SoundMode.NORMAL) SoundMode.VIBRATE else it
        },
        durationMinutes = additionalMinutes,
        nowEpochMillis = nowEpochMillis,
      )

    val baseEnd = maxOf(current.endsAtEpochMillis, nowEpochMillis)
    val newEnd = baseEnd + additionalMinutes * 60_000L
    val totalRemainingMinutes = (((newEnd - nowEpochMillis) + 59_999L) / 60_000L).toInt().coerceAtLeast(1)
    val updated = current.copy(
      durationMinutes = current.durationMinutes + additionalMinutes,
      endsAtEpochMillis = newEnd,
    )
    preferencesDataSource.setActiveTimer(updated)
    onScheduleAutoRestore(totalRemainingMinutes)
    onSyncActiveTimerNotification(updated)
    logTransition(
      title = "Timer estendido (+${additionalMinutes}m)",
      detail = "Restam ${totalRemainingMinutes} min em ${updated.targetMode.title}",
      source = TransitionTriggerSource.NOTIFICATION,
      mode = updated.targetMode,
    )
    onRefreshExternalSurfaces()
    refreshSystemState()
    updated
  }

  override suspend fun cancelOrRestoreTimedSession(): Result<Unit> = runCatching {
    onCancelAutoRestore()
    onSyncActiveTimerNotification(null)
    val prefs = preferencesDataSource.prefsFlow.first()
    val savedTimer = prefs.activeTimer
    preferencesDataSource.setActiveTimer(null)

    val restoreMode = savedTimer?.previousMode ?: SoundMode.NORMAL
    systemController.applySoundMode(restoreMode).getOrThrow()
    if (savedTimer != null && restoreMode == SoundMode.NORMAL) {
      systemController.applyAllStreams(savedTimer.previousStreams)
    }
    logTransition(
      title = "Timer finalizado • Modo restaurado",
      detail = "Retornou para ${restoreMode.title}",
      source = TransitionTriggerSource.RESTORE,
      mode = restoreMode,
    )
    // Reavalia se alguma regra de horário/local está ativa agora
    evaluateAndApplyRules(source = TransitionTriggerSource.RESTORE)
    onRefreshExternalSurfaces()
    refreshSystemState()
  }

  override suspend fun saveCurrentAsProfile(name: String, description: String): Result<SoundProfile> = runCatching {
    val trimmedName = name.trim()
    require(trimmedName.isNotEmpty()) { "O nome do perfil não pode ser vazio" }
    val profile = SoundProfile(
      id = "custom_${clockNowMillis()}",
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

  override suspend fun upsertProfile(
    profile: SoundProfile,
    applyImmediately: Boolean,
  ): Result<SoundProfile> = runCatching {
    val trimmedName = profile.name.trim()
    require(trimmedName.isNotEmpty()) { "O nome do perfil não pode ser vazio" }
    val normalized = profile.copy(
      name = trimmedName,
      description = profile.description.trim().ifEmpty { profile.summaryLabel() },
    )
    val prefsBefore = preferencesDataSource.prefsFlow.first()
    preferencesDataSource.upsertCustomProfile(normalized)
    preferencesDataSource.setOverriddenSessionKey(null)

    if (applyImmediately || prefsBefore.activeProfileId == normalized.id) {
      activateProfile(normalized, source = TransitionTriggerSource.MANUAL).getOrThrow()
    } else {
      evaluateAndApplyRules(source = TransitionTriggerSource.MANUAL)
      onRefreshExternalSurfaces()
      refreshSystemState()
    }
    normalized
  }

  override suspend fun deleteCustomProfile(profileId: String): Result<Unit> = runCatching {
    preferencesDataSource.deleteCustomProfile(profileId)
    onRefreshExternalSurfaces()
    refreshSystemState()
  }

  override suspend fun upsertScheduleRule(rule: ScheduleRule): Result<Unit> = runCatching {
    val prevPrefs = preferencesDataSource.prefsFlow.first()
    val wasPreviouslyActive = prevPrefs.lastAppliedRuleSessionKey?.contains(rule.id) == true
    preferencesDataSource.upsertScheduleRule(rule)
    preferencesDataSource.setOverriddenSessionKey(null)
    if (!rule.enabled && wasPreviouslyActive) {
      // Marca para garantir restauração se nenhuma outra regra estiver ativa
      preferencesDataSource.setLastAppliedRuleSessionKey("force_restore_${rule.id}")
    }
    val updatedPrefs = preferencesDataSource.prefsFlow.first()
    onRescheduleRuleAlarms(updatedPrefs.scheduleRules)
    evaluateAndApplyRules(source = TransitionTriggerSource.SCHEDULE)
    onRefreshExternalSurfaces()
    refreshSystemState()
  }

  override suspend fun deleteScheduleRule(ruleId: String): Result<Unit> = runCatching {
    val prevPrefs = preferencesDataSource.prefsFlow.first()
    val wasPreviouslyActive = prevPrefs.lastAppliedRuleSessionKey?.contains(ruleId) == true
    preferencesDataSource.deleteScheduleRule(ruleId)
    preferencesDataSource.setOverriddenSessionKey(null)
    if (wasPreviouslyActive) {
      preferencesDataSource.setLastAppliedRuleSessionKey("force_restore_$ruleId")
    }
    val updatedPrefs = preferencesDataSource.prefsFlow.first()
    onRescheduleRuleAlarms(updatedPrefs.scheduleRules)
    evaluateAndApplyRules(source = TransitionTriggerSource.SCHEDULE)
    onRefreshExternalSurfaces()
    refreshSystemState()
  }

  override suspend fun upsertLocationRule(rule: LocationRule): Result<Unit> = runCatching {
    val prevPrefs = preferencesDataSource.prefsFlow.first()
    val wasPreviouslyActive = prevPrefs.lastAppliedRuleSessionKey?.contains(rule.id) == true
    preferencesDataSource.upsertLocationRule(rule)
    preferencesDataSource.setOverriddenSessionKey(null)
    if ((!rule.enabled || !rule.isCurrentlyInside) && wasPreviouslyActive) {
      preferencesDataSource.setLastAppliedRuleSessionKey("force_restore_${rule.id}")
    }
    evaluateAndApplyRules(source = TransitionTriggerSource.LOCATION)
    onRefreshExternalSurfaces()
    refreshSystemState()
  }

  override suspend fun deleteLocationRule(ruleId: String): Result<Unit> = runCatching {
    val prevPrefs = preferencesDataSource.prefsFlow.first()
    val wasPreviouslyActive = prevPrefs.lastAppliedRuleSessionKey?.contains(ruleId) == true
    preferencesDataSource.deleteLocationRule(ruleId)
    preferencesDataSource.setOverriddenSessionKey(null)
    if (wasPreviouslyActive) {
      preferencesDataSource.setLastAppliedRuleSessionKey("force_restore_$ruleId")
    }
    evaluateAndApplyRules(source = TransitionTriggerSource.LOCATION)
    onRefreshExternalSurfaces()
    refreshSystemState()
  }

  override suspend fun toggleLocationPresence(ruleId: String, inside: Boolean): Result<Unit> = runCatching {
    val prefs = preferencesDataSource.prefsFlow.first()
    val target = prefs.locationRules.firstOrNull { it.id == ruleId } ?: return@runCatching
    val updated = target.copy(isCurrentlyInside = inside)
    preferencesDataSource.upsertLocationRule(updated)
    preferencesDataSource.setOverriddenSessionKey(null)
    if (!inside && prefs.lastAppliedRuleSessionKey?.contains(ruleId) == true) {
      preferencesDataSource.setLastAppliedRuleSessionKey("force_restore_$ruleId")
    }
    evaluateAndApplyRules(source = TransitionTriggerSource.LOCATION)
    onRefreshExternalSurfaces()
    refreshSystemState()
  }

  override suspend fun setRestorePolicy(policy: RestorePolicy): Result<Unit> = runCatching {
    preferencesDataSource.setRestorePolicy(policy)
    onRefreshExternalSurfaces()
    refreshSystemState()
  }

  override suspend fun setDefaultDayProfileId(profileId: String): Result<Unit> = runCatching {
    preferencesDataSource.setDefaultDayProfileId(profileId)
    val prefs = preferencesDataSource.prefsFlow.first()
    if (prefs.activeTimer == null && prefs.lastAppliedRuleSessionKey == null) {
      val allProfiles = mergeProfiles(prefs.customProfiles)
      val selectedDefault = allProfiles.firstOrNull { it.id == profileId }
      if (selectedDefault != null) {
        applyProfileToSystem(selectedDefault)
        preferencesDataSource.setActiveProfileId(selectedDefault.id)
      }
    }
    onRefreshExternalSurfaces()
    refreshSystemState()
  }

  override suspend fun setUseDynamicColor(enabled: Boolean): Result<Unit> = runCatching {
    preferencesDataSource.setUseDynamicColor(enabled)
    refreshSystemState()
  }

  override suspend fun setThemeMode(mode: ThemeMode): Result<Unit> = runCatching {
    preferencesDataSource.setThemeMode(mode)
    refreshSystemState()
  }

  override suspend fun setLocationDisclosureAccepted(accepted: Boolean): Result<Unit> = runCatching {
    preferencesDataSource.setLocationDisclosureAccepted(accepted)
    refreshSystemState()
  }

  override suspend fun setMuteMediaOnSilent(enabled: Boolean): Result<Unit> = runCatching {
    preferencesDataSource.setMuteMediaOnSilent(enabled)
    syncMediaProtectionNow()
    onRefreshExternalSurfaces()
    refreshSystemState()
  }

  override suspend fun setPreserveMediaOnBluetooth(enabled: Boolean): Result<Unit> = runCatching {
    preferencesDataSource.setPreserveMediaOnBluetooth(enabled)
    syncMediaProtectionNow()
    onRefreshExternalSurfaces()
    refreshSystemState()
  }

  override suspend fun setSmartVolumeTriggerEnabled(enabled: Boolean): Result<Unit> = runCatching {
    preferencesDataSource.setSmartVolumeTriggerEnabled(enabled)
    if (!enabled) {
      onDismissSmartPrompt()
    }
    refreshSystemState()
  }

  override suspend fun clearTransitionLogs(): Result<Unit> = runCatching {
    preferencesDataSource.clearTransitionLogs()
    refreshSystemState()
  }

  override suspend fun reapplyActiveRule(): Result<Unit> = runCatching {
    preferencesDataSource.setOverriddenSessionKey(null)
    preferencesDataSource.setLastAppliedRuleSessionKey(null)
    systemController.clearExternalBroadcastFlag()
    evaluateAndApplyRules(source = TransitionTriggerSource.MANUAL).getOrThrow()
    onRefreshExternalSurfaces()
    refreshSystemState()
  }

  override suspend fun evaluateAndApplyRules(
    source: TransitionTriggerSource,
    nowEpochMillis: Long,
  ): Result<RuleEvaluationDecision> = runCatching {
    val prefs = preferencesDataSource.prefsFlow.first()
    val allProfiles = mergeProfiles(prefs.customProfiles)
    val nowDateTime = LocalDateTime.ofInstant(
      Instant.ofEpochMilli(nowEpochMillis),
      ZoneId.systemDefault(),
    )

    // Se o sistema detectou que o usuário apertou o botão físico de volume durante uma regra ativa
    if (systemController.lastBroadcastWasExternal && prefs.lastAppliedRuleSessionKey != null) {
      preferencesDataSource.setOverriddenSessionKey(prefs.lastAppliedRuleSessionKey)
      systemController.clearExternalBroadcastFlag()
    }

    val updatedPrefs = preferencesDataSource.prefsFlow.first()
    val decision = RuleEvaluator.evaluate(
      nowDateTime = nowDateTime,
      nowEpochMillis = nowEpochMillis,
      profiles = allProfiles,
      scheduleRules = updatedPrefs.scheduleRules,
      locationRules = updatedPrefs.locationRules,
      activeTimer = updatedPrefs.activeTimer,
      overriddenSessionKey = updatedPrefs.overriddenSessionKey,
      restorePolicy = updatedPrefs.restorePolicy,
      defaultDayProfileId = updatedPrefs.defaultDayProfileId,
      savedSnapshot = updatedPrefs.savedSnapshot,
    )

    when (decision) {
      is RuleEvaluationDecision.TimerActive -> {
        // Mantém o timer atual sem sobrescrever
      }

      is RuleEvaluationDecision.RuleOverriddenByUser -> {
        // Respeita o override manual do usuário até a próxima mudança de sessão
      }

      is RuleEvaluationDecision.ApplyRule -> {
        val winner = decision.winner
        // Se estamos entrando em uma sessão de regra a partir do estado livre, salva snapshot de entrada
        if (updatedPrefs.lastAppliedRuleSessionKey == null) {
          preferencesDataSource.setSavedSnapshot(
            SavedStateSnapshot(
              mode = systemController.readCurrentMode(),
              streams = systemController.readVolumeStreams(),
              capturedAtEpochMillis = nowEpochMillis,
            ),
          )
        }
        // Se a sessão mudou, limpa qualquer override antigo
        if (updatedPrefs.overriddenSessionKey != null &&
          updatedPrefs.overriddenSessionKey != winner.sessionKey
        ) {
          preferencesDataSource.setOverriddenSessionKey(null)
        }

        // Aplica o perfil se ainda não for a sessão aplicada (ou se chamado explicitamente)
        applyProfileToSystem(winner.profile).getOrThrow()
        preferencesDataSource.setActiveProfileId(winner.profile.id)

        if (updatedPrefs.lastAppliedRuleSessionKey != winner.sessionKey) {
          preferencesDataSource.setLastAppliedRuleSessionKey(winner.sessionKey)
          logTransition(
            title = "Regra '${winner.ruleName}' ativada",
            detail = "${winner.subtitle} -> ${winner.profile.name} (${winner.profile.mode.title})",
            source = winner.source,
            mode = winner.profile.mode,
          )
        }
      }

      is RuleEvaluationDecision.NoActiveRules -> {
        // Se havia uma regra ativa que acabou de se encerrar, executa a política de restauração
        if (updatedPrefs.lastAppliedRuleSessionKey != null) {
          preferencesDataSource.setLastAppliedRuleSessionKey(null)
          preferencesDataSource.setOverriddenSessionKey(null)

          if (decision.restorePolicy == RestorePolicy.SNAPSHOT && decision.savedSnapshot != null) {
            val snap = decision.savedSnapshot
            systemController.applySoundMode(snap.mode).getOrThrow()
            if (snap.mode == SoundMode.NORMAL) {
              systemController.applyAllStreams(snap.streams).getOrThrow()
            }
            preferencesDataSource.setActiveProfileId(null)
            preferencesDataSource.setSavedSnapshot(null)
            logTransition(
              title = "Período encerrado • Snapshot restaurado",
              detail = "Retornou para o estado anterior (${snap.mode.title})",
              source = TransitionTriggerSource.RESTORE,
              mode = snap.mode,
            )
          } else {
            val fallback = decision.fallbackProfile
            applyProfileToSystem(fallback).getOrThrow()
            preferencesDataSource.setActiveProfileId(fallback.id)
            preferencesDataSource.setSavedSnapshot(null)
            logTransition(
              title = "Período encerrado • ${fallback.name}",
              detail = "Restaurado para o perfil padrão do dia (${fallback.mode.title})",
              source = TransitionTriggerSource.RESTORE,
              mode = fallback.mode,
            )
          }
        }
      }
    }

    onRescheduleRuleAlarms(updatedPrefs.scheduleRules)
    onSyncActiveTimerNotification(updatedPrefs.activeTimer)
    onRefreshExternalSurfaces()
    refreshSystemState()
    decision
  }

  override suspend fun cycleSoundModeFromQuickTile(): Result<SoundMode> = runCatching {
    val current = systemController.readCurrentMode()
    val nextMode = when (current) {
      SoundMode.NORMAL -> SoundMode.VIBRATE
      SoundMode.VIBRATE -> if (systemController.hasDndPermission()) SoundMode.SILENT else SoundMode.NORMAL
      SoundMode.SILENT -> SoundMode.NORMAL
    }
    setSoundMode(nextMode, source = TransitionTriggerSource.QUICK_TILE).getOrThrow()
    nextMode
  }

  override suspend fun handleExternalAudioChange(): Result<Unit> = runCatching {
    if (!systemController.lastBroadcastWasExternal) return@runCatching
    systemController.clearExternalBroadcastFlag()

    val prefs = preferencesDataSource.prefsFlow.first()
    val currentMode = systemController.readCurrentMode()
    val streams = systemController.readVolumeStreams()

    // 1. Se havia uma regra ativa, marca como override manual pelo botão físico
    if (prefs.lastAppliedRuleSessionKey != null && prefs.overriddenSessionKey != prefs.lastAppliedRuleSessionKey) {
      preferencesDataSource.setOverriddenSessionKey(prefs.lastAppliedRuleSessionKey)
      if (prefs.smartVolumeTriggerEnabled) {
        val ruleName = prefs.scheduleRules.firstOrNull {
          prefs.lastAppliedRuleSessionKey.contains(it.id)
        }?.name ?: prefs.locationRules.firstOrNull {
          prefs.lastAppliedRuleSessionKey.contains(it.id)
        }?.name ?: "Ativa"
        onRuleOverrideAlert(ruleName)
      }
    }

    // 2. Sincronização automática de Mídia + Notificação Inteligente quando silenciado pelo botão físico
    if (currentMode != SoundMode.NORMAL) {
      var mediaAutoMuted = false
      val skipBt = prefs.preserveMediaOnBluetooth && systemController.isBluetoothHeadsetConnected()
      if (prefs.muteMediaOnSilent && !skipBt && streams.mediaPercent > 0) {
        preferencesDataSource.setSavedMediaBeforeMute(streams.mediaPercent)
        systemController.applyStreamVolume(VolumeStreamType.MEDIA, 0)
        mediaAutoMuted = true
      }

      if (prefs.smartVolumeTriggerEnabled && prefs.activeTimer == null) {
        onSmartRestorePrompt(currentMode, mediaAutoMuted)
      }

      logTransition(
        title = "Botão de volume → ${currentMode.title}",
        detail = if (mediaAutoMuted) "Mídia silenciada automaticamente (0%)" else "Alterado pelos botões físicos",
        source = TransitionTriggerSource.HARDWARE_BUTTON,
        mode = currentMode,
      )
    } else {
      onDismissSmartPrompt()
      val savedMedia = prefs.savedMediaBeforeMute
      if (savedMedia != null && streams.mediaPercent == 0) {
        systemController.applyStreamVolume(VolumeStreamType.MEDIA, savedMedia)
        preferencesDataSource.setSavedMediaBeforeMute(null)
      }
    }

    onRefreshExternalSurfaces()
    refreshSystemState()
  }

  private suspend fun syncMediaProtectionNow() {
    val currentMode = systemController.readCurrentMode()
    if (currentMode == SoundMode.NORMAL) return
    val prefs = preferencesDataSource.prefsFlow.first()
    val streams = systemController.readVolumeStreams()
    val skipBt = prefs.preserveMediaOnBluetooth && systemController.isBluetoothHeadsetConnected()
    val shouldMute = prefs.muteMediaOnSilent && !skipBt

    if (shouldMute && streams.mediaPercent > 0) {
      preferencesDataSource.setSavedMediaBeforeMute(streams.mediaPercent)
      systemController.applyStreamVolume(VolumeStreamType.MEDIA, 0)
    } else if (!shouldMute && streams.mediaPercent == 0 && prefs.savedMediaBeforeMute != null) {
      systemController.applyStreamVolume(VolumeStreamType.MEDIA, prefs.savedMediaBeforeMute)
      preferencesDataSource.setSavedMediaBeforeMute(null)
    }
  }

  private fun applyProfileToSystem(profile: SoundProfile): Result<Unit> = runCatching {
    systemController.applySoundMode(profile.mode).getOrThrow()

    val skipMediaBecausePlaying = profile.preserveMediaIfPlaying && systemController.isMusicActive()
    val skipMediaBecauseBt = profile.skipMediaOnBluetooth && systemController.isBluetoothHeadsetConnected()
    val shouldChangeMedia = !skipMediaBecausePlaying && !skipMediaBecauseBt

    if (profile.mode == SoundMode.NORMAL) {
      systemController.applyStreamVolume(VolumeStreamType.RING, profile.streams.ringPercent).getOrThrow()
      systemController.applyStreamVolume(VolumeStreamType.NOTIFICATION, profile.streams.notificationPercent).getOrThrow()
    }
    if (shouldChangeMedia) {
      systemController.applyStreamVolume(VolumeStreamType.MEDIA, profile.streams.mediaPercent).getOrThrow()
    }
    systemController.applyStreamVolume(VolumeStreamType.ALARM, profile.streams.alarmPercent).getOrThrow()
  }

  private suspend fun markManualOverrideIfRuleActive() {
    val prefs = preferencesDataSource.prefsFlow.first()
    val nowMillis = clockNowMillis()
    val nowDateTime = LocalDateTime.ofInstant(
      Instant.ofEpochMilli(nowMillis),
      ZoneId.systemDefault(),
    )
    val allProfiles = mergeProfiles(prefs.customProfiles)
    val decision = RuleEvaluator.evaluate(
      nowDateTime = nowDateTime,
      nowEpochMillis = nowMillis,
      profiles = allProfiles,
      scheduleRules = prefs.scheduleRules,
      locationRules = prefs.locationRules,
      activeTimer = prefs.activeTimer,
      overriddenSessionKey = null,
      restorePolicy = prefs.restorePolicy,
      defaultDayProfileId = prefs.defaultDayProfileId,
      savedSnapshot = prefs.savedSnapshot,
    )
    if (decision is RuleEvaluationDecision.ApplyRule) {
      preferencesDataSource.setLastAppliedRuleSessionKey(decision.winner.sessionKey)
      preferencesDataSource.setOverriddenSessionKey(decision.winner.sessionKey)
    }
  }

  private suspend fun logTransition(
    title: String,
    detail: String,
    source: TransitionTriggerSource,
    mode: SoundMode,
  ) {
    val now = clockNowMillis()
    preferencesDataSource.appendTransitionLog(
      TransitionLogEntry(
        id = "log_${now}_${title.hashCode()}",
        timestampEpochMillis = now,
        title = title,
        detail = detail,
        source = source,
        appliedMode = mode,
      ),
    )
  }

  private fun mergeProfiles(savedProfiles: List<SoundProfile>): List<SoundProfile> {
    val savedById = savedProfiles.associateBy { it.id }
    val presetIds = SoundProfile.defaultPresets.map { it.id }.toSet()
    val resolvedPresets = SoundProfile.defaultPresets.map { preset ->
      savedById[preset.id]?.copy(isPreset = true) ?: preset
    }
    val extraCustom = savedProfiles.filterNot { it.id in presetIds }
    return resolvedPresets + extraCustom
  }

  private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
  )
}
