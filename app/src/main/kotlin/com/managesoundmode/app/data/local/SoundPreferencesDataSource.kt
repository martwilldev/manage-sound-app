package com.managesoundmode.app.data.local

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.domain.model.TimedModeSession
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
)

interface SoundPreferencesDataSource {
  val prefsFlow: Flow<PersistedSoundPrefs>
  suspend fun setActiveProfileId(profileId: String?)
  suspend fun setActiveTimer(session: TimedModeSession?)
  suspend fun upsertCustomProfile(profile: SoundProfile)
  suspend fun deleteCustomProfile(profileId: String)
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

        PersistedSoundPrefs(
          activeProfileId = activeProfileId,
          activeTimer = activeTimer,
          customProfiles = customProfiles,
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

  private companion object {
    const val TAG = "SoundPrefsDataSource"
    val KEY_ACTIVE_PROFILE_ID = stringPreferencesKey("active_profile_id")
    val KEY_ACTIVE_TIMER_JSON = stringPreferencesKey("active_timer_json")
    val KEY_CUSTOM_PROFILES_JSON = stringPreferencesKey("custom_profiles_json")
  }
}
