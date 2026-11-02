package com.managesoundmode.app.ui.main

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RingVolume
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.managesoundmode.app.R
import com.managesoundmode.app.domain.model.LocationRule
import com.managesoundmode.app.domain.model.RestorePolicy
import com.managesoundmode.app.domain.model.ScheduleRule
import com.managesoundmode.app.domain.model.SoundMode
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.domain.model.TimedModeSession
import com.managesoundmode.app.domain.model.VolumeStreamType
import com.managesoundmode.app.domain.model.VolumeStreams
import com.managesoundmode.app.domain.model.Weekday
import com.managesoundmode.app.ui.components.PillVolumeSliderRow
import com.managesoundmode.app.ui.components.ProminentLocationDisclosureDialog
import com.managesoundmode.app.ui.components.groupedCardShape
import com.managesoundmode.app.ui.theme.ManageSoundModeTheme
import com.managesoundmode.app.ui.theme.SoundSilentRose
import com.managesoundmode.app.ui.theme.SoundTimerBlue
import com.managesoundmode.app.ui.theme.SoundTimerContainerDark
import com.managesoundmode.app.ui.theme.accentColor
import com.managesoundmode.app.ui.theme.containerColor

enum class MainAppTab(
  val label: String,
  val icon: ImageVector,
  val tag: String,
) {
  NOW(label = "Agora", icon = Icons.AutoMirrored.Filled.VolumeUp, tag = "tab_now"),
  SCHEDULES(label = "Horários", icon = Icons.Default.Schedule, tag = "tab_schedules"),
  LOCATIONS(label = "Locais", icon = Icons.Default.LocationOn, tag = "tab_locations"),
  SETTINGS(label = "Perfis & Ajustes", icon = Icons.Default.Tune, tag = "tab_settings"),
}

