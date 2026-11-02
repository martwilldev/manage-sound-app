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
  suspend fun cancelOrRestoreTimedSession(): Result<Unit>
  suspend fun saveCurrentAsProfile(name: String, description: String): Result<SoundProfile>
  suspend fun deleteCustomProfile(profileId: String): Result<Unit>

  suspend fun upsertScheduleRule(rule: ScheduleRule): Result<Unit>
  suspend fun deleteScheduleRule(ruleId: String): Result<Unit>
  suspend fun upsertLocationRule(rule: LocationRule): Result<Unit>
  suspend fun deleteLocationRule(ruleId: String): Result<Unit>
  suspend fun toggleLocationPresence(ruleId: String, inside: Boolean): Result<Unit>

  suspend fun setRestorePolicy(policy: RestorePolicy): Result<Unit>
  suspend fun setDefaultDayProfileId(profileId: String): Result<Unit>
  suspend fun setUseDynamicColor(enabled: Boolean): Result<Unit>
  suspend fun setLocationDisclosureAccepted(accepted: Boolean): Result<Unit>
  suspend fun clearTransitionLogs(): Result<Unit>

  suspend fun reapplyActiveRule(): Result<Unit>
  suspend fun evaluateAndApplyRules(
    source: TransitionTriggerSource = TransitionTriggerSource.SCHEDULE,
    nowEpochMillis: Long = System.currentTimeMillis(),
  ): Result<RuleEvaluationDecision>
  suspend fun cycleSoundModeFromQuickTile(): Result<SoundMode>
}

class DefaultSoundModeRepository(
  private val systemController: SoundSystemController,
  private val preferencesDataSource: SoundPreferencesDataSource,
  private val onScheduleAutoRestore: (durationMinutes: Int) -> Unit = {},
  private val onCancelAutoRestore: () -> Unit = {},
  private val onRescheduleRuleAlarms: (scheduleRules: List<ScheduleRule>) -> Unit = {},
  private val clockNowMillis: () -> Long = { System.currentTimeMillis() },
) : SoundModeRepository {

  private val manualRefreshTrigger = MutableStateFlow(0L)

  override val preferencesStream: Flow<PersistedSoundPrefs> = preferencesDataSource.prefsFlow

  override val profilesStream: Flow<List<SoundProfile>> =
    preferencesDataSource.prefsFlow.combine(manualRefreshTrigger) { prefs, _ ->
      SoundProfile.defaultPresets + prefs.customProfiles
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
    val allProfiles = SoundProfile.defaultPresets + prefs.customProfiles

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
    return systemController.applySoundMode(mode).map {
      markManualOverrideIfRuleActive()
      preferencesDataSource.setActiveProfileId(null)
      logTransition(
        title = "Modo alterado para ${mode.title}",
        detail = "Alternância direta de modo de toque",
        source = source,
        mode = mode,
      )
      refreshSystemState()
    }
  }

  override suspend fun setStreamVolume(type: VolumeStreamType, percent: Int): Result<Unit> {
    return systemController.applyStreamVolume(type, percent).map {
      markManualOverrideIfRuleActive()
      preferencesDataSource.setActiveProfileId(null)
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
      logTransition(
        title = "Timer iniciado: ${targetMode.title} por ${durationMinutes}m",
        detail = "Restaurará automaticamente para ${previousMode.title}",
        source = TransitionTriggerSource.TIMER,
        mode = targetMode,
      )
      refreshSystemState()
      session
    }
  }

  override suspend fun cancelOrRestoreTimedSession(): Result<Unit> = runCatching {
    onCancelAutoRestore()
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

  override suspend fun deleteCustomProfile(profileId: String): Result<Unit> = runCatching {
    preferencesDataSource.deleteCustomProfile(profileId)
    refreshSystemState()
  }

  override suspend fun upsertScheduleRule(rule: ScheduleRule): Result<Unit> = runCatching {
    preferencesDataSource.upsertScheduleRule(rule)
    val updatedPrefs = preferencesDataSource.prefsFlow.first()
    onRescheduleRuleAlarms(updatedPrefs.scheduleRules)
    evaluateAndApplyRules(source = TransitionTriggerSource.SCHEDULE)
    refreshSystemState()
  }

  override suspend fun deleteScheduleRule(ruleId: String): Result<Unit> = runCatching {
    preferencesDataSource.deleteScheduleRule(ruleId)
    val updatedPrefs = preferencesDataSource.prefsFlow.first()
    onRescheduleRuleAlarms(updatedPrefs.scheduleRules)
    evaluateAndApplyRules(source = TransitionTriggerSource.SCHEDULE)
    refreshSystemState()
  }

  override suspend fun upsertLocationRule(rule: LocationRule): Result<Unit> = runCatching {
    preferencesDataSource.upsertLocationRule(rule)
    evaluateAndApplyRules(source = TransitionTriggerSource.LOCATION)
    refreshSystemState()
  }

  override suspend fun deleteLocationRule(ruleId: String): Result<Unit> = runCatching {
    preferencesDataSource.deleteLocationRule(ruleId)
    evaluateAndApplyRules(source = TransitionTriggerSource.LOCATION)
    refreshSystemState()
  }

  override suspend fun toggleLocationPresence(ruleId: String, inside: Boolean): Result<Unit> = runCatching {
    val prefs = preferencesDataSource.prefsFlow.first()
    val target = prefs.locationRules.firstOrNull { it.id == ruleId } ?: return@runCatching
    val updated = target.copy(isCurrentlyInside = inside)
    preferencesDataSource.upsertLocationRule(updated)
    evaluateAndApplyRules(source = TransitionTriggerSource.LOCATION)
    refreshSystemState()
  }

  override suspend fun setRestorePolicy(policy: RestorePolicy): Result<Unit> = runCatching {
    preferencesDataSource.setRestorePolicy(policy)
    refreshSystemState()
  }

  override suspend fun setDefaultDayProfileId(profileId: String): Result<Unit> = runCatching {
    preferencesDataSource.setDefaultDayProfileId(profileId)
    refreshSystemState()
  }

  override suspend fun setUseDynamicColor(enabled: Boolean): Result<Unit> = runCatching {
    preferencesDataSource.setUseDynamicColor(enabled)
    refreshSystemState()
  }

  override suspend fun setLocationDisclosureAccepted(accepted: Boolean): Result<Unit> = runCatching {
    preferencesDataSource.setLocationDisclosureAccepted(accepted)
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
    refreshSystemState()
  }

  override suspend fun evaluateAndApplyRules(
    source: TransitionTriggerSource,
    nowEpochMillis: Long,
  ): Result<RuleEvaluationDecision> = runCatching {
    val prefs = preferencesDataSource.prefsFlow.first()
    val allProfiles = SoundProfile.defaultPresets + prefs.customProfiles
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
    val allProfiles = SoundProfile.defaultPresets + prefs.customProfiles
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

  private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
  )
}
