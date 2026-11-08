package com.managesoundmode.app.system

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Build
import android.util.Log
import com.managesoundmode.app.domain.model.LocationRule
import com.managesoundmode.app.receiver.SystemAutomationReceiver
import java.util.concurrent.ConcurrentHashMap

/**
 * Resultado da avaliação de presença com filtro de permanência (*Dwell* de 2 minutos) + Wi-Fi SSID.
 */
sealed interface GeofencePresenceDecision {
  data class ConfirmedInside(val viaWifi: Boolean) : GeofencePresenceDecision
  data class PendingDwell(val remainingMillis: Long) : GeofencePresenceDecision
  data object ConfirmedOutside : GeofencePresenceDecision
}

/**
 * [GEO-01] Motor de Geofencing Passivo em Background com transição *Dwell* (2 minutos) e
 * confirmação híbrida por Wi-Fi SSID.
 *
 * Combina `LocationManager.addProximityAlert`, `LocationManager.PASSIVE_PROVIDER` e alarmes
 * de verificação de permanência (*Dwell*) para evitar falsos positivos ao passar rapidamente
 * por um local de carro ou transporte público.
 */
object BackgroundGeofenceManager {
  private const val TAG = "BackgroundGeofenceMgr"
  const val DEFAULT_DWELL_DURATION_MILLIS: Long = 2 * 60 * 1000L // 2 minutos
  private const val PASSIVE_UPDATE_INTERVAL_MILLIS: Long = 120_000L
  private const val PASSIVE_UPDATE_MIN_DISTANCE_METERS: Float = 35f
  private const val DWELL_ALARM_REQUEST_CODE: Int = 8401
  private const val PASSIVE_LOCATION_REQUEST_CODE: Int = 8402

  /**
   * Armazena em memória o início da permanência candidata (`ruleId -> firstEnteredEpochMillis`)
   * para confirmar a transição após [DEFAULT_DWELL_DURATION_MILLIS].
   */
  private val dwellCandidates = ConcurrentHashMap<String, Long>()
  private val registeredRuleIds = ConcurrentHashMap.newKeySet<String>()

  fun clearDwellCandidates() {
    dwellCandidates.clear()
  }

  fun getDwellCandidateStartMillis(ruleId: String): Long? = dwellCandidates[ruleId]

  /**
   * Avalia deterministicamente se o usuário entrou, está aguardando o tempo de *Dwell* (2 min)
   * ou saiu da cerca geográfica [rule].
   */
  fun evaluatePresenceWithDwell(
    rule: LocationRule,
    latitude: Double?,
    longitude: Double?,
    connectedWifiSsid: String?,
    nowEpochMillis: Long = System.currentTimeMillis(),
    dwellDurationMillis: Long = DEFAULT_DWELL_DURATION_MILLIS,
  ): GeofencePresenceDecision {
    if (!rule.enabled) {
      dwellCandidates.remove(rule.id)
      return GeofencePresenceDecision.ConfirmedOutside
    }

    // 1. Confirmação instantânea via Wi-Fi SSID (quando configurado e conectado)
    if (LocationProximityHelper.matchesWifiSsid(rule.wifiSsid, connectedWifiSsid)) {
      dwellCandidates[rule.id] = nowEpochMillis - dwellDurationMillis
      return GeofencePresenceDecision.ConfirmedInside(viaWifi = true)
    }

    // 2. Avaliação geodésica por coordenadas GPS/Rede
    if (latitude != null && longitude != null && rule.hasCoordinates) {
      val distance = LocationProximityHelper.distanceMeters(
        lat1 = latitude,
        lon1 = longitude,
        lat2 = rule.latitude,
        lon2 = rule.longitude,
      )
      val insideRadius = distance <= rule.radiusMeters.toFloat()
      if (insideRadius) {
        if (rule.isCurrentlyInside) {
          return GeofencePresenceDecision.ConfirmedInside(viaWifi = false)
        }
        val candidateStart = dwellCandidates.getOrPut(rule.id) { nowEpochMillis }
        val elapsed = (nowEpochMillis - candidateStart).coerceAtLeast(0L)
        return if (elapsed >= dwellDurationMillis) {
          GeofencePresenceDecision.ConfirmedInside(viaWifi = false)
        } else {
          GeofencePresenceDecision.PendingDwell(
            remainingMillis = (dwellDurationMillis - elapsed).coerceAtLeast(1_000L),
          )
        }
      }
    }

    // 3. Fora do raio e sem Wi-Fi correspondente
    dwellCandidates.remove(rule.id)
    return GeofencePresenceDecision.ConfirmedOutside
  }