@Composable
fun MainScreen(
  modifier: Modifier = Modifier,
  viewModel: MainScreenViewModel = viewModel(factory = MainScreenViewModel.Factory),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current

  LifecycleResumeEffect(Unit) {
    viewModel.onRefreshSystemState()
    onPauseOrDispose {}
  }

  when (val state = uiState) {
    MainScreenUiState.Loading -> {
      Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
      ) {
        CircularProgressIndicator()
      }
    }

    is MainScreenUiState.Error -> {
      Box(
        modifier = modifier
          .fillMaxSize()
          .padding(24.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = state.message,
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.error,
        )
      }
    }

    is MainScreenUiState.Ready -> {
      ManageSoundModeTheme(dynamicColor = state.useDynamicColor) {
        MainDashboardContent(
          state = state,
          onSelectMode = viewModel::onSelectMode,
          onStreamVolumeChange = viewModel::onStreamVolumeChange,
          onActivateProfile = viewModel::onActivateProfile,
          onStartTimer = viewModel::onStartTimer,
          onCancelTimer = viewModel::onCancelTimer,
          onSaveProfile = viewModel::onSaveCurrentAsProfile,
          onDismissMessage = viewModel::onDismissMessage,
          onRequestDndPermission = {
            val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS).apply {
              addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            runCatching { context.startActivity(intent) }
          },
          onRequestExactAlarmPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
              val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
              }
              runCatching { context.startActivity(intent) }
            }
          },
          onReapplyActiveRule = viewModel::onReapplyActiveRule,
          onUpsertScheduleRule = viewModel::onUpsertScheduleRule,
          onToggleScheduleRule = viewModel::onToggleScheduleRule,
          onToggleScheduleWeekday = viewModel::onToggleScheduleRuleWeekday,
          onUpdateScheduleProfile = viewModel::onUpdateScheduleRuleProfile,
          onToggleScheduleHighPriority = viewModel::onToggleScheduleRuleHighPriority,
          onDeleteScheduleRule = viewModel::onDeleteScheduleRule,
          onUpsertLocationRule = viewModel::onUpsertLocationRule,
          onToggleLocationRule = viewModel::onToggleLocationRule,
          onToggleLocationPresence = viewModel::onToggleLocationPresence,
          onDeleteLocationRule = viewModel::onDeleteLocationRule,
          onAcceptLocationDisclosure = viewModel::onAcceptLocationDisclosure,
          onDeleteCustomProfile = viewModel::onDeleteCustomProfile,
          onSetRestorePolicy = viewModel::onSetRestorePolicy,
          onSetUseDynamicColor = viewModel::onSetUseDynamicColor,
          onClearLogs = viewModel::onClearTransitionLogs,
          modifier = modifier,
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainDashboardContent(
  state: MainScreenUiState.Ready,
  onSelectMode: (SoundMode) -> Unit,
  onStreamVolumeChange: (VolumeStreamType, Int) -> Unit,
  onActivateProfile: (SoundProfile) -> Unit,
  onStartTimer: (SoundMode, Int) -> Unit,
  onCancelTimer: () -> Unit,
  onSaveProfile: (String, String) -> Unit,
  onDismissMessage: () -> Unit,
  onRequestDndPermission: () -> Unit,
  onRequestExactAlarmPermission: () -> Unit = {},
  onReapplyActiveRule: () -> Unit = {},
  onUpsertScheduleRule: (ScheduleRule) -> Unit = {},
  onToggleScheduleRule: (ScheduleRule, Boolean) -> Unit = { _, _ -> },
  onToggleScheduleWeekday: (ScheduleRule, Weekday) -> Unit = { _, _ -> },
  onUpdateScheduleProfile: (ScheduleRule, String) -> Unit = { _, _ -> },
  onToggleScheduleHighPriority: (ScheduleRule, Boolean) -> Unit = { _, _ -> },
  onDeleteScheduleRule: (String) -> Unit = {},
  onUpsertLocationRule: (LocationRule) -> Unit = {},
  onToggleLocationRule: (LocationRule, Boolean) -> Unit = { _, _ -> },
  onToggleLocationPresence: (String, Boolean) -> Unit = { _, _ -> },
  onDeleteLocationRule: (String) -> Unit = {},
  onAcceptLocationDisclosure: () -> Unit = {},
  onDeleteCustomProfile: (String) -> Unit = {},
  onSetRestorePolicy: (RestorePolicy) -> Unit = {},
  onSetUseDynamicColor: (Boolean) -> Unit = {},
  onClearLogs: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val snackbarHostState = remember { SnackbarHostState() }
  var selectedTab by rememberSaveable { mutableStateOf(MainAppTab.NOW) }
  var showSaveProfileDialog by rememberSaveable { mutableStateOf(false) }
  var editingScheduleRule by remember { mutableStateOf<ScheduleRule?>(null) }
  var showScheduleDialog by rememberSaveable { mutableStateOf(false) }
  var showLocationDisclosureDialog by rememberSaveable { mutableStateOf(false) }
  var showAddLocationDialog by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(state.statusMessage) {
    val msg = state.statusMessage ?: return@LaunchedEffect
    snackbarHostState.showSnackbar(msg)
    onDismissMessage()
  }

  if (showSaveProfileDialog) {
    SaveProfileDialog(
      currentMode = state.currentMode,
      onConfirm = { name, desc ->
        showSaveProfileDialog = false
        onSaveProfile(name, desc)
      },
      onDismiss = { showSaveProfileDialog = false },
    )
  }

  if (showScheduleDialog) {
    AddOrEditScheduleDialog(
      initialRule = editingScheduleRule,
      profiles = state.profiles,
      onConfirm = { rule ->
        showScheduleDialog = false
        editingScheduleRule = null
        onUpsertScheduleRule(rule)
      },
      onDismiss = {
        showScheduleDialog = false
        editingScheduleRule = null
      },
    )
  }

  if (showLocationDisclosureDialog) {
    ProminentLocationDisclosureDialog(
      onAcceptAndContinue = {
        showLocationDisclosureDialog = false
        onAcceptLocationDisclosure()
        showAddLocationDialog = true
      },
      onDismiss = { showLocationDisclosureDialog = false },
    )
  }

  if (showAddLocationDialog) {
    AddLocationRuleDialog(
      profiles = state.profiles,
      onConfirm = { rule ->
        showAddLocationDialog = false
        onUpsertLocationRule(rule)
      },
      onDismiss = { showAddLocationDialog = false },
    )
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
          )
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
          titleContentColor = MaterialTheme.colorScheme.onBackground,
        ),
      )
    },
    floatingActionButton = {
      when (selectedTab) {
        MainAppTab.SCHEDULES -> {
          ExtendedFloatingActionButton(
            onClick = {
              editingScheduleRule = null
              showScheduleDialog = true
            },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Novo Horário") },
            modifier = Modifier.testTag("add_schedule_fab"),
          )
        }
        MainAppTab.LOCATIONS -> {
          ExtendedFloatingActionButton(
            onClick = {
              if (!state.hasAcceptedLocationDisclosure) {
                showLocationDisclosureDialog = true
              } else {
                showAddLocationDialog = true
              }
            },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Novo Local") },
            modifier = Modifier.testTag("add_location_fab"),
          )
        }
        else -> {}
      }
    },
    bottomBar = {
      NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
      ) {
        MainAppTab.entries.forEach { tab ->
          NavigationBarItem(
            selected = selectedTab == tab,
            onClick = { selectedTab = tab },
            icon = {
              Icon(
                imageVector = tab.icon,
                contentDescription = tab.label,
              )
            },
            label = { Text(tab.label) },
            modifier = Modifier.testTag(tab.tag),
          )
        }
      }
    },
  ) { innerPadding ->
    when (selectedTab) {
      MainAppTab.NOW -> {
        NowTabContent(
          state = state,
          onSelectMode = onSelectMode,
          onStreamVolumeChange = onStreamVolumeChange,
          onActivateProfile = onActivateProfile,
          onStartTimer = onStartTimer,
          onCancelTimer = onCancelTimer,
          onOpenSaveProfileDialog = { showSaveProfileDialog = true },
          onRequestDndPermission = onRequestDndPermission,
          onReapplyActiveRule = onReapplyActiveRule,
          modifier = Modifier.padding(innerPadding),
        )
      }

      MainAppTab.SCHEDULES -> {
        SchedulesTabContent(
          scheduleRules = state.scheduleRules,
          profiles = state.profiles,
          canScheduleExactAlarms = state.canScheduleExactAlarms,
          onToggleRule = onToggleScheduleRule,
          onToggleWeekday = onToggleScheduleWeekday,
          onSelectProfile = onUpdateScheduleProfile,
          onToggleHighPriority = onToggleScheduleHighPriority,
          onEditRule = { rule ->
            editingScheduleRule = rule
            showScheduleDialog = true
          },
          onDeleteRule = onDeleteScheduleRule,
          onRequestExactAlarmPermission = onRequestExactAlarmPermission,
          modifier = Modifier.padding(innerPadding),
        )
      }

      MainAppTab.LOCATIONS -> {
        LocationsTabContent(
          locationRules = state.locationRules,
          profiles = state.profiles,
          onToggleRule = onToggleLocationRule,
          onTogglePresence = onToggleLocationPresence,
          onDeleteRule = onDeleteLocationRule,
          modifier = Modifier.padding(innerPadding),
        )
      }

      MainAppTab.SETTINGS -> {
        SettingsAndProfilesTabContent(
          state = state,
          onActivateProfile = onActivateProfile,
          onDeleteCustomProfile = onDeleteCustomProfile,
          onOpenSaveProfileDialog = { showSaveProfileDialog = true },
          onSetRestorePolicy = onSetRestorePolicy,
          onSetUseDynamicColor = onSetUseDynamicColor,
          onRequestDndPermission = onRequestDndPermission,
          onRequestExactAlarmPermission = onRequestExactAlarmPermission,
          onClearLogs = onClearLogs,
          modifier = Modifier.padding(innerPadding),
        )
      }
    }
  }
}

