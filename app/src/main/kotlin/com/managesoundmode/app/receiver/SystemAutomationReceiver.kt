package com.managesoundmode.app.receiver

import android.app.AlarmManager
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import com.managesoundmode.app.ManageSoundApplication
import com.managesoundmode.app.domain.model.SoundMode
import com.managesoundmode.app.domain.model.TransitionTriggerSource
import com.managesoundmode.app.system.LocationProximityHelper
import com.managesoundmode.app.system.SystemZenRuleManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SystemAutomationReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context?, intent: Intent?) {
    val app = context?.applicationContext as? ManageSoundApplication ?: return
    val action = intent?.action ?: return

    val pendingResult = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      try {
        val repo = app.container.soundModeRepository
        when (action) {
          ACTION_EVALUATE_RULES_ALARM -> {
            repo.evaluateAndApplyRules(source = TransitionTriggerSource.SCHEDULE)
          }

          Intent.ACTION_BOOT_COMPLETED,
          Intent.ACTION_MY_PACKAGE_REPLACED,
          Intent.ACTION_TIME_CHANGED,
          Intent.ACTION_TIMEZONE_CHANGED,
          AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
          -> {
            repo.syncBackgroundGeofencesNow()
            repo.syncSystemZenRulesNow()
            repo.evaluateAndApplyRules(source = TransitionTriggerSource.BOOT)
          }

          NotificationManager.ACTION_AUTOMATIC_ZEN_RULE_STATUS_CHANGED -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
              val ruleId = intent.getStringExtra(NotificationManager.EXTRA_AUTOMATIC_ZEN_RULE_ID)
              val status = intent.getIntExtra(
                NotificationManager.EXTRA_AUTOMATIC_ZEN_RULE_STATUS,
                ZEN_RULE_STATUS_UNKNOWN,
              )
              if (!ruleId.isNullOrBlank()) {
                val profileId = SystemZenRuleManager.resolveProfileIdForSystemRuleId(app, ruleId)
                if (profileId != null) {
                  when (status) {
                    ZEN_RULE_STATUS_ENABLED,
                    ZEN_RULE_STATUS_ACTIVATED,
                    -> {
                      val target = repo.profilesStream.first().firstOrNull { it.id == profileId }
                      if (target != null) {
                        repo.activateProfile(target, source = TransitionTriggerSource.SCHEDULE)
                      }
                    }
                    ZEN_RULE_STATUS_DEACTIVATED,
                    ZEN_RULE_STATUS_DISABLED,
                    -> {
                      repo.evaluateAndApplyRules(source = TransitionTriggerSource.SCHEDULE)
                    }
                  }
                }
              }
            }
          }

          ACTION_GEOFENCE_TRANSITION,
          ACTION_PASSIVE_LOCATION_UPDATE,
          ACTION_GEOFENCE_DWELL_CHECK,
          -> {
            val intentLocation: Location? =
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(LocationManager.KEY_LOCATION_CHANGED, Location::class.java)
              } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(LocationManager.KEY_LOCATION_CHANGED)
              }
            val coords = if (intentLocation != null) {
              intentLocation.latitude to intentLocation.longitude
            } else {
              LocationProximityHelper.fetchCurrentDeviceLocation(app)?.let {
                it.latitude to it.longitude
              }
            }
            val wifiSsid = LocationProximityHelper.readConnectedWifiSsid(app)
            repo.processBackgroundGeofenceSignal(
              latitude = coords?.first,
              longitude = coords?.second,
              connectedWifiSsid = wifiSsid,
            )
          }

          ACTION_NOTIFICATION_START_TIMER -> {
            val minutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 60).coerceAtLeast(1)
            val currentMode = app.container.soundSystemController.readCurrentMode()
            val targetMode = if (currentMode == SoundMode.NORMAL) SoundMode.VIBRATE else currentMode
            repo.startTimedSession(targetMode = targetMode, durationMinutes = minutes)
          }

          ACTION_NOTIFICATION_EXTEND_TIMER -> {
            val addMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 30).coerceAtLeast(1)
            repo.extendTimedSession(additionalMinutes = addMinutes)
          }

          ACTION_NOTIFICATION_RESTORE_NOW -> {
            repo.cancelOrRestoreTimedSession()
          }

          ACTION_NOTIFICATION_REAPPLY_RULE -> {
            repo.reapplyActiveRule()
          }

          ACTION_WIDGET_SET_MODE -> {
            val modeRaw = intent.getStringExtra(EXTRA_SOUND_MODE) ?: return@launch
            val targetMode = runCatching { SoundMode.valueOf(modeRaw) }.getOrNull() ?: return@launch
            repo.setSoundMode(targetMode, source = TransitionTriggerSource.WIDGET)
          }

          ACTION_WIDGET_CYCLE_MODE -> {
            repo.cycleSoundModeFromQuickTile()
          }

          ACTION_ACTIVATE_PROFILE -> {
            val profileId = intent.getStringExtra(EXTRA_PROFILE_ID) ?: return@launch
            val profiles = repo.profilesStream.first()
            val target = profiles.firstOrNull { it.id == profileId } ?: return@launch
            repo.activateProfile(target, source = TransitionTriggerSource.WIDGET)
          }
        }
      } catch (e: Exception) {
        Log.e(TAG, "Erro ao processar evento de automação ($action)", e)
      } finally {
        pendingResult.finish()
      }
    }
  }

  companion object {
    private const val TAG = "SystemAutoReceiver"
    const val ACTION_EVALUATE_RULES_ALARM =
      "com.managesoundmode.app.action.EVALUATE_RULES_ALARM"
    const val ACTION_GEOFENCE_TRANSITION =
      "com.managesoundmode.app.action.GEOFENCE_TRANSITION"
    const val ACTION_PASSIVE_LOCATION_UPDATE =
      "com.managesoundmode.app.action.PASSIVE_LOCATION_UPDATE"
    const val ACTION_GEOFENCE_DWELL_CHECK =
      "com.managesoundmode.app.action.GEOFENCE_DWELL_CHECK"
    const val ACTION_NOTIFICATION_START_TIMER =
      "com.managesoundmode.app.action.NOTIFICATION_START_TIMER"
    const val ACTION_NOTIFICATION_EXTEND_TIMER =
      "com.managesoundmode.app.action.NOTIFICATION_EXTEND_TIMER"
    const val ACTION_NOTIFICATION_RESTORE_NOW =
      "com.managesoundmode.app.action.NOTIFICATION_RESTORE_NOW"
    const val ACTION_NOTIFICATION_REAPPLY_RULE =
      "com.managesoundmode.app.action.NOTIFICATION_REAPPLY_RULE"
    const val ACTION_WIDGET_SET_MODE =
      "com.managesoundmode.app.action.WIDGET_SET_MODE"
    const val ACTION_WIDGET_CYCLE_MODE =
      "com.managesoundmode.app.action.WIDGET_CYCLE_MODE"
    const val ACTION_ACTIVATE_PROFILE =
      "com.managesoundmode.app.action.ACTIVATE_PROFILE"

    const val EXTRA_DURATION_MINUTES = "extra_duration_minutes"
    const val EXTRA_SOUND_MODE = "extra_sound_mode"
    const val EXTRA_PROFILE_ID = "extra_profile_id"
    const val EXTRA_LOCATION_RULE_ID = "extra_location_rule_id"

    private const val ZEN_RULE_STATUS_UNKNOWN = 0
    private const val ZEN_RULE_STATUS_ENABLED = 1
    private const val ZEN_RULE_STATUS_DISABLED = 2
    private const val ZEN_RULE_STATUS_ACTIVATED = 4
    private const val ZEN_RULE_STATUS_DEACTIVATED = 5
  }
}
