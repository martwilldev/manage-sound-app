package com.managesoundmode.app.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.managesoundmode.app.domain.model.ScheduleRule
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.domain.model.Weekday
import com.managesoundmode.app.ui.common.AppLabels
import com.managesoundmode.app.ui.common.AppTestTags
import com.managesoundmode.app.ui.components.M3CheckSwitch
import com.managesoundmode.app.ui.components.profileIconForKey
import com.managesoundmode.app.ui.theme.onActivePillColor
import com.managesoundmode.app.ui.theme.onTonalContainerColor
import com.managesoundmode.app.ui.theme.tonalAccentColor
import com.managesoundmode.app.ui.theme.tonalContainerColor
import java.util.Locale

@Composable
internal fun SchedulesTabContent(
  scheduleRules: List<ScheduleRule>,
  profiles: List<SoundProfile>,
  canScheduleExactAlarms: Boolean,
  onToggleRule: (ScheduleRule, Boolean) -> Unit,
  onEditRule: (ScheduleRule) -> Unit,
  onDeleteRule: (String) -> Unit = {},
  onDuplicateRule: (ScheduleRule) -> Unit = {},
  onRequestExactAlarmPermission: () -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .testTag(AppTestTags.Schedules.LIST),
    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 6.dp, bottom = 104.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    item {
      Text(
        text = AppLabels.Schedules.listHint,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 4.dp, vertical = 4.dp),
      )
    }

    if (!canScheduleExactAlarms) {
      item {
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
            .testTag(AppTestTags.Schedules.EXACT_ALARM_BANNER),
          shape = RoundedCornerShape(24.dp),
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
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
                text = AppLabels.Schedules.exactAlarmBannerTitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer,
              )
            }
            TextButton(
              onClick = onRequestExactAlarmPermission,
              modifier = Modifier.testTag(AppTestTags.Schedules.AUTHORIZE_EXACT_ALARM_BUTTON),
            ) {
              Text(AppLabels.Common.authorize, fontWeight = FontWeight.Bold)
            }
          }
        }
      }
    }

    itemsIndexed(
      items = scheduleRules,
      key = { _, rule -> rule.id },
    ) { _, rule ->
      val targetProfile = profiles.firstOrNull { it.id == rule.targetProfileId }
        ?: profiles.firstOrNull()

      GoogleClockScheduleCard(
        rule = rule,
        targetProfile = targetProfile,
        shape = RoundedCornerShape(24.dp),
        onClick = { onEditRule(rule) },
        onToggleEnabled = { enabled -> onToggleRule(rule, enabled) },
        onDuplicate = { onDuplicateRule(rule) },
        onDelete = { onDeleteRule(rule.id) },
      )
    }
  }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun GoogleClockScheduleCard(
  rule: ScheduleRule,
  targetProfile: SoundProfile?,
  shape: RoundedCornerShape,
  onClick: () -> Unit,
  onToggleEnabled: (Boolean) -> Unit,
  onDuplicate: () -> Unit,
  onDelete: () -> Unit,
) {
  val haptics = LocalHapticFeedback.current
  var showContextMenu by remember { mutableStateOf(false) }

  val modeAccent = targetProfile?.mode?.tonalAccentColor() ?: MaterialTheme.colorScheme.primary
  val containerColor = if (rule.enabled) {
    MaterialTheme.colorScheme.surfaceContainerLow
  } else {
    MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.68f)
  }
  val borderStroke = if (rule.enabled) {
    BorderStroke(1.dp, modeAccent.copy(alpha = 0.38f))
  } else {
    BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
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
      .testTag(AppTestTags.Schedules.card(rule.id)),
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
      // Linha 1: Avatar Circular + Nome da Rotina + Menu (⋮)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Box(
            modifier = Modifier
              .size(34.dp)
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
              imageVector = targetProfile?.let { profileIconForKey(it.iconKey) } ?: Icons.Default.AccessTime,
              contentDescription = null,
              tint = if (rule.enabled) {
                modeAccent
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
              modifier = Modifier.size(18.dp),
            )
          }
          Text(
            text = rule.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = if (rule.enabled) {
              MaterialTheme.colorScheme.onSurface
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant
            },
          )
        }

        Box {
          IconButton(
            onClick = { showContextMenu = true },
            modifier = Modifier
              .size(34.dp)
              .testTag(AppTestTags.Schedules.optionsButton(rule.id)),
          ) {
            Icon(
              imageVector = Icons.Default.MoreVert,
              contentDescription = AppLabels.Schedules.optionsContentDescription(rule.name),
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(18.dp),
            )
          }

          DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
          ) {
            DropdownMenuItem(
              text = { Text(AppLabels.Schedules.editScheduleMenu) },
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
              modifier = Modifier.testTag(AppTestTags.Schedules.menuEditItem(rule.id)),
            )
            DropdownMenuItem(
              text = { Text(AppLabels.Schedules.duplicateScheduleMenu) },
              leadingIcon = {
                Icon(
                  imageVector = Icons.Default.ContentCopy,
                  contentDescription = null,
                )
              },
              onClick = {
                showContextMenu = false
                onDuplicate()
              },
              modifier = Modifier.testTag(AppTestTags.Schedules.menuDuplicateItem(rule.id)),
            )
            DropdownMenuItem(
              text = {
                Text(
                  if (rule.enabled) {
                    AppLabels.Schedules.disableScheduleMenu
                  } else {
                    AppLabels.Schedules.enableScheduleMenu
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
              modifier = Modifier.testTag(AppTestTags.Schedules.menuToggleItem(rule.id)),
            )
            HorizontalDivider()
            DropdownMenuItem(
              text = {
                Text(
                  text = AppLabels.Schedules.deleteScheduleMenu,
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
              modifier = Modifier.testTag(AppTestTags.Schedules.menuDeleteItem(rule.id)),
            )
          }
        }
      }

      // Linha 2: Horário Tabular ZENITH + Switch
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = rule.formattedWindow(),
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = if (rule.enabled) FontWeight.Bold else FontWeight.Light,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          modifier = Modifier
            .weight(1f)
            .padding(end = 8.dp),
          color = if (rule.enabled) {
            MaterialTheme.colorScheme.onSurface
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
          },
        )

        M3CheckSwitch(
          checked = rule.enabled,
          onCheckedChange = onToggleEnabled,
          modifier = Modifier.testTag(AppTestTags.Schedules.switch(rule.id)),
        )
      }

      // Linha 3: Badges Semânticos + Resumo dos Dias da Semana
      FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
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
            Text(
              text = "${targetProfile.name} • ${targetProfile.mode.title}",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              softWrap = false,
              color = if (rule.enabled) {
                targetProfile.mode.onTonalContainerColor()
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
        }

        if (rule.highPriority) {
          Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.tertiaryContainer,
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = Icons.Default.PriorityHigh,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(12.dp),
              )
              Text(
                text = AppLabels.Schedules.highPriorityBadge,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
              )
            }
          }
        }

        Text(
          text = rule.formattedDaysSummary(),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.align(Alignment.CenterVertically),
        )
      }
    }
  }
}

