package com.managesoundmode.app.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.managesoundmode.app.domain.model.LocationRule
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.ui.components.M3CheckSwitch
import com.managesoundmode.app.ui.components.groupedCardShape
import com.managesoundmode.app.ui.theme.accentColor
import kotlin.math.roundToInt

@Composable
internal fun LocationsTabContent(
  locationRules: List<LocationRule>,
  profiles: List<SoundProfile>,
  onToggleRule: (LocationRule, Boolean) -> Unit,
  onTogglePresence: (String, Boolean) -> Unit,
  onDeleteRule: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .testTag("locations_tab_list"),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    item {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Text(
          text = "Locais Automáticos (Geofence & Wi-Fi)",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
          text = "Ativa perfis ao permanecer no local por 2 min (DWELL) ou conectar ao Wi-Fi, e restaura ao sair do raio.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
        onToggleEnabled = { enabled -> onToggleRule(rule, enabled) },
        onTogglePresence = { inside -> onTogglePresence(rule.id, inside) },
        onDelete = { onDeleteRule(rule.id) },
      )
    }
  }
}

@Composable
private fun LocationRuleCard(
  rule: LocationRule,
  targetProfile: SoundProfile?,
  shape: RoundedCornerShape,
  onToggleEnabled: (Boolean) -> Unit,
  onTogglePresence: (Boolean) -> Unit,
  onDelete: () -> Unit,
) {
  val containerColor = when {
    rule.enabled && rule.isCurrentlyInside -> MaterialTheme.colorScheme.secondaryContainer
    rule.enabled -> MaterialTheme.colorScheme.surfaceContainerHigh
    else -> MaterialTheme.colorScheme.surfaceContainerLow
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("location_card_${rule.id}"),
    shape = shape,
    colors = CardDefaults.cardColors(containerColor = containerColor),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          modifier = Modifier.weight(1f),
        ) {
          Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Column {
            Text(
              text = rule.name,
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.SemiBold,
            )
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Text(
                text = "Raio de ${rule.radiusMeters}m",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
              if (rule.wifiSsid.isNotBlank()) {
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
        val accent = targetProfile.mode.accentColor()
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = accent.copy(alpha = 0.18f),
        ) {
          Text(
            text = "Ao entrar: ${targetProfile.name} (${targetProfile.mode.title} • Toque ${targetProfile.streams.ringPercent}%, Mídia ${targetProfile.streams.mediaPercent}%)",
            style = MaterialTheme.typography.labelMedium,
            color = accent,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
          )
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        AssistChip(
          onClick = { onTogglePresence(!rule.isCurrentlyInside) },
          leadingIcon = {
            Icon(
              imageVector = Icons.Default.MyLocation,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
          },
          label = {
            Text(
              if (rule.isCurrentlyInside) "No local agora (Toque p/ sair)" else "Simular chegada no local",
            )
          },
          colors = AssistChipDefaults.assistChipColors(
            containerColor = if (rule.isCurrentlyInside) {
              MaterialTheme.colorScheme.primaryContainer
            } else {
              MaterialTheme.colorScheme.surfaceContainerHighest
            },
          ),
          modifier = Modifier.testTag("toggle_location_presence_${rule.id}"),
        )

        TextButton(onClick = onDelete) {
          Icon(
            imageVector = Icons.Default.DeleteOutline,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text("Remover")
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AddLocationRuleDialog(
  profiles: List<SoundProfile>,
  onConfirm: (LocationRule) -> Unit,
  onDismiss: () -> Unit,
) {
  var name by rememberSaveable { mutableStateOf("") }
  var wifiSsid by rememberSaveable { mutableStateOf("") }
  var radius by rememberSaveable { mutableFloatStateOf(150f) }
  var selectedProfileId by rememberSaveable {
    mutableStateOf(profiles.firstOrNull { it.id == "preset_work_high" }?.id ?: profiles.first().id)
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Novo Local Automático") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Nome do local (ex.: Escritório, Academia)") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("location_name_input"),
        )
        OutlinedTextField(
          value = wifiSsid,
          onValueChange = { wifiSsid = it },
          label = { Text("Rede Wi-Fi opcional (ex.: Office-5G)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )

        Text(
          text = "Raio do Geofence: ${radius.roundToInt()} metros (mín. 150m recomendado)",
          style = MaterialTheme.typography.labelLarge,
        )
        Slider(
          value = radius,
          onValueChange = { radius = it },
          valueRange = 150f..500f,
        )

        Text(
          text = "Perfil ao permanecer no local:",
          style = MaterialTheme.typography.labelLarge,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          profiles.forEach { profile ->
            FilterChip(
              selected = selectedProfileId == profile.id,
              onClick = { selectedProfileId = profile.id },
              label = { Text(profile.name) },
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          val resolvedName = name.trim().ifEmpty { "Local Personalizado" }
          onConfirm(
            LocationRule(
              id = "loc_${System.currentTimeMillis()}",
              name = resolvedName,
              latitude = -23.5614,
              longitude = -46.6559,
              radiusMeters = radius.roundToInt().coerceIn(150, 500),
              wifiSsid = wifiSsid.trim(),
              targetProfileId = selectedProfileId,
              enabled = true,
              isCurrentlyInside = false,
            ),
          )
        },
        modifier = Modifier.testTag("confirm_save_location_button"),
      ) {
        Text("Salvar Local")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancelar")
      }
    },
  )
}
