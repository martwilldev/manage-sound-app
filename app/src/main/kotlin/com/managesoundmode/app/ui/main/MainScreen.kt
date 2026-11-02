package com.managesoundmode.app.ui.main

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RingVolume
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.managesoundmode.app.domain.model.LocationRule
import com.managesoundmode.app.domain.model.RestorePolicy
import com.managesoundmode.app.domain.model.ScheduleRule
import com.managesoundmode.app.domain.model.SoundMode
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.domain.model.ThemeMode
import com.managesoundmode.app.domain.model.TimedModeSession
import com.managesoundmode.app.domain.model.VolumeStreamType
import com.managesoundmode.app.domain.model.VolumeStreams
import com.managesoundmode.app.system.SmartNotificationManager
import com.managesoundmode.app.ui.components.PillVolumeSliderRow
import com.managesoundmode.app.ui.components.ProfileEditorBottomSheet
import com.managesoundmode.app.ui.components.ProminentLocationDisclosureDialog
import com.managesoundmode.app.ui.components.modeIcon
import com.managesoundmode.app.ui.components.profileIconForKey
import com.managesoundmode.app.ui.theme.ManageSoundModeTheme
import com.managesoundmode.app.ui.theme.onTonalContainerColor
import com.managesoundmode.app.ui.theme.tonalContainerColor
import com.managesoundmode.app.widget.SoundModeGlanceWidget

enum class MainAppTab(
  val label: String,
  val topBarTitle: String,
  val icon: ImageVector,
  val tag: String,
) {
  NOW(
    label = "Agora",
    topBarTitle = "Som e Modos",
    icon = Icons.AutoMirrored.Filled.VolumeUp,
    tag = "tab_now",
  ),
  PROFILES(
    label = "Perfis",
    topBarTitle = "Perfis",
    icon = Icons.Default.Style,
    tag = "tab_profiles",
  ),
  SCHEDULES(
    label = "Horários",
    topBarTitle = "Horários",
    icon = Icons.Default.Schedule,
    tag = "tab_schedules",
  ),
  LOCATIONS(
    label = "Locais",
    topBarTitle = "Locais",
    icon = Icons.Default.LocationOn,
    tag = "tab_locations",
  ),
  SETTINGS(
    label = "Ajustes",
    topBarTitle = "Ajustes",
    icon = Icons.Default.Tune,
    tag = "tab_settings",
  ),
}

