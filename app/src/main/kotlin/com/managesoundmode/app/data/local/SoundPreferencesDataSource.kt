package com.managesoundmode.app.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.managesoundmode.app.domain.model.AccessControlConfig
import com.managesoundmode.app.domain.model.LocationRule
import com.managesoundmode.app.domain.model.RestorePolicy
import com.managesoundmode.app.domain.model.SavedStateSnapshot
import com.managesoundmode.app.domain.model.ScheduleRule
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.domain.model.ThemeMode
import com.managesoundmode.app.domain.model.TimedModeSession
import com.managesoundmode.app.domain.model.TransitionLogEntry
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.soundDataStore: DataStore<Preferences> by preferencesDataStore(
  name = "manage_sound_mode_prefs",
)

@Serializable
data class SoundPrefsBackupPayload(
  val version: Int = 1,
  val customProfiles: List<SoundProfile> = emptyList(),
  val deletedProfileIds: Set<String> = emptySet(),
  val scheduleRules: List<ScheduleRule> = ScheduleRule.defaultSchedules,
  val locationRules: List<LocationRule> = LocationRule.defaultLocations,
  val restorePolicy: RestorePolicy = RestorePolicy.DEFAULT_PROFILE,
  val defaultDayProfileId: String = SoundProfile.DEFAULT_DAY_PROFILE_ID,
  val useDynamicColor: Boolean = true,
  val themeMode: ThemeMode = ThemeMode.DARK,
  val appLanguageCode: String = "PT_BR",
  val automationsPaused: Boolean = false,
  val hapticFeedbackEnabled: Boolean = true,
  val notifyOnAutoTransition: Boolean = true,
  val defaultTimerMinutes: Int = 30,
  val muteMediaOnSilent: Boolean = true,
  val preserveMediaOnBluetooth: Boolean = true,
  val smartVolumeTriggerEnabled: Boolean = true,
  val persistentQuickBarEnabled: Boolean = true,
  val quickTileSlot1ProfileId: String = "preset_work_high",
  val quickTileSlot2ProfileId: String = "preset_sleep",
  val accessControlConfig: AccessControlConfig = AccessControlConfig(),
  val systemZenRulesEnabled: Boolean = true,
)

data class PersistedSoundPrefs(
  val activeProfileId: String? = null,
  val activeTimer: TimedModeSession? = null,
  val customProfiles: List<SoundProfile> = emptyList(),
  val deletedProfileIds: Set<String> = emptySet(),
  val scheduleRules: List<ScheduleRule> = ScheduleRule.defaultSchedules,
  val locationRules: List<LocationRule> = LocationRule.defaultLocations,
  val restorePolicy: RestorePolicy = RestorePolicy.DEFAULT_PROFILE,
  val defaultDayProfileId: String = SoundProfile.DEFAULT_DAY_PROFILE_ID,
  val savedSnapshot: SavedStateSnapshot? = null,
  val lastAppliedRuleSessionKey: String? = null,
  val overriddenSessionKey: String? = null,
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
  val savedMediaBeforeMute: Int? = null,
  val appLanguageCode: String = "PT_BR",
  val automationsPaused: Boolean = false,
  val hapticFeedbackEnabled: Boolean = true,
  val notifyOnAutoTransition: Boolean = true,
  val defaultTimerMinutes: Int = 30,
  val hasCompletedOnboarding: Boolean = false,
  val accessControlConfig: AccessControlConfig = AccessControlConfig(),
  val systemZenRulesEnabled: Boolean = true,
)

