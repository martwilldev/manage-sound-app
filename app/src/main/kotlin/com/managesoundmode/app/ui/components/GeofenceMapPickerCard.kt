package com.managesoundmode.app.ui.components

import android.annotation.SuppressLint
import android.graphics.Color as AndroidColor
import android.view.MotionEvent
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.managesoundmode.app.system.AddressSearchResult
import com.managesoundmode.app.ui.common.AppLabels
import com.managesoundmode.app.ui.common.AppTestTags
import java.util.Locale

private class OsmWebMapBridge(
  private val onMapReadyCallback: () -> Unit,
  private val onCoordinatePickedCallback: (Double, Double) -> Unit,
) {
  @JavascriptInterface
  fun onMapReady() {
    onMapReadyCallback()
  }

  @JavascriptInterface
  fun onLocationSelected(lat: Double, lng: Double) {
    if (lat.isFinite() && lng.isFinite() && lat in -90.0..90.0 && lng in -180.0..180.0) {
      onCoordinatePickedCallback(lat, lng)
    }
  }
}

/**
 * Cartão completo de Mapa Interativo OpenStreetMap (100% gratuito, sem necessidade de API Key):
 * - Busca de endereço integrada (Nominatim OSM + Android Geocoder)
 * - Botão "Usar minha localização atual (GPS)"
 * - Mapa Leaflet + OpenStreetMap / CartoDB com pino arrastável e toque para posicionar
 * - Círculo geodésico real em metros que cresce/diminui conforme o slider de raio da cerca
 * - Fallback visual em Canvas Topográfico se o aparelho estiver sem internet
 */
