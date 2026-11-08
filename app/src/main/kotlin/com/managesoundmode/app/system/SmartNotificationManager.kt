package com.managesoundmode.app.system

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.managesoundmode.app.MainActivity
import com.managesoundmode.app.R
import com.managesoundmode.app.domain.model.SoundMode
import com.managesoundmode.app.domain.model.TimedModeSession
import com.managesoundmode.app.domain.model.VolumeStreams
import com.managesoundmode.app.receiver.SystemAutomationReceiver

object SmartNotificationManager {

  private const val CHANNEL_SMART_PROMPT = "smart_volume_prompt"
  private const val CHANNEL_ACTIVE_TIMER = "active_timer_status"
  private const val CHANNEL_RULE_OVERRIDE = "rule_override_alert"
  private const val CHANNEL_PERSISTENT_QUICK_BAR = "persistent_quick_bar"

  private const val NOTIFICATION_ID_SMART_PROMPT = 3101
  private const val NOTIFICATION_ID_ACTIVE_TIMER = 3102
  private const val NOTIFICATION_ID_RULE_OVERRIDE = 3103
  private const val NOTIFICATION_ID_PERSISTENT_BAR = 3104

  fun ensureChannels(context: Context) {
    val manager = context.getSystemService(NotificationManager::class.java) ?: return

    val promptChannel = NotificationChannel(
      CHANNEL_SMART_PROMPT,
      "Sugestão de Timer ao Silenciar",
      NotificationManager.IMPORTANCE_HIGH,
    ).apply {
      description = "Pergunta se você deseja restaurar o som automaticamente ao silenciar pelo botão de volume"
      setSound(null, null)
      enableVibration(false)
    }

    val timerChannel = NotificationChannel(
      CHANNEL_ACTIVE_TIMER,
      "Timer de Restauração Ativo",
      NotificationManager.IMPORTANCE_LOW,
    ).apply {
      description = "Mostra a contagem regressiva nativa enquanto um modo temporizado está ativo"
      setSound(null, null)
      enableVibration(false)
    }

    val overrideChannel = NotificationChannel(
      CHANNEL_RULE_OVERRIDE,
      "Aviso de Pausa em Rotinas",
      NotificationManager.IMPORTANCE_LOW,
    ).apply {
      description = "Permite reativar em 1 toque uma rotina alterada pelos botões físicos"
      setSound(null, null)
      enableVibration(false)
    }

    val quickBarChannel = NotificationChannel(
      CHANNEL_PERSISTENT_QUICK_BAR,
      "Gadget de Controle Rápido no Header",
      NotificationManager.IMPORTANCE_LOW,
    ).apply {
      description = "Barra rápida persistente na gaveta de notificações para alternar Som, Vibrar e Silêncio em 1 toque"
      setSound(null, null)
      enableVibration(false)
      setShowBadge(false)
    }

    manager.createNotificationChannels(
      listOf(promptChannel, timerChannel, overrideChannel, quickBarChannel),
    )
  }

