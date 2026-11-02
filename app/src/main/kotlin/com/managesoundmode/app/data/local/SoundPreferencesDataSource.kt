package com.managesoundmode.app.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val Context.soundDataStore: DataStore<Preferences> by preferencesDataStore(
  name = "manage_sound_mode_prefs",
)

data class PersistedSoundPrefs(
  val activeProfileId: String? = null,
  val activeTimer: TimedModeSession? = null,
  val customProfiles: List<SoundProfile> = emptyList(),
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

        PersistedSoundPrefs(
          activeProfileId = activeProfileId,
          activeTimer = activeTimer,
          customProfiles = customProfiles,
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
    }
  }

  override suspend fun deleteCustomProfile(profileId: String) {
    context.soundDataStore.edit { prefs ->
      val currentList = prefs[KEY_CUSTOM_PROFILES_JSON]?.let { raw ->
        runCatching { json.decodeFromString<List<SoundProfile>>(raw) }.getOrDefault(emptyList())
      } ?: emptyList()
      val updated = currentList.filterNot { it.id == profileId }
      prefs[KEY_CUSTOM_PROFILES_JSON] = json.encodeToString(updated)
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

  private companion object {
    const val TAG = "SoundPrefsDataSource"
    const val MAX_LOG_ENTRIES = 30
    val KEY_ACTIVE_PROFILE_ID = stringPreferencesKey("active_profile_id")
    val KEY_ACTIVE_TIMER_JSON = stringPreferencesKey("active_timer_json")
    val KEY_CUSTOM_PROFILES_JSON = stringPreferencesKey("custom_profiles_json")
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
  }
}
