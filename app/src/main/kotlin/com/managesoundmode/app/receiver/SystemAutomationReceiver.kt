package com.managesoundmode.app.receiver

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.managesoundmode.app.ManageSoundApplication
import com.managesoundmode.app.domain.model.TransitionTriggerSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SystemAutomationReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context?, intent: Intent?) {
    val app = context?.applicationContext as? ManageSoundApplication ?: return
    val action = intent?.action ?: return

    val triggerSource = when (action) {
      ACTION_EVALUATE_RULES_ALARM -> TransitionTriggerSource.SCHEDULE
      Intent.ACTION_BOOT_COMPLETED,
      Intent.ACTION_MY_PACKAGE_REPLACED,
      Intent.ACTION_TIME_CHANGED,
      Intent.ACTION_TIMEZONE_CHANGED,
      AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED,
      -> TransitionTriggerSource.BOOT
      else -> return
    }

    val pendingResult = goAsync()
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
      try {
        app.container.soundModeRepository.evaluateAndApplyRules(source = triggerSource)
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
  }
}