interface SoundPreferencesDataSource {
  val prefsFlow: Flow<PersistedSoundPrefs>
  suspend fun setActiveProfileId(profileId: String?)
  suspend fun setActiveTimer(session: TimedModeSession?)
  suspend fun upsertCustomProfile(profile: SoundProfile)
  suspend fun deleteCustomProfile(profileId: String)
  suspend fun upsertScheduleRule(rule: ScheduleRule) {}
  suspend fun deleteScheduleRule(ruleId: String) {}
  suspend fun upsertLocationRule(rule: LocationRule) {}
  suspend fun deleteLocationRule(ruleId: String) {}
  suspend fun setRestorePolicy(policy: RestorePolicy) {}
  suspend fun setDefaultDayProfileId(profileId: String) {}
  suspend fun setSavedSnapshot(snapshot: SavedStateSnapshot?) {}
  suspend fun setLastAppliedRuleSessionKey(sessionKey: String?) {}
  suspend fun setOverriddenSessionKey(sessionKey: String?) {}
  suspend fun appendTransitionLog(entry: TransitionLogEntry) {}
  suspend fun clearTransitionLogs() {}
  suspend fun setUseDynamicColor(enabled: Boolean) {}
  suspend fun setThemeMode(mode: ThemeMode) {}
  suspend fun setLocationDisclosureAccepted(accepted: Boolean) {}
  suspend fun setMuteMediaOnSilent(enabled: Boolean) {}
  suspend fun setPreserveMediaOnBluetooth(enabled: Boolean) {}
  suspend fun setSmartVolumeTriggerEnabled(enabled: Boolean) {}
  suspend fun setPersistentQuickBarEnabled(enabled: Boolean) {}
  suspend fun setQuickTileSlot1ProfileId(profileId: String) {}
  suspend fun setQuickTileSlot2ProfileId(profileId: String) {}
  suspend fun setSavedMediaBeforeMute(percent: Int?) {}
  suspend fun setAppLanguageCode(languageCode: String) {}
  suspend fun setAutomationsPaused(paused: Boolean) {}
  suspend fun setHapticFeedbackEnabled(enabled: Boolean) {}
  suspend fun setNotifyOnAutoTransition(enabled: Boolean) {}
  suspend fun setDefaultTimerMinutes(minutes: Int) {}
  suspend fun setOnboardingCompleted(completed: Boolean) {}
  suspend fun setAccessControlConfig(config: AccessControlConfig) {}
  suspend fun setSystemZenRulesEnabled(enabled: Boolean) {}
  suspend fun resetFactoryDefaults() {}
  suspend fun exportBackupJson(): String = "{}"
  suspend fun importBackupJson(rawJson: String) {}
}