@Composable
private fun NowTabContent(
  state: MainScreenUiState.Ready,
  onSelectMode: (SoundMode) -> Unit,
  onStreamVolumeChange: (VolumeStreamType, Int) -> Unit,
  onActivateProfile: (SoundProfile) -> Unit,
  onStartTimer: (SoundMode, Int) -> Unit,
  onCancelTimer: () -> Unit,
  onOpenSaveProfileDialog: () -> Unit,
  onRequestDndPermission: () -> Unit,
  onReapplyActiveRule: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptics = LocalHapticFeedback.current

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .testTag("now_tab_list"),
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    item {
      CurrentModeHeroCard(
        currentMode = state.currentMode,
        activeTimer = state.activeTimer,
        activeRuleTitle = state.activeRuleTitle,
        activeRuleSubtitle = state.activeRuleSubtitle,
      )
    }

    if (state.isManuallyOverridden) {
      item {
        ManualOverrideBanner(onReapplyActiveRule = onReapplyActiveRule)
      }
    }

    if (!state.hasDndPermission) {
      item {
        DndPermissionBanner(onRequestDndPermission = onRequestDndPermission)
      }
    }

    item {
      TactileModeSelectorSection(
        currentMode = state.currentMode,
        onSelectMode = { mode ->
          haptics.performHapticFeedback(HapticFeedbackType.LongPress)
          onSelectMode(mode)
        },
      )
    }

    item {
      QuickTimerSection(
        activeTimer = state.activeTimer,
        onStartTimer = { targetMode, minutes ->
          haptics.performHapticFeedback(HapticFeedbackType.LongPress)
          onStartTimer(targetMode, minutes)
        },
        onCancelTimer = {
          haptics.performHapticFeedback(HapticFeedbackType.LongPress)
          onCancelTimer()
        },
      )
    }

    item {
      VolumeStreamsCard(
        streams = state.streams,
        onStreamVolumeChange = onStreamVolumeChange,
      )
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Perfis Rápidos",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )
        FilledTonalButton(
          onClick = onOpenSaveProfileDialog,
          modifier = Modifier.testTag("save_profile_button"),
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Salvar Atual")
        }
      }
    }

    itemsIndexed(
      items = state.profiles,
      key = { _, profile -> profile.id },
    ) { index, profile ->
      SoundProfileCard(
        profile = profile,
        isActive = state.activeProfileId == profile.id,
        shape = groupedCardShape(index = index, totalCount = state.profiles.size),
        onClick = {
          haptics.performHapticFeedback(HapticFeedbackType.LongPress)
          onActivateProfile(profile)
        },
      )
    }
  }
}

