package com.managesoundmode.app.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.managesoundmode.app.domain.model.LocationRule
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.ui.components.M3CheckSwitch
import com.managesoundmode.app.ui.components.groupedCardShape
import com.managesoundmode.app.ui.components.profileIconForKey
import com.managesoundmode.app.ui.theme.onTonalContainerColor
import com.managesoundmode.app.ui.theme.tonalContainerColor
import kotlin.math.roundToInt

@Composable
internal fun LocationsTabContent(
  locationRules: List<LocationRule>,
  profiles: List<SoundProfile>,
  onToggleRule: (LocationRule, Boolean) -> Unit,
  onEditRule: (LocationRule) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .testTag("locations_tab_list"),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
    verticalArrangement = Arrangement.spacedBy(3.dp),
  ) {
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
      )
    }
  }
}

@Composable
private fun LocationRuleCard(
  rule: LocationRule,
  targetProfile: SoundProfile?,
  shape: RoundedCornerShape,
  onClick: () -> Unit,
  onToggleEnabled: (Boolean) -> Unit,
) {
  val containerColor = when {
    rule.enabled && rule.isCurrentlyInside -> MaterialTheme.colorScheme.secondaryContainer
    rule.enabled -> MaterialTheme.colorScheme.surfaceContainerHigh
    else -> MaterialTheme.colorScheme.surfaceContainerLow
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(shape)
      .clickable(onClick = onClick)
      .testTag("location_card_${rule.id}"),
    shape = shape,
    colors = CardDefaults.cardColors(containerColor = containerColor),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(14.dp),
          modifier = Modifier.weight(1f),
        ) {
          Box(
            modifier = Modifier
              .size(44.dp)
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
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                text = rule.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (rule.enabled) {
                  MaterialTheme.colorScheme.onSurface
                } else {
                  MaterialTheme.colorScheme.onSurfaceVariant
                },
              )
              if (rule.isCurrentlyInside) {
                Surface(
                  shape = RoundedCornerShape(50),
                  color = MaterialTheme.colorScheme.primary,
                ) {
                  Text(
                    text = "No local",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                  )
                }
              }
            }

            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text = "Raio ${rule.radiusMeters}m",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              if (rule.wifiSsid.isNotBlank()) {
                Text(
                  text = "•",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                  imageVector = Icons.Default.Wifi,
                  contentDescription = null,
                  modifier = Modifier.size(14.dp),
                  tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                  text = rule.wifiSsid,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.primary,
                )
              }
            }
          }
        }

        M3CheckSwitch(
          checked = rule.enabled,
          onCheckedChange = onToggleEnabled,
          modifier = Modifier.testTag("location_switch_${rule.id}"),
        )
      }

      if (targetProfile != null) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Surface(
            shape = RoundedCornerShape(50),
            color = if (rule.enabled) {
              targetProfile.mode.tonalContainerColor()
            } else {
              MaterialTheme.colorScheme.surfaceContainerHighest
            },
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
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
  onTogglePresence: ((String, Boolean) -> Unit)? = null,
  onDelete: ((String) -> Unit)? = null,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var name by rememberSaveable { mutableStateOf(initialRule?.name ?: "") }
  var wifiSsid by rememberSaveable { mutableStateOf(initialRule?.wifiSsid ?: "") }
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

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    sheetState = sheetState,
    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(start = 24.dp, end = 24.dp, bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = if (initialRule == null) "Novo Local" else "Editar Local",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.SemiBold,
        )
        if (initialRule != null && onDelete != null) {
          TextButton(onClick = { onDelete(initialRule.id) }) {
            Icon(
              imageVector = Icons.Default.DeleteOutline,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Excluir")
          }
        }
      }

      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Nome do local (ex.: Escritório, Faculdade)") },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("location_name_input"),
      )

      OutlinedTextField(
        value = wifiSsid,
        onValueChange = { wifiSsid = it },
        label = { Text("Rede Wi-Fi opcional (confirmação indoor)") },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth(),
      )

      Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text(
            text = "Raio do Perímetro (Geofence)",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(
            text = "${radius.roundToInt()} metros",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
          )
        }
        Slider(
          value = radius,
          onValueChange = { radius = it },
          valueRange = 150f..500f,
        )
      }

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
            onClick = { selectedProfileId = profile.id },
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

      Button(
        onClick = {
          val resolvedName = name.trim().ifEmpty { "Local Personalizado" }
          onConfirm(
            LocationRule(
              id = initialRule?.id ?: "loc_${System.currentTimeMillis()}",
              name = resolvedName,
              latitude = initialRule?.latitude ?: -23.5614,
              longitude = initialRule?.longitude ?: -46.6559,
              radiusMeters = radius.roundToInt().coerceIn(150, 500),
              wifiSsid = wifiSsid.trim(),
              targetProfileId = selectedProfileId,
              enabled = initialRule?.enabled ?: true,
              isCurrentlyInside = initialRule?.isCurrentlyInside ?: false,
            ),
          )
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("confirm_save_location_button"),
      ) {
        Text("Salvar Local")
      }
    }
  }
}
