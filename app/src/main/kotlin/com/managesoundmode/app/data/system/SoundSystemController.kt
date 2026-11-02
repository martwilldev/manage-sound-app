package com.managesoundmode.app.data.system

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.managesoundmode.app.domain.model.SoundMode
import com.managesoundmode.app.domain.model.VolumeStreamType
import com.managesoundmode.app.domain.model.VolumeStreams
import kotlin.math.roundToInt
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class DndPermissionRequiredException :
  IllegalStateException("Permissão 'Não Perturbe' (DND) necessária para alterar para o modo Silencioso.")

interface SoundSystemController {
  val systemChanges: Flow<Unit>
  fun readCurrentMode(): SoundMode
  fun readVolumeStreams(): VolumeStreams
  fun hasDndPermission(): Boolean
  fun applySoundMode(mode: SoundMode): Result<SoundMode>
  fun applyStreamVolume(type: VolumeStreamType, percent: Int): Result<VolumeStreams>
  fun applyAllStreams(streams: VolumeStreams): Result<VolumeStreams>
}

class AndroidSoundSystemController(
  private val appContext: Context,
) : SoundSystemController {

  private val audioManager: AudioManager? =
    appContext.getSystemService(AudioManager::class.java)

  private val notificationManager: NotificationManager? =
    appContext.getSystemService(NotificationManager::class.java)

  override val systemChanges: Flow<Unit> = callbackFlow {
    trySend(Unit)
    val receiver = object : BroadcastReceiver() {
      override fun onReceive(context: Context?, intent: Intent?) {
        trySend(Unit)
      }
    }
    val filter = IntentFilter().apply {
      addAction(AudioManager.RINGER_MODE_CHANGED_ACTION)
      addAction(NotificationManager.ACTION_INTERRUPTION_FILTER_CHANGED)
    }
    ContextCompat.registerReceiver(
      appContext,
      receiver,
      filter,
      ContextCompat.RECEIVER_NOT_EXPORTED,
    )
    awaitClose {
      runCatching { appContext.unregisterReceiver(receiver) }
        .onFailure { Log.w(TAG, "Falha ao desregistrar receiver de áudio", it) }
    }
  }

  override fun readCurrentMode(): SoundMode {
    val ringerMode = audioManager?.ringerMode ?: AudioManager.RINGER_MODE_NORMAL
    return when (ringerMode) {
      AudioManager.RINGER_MODE_SILENT -> SoundMode.SILENT
      AudioManager.RINGER_MODE_VIBRATE -> SoundMode.VIBRATE
      else -> SoundMode.NORMAL
    }
  }

  override fun readVolumeStreams(): VolumeStreams {
    val manager = audioManager ?: return VolumeStreams()
    return VolumeStreams(
      ringPercent = readStreamPercent(manager, AudioManager.STREAM_RING),
      notificationPercent = readStreamPercent(manager, AudioManager.STREAM_NOTIFICATION),
      mediaPercent = readStreamPercent(manager, AudioManager.STREAM_MUSIC),
      alarmPercent = readStreamPercent(manager, AudioManager.STREAM_ALARM),
    )
  }

  override fun hasDndPermission(): Boolean {
    return notificationManager?.isNotificationPolicyAccessGranted ?: false
  }

  override fun applySoundMode(mode: SoundMode): Result<SoundMode> = runCatching {
    val manager = requireNotNull(audioManager) { "AudioManager indisponível no dispositivo" }
    if ((mode == SoundMode.SILENT || readCurrentMode() == SoundMode.SILENT) && !hasDndPermission()) {
      throw DndPermissionRequiredException()
    }
    val targetRinger = when (mode) {
      SoundMode.NORMAL -> AudioManager.RINGER_MODE_NORMAL
      SoundMode.VIBRATE -> AudioManager.RINGER_MODE_VIBRATE
      SoundMode.SILENT -> AudioManager.RINGER_MODE_SILENT
    }
    manager.ringerMode = targetRinger
    readCurrentMode()
  }.onFailure { error ->
    Log.e(TAG, "Erro ao aplicar modo de som: $mode", error)
  }

  override fun applyStreamVolume(type: VolumeStreamType, percent: Int): Result<VolumeStreams> = runCatching {
    val manager = requireNotNull(audioManager) { "AudioManager indisponível no dispositivo" }
    val streamConstant = type.toAndroidStream()
    setStreamPercent(manager, streamConstant, percent)
    readVolumeStreams()
  }.onFailure { error ->
    Log.e(TAG, "Erro ao ajustar canal $type para $percent%", error)
  }

  override fun applyAllStreams(streams: VolumeStreams): Result<VolumeStreams> = runCatching {
    val manager = requireNotNull(audioManager) { "AudioManager indisponível no dispositivo" }
    setStreamPercent(manager, AudioManager.STREAM_RING, streams.ringPercent)
    setStreamPercent(manager, AudioManager.STREAM_NOTIFICATION, streams.notificationPercent)
    setStreamPercent(manager, AudioManager.STREAM_MUSIC, streams.mediaPercent)
    setStreamPercent(manager, AudioManager.STREAM_ALARM, streams.alarmPercent)
    readVolumeStreams()
  }.onFailure { error ->
    Log.e(TAG, "Erro ao aplicar múltiplos canais de volume", error)
  }

  private fun readStreamPercent(manager: AudioManager, streamType: Int): Int {
    val max = manager.getStreamMaxVolume(streamType).coerceAtLeast(1)
    val current = manager.getStreamVolume(streamType).coerceIn(0, max)
    return ((current.toFloat() / max.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
  }

  private fun setStreamPercent(manager: AudioManager, streamType: Int, percent: Int) {
    val clampedPercent = percent.coerceIn(0, 100)
    val max = manager.getStreamMaxVolume(streamType).coerceAtLeast(1)
    val targetIndex = ((clampedPercent / 100f) * max).roundToInt().coerceIn(0, max)
    manager.setStreamVolume(streamType, targetIndex, 0)
  }

  private fun VolumeStreamType.toAndroidStream(): Int = when (this) {
    VolumeStreamType.RING -> AudioManager.STREAM_RING
    VolumeStreamType.NOTIFICATION -> AudioManager.STREAM_NOTIFICATION
    VolumeStreamType.MEDIA -> AudioManager.STREAM_MUSIC
    VolumeStreamType.ALARM -> AudioManager.STREAM_ALARM
  }

  private companion object {
    const val TAG = "AndroidSoundController"
  }
}