@Composable
private fun CurrentModeHeroCard(
  currentMode: SoundMode,
  activeTimer: TimedModeSession?,
  activeRuleTitle: String?,
  activeRuleSubtitle: String?,
) {
  val animatedAccent by animateColorAsState(
    targetValue = currentMode.accentColor(),
    animationSpec = tween(durationMillis = 200),
    label = "heroAccent",
  )
  val animatedContainer by animateColorAsState(
    targetValue = currentMode.containerColor(),
    animationSpec = tween(durationMillis = 200),
    label = "heroContainer",
  )

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("hero_mode_card"),
    shape = RoundedCornerShape(28.dp),
    colors = CardDefaults.cardColors(containerColor = animatedContainer),
    border = BorderStroke(1.5.dp, animatedAccent.copy(alpha = 0.7f)),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Box(
          modifier = Modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(animatedAccent.copy(alpha = 0.2f)),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = currentMode.icon(),
            contentDescription = null,
            tint = animatedAccent,
            modifier = Modifier.size(28.dp),
          )
        }
        Column {
          Text(
            text = activeRuleTitle?.uppercase() ?: "MODO ATUAL",
            style = MaterialTheme.typography.labelLarge,
            color = animatedAccent,
          )
          Text(
            text = currentMode.title,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
          )
        }
      }

      Text(
        text = activeRuleSubtitle ?: currentMode.subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = 0.88f),
      )

      if (activeTimer != null) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = SoundTimerContainerDark,
          border = BorderStroke(1.dp, SoundTimerBlue),
          modifier = Modifier.padding(top = 4.dp),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = Icons.Default.AccessTime,
              contentDescription = null,
              tint = SoundTimerBlue,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = "Auto-Restore ativo (${activeTimer.durationMinutes} min) -> voltará para ${activeTimer.previousMode.title}",
              style = MaterialTheme.typography.labelLarge,
              color = Color.White,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun ManualOverrideBanner(
  onReapplyActiveRule: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("manual_override_banner"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.secondaryContainer,
    ),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(
          text = "Alterado manualmente nesta sessão",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Text(
          text = "Sua escolha manual foi respeitada até o próximo horário.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      FilledTonalButton(
        onClick = onReapplyActiveRule,
        modifier = Modifier.testTag("reapply_rule_button"),
      ) {
        Icon(
          imageVector = Icons.Default.Refresh,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text("Reaplicar")
      }
    }
  }
}