@OptIn(ExperimentalLayoutApi::class)
@SuppressLint("SetJavaScriptEnabled", "ClickableViewAccessibility")
@Composable
fun GeofenceMapPickerCard(
  latitude: Double,
  longitude: Double,
  radiusMeters: Int,
  isFetchingGps: Boolean,
  isSearchingAddress: Boolean,
  searchResults: List<AddressSearchResult>,
  onCoordinatesChanged: (latitude: Double, longitude: Double) -> Unit,
  onRequestCurrentGps: () -> Unit,
  onSearchAddress: (query: String) -> Unit,
  onSelectSearchResult: (AddressSearchResult) -> Unit,
  modifier: Modifier = Modifier,
) {
  val focusManager = LocalFocusManager.current
  val isDark = isSystemInDarkTheme()
  var searchQuery by rememberSaveable { mutableStateOf("") }
  var webMapReady by remember { mutableStateOf(false) }
  var webViewRef by remember { mutableStateOf<WebView?>(null) }
  val currentOnCoordinatesChanged by rememberUpdatedState(onCoordinatesChanged)

  val htmlContent = remember(isDark) {
    buildLeafletMapHtml(
      initialLat = latitude,
      initialLng = longitude,
      initialRadius = radiusMeters,
      isDark = isDark,
    )
  }

  LaunchedEffect(latitude, longitude, radiusMeters, webMapReady) {
    val wv = webViewRef ?: return@LaunchedEffect
    if (webMapReady) {
      val js = String.format(
        Locale.US,
        "if (window.updateGeofence) { window.updateGeofence(%.6f, %.6f, %d); }",
        latitude,
        longitude,
        radiusMeters,
      )
      wv.evaluateJavascript(js, null)
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      webViewRef?.destroy()
      webViewRef = null
    }
  }

  Card(
    modifier = modifier
      .fillMaxWidth()
      .testTag(AppTestTags.GeofenceMap.CARD),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // Cabeçalho + Botão GPS Atual
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = AppLabels.GeofenceMap.mapCardTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
          )
          Text(
            text = AppLabels.GeofenceMap.mapCardSubtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }

        FilledTonalButton(
          onClick = onRequestCurrentGps,
          enabled = !isFetchingGps,
          shape = RoundedCornerShape(14.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
          modifier = Modifier.testTag(AppTestTags.GeofenceMap.USE_CURRENT_GPS_BUTTON),
        ) {
          if (isFetchingGps) {
            CircularProgressIndicator(
              modifier = Modifier.size(16.dp),
              strokeWidth = 2.dp,
            )
          } else {
            Icon(
              imageVector = Icons.Default.MyLocation,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
          }
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isFetchingGps) {
              AppLabels.GeofenceMap.fetchingGpsLabel
            } else {
              AppLabels.GeofenceMap.useCurrentGpsLabel
            },
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            softWrap = false,
          )
        }
      }

      // Barra de busca de endereço gratuita (OpenStreetMap Nominatim + Geocoder)
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = {
            Text(
              text = AppLabels.GeofenceMap.searchPlaceholder,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          },
          singleLine = true,
          shape = RoundedCornerShape(16.dp),
          keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
          keyboardActions = KeyboardActions(
            onSearch = {
              focusManager.clearFocus()
              onSearchAddress(searchQuery)
            },
          ),
          trailingIcon = {
            IconButton(
              onClick = {
                focusManager.clearFocus()
                onSearchAddress(searchQuery)
              },
              enabled = searchQuery.trim().length >= 2 && !isSearchingAddress,
              modifier = Modifier.testTag(AppTestTags.GeofenceMap.SEARCH_ADDRESS_BUTTON),
            ) {
              if (isSearchingAddress) {
                CircularProgressIndicator(
                  modifier = Modifier.size(18.dp),
                  strokeWidth = 2.dp,
                )
              } else {
                Icon(
                  imageVector = Icons.Default.Search,
                  contentDescription = AppLabels.GeofenceMap.searchContentDescription,
                )
              }
            }
          },
          modifier = Modifier
            .weight(1f)
            .testTag(AppTestTags.GeofenceMap.SEARCH_ADDRESS_INPUT),
        )
      }

      // Sugestões de endereço retornadas pela busca
      if (searchResults.isNotEmpty()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = AppLabels.GeofenceMap.searchResultsHint,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
          )
          searchResults.forEachIndexed { idx, result ->
            Surface(
              onClick = { onSelectSearchResult(result) },
              shape = RoundedCornerShape(14.dp),
              color = MaterialTheme.colorScheme.surfaceContainerHigh,
              modifier = Modifier
                .fillMaxWidth()
                .testTag(AppTestTags.GeofenceMap.searchResult(idx)),
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.LocationOn,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp),
                )
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = result.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                  )
                  Text(
                    text = result.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                  )
                }
              }
            }
          }
        }
      }

      // Container do Mapa Interativo (OpenStreetMap + Radar Topográfico Fallback)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(265.dp)
          .clip(RoundedCornerShape(20.dp))
          .background(MaterialTheme.colorScheme.surfaceContainerHighest),
      ) {
        // Camada base radar (visível instantaneamente enquanto os blocos OSM carregam ou offline)
        OfflineRadarGeofenceCanvas(
          latitude = latitude,
          longitude = longitude,
          radiusMeters = radiusMeters,
          onTapOffset = { dLat, dLng ->
            val nextLat = (latitude + dLat).coerceIn(-85.0, 85.0)
            val nextLng = (longitude + dLng).coerceIn(-180.0, 180.0)
            currentOnCoordinatesChanged(nextLat, nextLng)
          },
          modifier = Modifier.fillMaxSize(),
        )

        // Mapa OpenStreetMap Interativo (Leaflet)
        AndroidView(
          modifier = Modifier
            .fillMaxSize()
            .testTag(AppTestTags.GeofenceMap.OSM_WEBVIEW),
          factory = { context ->
            WebView(context).apply {
              layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
              )
              setBackgroundColor(AndroidColor.TRANSPARENT)
              settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                cacheMode = WebSettings.LOAD_DEFAULT
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                userAgentString = "ModosDeSomAndroid/0.2.0 (OpenStreetMap Geofence Picker)"
              }
              webChromeClient = WebChromeClient()
              webViewClient = WebViewClient()
              addJavascriptInterface(
                OsmWebMapBridge(
                  onMapReadyCallback = {
                    post { webMapReady = true }
                  },
                  onCoordinatePickedCallback = { lat, lng ->
                    post { currentOnCoordinatesChanged(lat, lng) }
                  },
                ),
                "AndroidMapBridge",
              )
              // Impede que o ScrollView pai roube o gesto de arrastar o mapa
              setOnTouchListener { v, event ->
                when (event.actionMasked) {
                  MotionEvent.ACTION_DOWN,
                  MotionEvent.ACTION_MOVE,
                  MotionEvent.ACTION_POINTER_DOWN -> {
                    v.parent?.requestDisallowInterceptTouchEvent(true)
                  }
                  MotionEvent.ACTION_UP,
                  MotionEvent.ACTION_CANCEL -> {
                    v.parent?.requestDisallowInterceptTouchEvent(false)
                  }
                }
                false
              }
              loadDataWithBaseURL(
                "https://openstreetmap.org",
                htmlContent,
                "text/html",
                "UTF-8",
                null,
              )
              webViewRef = this
            }
          },
        )

        // Controles flutuantes de Zoom e Centralizar à direita do mapa
        Column(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(10.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Surface(
            onClick = {
              webViewRef?.evaluateJavascript("if(window.zoomMap) window.zoomMap(1);", null)
            },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
            tonalElevation = 4.dp,
            modifier = Modifier.size(36.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.Add,
                contentDescription = AppLabels.GeofenceMap.zoomInContentDescription,
                modifier = Modifier.size(18.dp),
              )
            }
          }
          Surface(
            onClick = {
              webViewRef?.evaluateJavascript("if(window.zoomMap) window.zoomMap(-1);", null)
            },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
            tonalElevation = 4.dp,
            modifier = Modifier.size(36.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = AppLabels.GeofenceMap.zoomOutContentDescription,
                modifier = Modifier.size(18.dp),
              )
            }
          }
          Surface(
            onClick = {
              val js = String.format(
                Locale.US,
                "if(window.recenterGeofence) window.recenterGeofence(%.6f, %.6f, %d);",
                latitude,
                longitude,
                radiusMeters,
              )
              webViewRef?.evaluateJavascript(js, null)
            },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.94f),
            tonalElevation = 4.dp,
            modifier = Modifier.size(36.dp),
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = Icons.Default.CenterFocusStrong,
                contentDescription = AppLabels.GeofenceMap.recenterContentDescription,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(18.dp),
              )
            }
          }
        }

        // Pílula inferior exibindo coordenadas em tempo real e o raio da cerca
        Surface(
          shape = RoundedCornerShape(50),
          color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
          tonalElevation = 4.dp,
          modifier = Modifier
            .align(Alignment.BottomStart)
            .padding(10.dp),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Default.LocationOn,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(14.dp),
            )
            Text(
              text = AppLabels.GeofenceMap.coordinatesAndRadiusBadge(
                latitude,
                longitude,
                radiusMeters,
              ),
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              softWrap = false,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun OfflineRadarGeofenceCanvas(
  latitude: Double,
  longitude: Double,
  radiusMeters: Int,
  onTapOffset: (dLat: Double, dLng: Double) -> Unit,
  modifier: Modifier = Modifier,
) {
  val primaryColor = MaterialTheme.colorScheme.primary
  val gridColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.14f)

  Canvas(
    modifier = modifier.pointerInput(radiusMeters) {
      detectTapGestures { offset ->
        val normX = (offset.x - size.width / 2f) / (size.width / 2f)
        val normY = (size.height / 2f - offset.y) / (size.height / 2f)
        val scaleDeg = (radiusMeters / 111_000.0) * 1.8
        onTapOffset(normY * scaleDeg, normX * scaleDeg)
      }
    },
  ) {
    val step = size.minDimension / 6f
    var x = step
    while (x < size.width) {
      drawLine(
        color = gridColor,
        start = Offset(x, 0f),
        end = Offset(x, size.height),
        strokeWidth = 1.dp.toPx(),
      )
      x += step
    }
    var y = step
    while (y < size.height) {
      drawLine(
        color = gridColor,
        start = Offset(0f, y),
        end = Offset(size.width, y),
        strokeWidth = 1.dp.toPx(),
      )
      y += step
    }

    val center = Offset(size.width / 2f, size.height / 2f)
    val normalizedRadius = ((radiusMeters - 100f) / 900f).coerceIn(0.15f, 1f)
    val circleRadiusPx = (size.minDimension * 0.18f) + (size.minDimension * 0.26f * normalizedRadius)

    drawCircle(
      color = primaryColor.copy(alpha = 0.18f),
      radius = circleRadiusPx,
      center = center,
    )
    drawCircle(
      color = primaryColor,
      radius = circleRadiusPx,
      center = center,
      style = Stroke(
        width = 2.dp.toPx(),
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)),
      ),
    )
    drawCircle(
      color = Color.White,
      radius = 9.dp.toPx(),
      center = center,
    )
    drawCircle(
      color = primaryColor,
      radius = 6.dp.toPx(),
      center = center,
    )
  }
}