class DataStoreSoundPreferencesDataSource(
  private val context: Context,
  private val json: Json = Json { ignoreUnknownKeys = true },
) : SoundPreferencesDataSource {

  override val prefsFlow: Flow<PersistedSoundPrefs> =
    context.soundDataStore.data
      .catch { exception ->
        if (exception is IOException) {
          Log.w(TAG, "Erro ao ler Preferences DataStore; emitindo vazio", exception)
          emit(emptyPreferences())
        } else {
          throw exception
        }
      }
      .map { prefs ->
        val activeProfileId = prefs[KEY_ACTIVE_PROFILE_ID]
        val activeTimer = prefs[KEY_ACTIVE_TIMER_JSON]?.let { raw ->
          runCatching { json.decodeFromString<TimedModeSession>(raw) }
            .onFailure { Log.w(TAG, "Falha ao desserializar timer salvo", it) }
            .getOrNull()
        }
        val customProfiles = prefs[KEY_CUSTOM_PROFILES_JSON]?.let { raw ->
          runCatching { json.decodeFromString<List<SoundProfile>>(raw) }
            .onFailure { Log.w(TAG, "Falha ao desserializar perfis customizados", it) }
            .getOrDefault(emptyList())
        } ?: emptyList()

        val deletedProfileIds = prefs[KEY_DELETED_PROFILE_IDS_JSON]?.let { raw ->
          runCatching { json.decodeFromString<Set<String>>(raw) }
            .getOrDefault(emptySet())
        } ?: emptySet()

        val scheduleRules = prefs[KEY_SCHEDULE_RULES_JSON]?.let { raw ->
          runCatching { json.decodeFromString<List<ScheduleRule>>(raw) }
            .onFailure { Log.w(TAG, "Falha ao desserializar regras de horário", it) }
            .getOrDefault(ScheduleRule.defaultSchedules)
        } ?: ScheduleRule.defaultSchedules

        val locationRules = prefs[KEY_LOCATION_RULES_JSON]?.let { raw ->
          runCatching { json.decodeFromString<List<LocationRule>>(raw) }
            .onFailure { Log.w(TAG, "Falha ao desserializar regras de local", it) }
            .getOrDefault(LocationRule.defaultLocations)
        } ?: LocationRule.defaultLocations

        val restorePolicy = prefs[KEY_RESTORE_POLICY]?.let { raw ->
          runCatching { RestorePolicy.valueOf(raw) }.getOrDefault(RestorePolicy.DEFAULT_PROFILE)
        } ?: RestorePolicy.DEFAULT_PROFILE

        val defaultDayProfileId =
          prefs[KEY_DEFAULT_DAY_PROFILE_ID] ?: SoundProfile.DEFAULT_DAY_PROFILE_ID

        val savedSnapshot = prefs[KEY_SAVED_SNAPSHOT_JSON]?.let { raw ->
          runCatching { json.decodeFromString<SavedStateSnapshot>(raw) }.getOrNull()
        }

        val transitionLogs = prefs[KEY_TRANSITION_LOGS_JSON]?.let { raw ->
          runCatching { json.decodeFromString<List<TransitionLogEntry>>(raw) }
            .getOrDefault(emptyList())
        } ?: emptyList()

        val themeMode = prefs[KEY_THEME_MODE]?.let { raw ->
          runCatching { ThemeMode.valueOf(raw) }.getOrDefault(ThemeMode.DARK)
        } ?: ThemeMode.DARK

        val accessControlConfig = prefs[KEY_ACCESS_CONTROL_JSON]?.let { raw ->
          runCatching { json.decodeFromString<AccessControlConfig>(raw) }
            .getOrDefault(AccessControlConfig())
        } ?: AccessControlConfig()

        PersistedSoundPrefs(
          activeProfileId = activeProfileId,
          activeTimer = activeTimer,
          customProfiles = customProfiles,
          deletedProfileIds = deletedProfileIds,
          scheduleRules = scheduleRules,
          locationRules = locationRules,
          restorePolicy = restorePolicy,
          defaultDayProfileId = defaultDayProfileId,
          savedSnapshot = savedSnapshot,
          lastAppliedRuleSessionKey = prefs[KEY_LAST_APPLIED_RULE_SESSION_KEY],
          overriddenSessionKey = prefs[KEY_OVERRIDDEN_SESSION_KEY],
          transitionLogs = transitionLogs,
          useDynamicColor = prefs[KEY_USE_DYNAMIC_COLOR] ?: true,
          themeMode = themeMode,
          hasAcceptedLocationDisclosure = prefs[KEY_LOCATION_DISCLOSURE_ACCEPTED] ?: false,
          muteMediaOnSilent = prefs[KEY_MUTE_MEDIA_ON_SILENT] ?: true,
          preserveMediaOnBluetooth = prefs[KEY_PRESERVE_MEDIA_ON_BT] ?: true,
          smartVolumeTriggerEnabled = prefs[KEY_SMART_VOLUME_TRIGGER] ?: true,
          persistentQuickBarEnabled = prefs[KEY_PERSISTENT_QUICK_BAR] ?: true,
          quickTileSlot1ProfileId = prefs[KEY_QUICK_TILE_SLOT1_PROFILE_ID] ?: "preset_work_high",
          quickTileSlot2ProfileId = prefs[KEY_QUICK_TILE_SLOT2_PROFILE_ID] ?: "preset_sleep",
          savedMediaBeforeMute = prefs[KEY_SAVED_MEDIA_BEFORE_MUTE],
          appLanguageCode = prefs[KEY_APP_LANGUAGE_CODE] ?: "PT_BR",
          automationsPaused = prefs[KEY_AUTOMATIONS_PAUSED] ?: false,
          hapticFeedbackEnabled = prefs[KEY_HAPTIC_FEEDBACK_ENABLED] ?: true,
          notifyOnAutoTransition = prefs[KEY_NOTIFY_ON_AUTO_TRANSITION] ?: true,
          defaultTimerMinutes = prefs[KEY_DEFAULT_TIMER_MINUTES] ?: 30,
          hasCompletedOnboarding = prefs[KEY_HAS_COMPLETED_ONBOARDING] ?: false,
          accessControlConfig = accessControlConfig,
          systemZenRulesEnabled = prefs[KEY_SYSTEM_ZEN_RULES_ENABLED] ?: true,
        )
      }

  override suspend fun setActiveProfileId(profileId: String?) {
    context.soundDataStore.edit { prefs ->
      if (profileId == null) {
        prefs.remove(KEY_ACTIVE_PROFILE_ID)
      } else {
        prefs[KEY_ACTIVE_PROFILE_ID] = profileId
      }
    }
  }

  override suspend fun setActiveTimer(session: TimedModeSession?) {
    context.soundDataStore.edit { prefs ->
      if (session == null) {
        prefs.remove(KEY_ACTIVE_TIMER_JSON)
      } else {
        prefs[KEY_ACTIVE_TIMER_JSON] = json.encodeToString(session)
      }
    }
  }

  override suspend fun upsertCustomProfile(profile: SoundProfile) {
    context.soundDataStore.edit { prefs ->
      val currentList = prefs[KEY_CUSTOM_PROFILES_JSON]?.let { raw ->
        runCatching { json.decodeFromString<List<SoundProfile>>(raw) }.getOrDefault(emptyList())
      } ?: emptyList()
      val updated = currentList.filterNot { it.id == profile.id } + profile
      prefs[KEY_CUSTOM_PROFILES_JSON] = json.encodeToString(updated)

      val deletedIds = prefs[KEY_DELETED_PROFILE_IDS_JSON]?.let { raw ->
        runCatching { json.decodeFromString<Set<String>>(raw) }.getOrDefault(emptySet())
      } ?: emptySet()
      if (profile.id in deletedIds) {
        prefs[KEY_DELETED_PROFILE_IDS_JSON] = json.encodeToString(deletedIds - profile.id)
      }
    }
  }

  override suspend fun deleteCustomProfile(profileId: String) {
    context.soundDataStore.edit { prefs ->
      val currentList = prefs[KEY_CUSTOM_PROFILES_JSON]?.let { raw ->
        runCatching { json.decodeFromString<List<SoundProfile>>(raw) }.getOrDefault(emptyList())
      } ?: emptyList()
      val updated = currentList.filterNot { it.id == profileId }
      prefs[KEY_CUSTOM_PROFILES_JSON] = json.encodeToString(updated)

      val deletedIds = prefs[KEY_DELETED_PROFILE_IDS_JSON]?.let { raw ->
        runCatching { json.decodeFromString<Set<String>>(raw) }.getOrDefault(emptySet())
      } ?: emptySet()
      prefs[KEY_DELETED_PROFILE_IDS_JSON] = json.encodeToString(deletedIds + profileId)
    }
  }

  override suspend fun upsertScheduleRule(rule: ScheduleRule) {
    context.soundDataStore.edit { prefs ->
      val currentList = prefs[KEY_SCHEDULE_RULES_JSON]?.let { raw ->
        runCatching { json.decodeFromString<List<ScheduleRule>>(raw) }
          .getOrDefault(ScheduleRule.defaultSchedules)
      } ?: ScheduleRule.defaultSchedules
      val existingIndex = currentList.indexOfFirst { it.id == rule.id }
      val updated = if (existingIndex >= 0) {
        currentList.toMutableList().apply { set(existingIndex, rule) }
      } else {
        currentList + rule
      }
      prefs[KEY_SCHEDULE_RULES_JSON] = json.encodeToString(updated)
    }
  }

  override suspend fun deleteScheduleRule(ruleId: String) {
    context.soundDataStore.edit { prefs ->
      val currentList = prefs[KEY_SCHEDULE_RULES_JSON]?.let { raw ->
        runCatching { json.decodeFromString<List<ScheduleRule>>(raw) }
          .getOrDefault(ScheduleRule.defaultSchedules)
      } ?: ScheduleRule.defaultSchedules
      val updated = currentList.filterNot { it.id == ruleId }
      prefs[KEY_SCHEDULE_RULES_JSON] = json.encodeToString(updated)
    }
  }

  override suspend fun upsertLocationRule(rule: LocationRule) {
    context.soundDataStore.edit { prefs ->
      val currentList = prefs[KEY_LOCATION_RULES_JSON]?.let { raw ->
        runCatching { json.decodeFromString<List<LocationRule>>(raw) }
          .getOrDefault(LocationRule.defaultLocations)
      } ?: LocationRule.defaultLocations
      val existingIndex = currentList.indexOfFirst { it.id == rule.id }
      val updated = if (existingIndex >= 0) {
        currentList.toMutableList().apply { set(existingIndex, rule) }
      } else {
        currentList + rule
      }
      prefs[KEY_LOCATION_RULES_JSON] = json.encodeToString(updated)
    }
  }

  override suspend fun deleteLocationRule(ruleId: String) {
    context.soundDataStore.edit { prefs ->
      val currentList = prefs[KEY_LOCATION_RULES_JSON]?.let { raw ->
        runCatching { json.decodeFromString<List<LocationRule>>(raw) }
          .getOrDefault(LocationRule.defaultLocations)
      } ?: LocationRule.defaultLocations
      val updated = currentList.filterNot { it.id == ruleId }
      prefs[KEY_LOCATION_RULES_JSON] = json.encodeToString(updated)
    }
  }

  override suspend fun setRestorePolicy(policy: RestorePolicy) {
    context.soundDataStore.edit { prefs ->
      prefs[KEY_RESTORE_POLICY] = policy.name
    }
  }

  override suspend fun setDefaultDayProfileId(profileId: String) {
    context.soundDataStore.edit { prefs ->
      prefs[KEY_DEFAULT_DAY_PROFILE_ID] = profileId
    }
  }

  override suspend fun setSavedSnapshot(snapshot: SavedStateSnapshot?) {
    context.soundDataStore.edit { prefs ->
      if (snapshot == null) {
        prefs.remove(KEY_SAVED_SNAPSHOT_JSON)
      } else {
        prefs[KEY_SAVED_SNAPSHOT_JSON] = json.encodeToString(snapshot)
      }
    }
  }

  override suspend fun setLastAppliedRuleSessionKey(sessionKey: String?) {
    context.soundDataStore.edit { prefs ->
      if (sessionKey == null) {
        prefs.remove(KEY_LAST_APPLIED_RULE_SESSION_KEY)
      } else {
        prefs[KEY_LAST_APPLIED_RULE_SESSION_KEY] = sessionKey
      }
    }
  }

  override suspend fun setOverriddenSessionKey(sessionKey: String?) {
    context.soundDataStore.edit { prefs ->
      if (sessionKey == null) {
        prefs.remove(KEY_OVERRIDDEN_SESSION_KEY)
      } else {
        prefs[KEY_OVERRIDDEN_SESSION_KEY] = sessionKey
      }
    }
  }

  override suspend fun appendTransitionLog(entry: TransitionLogEntry) {
    context.soundDataStore.edit { prefs ->
      val currentList = prefs[KEY_TRANSITION_LOGS_JSON]?.let { raw ->
        runCatching { json.decodeFromString<List<TransitionLogEntry>>(raw) }.getOrDefault(emptyList())
      } ?: emptyList()
      val updated = (listOf(entry) + currentList).take(MAX_LOG_ENTRIES)
      prefs[KEY_TRANSITION_LOGS_JSON] = json.encodeToString(updated)
    }
  }

  override suspend fun clearTransitionLogs() {
    context.soundDataStore.edit { prefs ->
      prefs.remove(KEY_TRANSITION_LOGS_JSON)
    }
  }

  override suspend fun setUseDynamicColor(enabled: Boolean) {
    context.soundDataStore.edit { prefs ->
      prefs[KEY_USE_DYNAMIC_COLOR] = enabled
    }
  }

  override suspend fun setThemeMode(mode: ThemeMode) {
    context.soundDataStore.edit { prefs ->
      prefs[KEY_THEME_MODE] = mode.name
    }
  }

  override suspend fun setLocationDisclosureAccepted(accepted: Boolean) {
    context.soundDataStore.edit { prefs ->
      prefs[KEY_LOCATION_DISCLOSURE_ACCEPTED] = accepted
    }
  }

  override suspend fun setMuteMediaOnSilent(enabled: Boolean) {
    context.soundDataStore.edit { prefs ->
      prefs[KEY_MUTE_MEDIA_ON_SILENT] = enabled
    }
  }

  override suspend fun setPreserveMediaOnBluetooth(enabled: Boolean) {
    context.soundDataStore.edit { prefs ->
      prefs[KEY_PRESERVE_MEDIA_ON_BT] = enabled
    }
  }

  override suspend fun setSmartVolumeTriggerEnabled(enabled: Boolean) {
    context.soundDataStore.edit { prefs ->
      prefs[KEY_SMART_VOLUME_TRIGGER] = enabled
    }
  }

  override suspend fun setPersistentQuickBarEnabled(enabled: Boolean) {
    context.soundDataStore.edit { prefs ->
      prefs[KEY_PERSISTENT_QUICK_BAR] = enabled
    }
  }

  override suspend fun setQuickTileSlot1ProfileId(profileId: String) {
    context.soundDataStore.edit { prefs ->
      prefs[KEY_QUICK_TILE_SLOT1_PROFILE_ID] = profileId
    }
  }

  override suspend fun setQuickTileSlot2ProfileId(profileId: String) {
    context.soundDataStore.edit { prefs ->
      prefs[KEY_QUICK_TILE_SLOT2_PROFILE_ID] = profileId
    }
  }

  override suspend fun setSavedMediaBeforeMute(percent: Int?) {
    context.soundDataStore.edit { prefs ->
      if (percent == null) {
        prefs.remove(KEY_SAVED_MEDIA_BEFORE_MUTE)
      } else {
        prefs[KEY_SAVED_MEDIA_BEFORE_MUTE] = percent
      }
    }
  }

  override suspend fun setAppLanguageCode(languageCode: String) {
    context.soundDataStore.edit { prefs ->
      prefs[KEY_APP_LANGUAGE_CODE] = languageCode
    }
  }

  override suspend fun setAutomationsPaused(paused: Boolean) {
    context.soundDataStore.edit { prefs ->
      prefs[KEY_AUTOMATIONS_PAUSED] = paused
    }
  }

  override suspend fun setHapticFeedbackEnabled(enabled: Boolean) {
    context.soundDataStore.edit { prefs ->
      prefs[KEY_HAPTIC_FEEDBACK_ENABLED] = enabled
    }
  }

  override suspend fun setNotifyOnAutoTransition(enabled: Boolean) {
    context.soundDataStore.edit { prefs ->
      prefs[KEY_NOTIFY_ON_AUTO_TRANSITION] = enabled
    }
  }

  override suspend fun setDefaultTimerMinutes(minutes: Int) {
    context.soundDataStore.edit { prefs ->
      prefs[KEY_DEFAULT_TIMER_MINUTES] = minutes.coerceIn(5, 720)
    }
  }

  override suspend fun setOnboardingCompleted(completed: Boolean) {
    context.soundDataStore.edit { prefs ->
      prefs[KEY_HAS_COMPLETED_ONBOARDING] = completed
    }
  }

  override suspend fun setAccessControlConfig(config: AccessControlConfig) {
    context.soundDataStore.edit { prefs ->
      prefs[KEY_ACCESS_CONTROL_JSON] = json.encodeToString(config)
    }
  }

  override suspend fun setSystemZenRulesEnabled(enabled: Boolean) {
    context.soundDataStore.edit { prefs ->
      prefs[KEY_SYSTEM_ZEN_RULES_ENABLED] = enabled
    }
  }

  override suspend fun resetFactoryDefaults() {
    context.soundDataStore.edit { prefs ->
      prefs.remove(KEY_CUSTOM_PROFILES_JSON)
      prefs.remove(KEY_DELETED_PROFILE_IDS_JSON)
      prefs.remove(KEY_SCHEDULE_RULES_JSON)
      prefs.remove(KEY_LOCATION_RULES_JSON)
      prefs[KEY_RESTORE_POLICY] = RestorePolicy.DEFAULT_PROFILE.name
      prefs[KEY_DEFAULT_DAY_PROFILE_ID] = SoundProfile.DEFAULT_DAY_PROFILE_ID
      prefs.remove(KEY_SAVED_SNAPSHOT_JSON)
      prefs.remove(KEY_LAST_APPLIED_RULE_SESSION_KEY)
      prefs.remove(KEY_OVERRIDDEN_SESSION_KEY)
      prefs[KEY_AUTOMATIONS_PAUSED] = false
      prefs[KEY_MUTE_MEDIA_ON_SILENT] = true
      prefs[KEY_PRESERVE_MEDIA_ON_BT] = true
      prefs[KEY_SMART_VOLUME_TRIGGER] = true
      prefs[KEY_PERSISTENT_QUICK_BAR] = true
      prefs[KEY_HAPTIC_FEEDBACK_ENABLED] = true
      prefs[KEY_NOTIFY_ON_AUTO_TRANSITION] = true
      prefs[KEY_DEFAULT_TIMER_MINUTES] = 30
      prefs[KEY_QUICK_TILE_SLOT1_PROFILE_ID] = "preset_work_high"
      prefs[KEY_QUICK_TILE_SLOT2_PROFILE_ID] = "preset_sleep"
      prefs.remove(KEY_ACCESS_CONTROL_JSON)
      prefs[KEY_SYSTEM_ZEN_RULES_ENABLED] = true
    }
  }

  override suspend fun exportBackupJson(): String {
    val current = prefsFlow.first()
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
      // Omite pinCode no arquivo exportado para evitar exposição de PIN em backups compartilhados
      accessControlConfig = current.accessControlConfig.copy(pinCode = null),
      systemZenRulesEnabled = current.systemZenRulesEnabled,
    )
    return json.encodeToString(payload)
  }

  override suspend fun importBackupJson(rawJson: String) {
    require(rawJson.length <= MAX_BACKUP_JSON_CHARS) { "Backup JSON excede o tamanho máximo permitido." }
    val payload = json.decodeFromString<SoundPrefsBackupPayload>(rawJson.trim())
    val sanitizedLocations = payload.locationRules.take(MAX_IMPORTED_ITEMS).map { rule ->
      rule.copy(
        latitude = if (rule.latitude.isFinite()) rule.latitude.coerceIn(-90.0, 90.0) else 0.0,
        longitude = if (rule.longitude.isFinite()) rule.longitude.coerceIn(-180.0, 180.0) else 0.0,
        radiusMeters = rule.radiusMeters.coerceIn(50, 5000),
      )
    }
    val sanitizedProfiles = payload.customProfiles.take(MAX_IMPORTED_ITEMS)
    val sanitizedSchedules = payload.scheduleRules.take(MAX_IMPORTED_ITEMS)

    context.soundDataStore.edit { prefs ->
      val existingAccess = prefs[KEY_ACCESS_CONTROL_JSON]?.let { raw ->
        runCatching { json.decodeFromString<AccessControlConfig>(raw) }.getOrNull()
      }
      val resolvedAccess = if (!existingAccess?.pinCode.isNullOrBlank() && payload.accessControlConfig.pinCode.isNullOrBlank()) {
        payload.accessControlConfig.copy(pinCode = existingAccess.pinCode)
      } else {
        payload.accessControlConfig
      }

      prefs[KEY_CUSTOM_PROFILES_JSON] = json.encodeToString(sanitizedProfiles)
      prefs[KEY_DELETED_PROFILE_IDS_JSON] = json.encodeToString(payload.deletedProfileIds.take(MAX_IMPORTED_ITEMS).toSet())
      prefs[KEY_SCHEDULE_RULES_JSON] = json.encodeToString(sanitizedSchedules)
      prefs[KEY_LOCATION_RULES_JSON] = json.encodeToString(sanitizedLocations)
      prefs[KEY_RESTORE_POLICY] = payload.restorePolicy.name
      prefs[KEY_DEFAULT_DAY_PROFILE_ID] = payload.defaultDayProfileId
      prefs[KEY_USE_DYNAMIC_COLOR] = payload.useDynamicColor
      prefs[KEY_THEME_MODE] = payload.themeMode.name
      prefs[KEY_APP_LANGUAGE_CODE] = payload.appLanguageCode
      prefs[KEY_AUTOMATIONS_PAUSED] = payload.automationsPaused
      prefs[KEY_HAPTIC_FEEDBACK_ENABLED] = payload.hapticFeedbackEnabled
      prefs[KEY_NOTIFY_ON_AUTO_TRANSITION] = payload.notifyOnAutoTransition
      prefs[KEY_DEFAULT_TIMER_MINUTES] = payload.defaultTimerMinutes.coerceIn(5, 720)
      prefs[KEY_MUTE_MEDIA_ON_SILENT] = payload.muteMediaOnSilent
      prefs[KEY_PRESERVE_MEDIA_ON_BT] = payload.preserveMediaOnBluetooth
      prefs[KEY_SMART_VOLUME_TRIGGER] = payload.smartVolumeTriggerEnabled
      prefs[KEY_PERSISTENT_QUICK_BAR] = payload.persistentQuickBarEnabled
      prefs[KEY_QUICK_TILE_SLOT1_PROFILE_ID] = payload.quickTileSlot1ProfileId
      prefs[KEY_QUICK_TILE_SLOT2_PROFILE_ID] = payload.quickTileSlot2ProfileId
      prefs[KEY_ACCESS_CONTROL_JSON] = json.encodeToString(resolvedAccess)
      prefs[KEY_SYSTEM_ZEN_RULES_ENABLED] = payload.systemZenRulesEnabled
    }
  }

  private companion object {
    const val TAG = "SoundPrefsDataSource"
    const val MAX_LOG_ENTRIES = 30
    const val MAX_IMPORTED_ITEMS = 100
    const val MAX_BACKUP_JSON_CHARS = 512_000
    val KEY_ACTIVE_PROFILE_ID = stringPreferencesKey("active_profile_id")
    val KEY_ACTIVE_TIMER_JSON = stringPreferencesKey("active_timer_json")
    val KEY_CUSTOM_PROFILES_JSON = stringPreferencesKey("custom_profiles_json")
    val KEY_DELETED_PROFILE_IDS_JSON = stringPreferencesKey("deleted_profile_ids_json")
    val KEY_SCHEDULE_RULES_JSON = stringPreferencesKey("schedule_rules_json")
    val KEY_LOCATION_RULES_JSON = stringPreferencesKey("location_rules_json")
    val KEY_RESTORE_POLICY = stringPreferencesKey("restore_policy")
    val KEY_DEFAULT_DAY_PROFILE_ID = stringPreferencesKey("default_day_profile_id")
    val KEY_SAVED_SNAPSHOT_JSON = stringPreferencesKey("saved_snapshot_json")
    val KEY_LAST_APPLIED_RULE_SESSION_KEY = stringPreferencesKey("last_applied_rule_session_key")
    val KEY_OVERRIDDEN_SESSION_KEY = stringPreferencesKey("overridden_session_key")
    val KEY_TRANSITION_LOGS_JSON = stringPreferencesKey("transition_logs_json")
    val KEY_USE_DYNAMIC_COLOR = booleanPreferencesKey("use_dynamic_color")
    val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
    val KEY_LOCATION_DISCLOSURE_ACCEPTED = booleanPreferencesKey("location_disclosure_accepted")
    val KEY_MUTE_MEDIA_ON_SILENT = booleanPreferencesKey("mute_media_on_silent")
    val KEY_PRESERVE_MEDIA_ON_BT = booleanPreferencesKey("preserve_media_on_bt")
    val KEY_SMART_VOLUME_TRIGGER = booleanPreferencesKey("smart_volume_trigger")
    val KEY_PERSISTENT_QUICK_BAR = booleanPreferencesKey("persistent_quick_bar_enabled")
    val KEY_QUICK_TILE_SLOT1_PROFILE_ID = stringPreferencesKey("quick_tile_slot1_profile_id")
    val KEY_QUICK_TILE_SLOT2_PROFILE_ID = stringPreferencesKey("quick_tile_slot2_profile_id")
    val KEY_SAVED_MEDIA_BEFORE_MUTE = intPreferencesKey("saved_media_before_mute")
    val KEY_APP_LANGUAGE_CODE = stringPreferencesKey("app_language_code")
    val KEY_AUTOMATIONS_PAUSED = booleanPreferencesKey("automations_paused")
    val KEY_HAPTIC_FEEDBACK_ENABLED = booleanPreferencesKey("haptic_feedback_enabled")
    val KEY_NOTIFY_ON_AUTO_TRANSITION = booleanPreferencesKey("notify_on_auto_transition")
    val KEY_DEFAULT_TIMER_MINUTES = intPreferencesKey("default_timer_minutes")
    val KEY_HAS_COMPLETED_ONBOARDING = booleanPreferencesKey("has_completed_onboarding")
    val KEY_ACCESS_CONTROL_JSON = stringPreferencesKey("access_control_json")
    val KEY_SYSTEM_ZEN_RULES_ENABLED = booleanPreferencesKey("system_zen_rules_enabled")
  }
}
