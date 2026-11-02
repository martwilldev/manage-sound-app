package com.managesoundmode.app.receiver

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.managesoundmode.app.ManageSoundApplication
import com.managesoundmode.app.domain.model.SoundMode
import com.managesoundmode.app.domain.model.TransitionTriggerSource
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
            repo.evaluateAndApplyRules(source = TransitionTriggerSource.BOOT)
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
  }
}
