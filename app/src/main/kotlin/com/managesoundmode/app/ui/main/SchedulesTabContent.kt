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
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.managesoundmode.app.ui.components.profileIconForKey
import com.managesoundmode.app.ui.theme.onTonalContainerColor
import com.managesoundmode.app.ui.theme.tonalContainerColor
import java.util.Locale

@Composable
internal fun SchedulesTabContent(
  scheduleRules: List<ScheduleRule>,
  profiles: List<SoundProfile>,
  canScheduleExactAlarms: Boolean,
  onToggleRule: (ScheduleRule, Boolean) -> Unit,
  onEditRule: (ScheduleRule) -> Unit,
  onRequestExactAlarmPermission: () -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .testTag("schedules_tab_list"),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
    verticalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    if (!canScheduleExactAlarms) {
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
          ),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              modifier = Modifier.weight(1f),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
              Icon(
                imageVector = Icons.Default.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
              )
              Text(
                text = "Autorize alarmes exatos para pontualidade",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
              )
            }
            TextButton(onClick = onRequestExactAlarmPermission) {
              Text("Autorizar")
            }
          }
        }
      }
    }

    itemsIndexed(
      items = scheduleRules,
      key = { _, rule -> rule.id },
    ) { index, rule ->
      val targetProfile = profiles.firstOrNull { it.id == rule.targetProfileId }
        ?: profiles.firstOrNull()

      GoogleClockScheduleCard(
        rule = rule,
        targetProfile = targetProfile,
        shape = groupedCardShape(index = index, totalCount = scheduleRules.size),
        onClick = { onEditRule(rule) },
        onToggleEnabled = { enabled -> onToggleRule(rule, enabled) },
      )
    }
  }
}

@Composable
private fun GoogleClockScheduleCard(
  rule: ScheduleRule,
  targetProfile: SoundProfile?,
  shape: RoundedCornerShape,
  onClick: () -> Unit,
  onToggleEnabled: (Boolean) -> Unit,
) {
  val containerColor = if (rule.enabled) {
    MaterialTheme.colorScheme.surfaceContainerHigh
  } else {
    MaterialTheme.colorScheme.surfaceContainerLow
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(shape)
      .clickable(onClick = onClick)
      .testTag("schedule_card_${rule.id}"),
    shape = shape,
    colors = CardDefaults.cardColors(containerColor = containerColor),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 18.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      // Linha 1: Nome da Rotina + Badge Tonal do Perfil
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
            imageVector = targetProfile?.let { profileIconForKey(it.iconKey) } ?: Icons.Default.AccessTime,
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
            fontWeight = FontWeight.Medium,
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
              ) {
                Icon(
                  imageVector = Icons.Default.PriorityHigh,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onTertiaryContainer,
                  modifier = Modifier.size(12.dp),
                )
                Text(
                  text = "Prioritário",
                  style = MaterialTheme.typography.labelSmall,
                  color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
              }
            }
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
            Text(
              text = "${targetProfile.name} • ${targetProfile.mode.title}",
              style = MaterialTheme.typography.labelMedium,
              color = if (rule.enabled) {
                targetProfile.mode.onTonalContainerColor()
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
        }
      }

      // Linha 2: Horário Grande Estilo Google Clock + Switch M3
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = rule.formattedWindow(),
          style = MaterialTheme.typography.displaySmall,
          fontWeight = if (rule.enabled) FontWeight.SemiBold else FontWeight.Light,
          color = if (rule.enabled) {
            MaterialTheme.colorScheme.onSurface
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
          },
        )

        M3CheckSwitch(
          checked = rule.enabled,
          onCheckedChange = onToggleEnabled,
          modifier = Modifier.testTag("schedule_switch_${rule.id}"),
        )
      }

      // Linha 3: Resumo dos Dias da Semana + Resumo de Volume
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = rule.formattedDaysSummary(),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (targetProfile != null) {
          Text(
            text = targetProfile.summaryLabel(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
    }
  }
}