/**
 * Tela dedicada Full-Screen ZENITH para criar ou editar uma rotina de horário.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun ScheduleEditorBottomSheet(
  initialRule: ScheduleRule?,
  profiles: List<SoundProfile>,
  onConfirm: (ScheduleRule) -> Unit,
  onAutoSave: (ScheduleRule) -> Unit = {},
  onDelete: ((String) -> Unit)? = null,
  onDismiss: () -> Unit,
) {
  val isEditing = initialRule != null
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

  var activeTimePickerTarget by rememberSaveable { mutableStateOf<String?>(null) }

  val selectedProfile = profiles.firstOrNull { it.id == selectedProfileId } ?: profiles.firstOrNull()
  val activeAccent = selectedProfile?.mode?.tonalAccentColor() ?: MaterialTheme.colorScheme.primary

  fun buildRule(): ScheduleRule {
    val resolvedName = name.trim().ifEmpty {
      initialRule?.name ?: AppLabels.Schedules.defaultScheduleName
    }
    return ScheduleRule(
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
    )
  }

  fun notifyAutoSaveIfEditing() {
    if (isEditing) {
      onAutoSave(buildRule())
    }
  }

  if (activeTimePickerTarget != null) {
    val isStart = activeTimePickerTarget == "start"
    val timePickerState = rememberTimePickerState(
      initialHour = if (isStart) startHour else endHour,
      initialMinute = if (isStart) startMinute else endMinute,
      is24Hour = true,
    )
    AlertDialog(
      onDismissRequest = { activeTimePickerTarget = null },
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
      title = {
        Text(
          if (isStart) {
            AppLabels.Schedules.selectStartTimeTitle
          } else {
            AppLabels.Schedules.selectEndTimeTitle
          },
          fontWeight = FontWeight.Bold,
        )
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
            notifyAutoSaveIfEditing()
          },
        ) {
          Text(AppLabels.Common.ok, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { activeTimePickerTarget = null }) {
          Text(AppLabels.Common.cancel)
        }
      },
    )
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
                  AppLabels.Schedules.editScheduleTitle
                } else {
                  AppLabels.Schedules.newScheduleTitle
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
                  color = activeAccent,
                )
              }
            }
          },
          actions = {
            if (isEditing && onDelete != null) {
              IconButton(
                onClick = { onDelete(initialRule.id) },
                modifier = Modifier.testTag(AppTestTags.Schedules.DELETE_TOP_BUTTON),
              ) {
                Icon(
                  imageVector = Icons.Default.DeleteOutline,
                  contentDescription = AppLabels.Schedules.deleteScheduleMenu,
                  tint = MaterialTheme.colorScheme.error,
                )
              }
            } else if (!isEditing) {
              TextButton(
                onClick = { onConfirm(buildRule()) },
                modifier = Modifier.testTag(AppTestTags.Schedules.CONFIRM_SAVE_BUTTON),
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
        // Cartões duplos ZENITH de Início e Fim
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          TimeBoxCard(
            label = AppLabels.Schedules.startTimeLabel,
            timeText = String.format(Locale.ROOT, "%02d:%02d", startHour, startMinute),
            onClick = { activeTimePickerTarget = "start" },
            modifier = Modifier.weight(1f),
          )
          TimeBoxCard(
            label = AppLabels.Schedules.endTimeLabel,
            timeText = String.format(Locale.ROOT, "%02d:%02d", endHour, endMinute),
            onClick = { activeTimePickerTarget = "end" },
            modifier = Modifier.weight(1f),
          )
        }

        OutlinedTextField(
          value = name,
          onValueChange = {
            name = it
            notifyAutoSaveIfEditing()
          },
          label = { Text(AppLabels.Schedules.scheduleNameLabel) },
          singleLine = true,
          shape = RoundedCornerShape(18.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag(AppTestTags.Schedules.NAME_INPUT),
        )

        // Dias da semana (7 círculos responsivos ZENITH)
        Text(
          text = AppLabels.Schedules.daysOfWeekSectionTitle,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Weekday.entries.forEach { day ->
            val selected = day in selectedDays
            Box(
              modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                  if (selected) {
                    activeAccent
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
                  notifyAutoSaveIfEditing()
                },
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = day.shortLabel,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                softWrap = false,
                color = if (selected) {
                  selectedProfile?.mode?.onActivePillColor() ?: MaterialTheme.colorScheme.onPrimary
                } else {
                  MaterialTheme.colorScheme.onSurfaceVariant
                },
              )
            }
          }
        }

        // Seleção exclusiva de Cena/Perfil
        Text(
          text = AppLabels.Schedules.targetProfileSectionTitle,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          profiles.forEach { profile ->
            val selected = selectedProfileId == profile.id
            FilterChip(
              selected = selected,
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

        Surface(
          shape = RoundedCornerShape(22.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            Column(
              modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            ) {
              Text(
                text = AppLabels.Schedules.highPriorityTitle,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
              )
              Text(
                text = AppLabels.Schedules.highPrioritySubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
            M3CheckSwitch(
              checked = highPriority,
              onCheckedChange = {
                highPriority = it
                notifyAutoSaveIfEditing()
              },
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
            Text(AppLabels.Schedules.createScheduleButton, fontWeight = FontWeight.Bold)
          }
        }
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
      .clip(RoundedCornerShape(24.dp))
      .clickable(onClick = onClick),
    shape = RoundedCornerShape(24.dp),
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 16.dp, horizontal = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = label,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        softWrap = false,
        color = MaterialTheme.colorScheme.primary,
      )
      Text(
        text = timeText,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        softWrap = false,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }
  }
}