  fun syncPersistentQuickBarNotification(
    context: Context,
    enabled: Boolean,
    currentMode: SoundMode,
    streams: VolumeStreams,
    activeProfileName: String?,
    activeTimer: TimedModeSession?,
  ) {
    val manager = NotificationManagerCompat.from(context)
    if (!enabled || !canPostNotifications(context)) {
      manager.cancel(NOTIFICATION_ID_PERSISTENT_BAR)
      return
    }

    ensureChannels(context)
    val now = System.currentTimeMillis()
    val remainingMin = activeTimer?.remainingMinutes(now)?.takeIf { it > 0 }

    val title = when {
      remainingMin != null -> "${currentMode.title} • Timer (${remainingMin}m restantes)"
      !activeProfileName.isNullOrBlank() -> "${currentMode.title} • Perfil $activeProfileName"
      else -> "Modo Atual: ${currentMode.title}"
    }

    val subtitle = when (currentMode) {
      SoundMode.NORMAL ->
        "Toque ${streams.ringPercent}% • Notif ${streams.notificationPercent}% • Mídia ${streams.mediaPercent}%"
      SoundMode.VIBRATE ->
        "Vibração ativa • Mídia ${streams.mediaPercent}% • Alarme ${streams.alarmPercent}%"
      SoundMode.SILENT ->
        "Silêncio total • Mídia ${streams.mediaPercent}% • Alarme ${streams.alarmPercent}%"
    }

    val smallIconRes = when (currentMode) {
      SoundMode.NORMAL -> R.drawable.ic_widget_volume_up
      SoundMode.VIBRATE -> R.drawable.ic_widget_vibration
      SoundMode.SILENT -> R.drawable.ic_widget_silent
    }

    val notification = NotificationCompat.Builder(context, CHANNEL_PERSISTENT_QUICK_BAR)
      .setSmallIcon(smallIconRes)
      .setContentTitle(title)
      .setContentText(subtitle)
      .setContentIntent(openAppPendingIntent(context))
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setSilent(true)
      .setShowWhen(false)
      .setPriority(NotificationCompat.PRIORITY_LOW)
      .setCategory(NotificationCompat.CATEGORY_SERVICE)
      .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
      .addAction(
        R.drawable.ic_widget_volume_up,
        if (currentMode == SoundMode.NORMAL) "● Som" else "Som",
        setModePendingIntent(context, requestCode = 420, mode = SoundMode.NORMAL),
      )
      .addAction(
        R.drawable.ic_widget_vibration,
        if (currentMode == SoundMode.VIBRATE) "● Vibrar" else "Vibrar",
        setModePendingIntent(context, requestCode = 421, mode = SoundMode.VIBRATE),
      )
      .addAction(
        R.drawable.ic_widget_silent,
        if (currentMode == SoundMode.SILENT) "● Silêncio" else "Silêncio",
        setModePendingIntent(context, requestCode = 422, mode = SoundMode.SILENT),
      )
      .build()

    postNotificationSafely(context, NOTIFICATION_ID_PERSISTENT_BAR, notification)
  }

  fun showSmartRestorePrompt(
    context: Context,
    mode: SoundMode,
    mediaAutoMuted: Boolean,
  ) {
    if (!canPostNotifications(context)) return
    ensureChannels(context)

    val subtitle = if (mediaAutoMuted) {
      "Mídia zerada automaticamente • Restaurar som depois?"
    } else {
      "Por quanto tempo deseja manter no modo ${mode.title}?"
    }

    val notification = NotificationCompat.Builder(context, CHANNEL_SMART_PROMPT)
      .setSmallIcon(R.drawable.ic_qs_sound_mode)
      .setContentTitle("Modo ${mode.title} ativado")
      .setContentText(subtitle)
      .setContentIntent(openAppPendingIntent(context))
      .setAutoCancel(true)
      .setSilent(true)
      .setTimeoutAfter(120_000L)
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setCategory(NotificationCompat.CATEGORY_STATUS)
      .addAction(
        0,
        "30 min",
        timerActionPendingIntent(context, requestCode = 401, durationMinutes = 30),
      )
      .addAction(
        0,
        "1 hora",
        timerActionPendingIntent(context, requestCode = 402, durationMinutes = 60),
      )
      .addAction(
        0,
        "2 horas",
        timerActionPendingIntent(context, requestCode = 403, durationMinutes = 120),
      )
      .build()

    postNotificationSafely(context, NOTIFICATION_ID_SMART_PROMPT, notification)
  }

  fun syncActiveTimerNotification(
    context: Context,
    session: TimedModeSession?,
  ) {
    val manager = NotificationManagerCompat.from(context)
    val now = System.currentTimeMillis()
    if (session == null || session.endsAtEpochMillis <= now) {
      manager.cancel(NOTIFICATION_ID_ACTIVE_TIMER)
      return
    }

    // Se um timer começou, remove o prompt de pergunta inicial
    manager.cancel(NOTIFICATION_ID_SMART_PROMPT)

    if (!canPostNotifications(context)) return
    ensureChannels(context)

    val notification = NotificationCompat.Builder(context, CHANNEL_ACTIVE_TIMER)
      .setSmallIcon(R.drawable.ic_qs_sound_mode)
      .setContentTitle("${session.targetMode.title} temporizado ativo")
      .setContentText("Voltará para ${session.previousMode.title} automaticamente")
      .setContentIntent(openAppPendingIntent(context))
      .setOngoing(true)
      .setOnlyAlertOnce(true)
      .setSilent(true)
      .setUsesChronometer(true)
      .setChronometerCountDown(true)
      .setWhen(session.endsAtEpochMillis)
      .setShowWhen(true)
      .setCategory(NotificationCompat.CATEGORY_PROGRESS)
      .addAction(
        0,
        "+30 min",
        extendTimerPendingIntent(context, addMinutes = 30),
      )
      .addAction(
        0,
        "Restaurar agora",
        restoreNowPendingIntent(context),
      )
      .build()

    postNotificationSafely(context, NOTIFICATION_ID_ACTIVE_TIMER, notification)
  }