@Composable
private fun DndPermissionBanner(
  onRequestDndPermission: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("dnd_permission_banner"),
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(
      containerColor = SoundSilentRose.copy(alpha = 0.15f),
    ),
    border = BorderStroke(1.dp, SoundSilentRose.copy(alpha = 0.6f)),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Icon(
          imageVector = Icons.Default.WarningAmber,
          contentDescription = null,
          tint = SoundSilentRose,
        )
        Text(
          text = "Permissão Não Perturbe (DND) Necessária",
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onBackground,
        )
      }
      Text(
        text = "Para alternar para o modo Silencioso ou sair do Não Perturbe automaticamente, conceda acesso à política de notificações do Android.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      Button(
        onClick = onRequestDndPermission,
        colors = ButtonDefaults.buttonColors(
          containerColor = SoundSilentRose,
          contentColor = Color.White,
        ),
        modifier = Modifier.heightIn(min = 48.dp),
      ) {
        Text("Conceder Permissão no Android")
      }
    }
  }
}

@Composable
private fun TactileModeSelectorSection(
  currentMode: SoundMode,
  onSelectMode: (SoundMode) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text(
      text = "Alternância Instantânea",
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onBackground,
    )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      SoundMode.entries.forEach { mode ->
        val selected = mode == currentMode
        val accent = mode.accentColor()
        val bgColor by animateColorAsState(
          targetValue = if (selected) accent.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceContainerLow,
          animationSpec = tween(180),
          label = "modeChipBg_${mode.name}",
        )
        val borderColor by animateColorAsState(
          targetValue = if (selected) accent else Color.Transparent,
          animationSpec = tween(180),
          label = "modeChipBorder_${mode.name}",
        )

        Surface(
          modifier = Modifier
            .weight(1f)
            .heightIn(min = 88.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable { onSelectMode(mode) }
            .semantics { contentDescription = "Modo ${mode.title}" }
            .testTag("mode_button_${mode.name}"),
          shape = RoundedCornerShape(22.dp),
          color = bgColor,
          border = BorderStroke(2.dp, borderColor),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 14.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Icon(
              imageVector = mode.icon(),
              contentDescription = null,
              tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(26.dp),
            )
            Text(
              text = mode.title,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
              color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun QuickTimerSection(
  activeTimer: TimedModeSession?,
  onStartTimer: (SoundMode, Int) -> Unit,
  onCancelTimer: () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
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
        ) {
          Icon(
            imageVector = Icons.Default.AccessTime,
            contentDescription = null,
            tint = SoundTimerBlue,
          )
          Text(
            text = "Modo Temporizado (Auto-Restore)",
            style = MaterialTheme.typography.titleMedium,
          )
        }
        if (activeTimer != null) {
          OutlinedButton(
            onClick = onCancelTimer,
            modifier = Modifier.testTag("cancel_timer_button"),
          ) {
            Text("Restaurar Agora")
          }
        }
      }

      Text(
        text = "Pausa rápida: manter em Vibrar e voltar ao modo anterior sozinho.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        listOf(15, 30, 45, 60).forEach { minutes ->
          AssistChip(
            onClick = { onStartTimer(SoundMode.VIBRATE, minutes) },
            label = { Text("Vibrar ${minutes}m") },
            colors = AssistChipDefaults.assistChipColors(
              labelColor = MaterialTheme.colorScheme.onSurface,
            ),
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 48.dp)
              .testTag("timer_chip_$minutes"),
          )
        }
      }
    }
  }
}