private fun buildLeafletMapHtml(
  initialLat: Double,
  initialLng: Double,
  initialRadius: Int,
  isDark: Boolean,
): String {
  val tileUrl = if (isDark) {
    "https://{s}.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}{r}.png"
  } else {
    "https://{s}.basemaps.cartocdn.com/rastertiles/voyager/{z}/{x}/{y}{r}.png"
  }
  return """
    <!DOCTYPE html>
    <html>
    <head>
      <meta charset="utf-8" />
      <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
      <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
      <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
      <style>
        html, body, #map {
          margin: 0;
          padding: 0;
          width: 100%;
          height: 100%;
          background: transparent;
          font-family: sans-serif;
        }
        .leaflet-control-attribution {
          font-size: 9px !important;
          opacity: 0.7;
        }
        .geofence-pin {
          width: 22px;
          height: 22px;
          border-radius: 50%;
          background: #2563EB;
          border: 3px solid #FFFFFF;
          box-shadow: 0 2px 8px rgba(0,0,0,0.45);
        }
      </style>
    </head>
    <body>
      <div id="map"></div>
      <script>
        var currentLat = $initialLat;
        var currentLng = $initialLng;
        var currentRadius = $initialRadius;
        var map = null;
        var marker = null;
        var circle = null;

        function zoomForRadius(r) {
          if (r <= 150) return 17;
          if (r <= 300) return 16;
          if (r <= 600) return 15;
          return 14;
        }

        function initMap() {
          if (typeof L === 'undefined') return;
          map = L.map('map', {
            zoomControl: false,
            attributionControl: true
          }).setView([currentLat, currentLng], zoomForRadius(currentRadius));

          var primaryLayer = L.tileLayer('$tileUrl', {
            maxZoom: 19,
            subdomains: 'abcd',
            attribution: '&copy; OpenStreetMap'
          }).addTo(map);

          primaryLayer.on('tileerror', function() {
            L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
              maxZoom: 19
            }).addTo(map);
          });

          var pinIcon = L.divIcon({
            className: '',
            html: '<div class="geofence-pin"></div>',
            iconSize: [22, 22],
            iconAnchor: [11, 11]
          });

          circle = L.circle([currentLat, currentLng], {
            color: '#3B82F6',
            weight: 2.5,
            fillColor: '#60A5FA',
            fillOpacity: 0.24,
            radius: currentRadius
          }).addTo(map);

          marker = L.marker([currentLat, currentLng], {
            draggable: true,
            icon: pinIcon
          }).addTo(map);

          marker.on('dragend', function(e) {
            var pos = marker.getLatLng();
            currentLat = pos.lat;
            currentLng = pos.lng;
            circle.setLatLng(pos);
            if (window.AndroidMapBridge) {
              window.AndroidMapBridge.onLocationSelected(pos.lat, pos.lng);
            }
          });

          map.on('click', function(e) {
            currentLat = e.latlng.lat;
            currentLng = e.latlng.lng;
            marker.setLatLng(e.latlng);
            circle.setLatLng(e.latlng);
            if (window.AndroidMapBridge) {
              window.AndroidMapBridge.onLocationSelected(e.latlng.lat, e.latlng.lng);
            }
          });

          if (window.AndroidMapBridge) {
            window.AndroidMapBridge.onMapReady();
          }
        }

        window.updateGeofence = function(lat, lng, radiusMeters) {
          if (!map || !marker || !circle) return;
          var dist = Math.abs(lat - currentLat) + Math.abs(lng - currentLng);
          currentLat = lat;
          currentLng = lng;
          currentRadius = radiusMeters;
          var latLng = L.latLng(lat, lng);
          marker.setLatLng(latLng);
          circle.setLatLng(latLng);
          circle.setRadius(radiusMeters);
          if (dist > 0.0002) {
            map.setView(latLng, zoomForRadius(radiusMeters), { animate: true });
          }
        };

        window.recenterGeofence = function(lat, lng, radiusMeters) {
          if (!map) return;
          map.setView([lat, lng], zoomForRadius(radiusMeters), { animate: true });
        };

        window.zoomMap = function(delta) {
          if (!map) return;
          map.setZoom(map.getZoom() + delta);
        };

        window.addEventListener('load', initMap);
      </script>
    </body>
    </html>
  """.trimIndent()
}