  fun showRuleOverrideAlert(
    context: Context,
    ruleName: String,
  ) {
    if (!canPostNotifications(context)) return
    ensureChannels(context)

    val notification = NotificationCompat.Builder(context, CHANNEL_RULE_OVERRIDE)
      .setSmallIcon(R.drawable.ic_qs_sound_mode)
      .setContentTitle("Rotina '$ruleName' pausada manualmente")
      .setContentText("Você alterou o som pelos botões físicos.")
      .setContentIntent(openAppPendingIntent(context))
      .setAutoCancel(true)
      .setSilent(true)
      .setTimeoutAfter(90_000L)
      .addAction(
        0,
        "Reativar rotina",
        reapplyRulePendingIntent(context),
      )
      .build()

    postNotificationSafely(context, NOTIFICATION_ID_RULE_OVERRIDE, notification)
  }

  @android.annotation.SuppressLint("MissingPermission")
  private fun postNotificationSafely(
    context: Context,
    notificationId: Int,
    notification: android.app.Notification,
  ) {
    if (!canPostNotifications(context)) return
    runCatching {
      NotificationManagerCompat.from(context).notify(notificationId, notification)
    }
  }

  fun dismissSmartPrompt(context: Context) {
    NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_SMART_PROMPT)
  }

  fun dismissRuleOverrideAlert(context: Context) {
    NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_RULE_OVERRIDE)
  }

  private fun canPostNotifications(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS,
      ) == PackageManager.PERMISSION_GRANTED
    } else {
      true
    }
  }

  private fun openAppPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    return PendingIntent.getActivity(
      context,
      400,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  private fun setModePendingIntent(
    context: Context,
    requestCode: Int,
    mode: SoundMode,
  ): PendingIntent {
    val intent = Intent(context, SystemAutomationReceiver::class.java).apply {
      action = SystemAutomationReceiver.ACTION_WIDGET_SET_MODE
      putExtra(SystemAutomationReceiver.EXTRA_SOUND_MODE, mode.name)
    }
    return PendingIntent.getBroadcast(
      context,
      requestCode,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  private fun timerActionPendingIntent(
    context: Context,
    requestCode: Int,
    durationMinutes: Int,
  ): PendingIntent {
    val intent = Intent(context, SystemAutomationReceiver::class.java).apply {
      action = SystemAutomationReceiver.ACTION_NOTIFICATION_START_TIMER
      putExtra(SystemAutomationReceiver.EXTRA_DURATION_MINUTES, durationMinutes)
    }
    return PendingIntent.getBroadcast(
      context,
      requestCode,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  private fun extendTimerPendingIntent(
    context: Context,
    addMinutes: Int,
  ): PendingIntent {
    val intent = Intent(context, SystemAutomationReceiver::class.java).apply {
      action = SystemAutomationReceiver.ACTION_NOTIFICATION_EXTEND_TIMER
      putExtra(SystemAutomationReceiver.EXTRA_DURATION_MINUTES, addMinutes)
    }
    return PendingIntent.getBroadcast(
      context,
      410,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  private fun restoreNowPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, SystemAutomationReceiver::class.java).apply {
      action = SystemAutomationReceiver.ACTION_NOTIFICATION_RESTORE_NOW
    }
    return PendingIntent.getBroadcast(
      context,
      411,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }

  private fun reapplyRulePendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, SystemAutomationReceiver::class.java).apply {
      action = SystemAutomationReceiver.ACTION_NOTIFICATION_REAPPLY_RULE
    }
    return PendingIntent.getBroadcast(
      context,
      412,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
  }
}
