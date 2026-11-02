package com.managesoundmode.app.ui.main

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.managesoundmode.app.domain.model.ScheduleRule
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.domain.model.Weekday
import com.managesoundmode.app.ui.components.M3CheckSwitch
import com.managesoundmode.app.ui.components.groupedCardShape
import com.managesoundmode.app.ui.theme.accentColor

@Composable
internal fun SchedulesTabContent(
  scheduleRules: List<ScheduleRule>,
  profiles: List<SoundProfile>,
  canScheduleExactAlarms: Boolean,
  onToggleRule: (ScheduleRule, Boolean) -> Unit,
  onToggleWeekday: (ScheduleRule, Weekday) -> Unit,
  onSelectProfile: (ScheduleRule, String) -> Unit,
  onToggleHighPriority: (ScheduleRule, Boolean) -> Unit,
  onEditRule: (ScheduleRule) -> Unit,
  onDeleteRule: (String) -> Unit,
  onRequestExactAlarmPermission: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var expandedRuleId by rememberSaveable { mutableStateOf<String?>(scheduleRules.firstOrNull()?.id) }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .testTag("schedules_tab_list"),
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
          text = "Alarme de Modos e Volumes",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
          text = "Agende janelas para silenciar à noite ou forçar volume de toque alto no horário comercial.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    if (!canScheduleExactAlarms) {
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
          shape = RoundedCornerShape(20.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
          ),
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
              )
              Text(
                text = "Ative Alarmes Exatos para Pontualidade",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
              )
            }
            Text(
              text = "Garante que os modos mudem exatamente no minuto agendado mesmo com a tela apagada (Doze Mode).",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onErrorContainer,
            )
            Button(onClick = onRequestExactAlarmPermission) {
              Text("Autorizar Alarmes Exatos")
            }
          }
        }
      }
    }

    itemsIndexed(
      items = scheduleRules,
      key = { _, rule -> rule.id },
    ) { index, rule ->
      val isExpanded = expandedRuleId == rule.id
      val targetProfile = profiles.firstOrNull { it.id == rule.targetProfileId }
        ?: profiles.firstOrNull()

      GoogleClockScheduleCard(
        rule = rule,
        targetProfile = targetProfile,
        profiles = profiles,
        isExpanded = isExpanded,
        shape = groupedCardShape(index = index, totalCount = scheduleRules.size),
        onToggleExpand = {
          expandedRuleId = if (isExpanded) null else rule.id
        },
        onToggleEnabled = { enabled -> onToggleRule(rule, enabled) },
        onToggleWeekday = { day -> onToggleWeekday(rule, day) },
        onSelectProfile = { profileId -> onSelectProfile(rule, profileId) },
        onToggleHighPriority = { hp -> onToggleHighPriority(rule, hp) },
        onEditClick = { onEditRule(rule) },
        onDeleteClick = { onDeleteRule(rule.id) },
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoogleClockScheduleCard(
  rule: ScheduleRule,
  targetProfile: SoundProfile?,
  profiles: List<SoundProfile>,
  isExpanded: Boolean,
  shape: RoundedCornerShape,
  onToggleExpand: () -> Unit,
  onToggleEnabled: (Boolean) -> Unit,
  onToggleWeekday: (Weekday) -> Unit,
  onSelectProfile: (String) -> Unit,
  onToggleHighPriority: (Boolean) -> Unit,
  onEditClick: () -> Unit,
  onDeleteClick: () -> Unit,
) {
  val haptics = LocalHapticFeedback.current
  val containerColor = if (rule.enabled) {
    MaterialTheme.colorScheme.surfaceContainerHigh
  } else {
    MaterialTheme.colorScheme.surfaceContainerLow
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .animateContentSize()
      .clip(shape)
      .clickable(onClick = onToggleExpand)
      .testTag("schedule_card_${rule.id}"),
    shape = shape,
    colors = CardDefaults.cardColors(containerColor = containerColor),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Icon(
            imageVector = Icons.Default.AccessTime,
            contentDescription = null,
            tint = if (rule.enabled) {
              MaterialTheme.colorScheme.primary
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(18.dp),
          )
          Text(
            text = rule.name,
            style = MaterialTheme.typography.titleMedium,
            color = if (rule.enabled) {
              MaterialTheme.colorScheme.onSurface
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant
            },
          )
          if (rule.highPriority) {
            Surface(
              shape = RoundedCornerShape(50),
              color = MaterialTheme.colorScheme.tertiaryContainer,
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.PriorityHigh,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onTertiaryContainer,
                  modifier = Modifier.size(12.dp),
                )
                Text(
                  text = "Prioridade",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
              }
            }
          }
        }

        Icon(
          imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }

      // Linha Principal estilo Google Clock: Horário Display Grande + Switch M3 com Check
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            text = rule.formattedWindow(),
            style = MaterialTheme.typography.displaySmall,
            fontWeight = if (rule.enabled) FontWeight.SemiBold else FontWeight.Light,
            color = if (rule.enabled) {
              MaterialTheme.colorScheme.onSurface
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            },
          )
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = rule.formattedDaysSummary(),
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (targetProfile != null) {
              val accent = targetProfile.mode.accentColor()
              Surface(
                shape = RoundedCornerShape(8.dp),
                color = accent.copy(alpha = 0.18f),
              ) {
                Text(
                  text = "${targetProfile.name} (${targetProfile.mode.title})",
                  style = MaterialTheme.typography.labelMedium,
                  color = accent,
                  modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                )
              }
            }
          }
        }

        M3CheckSwitch(
          checked = rule.enabled,
          onCheckedChange = onToggleEnabled,
          modifier = Modifier.testTag("schedule_switch_${rule.id}"),
        )
      }

      if (isExpanded) {
        HorizontalDivider(
          color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
          modifier = Modifier.padding(vertical = 4.dp),
        )

        // Seletor circular de 7 dias da semana (D S T Q Q S S) estilo Google Clock
        Text(
          text = "Dias da semana",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Weekday.entries.forEach { day ->
            val selected = day in rule.daysOfWeek
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(
                  if (selected) {
                    MaterialTheme.colorScheme.primary
                  } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                  },
                )
                .clickable {
                  haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                  onToggleWeekday(day)
                }
                .testTag("weekday_${rule.id}_${day.name}"),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = day.shortLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) {
                  MaterialTheme.colorScheme.onPrimary
                } else {
                  MaterialTheme.colorScheme.onSurfaceVariant
                },
              )
            }
          }
        }

        // Escolha rápida de Perfil para esta janela de horário
        Text(
          text = "Perfil aplicado neste período",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          profiles.forEach { profile ->
            val isSelected = profile.id == rule.targetProfileId
            FilterChip(
              selected = isSelected,
              onClick = { onSelectProfile(profile.id) },
              label = { Text(profile.name) },
            )
          }
        }

        // Chave "Sobrepor outras regras (Alta Prioridade)" (Estudo 03 Seção 3)
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Sobrepor outras regras (Alta Prioridade)",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Medium,
            )
            Text(
              text = "Garante que este horário vença conflitos mesmo se for para forçar Volume Alto.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          M3CheckSwitch(
            checked = rule.highPriority,
            onCheckedChange = onToggleHighPriority,
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          TextButton(onClick = onDeleteClick) {
            Icon(
              imageVector = Icons.Default.DeleteOutline,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Excluir")
          }
          Spacer(modifier = Modifier.width(8.dp))
          OutlinedButton(onClick = onEditClick) {
            Icon(
              imageVector = Icons.Default.Edit,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("Editar Horário")
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AddOrEditScheduleDialog(
  initialRule: ScheduleRule?,
  profiles: List<SoundProfile>,
  onConfirm: (ScheduleRule) -> Unit,
  onDismiss: () -> Unit,
) {
  var name by rememberSaveable { mutableStateOf(initialRule?.name ?: "") }
  var startHour by rememberSaveable { mutableIntStateOf(initialRule?.startHour ?: 22) }
  var startMinute by rememberSaveable { mutableIntStateOf(initialRule?.startMinute ?: 0) }
  var endHour by rememberSaveable { mutableIntStateOf(initialRule?.endHour ?: 7) }
  var endMinute by rememberSaveable { mutableIntStateOf(initialRule?.endMinute ?: 0) }
  var selectedProfileId by rememberSaveable {
    mutableStateOf(
      initialRule?.targetProfileId ?: profiles.firstOrNull()?.id ?: SoundProfile.DEFAULT_DAY_PROFILE_ID,
    )
  }
  var highPriority by rememberSaveable { mutableStateOf(initialRule?.highPriority ?: false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(if (initialRule == null) "Novo Horário Agendado" else "Editar Horário")
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Nome da rotina (ex.: Dormir, Trabalho)") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("schedule_name_input"),
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedTextField(
            value = startHour.toString(),
            onValueChange = { value ->
              value.toIntOrNull()?.let { if (it in 0..23) startHour = it }
            },
            label = { Text("Início (Hora)") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = startMinute.toString(),
            onValueChange = { value ->
              value.toIntOrNull()?.let { if (it in 0..59) startMinute = it }
            },
            label = { Text("Min") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          OutlinedTextField(
            value = endHour.toString(),
            onValueChange = { value ->
              value.toIntOrNull()?.let { if (it in 0..23) endHour = it }
            },
            label = { Text("Fim (Hora)") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
          OutlinedTextField(
            value = endMinute.toString(),
            onValueChange = { value ->
              value.toIntOrNull()?.let { if (it in 0..59) endMinute = it }
            },
            label = { Text("Min") },
            singleLine = true,
            modifier = Modifier.weight(1f),
          )
        }

        Text(
          text = "Perfil de Som:",
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
          val resolvedName = name.trim().ifEmpty { "Rotina de Horário" }
          val rule = ScheduleRule(
            id = initialRule?.id ?: "sched_${System.currentTimeMillis()}",
            name = resolvedName,
            startHour = startHour.coerceIn(0, 23),
            startMinute = startMinute.coerceIn(0, 59),
            endHour = endHour.coerceIn(0, 23),
            endMinute = endMinute.coerceIn(0, 59),
            daysOfWeek = initialRule?.daysOfWeek ?: Weekday.weekdays,
            targetProfileId = selectedProfileId,
            enabled = initialRule?.enabled ?: true,
            highPriority = highPriority,
          )
          onConfirm(rule)
        },
        modifier = Modifier.testTag("confirm_save_schedule_button"),
      ) {
        Text("Salvar")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancelar")
      }
    },
  )
}