@Composable
private fun VolumeStreamsCard(
  streams: VolumeStreams,
  onStreamVolumeChange: (VolumeStreamType, Int) -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(24.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Text(
        text = "Canais de Volume Independentes",
        style = MaterialTheme.typography.titleMedium,
      )

      PillVolumeSliderRow(
        type = VolumeStreamType.RING,
        icon = Icons.Default.RingVolume,
        percent = streams.ringPercent,
        onValueChangeFinished = { onStreamVolumeChange(VolumeStreamType.RING, it) },
      )
      PillVolumeSliderRow(
        type = VolumeStreamType.NOTIFICATION,
        icon = Icons.Default.Notifications,
        percent = streams.notificationPercent,
        onValueChangeFinished = { onStreamVolumeChange(VolumeStreamType.NOTIFICATION, it) },
      )
      PillVolumeSliderRow(
        type = VolumeStreamType.MEDIA,
        icon = Icons.Default.MusicNote,
        percent = streams.mediaPercent,
        onValueChangeFinished = { onStreamVolumeChange(VolumeStreamType.MEDIA, it) },
      )
      PillVolumeSliderRow(
        type = VolumeStreamType.ALARM,
        icon = Icons.Default.Alarm,
        percent = streams.alarmPercent,
        onValueChangeFinished = { onStreamVolumeChange(VolumeStreamType.ALARM, it) },
      )
    }
  }
}

@Composable
private fun SoundProfileCard(
  profile: SoundProfile,
  isActive: Boolean,
  shape: RoundedCornerShape,
  onClick: () -> Unit,
) {
  val modeAccent = profile.mode.accentColor()
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(shape)
      .clickable(onClick = onClick)
      .testTag("profile_card_${profile.id}"),
    shape = shape,
    colors = CardDefaults.cardColors(
      containerColor = if (isActive) {
        MaterialTheme.colorScheme.secondaryContainer
      } else {
        MaterialTheme.colorScheme.surfaceContainerLow
      },
    ),
    border = if (isActive) {
      BorderStroke(width = 1.5.dp, color = modeAccent)
    } else {
      null
    },
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = profile.name,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
          )
          Surface(
            shape = RoundedCornerShape(8.dp),
            color = modeAccent.copy(alpha = 0.2f),
          ) {
            Text(
              text = profile.mode.title,
              style = MaterialTheme.typography.labelLarge,
              color = modeAccent,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            )
          }
        }
        Text(
          text = profile.description,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = "Toque ${profile.streams.ringPercent}% • Mídia ${profile.streams.mediaPercent}% • Alarme ${profile.streams.alarmPercent}%",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        )
      }

      if (isActive) {
        Icon(
          imageVector = Icons.Default.CheckCircle,
          contentDescription = "Perfil Ativo",
          tint = modeAccent,
          modifier = Modifier.size(28.dp),
        )
      }
    }
  }
}

@Composable
private fun SaveProfileDialog(
  currentMode: SoundMode,
  onConfirm: (String, String) -> Unit,
  onDismiss: () -> Unit,
) {
  var name by rememberSaveable { mutableStateOf("") }
  var description by rememberSaveable { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Salvar Perfil Customizado") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = "Salva o modo atual (${currentMode.title}) e os níveis de volume configurados.",
          style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Nome do perfil") },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag("profile_name_input"),
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Descrição opcional") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(name, description) },
        enabled = name.trim().isNotEmpty(),
        modifier = Modifier.testTag("confirm_save_profile_button"),
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

private fun SoundMode.icon(): ImageVector = when (this) {
  SoundMode.NORMAL -> Icons.Default.NotificationsActive
  SoundMode.VIBRATE -> Icons.Default.Vibration
  SoundMode.SILENT -> Icons.Default.DoNotDisturbOn
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun MainDashboardPreview() {
  ManageSoundModeTheme(dynamicColor = false) {
    MainDashboardContent(
      state = MainScreenUiState.Ready(
        currentMode = SoundMode.NORMAL,
        streams = VolumeStreams(
          ringPercent = 85,
          notificationPercent = 75,
          mediaPercent = 50,
          alarmPercent = 90,
        ),
        hasDndPermission = true,
        profiles = SoundProfile.defaultPresets,
        activeProfileId = "preset_work_high",
        activeRuleTitle = "Rotina 'Trabalho (Comercial)' ativa",
        activeRuleSubtitle = "Horário 08:30 – 18:00 • Até 18:00 • Perfil Trabalho (Volume Alto)",
        scheduleRules = ScheduleRule.defaultSchedules,
        locationRules = LocationRule.defaultLocations,
      ),
      onSelectMode = {},
      onStreamVolumeChange = { _, _ -> },
      onActivateProfile = {},
      onStartTimer = { _, _ -> },
      onCancelTimer = {},
      onSaveProfile = { _, _ -> },
      onDismissMessage = {},
      onRequestDndPermission = {},
    )
  }
}
