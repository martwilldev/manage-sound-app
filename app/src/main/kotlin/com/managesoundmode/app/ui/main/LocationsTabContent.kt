package com.managesoundmode.app.ui.main

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.managesoundmode.app.domain.model.LocationRule
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.system.AddressSearchResult
import com.managesoundmode.app.system.LocationProximityHelper
import com.managesoundmode.app.ui.common.AppLabels
import com.managesoundmode.app.ui.common.AppTestTags
import com.managesoundmode.app.ui.components.GeofenceMapPickerCard
import com.managesoundmode.app.ui.components.M3CheckSwitch
import com.managesoundmode.app.ui.components.ProminentLocationDisclosureDialog
import com.managesoundmode.app.ui.components.profileIconForKey
import com.managesoundmode.app.ui.theme.onTonalContainerColor
import com.managesoundmode.app.ui.theme.tonalAccentColor
import com.managesoundmode.app.ui.theme.tonalContainerColor
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun LocationsTabContent(
  locationRules: List<LocationRule>,
  profiles: List<SoundProfile>,
  onToggleRule: (LocationRule, Boolean) -> Unit,
  onEditRule: (LocationRule) -> Unit,
  onTogglePresence: (String, Boolean) -> Unit = { _, _ -> },
  onDeleteRule: (String) -> Unit = {},
  onSyncGpsCoordinates: (Double, Double) -> Unit = { _, _ -> },
  onShowStatusMessage: (String) -> Unit = {},
  hasAcceptedLocationDisclosure: Boolean = true,
  onAcceptLocationDisclosure: () -> Unit = {},
  onSyncBackgroundGeofencesNow: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var isSyncingGps by remember { mutableStateOf(false) }
  var showGpsDisclosureModal by rememberSaveable { mutableStateOf(false) }

  fun triggerGpsSync() {
    if (isSyncingGps) return
    isSyncingGps = true
    scope.launch {
      val coords = LocationProximityHelper.fetchCurrentDeviceLocation(context)
      isSyncingGps = false
      if (coords != null) {
        onSyncGpsCoordinates(coords.latitude, coords.longitude)
      } else {
        onShowStatusMessage(AppLabels.Locations.gpsReadErrorMessage)
      }
    }
  }

  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions(),
  ) { permissions ->
    val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    if (granted) {
      triggerGpsSync()
    } else {
      onShowStatusMessage(AppLabels.Locations.gpsPermissionRequiredMessage)
    }
  }

  if (showGpsDisclosureModal) {
    ProminentLocationDisclosureDialog(
      onAcceptAndContinue = {
        showGpsDisclosureModal = false
        onAcceptLocationDisclosure()
        locationPermissionLauncher.launch(
          arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
          ),
        )
      },
      onDismiss = { showGpsDisclosureModal = false },
    )
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .testTag(AppTestTags.Locations.LIST),
    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 104.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    // Card de Radar GPS no topo da aba Locais
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag(AppTestTags.Locations.SYNC_GPS_CARD),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
      ) {
        FlowRow(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = AppLabels.Locations.geofenceHeaderTitle,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
            )
            Text(
              text = AppLabels.Locations.geofenceHeaderSubtitle,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          FilledTonalButton(
            onClick = {
              if (LocationProximityHelper.hasLocationPermission(context)) {
                triggerGpsSync()
              } else if (!hasAcceptedLocationDisclosure) {
                showGpsDisclosureModal = true
              } else {
                locationPermissionLauncher.launch(
                  arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                  ),
                )
              }
            },
            enabled = !isSyncingGps,
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.testTag(AppTestTags.Locations.VERIFY_GPS_BUTTON),
          ) {
            if (isSyncingGps) {
              CircularProgressIndicator(
                modifier = Modifier.size(15.dp),
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
              text = if (isSyncingGps) {
                AppLabels.Locations.readingGpsButton
              } else {
                AppLabels.Locations.verifyGpsButton
              },
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              softWrap = false,
            )
          }
        }
      }
    }

    // [GEO-01] Card de status do Geofencing Passivo em Background (Dwell 2 min + Wi-Fi SSID)
    item {
      val activeGeofenceCount = locationRules.count { it.enabled }
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag(AppTestTags.Locations.GEOFENCE_STATUS_CARD),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
      ) {
        FlowRow(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Box(
                modifier = Modifier
                  .size(8.dp)
                  .clip(CircleShape)
                  .background(
                    if (activeGeofenceCount > 0) {
                      MaterialTheme.colorScheme.primary
                    } else {
                      MaterialTheme.colorScheme.onSurfaceVariant
                    },
                  ),
              )
              Text(
                text = AppLabels.Locations.geofenceStatusTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
              )
            }
            Text(
              text = AppLabels.Locations.geofenceStatusSubtitle(activeGeofenceCount),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          FilledTonalButton(
            onClick = onSyncBackgroundGeofencesNow,
            shape = RoundedCornerShape(16.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.testTag(AppTestTags.Locations.SYNC_GEOFENCES_BUTTON),
          ) {
            Icon(
              imageVector = Icons.Default.LocationOn,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = AppLabels.Locations.syncGeofencesButton,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              softWrap = false,
            )
          }
        }
      }
    }

    itemsIndexed(
      items = locationRules,
      key = { _, rule -> rule.id },
    ) { _, rule ->
      val targetProfile = profiles.firstOrNull { it.id == rule.targetProfileId }
        ?: profiles.firstOrNull()

      LocationRuleCard(
        rule = rule,
        targetProfile = targetProfile,
        shape = RoundedCornerShape(24.dp),
        onClick = { onEditRule(rule) },
        onToggleEnabled = { enabled -> onToggleRule(rule, enabled) },
        onTogglePresence = { onTogglePresence(rule.id, !rule.isCurrentlyInside) },
        onDelete = { onDeleteRule(rule.id) },
      )
    }
  }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun LocationRuleCard(
  rule: LocationRule,
  targetProfile: SoundProfile?,
  shape: RoundedCornerShape,
  onClick: () -> Unit,
  onToggleEnabled: (Boolean) -> Unit,
  onTogglePresence: () -> Unit,
  onDelete: () -> Unit,
) {
  val haptics = LocalHapticFeedback.current
  var showContextMenu by remember { mutableStateOf(false) }

  val modeAccent = targetProfile?.mode?.tonalAccentColor() ?: MaterialTheme.colorScheme.primary
  val containerColor = when {
    rule.enabled && rule.isCurrentlyInside -> MaterialTheme.colorScheme.surfaceContainerHigh
    rule.enabled -> MaterialTheme.colorScheme.surfaceContainerLow
    else -> MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.68f)
  }
  val borderStroke = when {
    rule.enabled && rule.isCurrentlyInside -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    rule.enabled -> BorderStroke(1.dp, modeAccent.copy(alpha = 0.35f))
    else -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(shape)
      .combinedClickable(
        onClick = onClick,
        onLongClick = {
          haptics.performHapticFeedback(HapticFeedbackType.LongPress)
          showContextMenu = true
        },
      )
      .testTag(AppTestTags.Locations.card(rule.id)),
    shape = shape,
    border = borderStroke,
    colors = CardDefaults.cardColors(containerColor = containerColor),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.weight(1f),
        ) {
          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(
                if (rule.enabled) {
                  modeAccent.copy(alpha = 0.16f)
                } else {
                  MaterialTheme.colorScheme.surfaceContainerHighest
                },
              ),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = Icons.Default.LocationOn,
              contentDescription = null,
              tint = if (rule.enabled) {
                modeAccent
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
              modifier = Modifier.size(22.dp),
            )
          }

          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
          ) {
            Text(
              text = rule.name,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 2,
              overflow = TextOverflow.Ellipsis,
              color = if (rule.enabled) {
                MaterialTheme.colorScheme.onSurface
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
            )

            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(6.dp),
              verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
              Text(
                text = AppLabels.Locations.radiusLabel(rule.radiusMeters),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
              )
              Text(
                text = "• ${rule.formattedCoordinates()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
              )
              if (rule.wifiSsid.isNotBlank()) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                  Text(
                    text = "•",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                  )
                  Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.primary,
                  )
                  Text(
                    text = rule.wifiSsid,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.primary,
                  )
                }
              }
            }
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
          M3CheckSwitch(
            checked = rule.enabled,
            onCheckedChange = onToggleEnabled,
            modifier = Modifier.testTag(AppTestTags.Locations.switch(rule.id)),
          )

          Box {
            IconButton(
              onClick = { showContextMenu = true },
              modifier = Modifier
                .size(34.dp)
                .testTag(AppTestTags.Locations.optionsButton(rule.id)),
            ) {
              Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = AppLabels.Locations.optionsContentDescription(rule.name),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
              )
            }

            DropdownMenu(
              expanded = showContextMenu,
              onDismissRequest = { showContextMenu = false },
            ) {
              DropdownMenuItem(
                text = { Text(AppLabels.Locations.editLocationMenu) },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = null,
                  )
                },
                onClick = {
                  showContextMenu = false
                  onClick()
                },
                modifier = Modifier.testTag(AppTestTags.Locations.menuEditItem(rule.id)),
              )
              DropdownMenuItem(
                text = {
                  Text(
                    if (rule.isCurrentlyInside) {
                      AppLabels.Locations.markOutsideMenu
                    } else {
                      AppLabels.Locations.simulateInsideMenu
                    },
                  )
                },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = null,
                  )
                },
                onClick = {
                  showContextMenu = false
                  onTogglePresence()
                },
                modifier = Modifier.testTag(AppTestTags.Locations.menuPresenceItem(rule.id)),
              )
              DropdownMenuItem(
                text = {
                  Text(
                    if (rule.enabled) {
                      AppLabels.Locations.disableLocationMenu
                    } else {
                      AppLabels.Locations.enableLocationMenu
                    },
                  )
                },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                  )
                },
                onClick = {
                  showContextMenu = false
                  onToggleEnabled(!rule.enabled)
                },
                modifier = Modifier.testTag(AppTestTags.Locations.menuToggleItem(rule.id)),
              )
              HorizontalDivider()
              DropdownMenuItem(
                text = {
                  Text(
                    text = AppLabels.Locations.deleteLocationMenu,
                    color = MaterialTheme.colorScheme.error,
                  )
                },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                  )
                },
                onClick = {
                  showContextMenu = false
                  onDelete()
                },
                modifier = Modifier.testTag(AppTestTags.Locations.menuDeleteItem(rule.id)),
              )
            }
          }
        }
      }

      // Badges Semânticos ZENITH
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        if (rule.isCurrentlyInside) {
          Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primary,
          ) {
            Text(
              text = AppLabels.Locations.insideBadge,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              softWrap = false,
              color = MaterialTheme.colorScheme.onPrimary,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
        }

        if (targetProfile != null) {
          Surface(
            shape = RoundedCornerShape(50),
            color = if (rule.enabled) {
              targetProfile.mode.tonalContainerColor()
            } else {
              MaterialTheme.colorScheme.surfaceContainerHighest
            },
            border = if (rule.enabled) {
              BorderStroke(1.dp, modeAccent.copy(alpha = 0.30f))
            } else {
              null
            },
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = profileIconForKey(targetProfile.iconKey),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = if (rule.enabled) {
                  targetProfile.mode.onTonalContainerColor()
                } else {
                  MaterialTheme.colorScheme.onSurfaceVariant
                },
              )
              Text(
                text = "${targetProfile.name} • ${targetProfile.summaryLabel()}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                color = if (rule.enabled) {
                  targetProfile.mode.onTonalContainerColor()
                } else {
                  MaterialTheme.colorScheme.onSurfaceVariant
                },
              )
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun LocationEditorBottomSheet(
  initialRule: LocationRule?,
  profiles: List<SoundProfile>,
  knownWifiSsids: List<String> = emptyList(),
  onConfirm: (LocationRule) -> Unit,
  onAutoSave: (LocationRule) -> Unit = {},
  onTogglePresence: ((String, Boolean) -> Unit)? = null,
  onDelete: ((String) -> Unit)? = null,
  onDismiss: () -> Unit,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  val isEditing = initialRule != null

  var name by rememberSaveable { mutableStateOf(initialRule?.name ?: "") }
  var wifiSsid by rememberSaveable { mutableStateOf(initialRule?.wifiSsid ?: "") }
  var latitude by rememberSaveable { mutableDoubleStateOf(initialRule?.latitude ?: -23.5614) }
  var longitude by rememberSaveable { mutableDoubleStateOf(initialRule?.longitude ?: -46.6559) }
  var radius by rememberSaveable {
    mutableFloatStateOf((initialRule?.radiusMeters ?: 150).toFloat())
  }
  var selectedProfileId by rememberSaveable {
    mutableStateOf(
      initialRule?.targetProfileId
        ?: profiles.firstOrNull { it.id == "preset_work_high" }?.id
        ?: profiles.first().id,
    )
  }

  var isFetchingGps by remember { mutableStateOf(false) }
  var isSearchingAddress by remember { mutableStateOf(false) }
  var searchResults by remember { mutableStateOf<List<AddressSearchResult>>(emptyList()) }
  var showWifiDropdown by remember { mutableStateOf(false) }
  var wifiSearchQuery by rememberSaveable { mutableStateOf("") }
  var connectedWifiSsid by remember {
    mutableStateOf(LocationProximityHelper.readConnectedWifiSsid(context))
  }
  var availableWifiSsids by remember(knownWifiSsids) {
    mutableStateOf(
      LocationProximityHelper.readKnownAndAvailableWifiSsids(
        context = context,
        savedRuleSsids = knownWifiSsids,
      ),
    )
  }

  val filteredWifiSsids = remember(availableWifiSsids, wifiSearchQuery, wifiSsid) {
    val base = buildList {
      if (wifiSsid.isNotBlank() && availableWifiSsids.none { it.equals(wifiSsid, ignoreCase = true) }) {
        add(wifiSsid.trim())
      }
      addAll(availableWifiSsids)
    }
    val q = wifiSearchQuery.trim()
    if (q.isEmpty()) {
      base
    } else {
      base.filter { it.contains(q, ignoreCase = true) }
    }
  }

  fun refreshWifiList() {
    connectedWifiSsid = LocationProximityHelper.readConnectedWifiSsid(context)
    availableWifiSsids = LocationProximityHelper.readKnownAndAvailableWifiSsids(
      context = context,
      savedRuleSsids = knownWifiSsids,
    )
  }

  fun buildRule(): LocationRule {
    val resolvedName = name.trim().ifEmpty {
      initialRule?.name ?: AppLabels.Locations.defaultLocationName
    }
    return LocationRule(
      id = initialRule?.id ?: "loc_${System.currentTimeMillis()}",
      name = resolvedName,
      latitude = latitude,
      longitude = longitude,
      radiusMeters = radius.roundToInt().coerceIn(100, 1000),
      wifiSsid = wifiSsid.trim(),
      targetProfileId = selectedProfileId,
      enabled = initialRule?.enabled ?: true,
      isCurrentlyInside = initialRule?.isCurrentlyInside ?: false,
    )
  }

  fun notifyAutoSaveIfEditing() {
    if (isEditing) {
      onAutoSave(buildRule())
    }
  }

  fun fetchAndApplyDeviceGps() {
    if (isFetchingGps) return
    isFetchingGps = true
    scope.launch {
      val coords = LocationProximityHelper.fetchCurrentDeviceLocation(context)
      isFetchingGps = false
      if (coords != null) {
        latitude = coords.latitude
        longitude = coords.longitude
        notifyAutoSaveIfEditing()
      }
    }
  }

  val gpsPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions(),
  ) { permissions ->
    val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
      permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    if (granted) {
      refreshWifiList()
      fetchAndApplyDeviceGps()
    }
  }

  Dialog(
    onDismissRequest = {
      notifyAutoSaveIfEditing()
      onDismiss()
    },
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      dismissOnClickOutside = false,
      dismissOnBackPress = true,
    ),
  ) {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
      topBar = {
        TopAppBar(
          navigationIcon = {
            IconButton(
              onClick = {
                notifyAutoSaveIfEditing()
                onDismiss()
              },
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = AppLabels.Common.back,
              )
            }
          },
          title = {
            Column {
              Text(
                text = if (isEditing) {
                  AppLabels.Locations.editLocationTitle
                } else {
                  AppLabels.Locations.newLocationTitle
                },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              if (isEditing) {
                Text(
                  text = AppLabels.Common.autoSavedSubtitle,
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }
          },
          actions = {
            if (isEditing && onDelete != null) {
              IconButton(
                onClick = { onDelete(initialRule.id) },
                modifier = Modifier.testTag(AppTestTags.Locations.DELETE_TOP_BUTTON),
              ) {
                Icon(
                  imageVector = Icons.Default.DeleteOutline,
                  contentDescription = AppLabels.Locations.deleteLocationMenu,
                  tint = MaterialTheme.colorScheme.error,
                )
              }
            } else if (!isEditing) {
              TextButton(
                onClick = { onConfirm(buildRule()) },
                modifier = Modifier.testTag(AppTestTags.Locations.CONFIRM_SAVE_BUTTON),
              ) {
                Text(AppLabels.Common.create, fontWeight = FontWeight.Bold)
              }
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
          ),
        )
      },
    ) { innerPadding ->
      Column(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        OutlinedTextField(
          value = name,
          onValueChange = {
            name = it
            notifyAutoSaveIfEditing()
          },
          label = { Text(AppLabels.Locations.locationNameLabel) },
          singleLine = true,
          shape = RoundedCornerShape(18.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag(AppTestTags.Locations.NAME_INPUT),
        )

        // Mapa Interativo OpenStreetMap + Radar Topográfico ZENITH
        GeofenceMapPickerCard(
          latitude = latitude,
          longitude = longitude,
          radiusMeters = radius.roundToInt().coerceIn(100, 1000),
          isFetchingGps = isFetchingGps,
          isSearchingAddress = isSearchingAddress,
          searchResults = searchResults,
          onCoordinatesChanged = { newLat, newLng ->
            latitude = newLat
            longitude = newLng
            notifyAutoSaveIfEditing()
          },
          onRequestCurrentGps = {
            if (LocationProximityHelper.hasLocationPermission(context)) {
              fetchAndApplyDeviceGps()
            } else {
              gpsPermissionLauncher.launch(
                arrayOf(
                  Manifest.permission.ACCESS_FINE_LOCATION,
                  Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
              )
            }
          },
          onSearchAddress = { query ->
            if (query.trim().length >= 2 && !isSearchingAddress) {
              isSearchingAddress = true
              scope.launch {
                searchResults = LocationProximityHelper.searchAddress(context, query)
                isSearchingAddress = false
              }
            }
          },
          onSelectSearchResult = { result ->
            latitude = result.latitude
            longitude = result.longitude
            if (name.isBlank()) {
              name = result.title
            }
            searchResults = emptyList()
            notifyAutoSaveIfEditing()
          },
        )

        // Controle do Raio do Perímetro (Geofence)
        Surface(
          shape = RoundedCornerShape(22.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = AppLabels.Locations.radiusSectionTitle,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              Text(
                text = AppLabels.Locations.metersValue(radius.roundToInt()),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
              )
            }
            Slider(
              value = radius,
              onValueChange = { radius = it },
              onValueChangeFinished = { notifyAutoSaveIfEditing() },
              valueRange = 100f..1000f,
              modifier = Modifier.testTag(AppTestTags.Locations.RADIUS_SLIDER),
            )
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              listOf(100, 150, 250, 500, 1000).forEach { presetMeters ->
                FilterChip(
                  selected = radius.roundToInt() == presetMeters,
                  onClick = {
                    radius = presetMeters.toFloat()
                    notifyAutoSaveIfEditing()
                  },
                  label = {
                    Text(
                      text = "${presetMeters}m",
                      maxLines = 1,
                      softWrap = false,
                    )
                  },
                )
              }
            }
          }
        }

        // Seletor de Rede Wi-Fi Conhecida / Pareada (Confirmação Indoor)
        Surface(
          shape = RoundedCornerShape(22.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
              ) {
                Icon(
                  imageVector = Icons.Default.Wifi,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp),
                )
                Text(
                  text = AppLabels.Locations.wifiSsidLabel,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurface,
                )
              }

              IconButton(
                onClick = {
                  if (LocationProximityHelper.hasLocationPermission(context)) {
                    refreshWifiList()
                  } else {
                    gpsPermissionLauncher.launch(
                      arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                      ),
                    )
                  }
                },
                modifier = Modifier.size(32.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.Refresh,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(18.dp),
                )
              }
            }

            // Campo de Busca de Rede Wi-Fi Conhecida / Pareada + Menu Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
              OutlinedTextField(
                value = wifiSearchQuery,
                onValueChange = { wifiSearchQuery = it },
                placeholder = {
                  Text(
                    if (wifiSsid.isNotBlank()) {
                      "Selecionada: $wifiSsid (digite para buscar outra)"
                    } else {
                      "Buscar rede Wi-Fi conhecida..."
                    },
                  )
                },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                  )
                },
                trailingIcon = {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    if (wifiSearchQuery.isNotBlank() || wifiSsid.isNotBlank()) {
                      IconButton(
                        onClick = {
                          if (wifiSearchQuery.isNotBlank()) {
                            wifiSearchQuery = ""
                          } else {
                            wifiSsid = ""
                            notifyAutoSaveIfEditing()
                          }
                        },
                      ) {
                        Icon(
                          imageVector = Icons.Default.Close,
                          contentDescription = null,
                          modifier = Modifier.size(18.dp),
                        )
                      }
                    }
                    IconButton(onClick = { showWifiDropdown = !showWifiDropdown }) {
                      Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                      )
                    }
                  }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(),
              )

              DropdownMenu(
                expanded = showWifiDropdown,
                onDismissRequest = { showWifiDropdown = false },
              ) {
                DropdownMenuItem(
                  text = { Text("Nenhuma (Usar apenas GPS)") },
                  onClick = {
                    wifiSsid = ""
                    wifiSearchQuery = ""
                    showWifiDropdown = false
                    notifyAutoSaveIfEditing()
                  },
                )
                HorizontalDivider()
                filteredWifiSsids.forEach { candidateSsid ->
                  val isCurrentConnected = connectedWifiSsid != null &&
                    connectedWifiSsid.equals(candidateSsid, ignoreCase = true)
                  DropdownMenuItem(
                    text = {
                      Text(
                        text = if (isCurrentConnected) {
                          "$candidateSsid (Conectada agora)"
                        } else {
                          candidateSsid
                        },
                        fontWeight = if (wifiSsid.equals(candidateSsid, ignoreCase = true)) {
                          FontWeight.Bold
                        } else {
                          FontWeight.Normal
                        },
                      )
                    },
                    leadingIcon = {
                      Icon(
                        imageVector = Icons.Default.Wifi,
                        contentDescription = null,
                        tint = if (isCurrentConnected) {
                          MaterialTheme.colorScheme.primary
                        } else {
                          MaterialTheme.colorScheme.onSurfaceVariant
                        },
                      )
                    },
                    onClick = {
                      wifiSsid = candidateSsid
                      wifiSearchQuery = ""
                      showWifiDropdown = false
                      notifyAutoSaveIfEditing()
                    },
                  )
                }
              }
            }

            // Chips rápidos das redes conhecidas / pareadas / filtradas pela busca
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(8.dp),
              verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              val trimmedQuery = wifiSearchQuery.trim()
              if (
                trimmedQuery.isNotEmpty() &&
                availableWifiSsids.none { it.equals(trimmedQuery, ignoreCase = true) }
              ) {
                FilterChip(
                  selected = wifiSsid.equals(trimmedQuery, ignoreCase = true),
                  onClick = {
                    wifiSsid = trimmedQuery
                    wifiSearchQuery = ""
                    notifyAutoSaveIfEditing()
                  },
                  leadingIcon = {
                    Icon(
                      imageVector = Icons.Default.Wifi,
                      contentDescription = null,
                      modifier = Modifier.size(15.dp),
                    )
                  },
                  label = {
                    Text(
                      text = "Usar \"$trimmedQuery\"",
                      maxLines = 1,
                      softWrap = false,
                    )
                  },
                )
              }

              filteredWifiSsids.forEach { candidateSsid ->
                val selected = wifiSsid.trim().equals(candidateSsid, ignoreCase = true)
                val isCurrentConnected = connectedWifiSsid != null &&
                  connectedWifiSsid.equals(candidateSsid, ignoreCase = true)
                FilterChip(
                  selected = selected,
                  onClick = {
                    wifiSsid = if (selected) "" else candidateSsid
                    wifiSearchQuery = ""
                    notifyAutoSaveIfEditing()
                  },
                  leadingIcon = {
                    Icon(
                      imageVector = if (selected) Icons.Default.Check else Icons.Default.Wifi,
                      contentDescription = null,
                      modifier = Modifier.size(15.dp),
                    )
                  },
                  label = {
                    Text(
                      text = if (isCurrentConnected) {
                        "$candidateSsid • Conectada"
                      } else {
                        candidateSsid
                      },
                      maxLines = 1,
                      softWrap = false,
                    )
                  },
                )
              }
            }
          }
        }

        Text(
          text = AppLabels.Locations.targetProfileSectionTitle,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          profiles.forEach { profile ->
            FilterChip(
              selected = selectedProfileId == profile.id,
              onClick = {
                selectedProfileId = profile.id
                notifyAutoSaveIfEditing()
              },
              leadingIcon = {
                Icon(
                  imageVector = profileIconForKey(profile.iconKey),
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
              },
              label = { Text(profile.name) },
            )
          }
        }

        if (initialRule != null && onTogglePresence != null) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
          OutlinedButton(
            onClick = {
              onTogglePresence(initialRule.id, !initialRule.isCurrentlyInside)
              onDismiss()
            },
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag(AppTestTags.Locations.togglePresenceButton(initialRule.id)),
          ) {
            Icon(
              imageVector = Icons.Default.MyLocation,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              if (initialRule.isCurrentlyInside) {
                AppLabels.Locations.markOutsideMenu
              } else {
                AppLabels.Locations.testPresenceButton
              },
              fontWeight = FontWeight.SemiBold,
            )
          }
        }

        if (!isEditing) {
          Button(
            onClick = { onConfirm(buildRule()) },
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp),
          ) {
            Text(AppLabels.Locations.createLocationButton, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}
