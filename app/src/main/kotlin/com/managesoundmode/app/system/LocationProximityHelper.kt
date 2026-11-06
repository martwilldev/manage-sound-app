package com.managesoundmode.app.system

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray

data class DeviceCoordinates(
  val latitude: Double,
  val longitude: Double,
  val accuracyMeters: Float? = null,
)

data class AddressSearchResult(
  val title: String,
  val subtitle: String,
  val latitude: Double,
  val longitude: Double,
)

object LocationProximityHelper {

  fun hasLocationPermission(context: Context): Boolean {
    val fine = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    val coarse = ContextCompat.checkSelfPermission(
      context,
      Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    return fine || coarse
  }

  fun readConnectedWifiSsid(context: Context): String? =
    runCatching {
      val wifiManager =
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
          ?: return@runCatching null
      @Suppress("DEPRECATION")
      val rawSsid = wifiManager.connectionInfo?.ssid ?: return@runCatching null
      val cleaned = rawSsid.trim().removeSurrounding("\"")
      if (cleaned.isBlank() || cleaned.equals("<unknown ssid>", ignoreCase = true)) {
        null
      } else {
        cleaned
      }
    }.getOrNull()

  fun matchesWifiSsid(ruleWifiSsid: String?, connectedWifiSsid: String?): Boolean {
    val target = ruleWifiSsid?.trim()?.removeSurrounding("\"")?.takeIf { it.isNotEmpty() } ?: return false
    val current = connectedWifiSsid?.trim()?.removeSurrounding("\"")?.takeIf { it.isNotEmpty() } ?: return false
    return target.equals(current, ignoreCase = true)
  }

  /**
   * Calcula a distância geodésica em metros usando a fórmula de Haversine
   * (funciona tanto no Android quanto em testes unitários JVM).
   */
  fun distanceMeters(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double,
  ): Float {
    val earthRadiusMeters = 6371000.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
      cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
      sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return (earthRadiusMeters * c).toFloat()
  }

  @SuppressLint("MissingPermission")
  suspend fun fetchCurrentDeviceLocation(context: Context): DeviceCoordinates? =
    withContext(Dispatchers.IO) {
      if (!hasLocationPermission(context)) return@withContext null
      val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return@withContext null

      val providers = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          add(LocationManager.FUSED_PROVIDER)
        }
        add(LocationManager.GPS_PROVIDER)
        add(LocationManager.NETWORK_PROVIDER)
        add(LocationManager.PASSIVE_PROVIDER)
      }.distinct()

      // 1. Tenta obter leitura fresca em até 2,5s (Android 11+)
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        for (provider in providers) {
          val isEnabled = runCatching { locationManager.isProviderEnabled(provider) }.getOrDefault(false)
          if (!isEnabled) continue
          val fresh = withTimeoutOrNull(2500L) {
            requestSingleLocationApi30(locationManager, provider, context)
          }
          if (fresh != null) {
            return@withContext DeviceCoordinates(
              latitude = fresh.latitude,
              longitude = fresh.longitude,
              accuracyMeters = if (fresh.hasAccuracy()) fresh.accuracy else null,
            )
          }
        }
      }

      // 2. Fallback rápido para a última localização conhecida mais recente
      val bestLastKnown = providers
        .mapNotNull { provider ->
          runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        }
        .maxByOrNull { it.time }

      bestLastKnown?.let {
        DeviceCoordinates(
          latitude = it.latitude,
          longitude = it.longitude,
          accuracyMeters = if (it.hasAccuracy()) it.accuracy else null,
        )
      }
    }

  @SuppressLint("MissingPermission")
  private suspend fun requestSingleLocationApi30(
    locationManager: LocationManager,
    provider: String,
    context: Context,
  ): Location? = suspendCancellableCoroutine { continuation ->
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
      continuation.resume(null)
      return@suspendCancellableCoroutine
    }
    val cancellationSignal = CancellationSignal()
    continuation.invokeOnCancellation { cancellationSignal.cancel() }
    runCatching {
      locationManager.getCurrentLocation(
        provider,
        cancellationSignal,
        ContextCompat.getMainExecutor(context),
      ) { location ->
        if (continuation.isActive) {
          continuation.resume(location)
        }
      }
    }.onFailure {
      if (continuation.isActive) {
        continuation.resume(null)
      }
    }
  }

  /**
   * Busca endereços gratuitamente combinando o Geocoder nativo do Android e o
   * serviço aberto Nominatim do OpenStreetMap (sem necessidade de chave de API).
   */
  suspend fun searchAddress(
    context: Context,
    query: String,
  ): List<AddressSearchResult> = withContext(Dispatchers.IO) {
    val cleanQuery = query.trim()
    if (cleanQuery.length < 2) return@withContext emptyList()

    // 1. Tenta Nominatim OpenStreetMap (retorna nomes de locais, ruas e bairros ricos)
    val nominatimResults = runCatching { searchViaNominatim(cleanQuery) }.getOrDefault(emptyList())
    if (nominatimResults.isNotEmpty()) {
      return@withContext nominatimResults
    }

    // 2. Fallback para o Geocoder nativo do Android
    runCatching {
      @Suppress("DEPRECATION")
      val addresses = Geocoder(context, Locale.getDefault()).getFromLocationName(cleanQuery, 5)
      addresses.orEmpty().mapNotNull { addr ->
        if (!addr.hasLatitude() || !addr.hasLongitude()) return@mapNotNull null
        val primary = addr.featureName
          ?: addr.thoroughfare
          ?: addr.subLocality
          ?: addr.locality
          ?: cleanQuery
        val fullLine = (0..addr.maxAddressLineIndex)
          .mapNotNull { idx -> addr.getAddressLine(idx) }
          .joinToString(", ")
          .ifEmpty { "${addr.latitude}, ${addr.longitude}" }
        AddressSearchResult(
          title = primary,
          subtitle = fullLine,
          latitude = addr.latitude,
          longitude = addr.longitude,
        )
      }
    }.getOrDefault(emptyList())
  }

  private fun searchViaNominatim(query: String): List<AddressSearchResult> {
    val encoded = URLEncoder.encode(query, "UTF-8")
    val url = URL(
      "https://nominatim.openstreetmap.org/search?format=json&addressdetails=1&limit=4&accept-language=pt-BR&q=$encoded",
    )
    val conn = (url.openConnection() as HttpURLConnection).apply {
      requestMethod = "GET"
      connectTimeout = 4500
      readTimeout = 4500
      setRequestProperty("User-Agent", "ModosDeSomAndroid/0.2.0 (https://managesoundmode.app)")
      setRequestProperty("Accept", "application/json")
    }
    return try {
      if (conn.responseCode !in 200..299) return emptyList()
      val body = conn.inputStream.bufferedReader().use { it.readText() }
      val jsonArray = JSONArray(body)
      buildList {
        for (i in 0 until jsonArray.length()) {
          val obj = jsonArray.getJSONObject(i)
          val lat = obj.optString("lat").toDoubleOrNull() ?: continue
          val lon = obj.optString("lon").toDoubleOrNull() ?: continue
          val displayName = obj.optString("display_name", query)
          val parts = displayName.split(",").map { it.trim() }.filter { it.isNotEmpty() }
          val title = obj.optString("name").takeIf { it.isNotBlank() }
            ?: parts.firstOrNull()
            ?: query
          val subtitle = parts.drop(1).take(3).joinToString(", ").ifEmpty { displayName }
          add(
            AddressSearchResult(
              title = title,
              subtitle = subtitle,
              latitude = lat,
              longitude = lon,
            ),
          )
        }
      }
    } finally {
      conn.disconnect()
    }
  }
}
