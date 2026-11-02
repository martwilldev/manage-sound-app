package com.managesoundmode.app.system

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.managesoundmode.app.domain.engine.RuleEvaluator
import com.managesoundmode.app.domain.model.ScheduleRule
import com.managesoundmode.app.receiver.SystemAutomationReceiver

object ExactRuleAlarmScheduler {

  private const val TAG = "ExactRuleAlarmScheduler"
  private const val ALARM_REQUEST_CODE = 4201

  fun scheduleNextTransition(
    context: Context,
    scheduleRules: List<ScheduleRule>,
  ) {
    val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
    val pendingIntent = createAlarmPendingIntent(context)

    val nextTriggerMillis = RuleEvaluator.computeNextScheduleTransitionMillis(scheduleRules)
    if (nextTriggerMillis == null) {
      alarmManager.cancel(pendingIntent)
      return
    }

    runCatching {
      val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        alarmManager.canScheduleExactAlarms()
      } else {
        true
      }

      if (canExact) {
        alarmManager.setExactAndAllowWhileIdle(
          AlarmManager.RTC_WAKEUP,
          nextTriggerMillis,
          pendingIntent,
        )
      } else {
        alarmManager.setAndAllowWhileIdle(
          AlarmManager.RTC_WAKEUP,
          nextTriggerMillis,
          pendingIntent,
        )
      }
    }.onFailure { error ->
      Log.w(TAG, "Falha ao agendar próximo alarme de regra de som", error)
    }
  }

  private fun createAlarmPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, SystemAutomationReceiver::class.java).apply {
      action = SystemAutomationReceiver.ACTION_EVALUATE_RULES_ALARM
    }
    return PendingIntent.getBroadcast(
      context,
      ALARM_REQUEST_CODE,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }
}