@Composable
fun MainScreen(
  modifier: Modifier = Modifier,
  viewModel: MainScreenViewModel = viewModel(factory = MainScreenViewModel.Factory),
) {
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()
  val context = LocalContext.current

  val notificationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission(),
  ) { _ ->
    viewModel.onRefreshSystemState()
  }

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
      ManageSoundModeTheme(
        themeMode = state.themeMode,
        dynamicColor = state.useDynamicColor,
      ) {
        MainDashboardContent(
          state = state,
          onSelectMode = viewModel::onSelectMode,
          onStreamVolumeChange = viewModel::onStreamVolumeChange,
          onActivateProfile = viewModel::onActivateProfile,
          onStartTimer = viewModel::onStartTimer,
          onExtendTimer = viewModel::onExtendTimer,
          onCancelTimer = viewModel::onCancelTimer,
          onSaveProfile = viewModel::onSaveCurrentAsProfile,
          onUpsertProfile = viewModel::onUpsertProfile,
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
          onRequestNotificationPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !state.hasNotificationPermission) {
              notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
              val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
              }
              runCatching { context.startActivity(intent) }
            }
          },
          onReapplyActiveRule = viewModel::onReapplyActiveRule,
          onUpsertScheduleRule = viewModel::onUpsertScheduleRule,
          onToggleScheduleRule = viewModel::onToggleScheduleRule,
          onDeleteScheduleRule = viewModel::onDeleteScheduleRule,
          onUpsertLocationRule = viewModel::onUpsertLocationRule,
          onToggleLocationRule = viewModel::onToggleLocationRule,
          onToggleLocationPresence = viewModel::onToggleLocationPresence,
          onDeleteLocationRule = viewModel::onDeleteLocationRule,
          onAcceptLocationDisclosure = viewModel::onAcceptLocationDisclosure,
          onDeleteCustomProfile = viewModel::onDeleteCustomProfile,
          onSetRestorePolicy = viewModel::onSetRestorePolicy,
          onSetDefaultDayProfileId = viewModel::onSetDefaultDayProfileId,
          onSetThemeMode = viewModel::onSetThemeMode,
          onSetUseDynamicColor = viewModel::onSetUseDynamicColor,
          onSetMuteMediaOnSilent = viewModel::onSetMuteMediaOnSilent,
          onSetPreserveMediaOnBluetooth = viewModel::onSetPreserveMediaOnBluetooth,
          onSetSmartVolumeTriggerEnabled = viewModel::onSetSmartVolumeTriggerEnabled,
          onTestSmartNotification = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !state.hasNotificationPermission) {
              notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
              val previewMode = if (state.currentMode == SoundMode.NORMAL) SoundMode.VIBRATE else state.currentMode
              SmartNotificationManager.showSmartRestorePrompt(
                context = context,
                mode = previewMode,
                mediaAutoMuted = state.muteMediaOnSilent,
              )
            }
          },
          onPinHomeWidget = {
            val pinned = SoundModeGlanceWidget.requestPinWidget(context)
            if (!pinned) {
              val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
              }
              runCatching { context.startActivity(intent) }
            }
          },
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
  onExtendTimer: (Int) -> Unit = {},
  onUpsertProfile: (SoundProfile, Boolean) -> Unit = { profile, _ ->
    onSaveProfile(profile.name, profile.description)
  },
  onRequestExactAlarmPermission: () -> Unit = {},
  onRequestNotificationPermission: () -> Unit = {},
  onReapplyActiveRule: () -> Unit = {},
  onUpsertScheduleRule: (ScheduleRule) -> Unit = {},
  onToggleScheduleRule: (ScheduleRule, Boolean) -> Unit = { _, _ -> },
  onDeleteScheduleRule: (String) -> Unit = {},
  onUpsertLocationRule: (LocationRule) -> Unit = {},
  onToggleLocationRule: (LocationRule, Boolean) -> Unit = { _, _ -> },
  onToggleLocationPresence: (String, Boolean) -> Unit = { _, _ -> },
  onDeleteLocationRule: (String) -> Unit = {},
  onAcceptLocationDisclosure: () -> Unit = {},
  onDeleteCustomProfile: (String) -> Unit = {},
  onSetRestorePolicy: (RestorePolicy) -> Unit = {},
  onSetDefaultDayProfileId: (String) -> Unit = {},
  onSetThemeMode: (ThemeMode) -> Unit = {},
  onSetUseDynamicColor: (Boolean) -> Unit = {},
  onSetMuteMediaOnSilent: (Boolean) -> Unit = {},
  onSetPreserveMediaOnBluetooth: (Boolean) -> Unit = {},
  onSetSmartVolumeTriggerEnabled: (Boolean) -> Unit = {},
  onTestSmartNotification: () -> Unit = {},
  onPinHomeWidget: () -> Unit = {},
  onClearLogs: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val snackbarHostState = remember { SnackbarHostState() }
  var selectedTab by rememberSaveable { mutableStateOf(MainAppTab.NOW) }

  // Estado unificado para o BottomSheet Editor de Perfil
  var editingProfile by remember { mutableStateOf<SoundProfile?>(null) }
  var showProfileBottomSheet by rememberSaveable { mutableStateOf(false) }

  // Estado para o BottomSheet Editor de Horário
  var editingScheduleRule by remember { mutableStateOf<ScheduleRule?>(null) }
  var showScheduleBottomSheet by rememberSaveable { mutableStateOf(false) }

  // Estado para o BottomSheet Editor de Local
  var editingLocationRule by remember { mutableStateOf<LocationRule?>(null) }
  var showLocationDisclosureDialog by rememberSaveable { mutableStateOf(false) }
  var showLocationBottomSheet by rememberSaveable { mutableStateOf(false) }

  LaunchedEffect(state.statusMessage) {
    val msg = state.statusMessage ?: return@LaunchedEffect
    snackbarHostState.showSnackbar(msg)
    onDismissMessage()
  }

  if (showProfileBottomSheet) {
    ProfileEditorBottomSheet(
      initialProfile = editingProfile,
      defaultMode = state.currentMode,
      defaultStreams = state.streams,
      onSaveProfile = { profile, applyNow ->
        showProfileBottomSheet = false
        editingProfile = null
        onUpsertProfile(profile, applyNow)
        if (applyNow) {
          selectedTab = MainAppTab.NOW
        }
      },
      onDeleteProfile = { profileId ->
        showProfileBottomSheet = false
        editingProfile = null
        onDeleteCustomProfile(profileId)
      },
      onDismiss = {
        showProfileBottomSheet = false
        editingProfile = null
      },
    )
  }

  if (showScheduleBottomSheet) {
    ScheduleEditorBottomSheet(
      initialRule = editingScheduleRule,
      profiles = state.profiles,
      onConfirm = { rule ->
        showScheduleBottomSheet = false
        editingScheduleRule = null
        onUpsertScheduleRule(rule)
      },
      onDelete = { ruleId ->
        showScheduleBottomSheet = false
        editingScheduleRule = null
        onDeleteScheduleRule(ruleId)
      },
      onEditSelectedProfile = { profileToEdit ->
        editingProfile = profileToEdit
        showProfileBottomSheet = true
      },
      onDismiss = {
        showScheduleBottomSheet = false
        editingScheduleRule = null
      },
    )
  }

  if (showLocationDisclosureDialog) {
    ProminentLocationDisclosureDialog(
      onAcceptAndContinue = {
        showLocationDisclosureDialog = false
        onAcceptLocationDisclosure()
        editingLocationRule = null
        showLocationBottomSheet = true
      },
      onDismiss = { showLocationDisclosureDialog = false },
    )
  }

  if (showLocationBottomSheet) {
    LocationEditorBottomSheet(
      initialRule = editingLocationRule,
      profiles = state.profiles,
      onConfirm = { rule ->
        showLocationBottomSheet = false
        editingLocationRule = null
        onUpsertLocationRule(rule)
      },
      onTogglePresence = onToggleLocationPresence,
      onDelete = { ruleId ->
        showLocationBottomSheet = false
        editingLocationRule = null
        onDeleteLocationRule(ruleId)
      },
      onDismiss = {
        showLocationBottomSheet = false
        editingLocationRule = null
      },
    )
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = selectedTab.topBarTitle,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
          )
        },
        actions = {
          val isLight = state.themeMode == ThemeMode.LIGHT
          IconButton(
            onClick = {
              val nextTheme = if (isLight) ThemeMode.DARK else ThemeMode.LIGHT
              onSetThemeMode(nextTheme)
            },
            modifier = Modifier.testTag("quick_theme_toggle"),
          ) {
            Icon(
              imageVector = if (isLight) Icons.Default.DarkMode else Icons.Default.LightMode,
              contentDescription = if (isLight) "Ativar Modo Escuro" else "Ativar Modo Claro",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
          titleContentColor = MaterialTheme.colorScheme.onBackground,
        ),
      )
    },
    floatingActionButton = {
      when (selectedTab) {
        MainAppTab.PROFILES -> {
          ExtendedFloatingActionButton(
            onClick = {
              editingProfile = null
              showProfileBottomSheet = true
            },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Novo Perfil") },
            modifier = Modifier.testTag("save_profile_button"),
          )
        }
        MainAppTab.SCHEDULES -> {
          ExtendedFloatingActionButton(
            onClick = {
              editingScheduleRule = null
              showScheduleBottomSheet = true
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
                editingLocationRule = null
                showLocationBottomSheet = true
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
            label = {
              Text(
                text = tab.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            },
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
          onOpenProfilesTab = { selectedTab = MainAppTab.PROFILES },
          onStartTimer = onStartTimer,
          onExtendTimer = onExtendTimer,
          onCancelTimer = onCancelTimer,
          onRequestDndPermission = onRequestDndPermission,
          onReapplyActiveRule = onReapplyActiveRule,
          modifier = Modifier.padding(innerPadding),
        )
      }

      MainAppTab.PROFILES -> {
        ProfilesTabContent(
          state = state,
          onActivateProfile = { profile ->
            onActivateProfile(profile)
            selectedTab = MainAppTab.NOW
          },
          onEditProfile = { profile ->
            editingProfile = profile
            showProfileBottomSheet = true
          },
          onSetDefaultDayProfileId = onSetDefaultDayProfileId,
          modifier = Modifier.padding(innerPadding),
        )
      }

      MainAppTab.SCHEDULES -> {
        SchedulesTabContent(
          scheduleRules = state.scheduleRules,
          profiles = state.profiles,
          canScheduleExactAlarms = state.canScheduleExactAlarms,
          onToggleRule = onToggleScheduleRule,
          onEditRule = { rule ->
            editingScheduleRule = rule
            showScheduleBottomSheet = true
          },
          onRequestExactAlarmPermission = onRequestExactAlarmPermission,
          modifier = Modifier.padding(innerPadding),
        )
      }

      MainAppTab.LOCATIONS -> {
        LocationsTabContent(
          locationRules = state.locationRules,
          profiles = state.profiles,
          onToggleRule = onToggleLocationRule,
          onEditRule = { rule ->
            editingLocationRule = rule
            showLocationBottomSheet = true
          },
          modifier = Modifier.padding(innerPadding),
        )
      }

      MainAppTab.SETTINGS -> {
        SettingsAndProfilesTabContent(
          state = state,
          onSetRestorePolicy = onSetRestorePolicy,
          onSetThemeMode = onSetThemeMode,
          onSetUseDynamicColor = onSetUseDynamicColor,
          onSetMuteMediaOnSilent = onSetMuteMediaOnSilent,
          onSetPreserveMediaOnBluetooth = onSetPreserveMediaOnBluetooth,
          onSetSmartVolumeTriggerEnabled = onSetSmartVolumeTriggerEnabled,
          onTestSmartNotification = onTestSmartNotification,
          onPinHomeWidget = onPinHomeWidget,
          onRequestDndPermission = onRequestDndPermission,
          onRequestExactAlarmPermission = onRequestExactAlarmPermission,
          onRequestNotificationPermission = onRequestNotificationPermission,
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
  onOpenProfilesTab: () -> Unit,
  onStartTimer: (SoundMode, Int) -> Unit,
  onExtendTimer: (Int) -> Unit,
  onCancelTimer: () -> Unit,
  onRequestDndPermission: () -> Unit,
  onReapplyActiveRule: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptics = LocalHapticFeedback.current
  val activeProfile = state.profiles.firstOrNull { it.id == state.activeProfileId }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .testTag("now_tab_list"),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    if (!state.hasDndPermission) {
      item {
        DndPermissionBanner(onRequestDndPermission = onRequestDndPermission)
      }
    }

    if (state.isManuallyOverridden) {
      item {
        ManualOverrideBanner(onReapplyActiveRule = onReapplyActiveRule)
      }
    }

    // Bloco 1: Hero Card Contextual (Com Chip compacto do Perfil Ativo + Timer Embutido Sob Demanda)
    item {
      UnifiedHeroAndModeCard(
        currentMode = state.currentMode,
        activeProfile = activeProfile,
        activeTimer = state.activeTimer,
        activeRuleTitle = state.activeRuleTitle,
        activeRuleSubtitle = state.activeRuleSubtitle,
        muteMediaOnSilent = state.muteMediaOnSilent,
        onSelectMode = { mode ->
          haptics.performHapticFeedback(HapticFeedbackType.LongPress)
          onSelectMode(mode)
        },
        onOpenProfilesTab = onOpenProfilesTab,
        onStartTimer = { minutes ->
          haptics.performHapticFeedback(HapticFeedbackType.LongPress)
          val targetMode = if (state.currentMode == SoundMode.NORMAL) SoundMode.VIBRATE else state.currentMode
          onStartTimer(targetMode, minutes)
        },
        onExtendTimer = { addMinutes ->
          haptics.performHapticFeedback(HapticFeedbackType.LongPress)
          onExtendTimer(addMinutes)
        },
        onCancelTimer = {
          haptics.performHapticFeedback(HapticFeedbackType.LongPress)
          onCancelTimer()
        },
      )
    }

    // Bloco 2: Card Único Agrupado de Volumes (100% Zero Scroll no estado padrão)
    item {
      VolumeStreamsCard(
        streams = state.streams,
        onStreamVolumeChange = onStreamVolumeChange,
      )
    }
  }
}

@Composable
private fun UnifiedHeroAndModeCard(
  currentMode: SoundMode,
  activeProfile: SoundProfile?,
  activeTimer: TimedModeSession?,
  activeRuleTitle: String?,
  activeRuleSubtitle: String?,
  muteMediaOnSilent: Boolean,
  onSelectMode: (SoundMode) -> Unit,
  onOpenProfilesTab: () -> Unit,
  onStartTimer: (Int) -> Unit,
  onExtendTimer: (Int) -> Unit,
  onCancelTimer: () -> Unit,
) {
  val animatedContainer by animateColorAsState(
    targetValue = currentMode.tonalContainerColor(),
    animationSpec = tween(durationMillis = 220),
    label = "heroTonalContainer",
  )
  val animatedOnContainer by animateColorAsState(
    targetValue = currentMode.onTonalContainerColor(),
    animationSpec = tween(durationMillis = 220),
    label = "heroOnTonalContainer",
  )

  // O Timer Contextual aparece SOMENTE quando o celular está em Vibrar/Silencioso ou quando há um timer rodando
  val showContextualTimerRow = currentMode != SoundMode.NORMAL || activeTimer != null

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("hero_mode_card"),
    shape = RoundedCornerShape(28.dp),
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
      // Cabeçalho Tonal Expressivo + Chip Compacto do Perfil Ativo à direita
      Surface(
        shape = RoundedCornerShape(22.dp),
        color = animatedContainer,
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(animatedOnContainer.copy(alpha = 0.14f)),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = currentMode.modeIcon(),
                contentDescription = null,
                tint = animatedOnContainer,
                modifier = Modifier.size(24.dp),
              )
            }

            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = currentMode.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = animatedOnContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              val cleanSubtitle = when {
                activeRuleTitle != null -> activeRuleTitle
                currentMode != SoundMode.NORMAL && muteMediaOnSilent ->
                  "${currentMode.subtitle} • Mídia 0%"
                else -> activeRuleSubtitle ?: currentMode.subtitle
              }
              Text(
                text = cleanSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = animatedOnContainer.copy(alpha = 0.82f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          Spacer(modifier = Modifier.width(8.dp))

          // Chip Compacto do Perfil Ativo (Tocar abre a aba Perfis)
          Surface(
            modifier = Modifier
              .clip(RoundedCornerShape(14.dp))
              .clickable(onClick = onOpenProfilesTab)
              .testTag("active_profile_chip"),
            shape = RoundedCornerShape(14.dp),
            color = animatedOnContainer.copy(alpha = 0.14f),
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
              Icon(
                imageVector = activeProfile?.let { profileIconForKey(it.iconKey) } ?: Icons.Default.Style,
                contentDescription = null,
                tint = animatedOnContainer,
                modifier = Modifier.size(15.dp),
              )
              Text(
                text = activeProfile?.name ?: "Perfis",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = animatedOnContainer,
                maxLines = 1,
              )
              Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = animatedOnContainer.copy(alpha = 0.8f),
                modifier = Modifier.size(15.dp),
              )
            }
          }
        }
      }

      // Seletor Rápido dos 3 Modos (Pill Cards Tonais)
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        SoundMode.entries.forEach { mode ->
          val selected = mode == currentMode
          val chipBg by animateColorAsState(
            targetValue = if (selected) {
              MaterialTheme.colorScheme.primary
            } else {
              MaterialTheme.colorScheme.surfaceContainerHigh
            },
            animationSpec = tween(180),
            label = "modePillBg_${mode.name}",
          )
          val chipContent by animateColorAsState(
            targetValue = if (selected) {
              MaterialTheme.colorScheme.onPrimary
            } else {
              MaterialTheme.colorScheme.onSurface
            },
            animationSpec = tween(180),
            label = "modePillContent_${mode.name}",
          )

          Surface(
            modifier = Modifier
              .weight(1f)
              .heightIn(min = 48.dp)
              .clip(RoundedCornerShape(18.dp))
              .clickable { onSelectMode(mode) }
              .semantics { contentDescription = "Modo ${mode.title}" }
              .testTag("mode_button_${mode.name}"),
            shape = RoundedCornerShape(18.dp),
            color = chipBg,
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 11.dp),
              horizontalArrangement = Arrangement.Center,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Icon(
                imageVector = mode.modeIcon(),
                contentDescription = null,
                tint = chipContent,
                modifier = Modifier.size(18.dp),
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = when (mode) {
                  SoundMode.NORMAL -> "Som"
                  SoundMode.VIBRATE -> "Vibrar"
                  SoundMode.SILENT -> "Silêncio"
                },
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = chipContent,
                maxLines = 1,
              )
            }
          }
        }
      }

      // Gaveta Contextual de Timer: SÓ aparece em Vibrar ou Silencioso (ou quando há timer ativo)
      AnimatedVisibility(
        visible = showContextualTimerRow,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut(),
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
              )
              val remainingMinutes = activeTimer?.remainingMinutes(System.currentTimeMillis())
              Text(
                text = if (activeTimer != null && remainingMinutes != null && remainingMinutes > 0) {
                  "Volta em ${remainingMinutes}m"
                } else {
                  "Manter por:"
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            if (activeTimer != null) {
              Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Surface(
                  modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable { onExtendTimer(30) },
                  shape = RoundedCornerShape(50),
                  color = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                  Text(
                    text = "+30m",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                  )
                }
                FilledTonalButton(
                  onClick = onCancelTimer,
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                  modifier = Modifier.testTag("cancel_timer_button"),
                ) {
                  Text("Restaurar", style = MaterialTheme.typography.labelMedium)
                }
              }
            } else {
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(15 to "15m", 30 to "30m", 60 to "1h", 120 to "2h").forEach { (minutes, label) ->
                  Surface(
                    modifier = Modifier
                      .clip(RoundedCornerShape(50))
                      .clickable { onStartTimer(minutes) }
                      .testTag("timer_chip_$minutes"),
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                  ) {
                    Text(
                      text = label,
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.Medium,
                      color = MaterialTheme.colorScheme.onSurface,
                      modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    )
                  }
                }
              }
            }
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
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.secondaryContainer,
    ),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "Ajustado manualmente",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Text(
          text = "Retoma a rotina no próximo horário",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
        )
      }
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
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.errorContainer,
    ),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
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
        Column {
          Text(
            text = "Permissão Não Perturbe",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onErrorContainer,
          )
          Text(
            text = "Necessária para o modo Silencioso",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
          )
        }
      }
      Button(onClick = onRequestDndPermission) {
        Text("Ativar")
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
    shape = RoundedCornerShape(28.dp),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 18.dp, vertical = 14.dp),
      verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        text = "Volumes",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 2.dp),
      )

      PillVolumeSliderRow(
        type = VolumeStreamType.RING,
        icon = Icons.Default.RingVolume,
        percent = streams.ringPercent,
        onValueChangeFinished = { onStreamVolumeChange(VolumeStreamType.RING, it) },
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
      PillVolumeSliderRow(
        type = VolumeStreamType.NOTIFICATION,
        icon = Icons.Default.Notifications,
        percent = streams.notificationPercent,
        onValueChangeFinished = { onStreamVolumeChange(VolumeStreamType.NOTIFICATION, it) },
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
      PillVolumeSliderRow(
        type = VolumeStreamType.MEDIA,
        icon = Icons.Default.MusicNote,
        percent = streams.mediaPercent,
        onValueChangeFinished = { onStreamVolumeChange(VolumeStreamType.MEDIA, it) },
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
      PillVolumeSliderRow(
        type = VolumeStreamType.ALARM,
        icon = Icons.Default.Alarm,
        percent = streams.alarmPercent,
        onValueChangeFinished = { onStreamVolumeChange(VolumeStreamType.ALARM, it) },
      )
    }
  }
}

@Preview(showBackground = true, widthDp = 412, heightDp = 915)
@Composable
private fun MainDashboardPreview() {
  ManageSoundModeTheme(
    themeMode = ThemeMode.DARK,
    dynamicColor = false,
  ) {
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
        activeRuleTitle = "Rotina 'Trabalho' ativa",
        activeRuleSubtitle = "Horário 08:30 – 18:00 • Perfil Trabalho",
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
