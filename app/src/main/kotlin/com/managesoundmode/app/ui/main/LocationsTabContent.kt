package com.managesoundmode.app.ui.main

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
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
import com.managesoundmode.app.ui.components.GeofenceMapPickerCard
import com.managesoundmode.app.ui.components.M3CheckSwitch
import com.managesoundmode.app.ui.components.groupedCardShape
import com.managesoundmode.app.ui.components.profileIconForKey
import com.managesoundmode.app.ui.theme.onTonalContainerColor
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
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var isSyncingGps by remember { mutableStateOf(false) }

  fun triggerGpsSync() {
    if (isSyncingGps) return
    isSyncingGps = true
    scope.launch {
      val coords = LocationProximityHelper.fetchCurrentDeviceLocation(context)
      isSyncingGps = false
      if (coords != null) {
        onSyncGpsCoordinates(coords.latitude, coords.longitude)
      } else {
        onShowStatusMessage("Não foi possível ler o GPS agora. Verifique se a localização do aparelho está ativa.")
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
      onShowStatusMessage("Permissão de localização necessária para verificar cercas pelo GPS.")
    }
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .testTag("locations_tab_list"),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 104.dp),
    verticalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    // Card rápido de sincronização GPS no topo da aba Locais
    item {
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 8.dp)
          .testTag("sync_gps_locations_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
      ) {
        FlowRow(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Cercas Geográficas (OpenStreetMap)",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              text = "Toque em um local para ajustar o pino e o raio no mapa ou pressione e segure para excluir.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          FilledTonalButton(
            onClick = {
              if (LocationProximityHelper.hasLocationPermission(context)) {
                triggerGpsSync()
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
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            modifier = Modifier.testTag("verify_gps_proximity_button"),
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
              text = if (isSyncingGps) "Lendo GPS..." else "Verificar pelo GPS",
              style = MaterialTheme.typography.labelLarge,
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
    ) { index, rule ->
      val targetProfile = profiles.firstOrNull { it.id == rule.targetProfileId }
        ?: profiles.firstOrNull()

      LocationRuleCard(
        rule = rule,
        targetProfile = targetProfile,
        shape = groupedCardShape(index = index, totalCount = locationRules.size),
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

  val containerColor = when {
    rule.enabled && rule.isCurrentlyInside -> MaterialTheme.colorScheme.secondaryContainer
    rule.enabled -> MaterialTheme.colorScheme.surfaceContainerHigh
    else -> MaterialTheme.colorScheme.surfaceContainerLow
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
      .testTag("location_card_${rule.id}"),
    shape = shape,
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
              .size(42.dp)
              .clip(CircleShape)
              .background(
                if (rule.enabled) {
                  MaterialTheme.colorScheme.primaryContainer
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
                MaterialTheme.colorScheme.onPrimaryContainer
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
              fontWeight = FontWeight.SemiBold,
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
                text = "Raio ${rule.radiusMeters}m",
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
            modifier = Modifier.testTag("location_switch_${rule.id}"),
          )

          Box {
            IconButton(
              onClick = { showContextMenu = true },
              modifier = Modifier.size(34.dp),
            ) {
              Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Opções do local ${rule.name}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
              )
            }

            DropdownMenu(
              expanded = showContextMenu,
              onDismissRequest = { showContextMenu = false },
            ) {
              DropdownMenuItem(
                text = { Text("Editar local e mapa") },
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
              )
              DropdownMenuItem(
                text = {
                  Text(
                    if (rule.isCurrentlyInside) {
                      "Marcar como fora deste local"
                    } else {
                      "Simular presença neste local"
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
              )
              DropdownMenuItem(
                text = { Text(if (rule.enabled) "Desativar local" else "Ativar local") },
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
              )
              HorizontalDivider()
              DropdownMenuItem(
                text = {
                  Text(
                    text = "Excluir local",
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
              )
            }
          }
        }
      }

      // Badges responsivos em FlowRow (sem esmagamento vertical em zoom aumentado)
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
              text = "No local",
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

  fun buildRule(): LocationRule {
    val resolvedName = name.trim().ifEmpty { initialRule?.name ?: "Local Personalizado" }
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
                contentDescription = "Voltar",
              )
            }
          },
          title = {
            Column {
              Text(
                text = if (isEditing) "Editar Local" else "Novo Local",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              if (isEditing) {
                Text(
                  text = "Salvo automaticamente",
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
                modifier = Modifier.testTag("delete_location_top_button"),
              ) {
                Icon(
                  imageVector = Icons.Default.DeleteOutline,
                  contentDescription = "Excluir local",
                  tint = MaterialTheme.colorScheme.error,
                )
              }
            } else if (!isEditing) {
              TextButton(
                onClick = { onConfirm(buildRule()) },
                modifier = Modifier.testTag("confirm_save_location_button"),
              ) {
                Text("Criar", fontWeight = FontWeight.Bold)
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
          label = { Text("Nome do local (ex.: Escritório, Faculdade)") },
          singleLine = true,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag("location_name_input"),
        )

        // Mapa Interativo OpenStreetMap + Busca de Endereço + Botão GPS Atual
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

        // Controle do Raio do Perímetro (Geofence) sincronizado com o círculo no mapa
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Text(
              text = "Raio da Cerca Geográfica",
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = "${radius.roundToInt()} metros",
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
            modifier = Modifier.testTag("geofence_radius_slider"),
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

        OutlinedTextField(
          value = wifiSsid,
          onValueChange = {
            wifiSsid = it
            notifyAutoSaveIfEditing()
          },
          label = { Text("Rede Wi-Fi opcional (confirmação indoor)") },
          singleLine = true,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier.fillMaxWidth(),
        )

        Text(
          text = "Perfil ao permanecer no local",
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
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
          OutlinedButton(
            onClick = {
              onTogglePresence(initialRule.id, !initialRule.isCurrentlyInside)
              onDismiss()
            },
            modifier = Modifier
              .fillMaxWidth()
              .testTag("toggle_location_presence_${initialRule.id}"),
          ) {
            Icon(
              imageVector = Icons.Default.MyLocation,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              if (initialRule.isCurrentlyInside) {
                "Marcar como fora deste local"
              } else {
                "Testar presença neste local agora"
              },
            )
          }
        }

        if (!isEditing) {
          Button(
            onClick = { onConfirm(buildRule()) },
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp),
          ) {
            Text("Criar Local")
          }
        }
      }
    }
  }
}