  /**
   * Registra/atualiza os alertas de proximidade (`addProximityAlert`) e o listener passivo
   * (`PASSIVE_PROVIDER`) no `LocationManager` para todas as [locationRules] ativas.
   *
   * @return Quantidade de cercas registradas no sistema.
   */
  @SuppressLint("MissingPermission")
  fun syncGeofenceRegistrations(
    context: Context,
    locationRules: List<LocationRule>,
    automationsPaused: Boolean = false,
  ): Int {
    val locationManager =
      context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return 0

    // Limpa registros anteriores
    registeredRuleIds.toList().forEach { oldId ->
      runCatching {
        locationManager.removeProximityAlert(buildProximityPendingIntent(context, oldId))
      }
    }
    registeredRuleIds.clear()
    runCatching {
      locationManager.removeUpdates(buildPassiveLocationPendingIntent(context))
    }

    if (automationsPaused || !LocationProximityHelper.hasLocationPermission(context)) {
      return 0
    }

    val activeRules = locationRules.filter { it.enabled && it.hasCoordinates }
    if (activeRules.isEmpty()) return 0

    var registeredCount = 0
    for (rule in activeRules) {
      val radius = rule.radiusMeters.coerceIn(50, 2000).toFloat()
      val pendingIntent = buildProximityPendingIntent(context, rule.id)
      runCatching {
        locationManager.addProximityAlert(
          rule.latitude,
          rule.longitude,
          radius,
          -1L,
          pendingIntent,
        )
        registeredRuleIds.add(rule.id)
        registeredCount++
      }.onFailure { error ->
        Log.w(TAG, "Não foi possível registrar proximityAlert para ${rule.name}", error)
      }
    }

    // Registra receptor passivo para atualizações de localização em background (zero bateria extra)
    runCatching {
      if (locationManager.allProviders.contains(LocationManager.PASSIVE_PROVIDER)) {
        locationManager.requestLocationUpdates(
          LocationManager.PASSIVE_PROVIDER,
          PASSIVE_UPDATE_INTERVAL_MILLIS,
          PASSIVE_UPDATE_MIN_DISTANCE_METERS,
          buildPassiveLocationPendingIntent(context),
        )
      }
    }.onFailure { error ->
      Log.w(TAG, "Não foi possível registrar PASSIVE_PROVIDER para geofencing", error)
    }

    return registeredCount
  }

  /**
   * Agenda uma verificação de conclusão de *Dwell* (2 minutos após a entrada inicial no raio).
   */
  fun scheduleDwellVerificationAlarm(
    context: Context,
    delayMillis: Long = DEFAULT_DWELL_DURATION_MILLIS,
  ) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val triggerAtMillis = System.currentTimeMillis() + delayMillis.coerceAtLeast(1_000L)
    val pendingIntent = buildDwellCheckPendingIntent(context)
    runCatching {
      alarmManager.cancel(pendingIntent)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
        alarmManager.setAndAllowWhileIdle(
          AlarmManager.RTC_WAKEUP,
          triggerAtMillis,
          pendingIntent,
        )
      } else {
        alarmManager.setExactAndAllowWhileIdle(
          AlarmManager.RTC_WAKEUP,
          triggerAtMillis,
          pendingIntent,
        )
      }
    }.onFailure { error ->
      Log.w(TAG, "Falha ao agendar alarme de verificação Dwell", error)
    }
  }

  private fun buildProximityPendingIntent(context: Context, ruleId: String): PendingIntent {
    val intent = Intent(context, SystemAutomationReceiver::class.java).apply {
      action = SystemAutomationReceiver.ACTION_GEOFENCE_TRANSITION
      putExtra(SystemAutomationReceiver.EXTRA_LOCATION_RULE_ID, ruleId)
    }
    InternalIntentSecurity.signIntent(context, intent)
    val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
    return PendingIntent.getBroadcast(
      context,
      ruleId.hashCode(),
      intent,
      flags,
    )
  }

  private fun buildPassiveLocationPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, SystemAutomationReceiver::class.java).apply {
      action = SystemAutomationReceiver.ACTION_PASSIVE_LOCATION_UPDATE
    }
    InternalIntentSecurity.signIntent(context, intent)
    val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
    return PendingIntent.getBroadcast(
      context,
      PASSIVE_LOCATION_REQUEST_CODE,
      intent,
      flags,
    )
  }

  private fun buildDwellCheckPendingIntent(context: Context): PendingIntent {
    val intent = Intent(context, SystemAutomationReceiver::class.java).apply {
      action = SystemAutomationReceiver.ACTION_GEOFENCE_DWELL_CHECK
    }
    InternalIntentSecurity.signIntent(context, intent)
    val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    return PendingIntent.getBroadcast(
      context,
      DWELL_ALARM_REQUEST_CODE,
      intent,
      flags,
    )
  }
}