/**
 * BottomSheet nativo para criar ou editar uma rotina de horário com `TimePicker` oficial do Material 3.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun ScheduleEditorBottomSheet(
  initialRule: ScheduleRule?,
  profiles: List<SoundProfile>,
  onConfirm: (ScheduleRule) -> Unit,
  onDelete: ((String) -> Unit)? = null,
  onEditSelectedProfile: ((SoundProfile) -> Unit)? = null,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  val haptics = LocalHapticFeedback.current

  var name by rememberSaveable { mutableStateOf(initialRule?.name ?: "") }
  var startHour by rememberSaveable { mutableIntStateOf(initialRule?.startHour ?: 22) }
  var startMinute by rememberSaveable { mutableIntStateOf(initialRule?.startMinute ?: 30) }
  var endHour by rememberSaveable { mutableIntStateOf(initialRule?.endHour ?: 7) }
  var endMinute by rememberSaveable { mutableIntStateOf(initialRule?.endMinute ?: 0) }
  var selectedDays by remember {
    mutableStateOf(initialRule?.daysOfWeek ?: Weekday.weekdays)
  }
  var selectedProfileId by rememberSaveable {
    mutableStateOf(
      initialRule?.targetProfileId ?: profiles.firstOrNull()?.id ?: SoundProfile.DEFAULT_DAY_PROFILE_ID,
    )
  }
  var highPriority by rememberSaveable { mutableStateOf(initialRule?.highPriority ?: false) }

  // Estado para abrir o TimePicker nativo do Material 3 ("start" ou "end" ou null)
  var activeTimePickerTarget by rememberSaveable { mutableStateOf<String?>(null) }

  if (activeTimePickerTarget != null) {
    val isStart = activeTimePickerTarget == "start"
    val timePickerState = rememberTimePickerState(
      initialHour = if (isStart) startHour else endHour,
      initialMinute = if (isStart) startMinute else endMinute,
      is24Hour = true,
    )
    AlertDialog(
      onDismissRequest = { activeTimePickerTarget = null },
      title = {
        Text(if (isStart) "Horário de Início" else "Horário de Término")
      },
      text = {
        Box(
          modifier = Modifier.fillMaxWidth(),
          contentAlignment = Alignment.Center,
        ) {
          TimePicker(state = timePickerState)
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (isStart) {
              startHour = timePickerState.hour
              startMinute = timePickerState.minute
            } else {
              endHour = timePickerState.hour
              endMinute = timePickerState.minute
            }
            activeTimePickerTarget = null
          },
        ) {
          Text("OK")
        }
      },
      dismissButton = {
        TextButton(onClick = { activeTimePickerTarget = null }) {
          Text("Cancelar")
        }
      },
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
          text = if (initialRule == null) "Novo Horário" else "Editar Horário",
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

      // Cartões grandes de Início e Fim que abrem o TimePicker nativo do Android
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        TimeBoxCard(
          label = "INÍCIO",
          timeText = String.format(Locale.ROOT, "%02d:%02d", startHour, startMinute),
          onClick = { activeTimePickerTarget = "start" },
          modifier = Modifier.weight(1f),
        )
        TimeBoxCard(
          label = "FIM",
          timeText = String.format(Locale.ROOT, "%02d:%02d", endHour, endMinute),
          onClick = { activeTimePickerTarget = "end" },
          modifier = Modifier.weight(1f),
        )
      }

      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Nome da rotina (ex.: Dormir, Trabalho)") },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("schedule_name_input"),
      )

      // Dias da semana (7 círculos estilo Google Clock)
      Text(
        text = "Repetir nos dias",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Weekday.entries.forEach { day ->
          val selected = day in selectedDays
          Box(
            modifier = Modifier
              .size(42.dp)
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
                selectedDays = if (selected && selectedDays.size > 1) {
                  selectedDays - day
                } else {
                  selectedDays + day
                }
              },
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

      // Perfil vinculado + botão de editar volumes do perfil selecionado
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Perfil de som aplicado",
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val currentSelectedProfile = profiles.firstOrNull { it.id == selectedProfileId }
        if (currentSelectedProfile != null && onEditSelectedProfile != null) {
          TextButton(onClick = { onEditSelectedProfile(currentSelectedProfile) }) {
            Icon(
              imageVector = Icons.Default.Edit,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text("Ajustar volumes de '${currentSelectedProfile.name}'")
          }
        }
      }

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        profiles.forEach { profile ->
          val selected = selectedProfileId == profile.id
          FilterChip(
            selected = selected,
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

      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Alta Prioridade (Sobrepor outros)",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
          )
          Text(
            text = "Vence conflitos mesmo quando configurado para Som Alto",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        M3CheckSwitch(
          checked = highPriority,
          onCheckedChange = { highPriority = it },
        )
      }

      Button(
        onClick = {
          val resolvedName = name.trim().ifEmpty { "Rotina Agendada" }
          onConfirm(
            ScheduleRule(
              id = initialRule?.id ?: "sched_${System.currentTimeMillis()}",
              name = resolvedName,
              startHour = startHour,
              startMinute = startMinute,
              endHour = endHour,
              endMinute = endMinute,
              daysOfWeek = selectedDays,
              targetProfileId = selectedProfileId,
              enabled = initialRule?.enabled ?: true,
              highPriority = highPriority,
            ),
          )
        },
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("confirm_save_schedule_button"),
      ) {
        Text("Salvar Horário")
      }
    }
  }
}

@Composable
private fun TimeBoxCard(
  label: String,
  timeText: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier
      .clip(RoundedCornerShape(20.dp))
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.secondaryContainer,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 14.dp, horizontal = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
      )
      Text(
        text = timeText,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSecondaryContainer,
      )
    }
  }
}
