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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.RingVolume
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.managesoundmode.app.domain.model.AppAccessRole
import com.managesoundmode.app.domain.model.AppPermission
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
import com.managesoundmode.app.tile.QuickTileKind
import com.managesoundmode.app.tile.SoundModeTileService
import com.managesoundmode.app.ui.common.AppLabels
import com.managesoundmode.app.ui.common.AppLanguage
import com.managesoundmode.app.ui.common.AppTestTags
import com.managesoundmode.app.ui.components.FirstRunOnboardingDialog
import com.managesoundmode.app.ui.components.PillVolumeSliderRow
import com.managesoundmode.app.ui.components.ProfileEditorBottomSheet
import com.managesoundmode.app.ui.components.ProminentLocationDisclosureDialog
import com.managesoundmode.app.ui.components.modeIcon
import com.managesoundmode.app.ui.components.profileIconForKey
import com.managesoundmode.app.ui.theme.LocalZenithIsDark
import com.managesoundmode.app.ui.theme.ManageSoundModeTheme
import com.managesoundmode.app.ui.theme.onActivePillColor
import com.managesoundmode.app.ui.theme.onTonalContainerColor
import com.managesoundmode.app.ui.theme.orbGlowColor
import com.managesoundmode.app.ui.theme.tonalAccentColor
import com.managesoundmode.app.ui.theme.tonalContainerColor
import com.managesoundmode.app.widget.SoundModeGlanceWidget

enum class MainAppTab(
  val icon: ImageVector,
  val tag: String,
) {
  NOW(
    icon = Icons.AutoMirrored.Filled.VolumeUp,
    tag = AppTestTags.Tabs.NOW,
  ),
  PROFILES(
    icon = Icons.Default.Style,
    tag = AppTestTags.Tabs.PROFILES,
  ),
  SCHEDULES(
    icon = Icons.Default.Schedule,
    tag = AppTestTags.Tabs.SCHEDULES,
  ),
  LOCATIONS(
    icon = Icons.Default.LocationOn,
    tag = AppTestTags.Tabs.LOCATIONS,
  ),
  ;

  val label: String
    get() = when (this) {
      NOW -> AppLabels.Tabs.nowLabel
      PROFILES -> AppLabels.Tabs.profilesLabel
      SCHEDULES -> AppLabels.Tabs.schedulesLabel
      LOCATIONS -> AppLabels.Tabs.locationsLabel
    }

  val topBarTitle: String
    get() = when (this) {
      NOW -> AppLabels.Tabs.nowTopBarTitle
      PROFILES -> AppLabels.Tabs.profilesTopBarTitle
      SCHEDULES -> AppLabels.Tabs.schedulesTopBarTitle
      LOCATIONS -> AppLabels.Tabs.locationsTopBarTitle
    }
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

  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestMultiplePermissions(),
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
          onUpsertProfile = { profile, applyNow ->
            viewModel.onUpsertProfile(profile, applyNow, showFeedback = true)
          },
          onAutoSaveProfile = { profile ->
            viewModel.onUpsertProfile(profile, applyImmediately = false, showFeedback = false)
          },
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
          onUpsertScheduleRule = { rule ->
            viewModel.onUpsertScheduleRule(rule, showFeedback = true)
          },
          onAutoSaveScheduleRule = { rule ->
            viewModel.onUpsertScheduleRule(rule, showFeedback = false)
          },
          onToggleScheduleRule = viewModel::onToggleScheduleRule,
          onDeleteScheduleRule = viewModel::onDeleteScheduleRule,
          onUpsertLocationRule = { rule ->
            viewModel.onUpsertLocationRule(rule, showFeedback = true)
          },
          onAutoSaveLocationRule = { rule ->
            viewModel.onUpsertLocationRule(rule, showFeedback = false)
          },
          onToggleLocationRule = viewModel::onToggleLocationRule,
          onToggleLocationPresence = viewModel::onToggleLocationPresence,
          onDeleteLocationRule = viewModel::onDeleteLocationRule,
          onSyncLocationsWithGps = viewModel::onSyncLocationsWithGps,
          onShowStatusMessage = viewModel::onShowStatusMessage,
          onAcceptLocationDisclosure = viewModel::onAcceptLocationDisclosure,
          onDeleteCustomProfile = viewModel::onDeleteCustomProfile,
          onSetRestorePolicy = viewModel::onSetRestorePolicy,
          onSetDefaultDayProfileId = viewModel::onSetDefaultDayProfileId,
          onSetThemeMode = viewModel::onSetThemeMode,
          onSetUseDynamicColor = viewModel::onSetUseDynamicColor,
          onSetMuteMediaOnSilent = viewModel::onSetMuteMediaOnSilent,
          onSetPreserveMediaOnBluetooth = viewModel::onSetPreserveMediaOnBluetooth,
          onSetSmartVolumeTriggerEnabled = viewModel::onSetSmartVolumeTriggerEnabled,
          onSetPersistentQuickBarEnabled = { enabled ->
            viewModel.onSetPersistentQuickBarEnabled(enabled)
            if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !state.hasNotificationPermission) {
              notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
          },
          onRequestAddQuickTile = { kind, customLabel ->
            SoundModeTileService.requestAddTile(
              context = context,
              kind = kind,
              customLabel = customLabel,
            ) { message ->
              viewModel.onShowStatusMessage(message)
            }
          },
          onSetQuickTileSlotProfileId = viewModel::onSetQuickTileSlotProfileId,
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
          onSetAppLanguage = viewModel::onSetAppLanguage,
          onSetAutomationsPaused = viewModel::onSetAutomationsPaused,
          onSetHapticFeedbackEnabled = viewModel::onSetHapticFeedbackEnabled,
          onSetNotifyOnAutoTransition = viewModel::onSetNotifyOnAutoTransition,
          onSetDefaultTimerMinutes = viewModel::onSetDefaultTimerMinutes,
          onResetFactoryDefaults = viewModel::onResetFactoryDefaults,
          onDuplicateProfile = viewModel::onDuplicateProfile,
          onDuplicateScheduleRule = viewModel::onDuplicateScheduleRule,
          onExportBackupJson = viewModel::onExportBackupJson,
          onImportBackupJson = viewModel::onImportBackupJson,
          onRequestLocationPermission = {
            if (!state.hasLocationPermission) {
              locationPermissionLauncher.launch(
                arrayOf(
                  Manifest.permission.ACCESS_FINE_LOCATION,
                  Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
              )
            } else {
              val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", context.packageName, null)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
              }
              runCatching { context.startActivity(intent) }
            }
          },
          onCompleteOnboarding = viewModel::onCompleteOnboarding,
          onReopenOnboarding = viewModel::onReopenOnboarding,
          onSetAccessRole = viewModel::onSetAccessRole,
          onToggleAccessPermission = viewModel::onToggleAccessPermission,
          onSetAccessPin = viewModel::onSetAccessPin,
          onUnlockAdminWithPin = viewModel::onUnlockAdminWithPin,
          onSetSystemZenRulesEnabled = viewModel::onSetSystemZenRulesEnabled,
          onSyncSystemZenRulesNow = viewModel::onSyncSystemZenRulesNow,
          onSyncBackgroundGeofencesNow = viewModel::onSyncBackgroundGeofencesNow,
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
  onAutoSaveProfile: (SoundProfile) -> Unit = { profile ->
    onUpsertProfile(profile, false)
  },
  onRequestExactAlarmPermission: () -> Unit = {},
  onRequestNotificationPermission: () -> Unit = {},
  onReapplyActiveRule: () -> Unit = {},
  onUpsertScheduleRule: (ScheduleRule) -> Unit = {},
  onAutoSaveScheduleRule: (ScheduleRule) -> Unit = { rule ->
    onUpsertScheduleRule(rule)
  },
  onToggleScheduleRule: (ScheduleRule, Boolean) -> Unit = { _, _ -> },
  onDeleteScheduleRule: (String) -> Unit = {},
  onUpsertLocationRule: (LocationRule) -> Unit = {},
  onAutoSaveLocationRule: (LocationRule) -> Unit = { rule ->
    onUpsertLocationRule(rule)
  },
  onToggleLocationRule: (LocationRule, Boolean) -> Unit = { _, _ -> },
  onToggleLocationPresence: (String, Boolean) -> Unit = { _, _ -> },
  onDeleteLocationRule: (String) -> Unit = {},
  onSyncLocationsWithGps: (Double, Double) -> Unit = { _, _ -> },
  onShowStatusMessage: (String) -> Unit = {},
  onAcceptLocationDisclosure: () -> Unit = {},
  onDeleteCustomProfile: (String) -> Unit = {},
  onSetRestorePolicy: (RestorePolicy) -> Unit = {},
  onSetDefaultDayProfileId: (String) -> Unit = {},
  onSetThemeMode: (ThemeMode) -> Unit = {},
  onSetUseDynamicColor: (Boolean) -> Unit = {},
  onSetMuteMediaOnSilent: (Boolean) -> Unit = {},
  onSetPreserveMediaOnBluetooth: (Boolean) -> Unit = {},
  onSetSmartVolumeTriggerEnabled: (Boolean) -> Unit = {},
  onSetPersistentQuickBarEnabled: (Boolean) -> Unit = {},
  onRequestAddQuickTile: (QuickTileKind, String?) -> Unit = { _, _ -> },
  onSetQuickTileSlotProfileId: (Int, String) -> Unit = { _, _ -> },
  onTestSmartNotification: () -> Unit = {},
  onPinHomeWidget: () -> Unit = {},
  onClearLogs: () -> Unit = {},
  onSetAppLanguage: (AppLanguage) -> Unit = { AppLabels.currentLanguage = it },
  onSetAutomationsPaused: (Boolean) -> Unit = {},
  onSetHapticFeedbackEnabled: (Boolean) -> Unit = {},
  onSetNotifyOnAutoTransition: (Boolean) -> Unit = {},
  onSetDefaultTimerMinutes: (Int) -> Unit = {},
  onResetFactoryDefaults: () -> Unit = {},
  onDuplicateProfile: (SoundProfile) -> Unit = {},
  onDuplicateScheduleRule: (ScheduleRule) -> Unit = {},
  onExportBackupJson: ((String) -> Unit) -> Unit = {},
  onImportBackupJson: (String) -> Unit = {},
  onRequestLocationPermission: () -> Unit = {},
  onCompleteOnboarding: () -> Unit = {},
  onReopenOnboarding: () -> Unit = {},
  onSetAccessRole: (AppAccessRole) -> Unit = {},
  onToggleAccessPermission: (AppPermission, Boolean) -> Unit = { _, _ -> },
  onSetAccessPin: (String?) -> Unit = {},
  onUnlockAdminWithPin: (String, () -> Unit) -> Unit = { _, onSuccess -> onSuccess() },
  onSetSystemZenRulesEnabled: (Boolean) -> Unit = {},
  onSyncSystemZenRulesNow: () -> Unit = {},
  onSyncBackgroundGeofencesNow: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val snackbarHostState = remember { SnackbarHostState() }
  var selectedTab by rememberSaveable { mutableStateOf(MainAppTab.NOW) }
  var showGlobalOverflowMenu by remember { mutableStateOf(false) }
  var showSettingsFullScreen by rememberSaveable { mutableStateOf(false) }
  var globalSettingsDialogCategory by rememberSaveable { mutableStateOf<SettingsCategory?>(null) }
  var showGlobalResetConfirmDialog by rememberSaveable { mutableStateOf(false) }
  var showGlobalPinChallengeDialog by rememberSaveable { mutableStateOf(false) }
  var pendingLocationPermissionAfterDisclosure by rememberSaveable { mutableStateOf(false) }

  val isDark = LocalZenithIsDark.current
  val modeAccent = state.currentMode.tonalAccentColor()

  val openAccessControlOrChallenge: () -> Unit = {
    if (state.accessControlConfig.isPinRequired && state.accessControlConfig.activeRole != AppAccessRole.ADMIN) {
      showGlobalPinChallengeDialog = true
    } else {
      globalSettingsDialogCategory = SettingsCategory.ACCESS_CONTROL
    }
  }

  var editingProfile by remember { mutableStateOf<SoundProfile?>(null) }
  var showProfileBottomSheet by rememberSaveable { mutableStateOf(false) }

  var editingScheduleRule by remember { mutableStateOf<ScheduleRule?>(null) }
  var showScheduleBottomSheet by rememberSaveable { mutableStateOf(false) }

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
      onAutoSaveProfile = onAutoSaveProfile,
      onDeleteProfile = if (state.profiles.size > 1) {
        { profileId ->
          showProfileBottomSheet = false
          editingProfile = null
          onDeleteCustomProfile(profileId)
        }
      } else {
        null
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
      onAutoSave = onAutoSaveScheduleRule,
      onDelete = { ruleId ->
        showScheduleBottomSheet = false
        editingScheduleRule = null
        onDeleteScheduleRule(ruleId)
      },
      onDismiss = {
        showScheduleBottomSheet = false
        editingScheduleRule = null
      },
    )
  }

  val handleLocationPermissionRequest: () -> Unit = {
    if (!state.hasAcceptedLocationDisclosure && !state.hasLocationPermission) {
      pendingLocationPermissionAfterDisclosure = true
      showLocationDisclosureDialog = true
    } else {
      onRequestLocationPermission()
    }
  }

  if (!state.hasCompletedOnboarding) {
    FirstRunOnboardingDialog(
      state = state,
      onCompleteOnboarding = onCompleteOnboarding,
      onSetAppLanguage = onSetAppLanguage,
      onSetThemeMode = onSetThemeMode,
      onRequestDndPermission = onRequestDndPermission,
      onRequestNotificationPermission = onRequestNotificationPermission,
      onRequestExactAlarmPermission = onRequestExactAlarmPermission,
      onRequestLocationPermission = handleLocationPermissionRequest,
      onSetDefaultDayProfileId = onSetDefaultDayProfileId,
      onSetMuteMediaOnSilent = onSetMuteMediaOnSilent,
      onSetSmartVolumeTriggerEnabled = onSetSmartVolumeTriggerEnabled,
      onSetPersistentQuickBarEnabled = onSetPersistentQuickBarEnabled,
    )
  }

  if (showLocationDisclosureDialog) {
    ProminentLocationDisclosureDialog(
      onAcceptAndContinue = {
        showLocationDisclosureDialog = false
        onAcceptLocationDisclosure()
        if (pendingLocationPermissionAfterDisclosure) {
          pendingLocationPermissionAfterDisclosure = false
          onRequestLocationPermission()
        } else {
          editingLocationRule = null
          showLocationBottomSheet = true
        }
      },
      onDismiss = {
        showLocationDisclosureDialog = false
        pendingLocationPermissionAfterDisclosure = false
      },
    )
  }

  if (showLocationBottomSheet) {
    LocationEditorBottomSheet(
      initialRule = editingLocationRule,
      profiles = state.profiles,
      knownWifiSsids = state.locationRules
        .mapNotNull { it.wifiSsid.trim().takeIf(String::isNotEmpty) }
        .distinct(),
      onConfirm = { rule ->
        showLocationBottomSheet = false
        editingLocationRule = null
        onUpsertLocationRule(rule)
      },
      onAutoSave = onAutoSaveLocationRule,
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

  if (showGlobalPinChallengeDialog) {
    AccessPinChallengeDialog(
      config = state.accessControlConfig,
      onDismiss = { showGlobalPinChallengeDialog = false },
      onVerifyAndOpenSubScreen = {
        showGlobalPinChallengeDialog = false
        globalSettingsDialogCategory = SettingsCategory.ACCESS_CONTROL
      },
      onUnlockAdminWithPin = { pin ->
        onUnlockAdminWithPin(pin) {
          showGlobalPinChallengeDialog = false
        }
      },
      onShowStatusMessage = onShowStatusMessage,
    )
  }

  if (showGlobalResetConfirmDialog) {
    FactoryResetConfirmDialog(
      onConfirm = {
        showGlobalResetConfirmDialog = false
        globalSettingsDialogCategory = null
        onResetFactoryDefaults()
      },
      onDismiss = { showGlobalResetConfirmDialog = false },
    )
  }

  if (showSettingsFullScreen) {
    Dialog(
      onDismissRequest = { showSettingsFullScreen = false },
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
                onClick = { showSettingsFullScreen = false },
                modifier = Modifier.testTag(AppTestTags.Settings.CLOSE_SETTINGS_BUTTON),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = AppLabels.Common.back,
                )
              }
            },
            title = {
              Text(
                text = AppLabels.Tabs.settingsTopBarTitle,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
              )
            },
            colors = TopAppBarDefaults.topAppBarColors(
              containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
              titleContentColor = MaterialTheme.colorScheme.onBackground,
            ),
          )
        },
      ) { settingsPadding ->
        SettingsAndProfilesTabContent(
          state = state,
          onSetRestorePolicy = onSetRestorePolicy,
          onSetDefaultDayProfileId = onSetDefaultDayProfileId,
          onSetThemeMode = onSetThemeMode,
          onSetUseDynamicColor = onSetUseDynamicColor,
          onSetAppLanguage = onSetAppLanguage,
          onSetAutomationsPaused = onSetAutomationsPaused,
          onSetHapticFeedbackEnabled = onSetHapticFeedbackEnabled,
          onSetNotifyOnAutoTransition = onSetNotifyOnAutoTransition,
          onSetDefaultTimerMinutes = onSetDefaultTimerMinutes,
          onSetMuteMediaOnSilent = onSetMuteMediaOnSilent,
          onSetPreserveMediaOnBluetooth = onSetPreserveMediaOnBluetooth,
          onSetSmartVolumeTriggerEnabled = onSetSmartVolumeTriggerEnabled,
          onSetPersistentQuickBarEnabled = onSetPersistentQuickBarEnabled,
          onRequestAddQuickTile = onRequestAddQuickTile,
          onSetQuickTileSlotProfileId = onSetQuickTileSlotProfileId,
          onTestSmartNotification = onTestSmartNotification,
          onPinHomeWidget = onPinHomeWidget,
          onRequestDndPermission = onRequestDndPermission,
          onRequestExactAlarmPermission = onRequestExactAlarmPermission,
          onRequestNotificationPermission = onRequestNotificationPermission,
          onClearLogs = onClearLogs,
          onResetFactoryDefaults = onResetFactoryDefaults,
          onExportBackupJson = onExportBackupJson,
          onImportBackupJson = onImportBackupJson,
          onShowStatusMessage = onShowStatusMessage,
          onRequestLocationPermission = handleLocationPermissionRequest,
          onReopenOnboarding = {
            showSettingsFullScreen = false
            onReopenOnboarding()
          },
          onSetAccessRole = onSetAccessRole,
          onToggleAccessPermission = onToggleAccessPermission,
          onSetAccessPin = onSetAccessPin,
          onUnlockAdminWithPin = onUnlockAdminWithPin,
          onSetSystemZenRulesEnabled = onSetSystemZenRulesEnabled,
          onSyncSystemZenRulesNow = onSyncSystemZenRulesNow,
          modifier = Modifier.padding(settingsPadding),
        )
      }
    }
  }

  if (globalSettingsDialogCategory != null) {
    SettingsCategoryFullScreenDialog(
      category = globalSettingsDialogCategory!!,
      state = state,
      onDismiss = { globalSettingsDialogCategory = null },
      onSetRestorePolicy = onSetRestorePolicy,
      onSetDefaultDayProfileId = onSetDefaultDayProfileId,
      onSetThemeMode = onSetThemeMode,
      onSetUseDynamicColor = onSetUseDynamicColor,
      onSetAppLanguage = onSetAppLanguage,
      onSetAutomationsPaused = onSetAutomationsPaused,
      onSetHapticFeedbackEnabled = onSetHapticFeedbackEnabled,
      onSetNotifyOnAutoTransition = onSetNotifyOnAutoTransition,
      onSetDefaultTimerMinutes = onSetDefaultTimerMinutes,
      onSetMuteMediaOnSilent = onSetMuteMediaOnSilent,
      onSetPreserveMediaOnBluetooth = onSetPreserveMediaOnBluetooth,
      onSetSmartVolumeTriggerEnabled = onSetSmartVolumeTriggerEnabled,
      onTestSmartNotification = onTestSmartNotification,
      onSetPersistentQuickBarEnabled = onSetPersistentQuickBarEnabled,
      onRequestAddQuickTile = onRequestAddQuickTile,
      onSetQuickTileSlotProfileId = onSetQuickTileSlotProfileId,
      onPinHomeWidget = onPinHomeWidget,
      onRequestDndPermission = onRequestDndPermission,
      onRequestExactAlarmPermission = onRequestExactAlarmPermission,
      onRequestNotificationPermission = onRequestNotificationPermission,
      onClearLogs = onClearLogs,
      onRequestFactoryResetConfirm = { showGlobalResetConfirmDialog = true },
      onExportBackupJson = onExportBackupJson,
      onImportBackupJson = onImportBackupJson,
      onShowStatusMessage = onShowStatusMessage,
      onRequestLocationPermission = handleLocationPermissionRequest,
      onReopenOnboarding = {
        globalSettingsDialogCategory = null
        onReopenOnboarding()
      },
      onSetAccessRole = onSetAccessRole,
      onToggleAccessPermission = onToggleAccessPermission,
      onSetAccessPin = onSetAccessPin,
      onSetSystemZenRulesEnabled = onSetSystemZenRulesEnabled,
      onSyncSystemZenRulesNow = onSyncSystemZenRulesNow,
    )
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    topBar = {
      TopAppBar(
        title = {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Box(
              modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(modeAccent),
            )
            Text(
              text = selectedTab.topBarTitle,
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.Bold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        },
        actions = {
          if (state.accessControlConfig.isRestricted || state.accessControlConfig.isPinRequired) {
            Surface(
              shape = RoundedCornerShape(50),
              color = if (state.accessControlConfig.isRestricted) {
                MaterialTheme.colorScheme.tertiaryContainer
              } else {
                MaterialTheme.colorScheme.secondaryContainer
              },
              border = BorderStroke(
                width = 1.dp,
                color = if (state.accessControlConfig.isRestricted) {
                  MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)
                } else {
                  MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
                },
              ),
              modifier = Modifier
                .clip(RoundedCornerShape(50))
                .clickable(onClick = openAccessControlOrChallenge)
                .testTag(AppTestTags.TopBar.ROLE_BADGE_CHIP),
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.Lock,
                  contentDescription = null,
                  modifier = Modifier.size(14.dp),
                )
                Text(
                  text = state.accessControlConfig.activeRole.shortBadge,
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.SemiBold,
                )
              }
            }
          }

          // Alternador rápido Dark (Noite Boreal) / Light (Alvorada Nórdica)
          IconButton(
            onClick = {
              val nextTheme = if (isDark) ThemeMode.LIGHT else ThemeMode.DARK
              onSetThemeMode(nextTheme)
            },
            modifier = Modifier.testTag(AppTestTags.TopBar.QUICK_THEME_TOGGLE),
          ) {
            Icon(
              imageVector = if (isDark) Icons.Default.LightMode else Icons.Default.DarkMode,
              contentDescription = if (isDark) {
                AppLabels.TopBar.enableLightMode
              } else {
                AppLabels.TopBar.enableDarkMode
              },
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          Box {
            IconButton(
              onClick = { showGlobalOverflowMenu = true },
              modifier = Modifier.testTag(AppTestTags.TopBar.OVERFLOW_MENU_BUTTON),
            ) {
              Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = AppLabels.TopBar.overflowMenuDescription,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }

            DropdownMenu(
              expanded = showGlobalOverflowMenu,
              onDismissRequest = { showGlobalOverflowMenu = false },
            ) {
              DropdownMenuItem(
                text = {
                  Text(
                    text = AppLabels.TopBar.openSettingsMenu,
                    fontWeight = FontWeight.SemiBold,
                  )
                },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                  )
                },
                onClick = {
                  showGlobalOverflowMenu = false
                  showSettingsFullScreen = true
                },
                modifier = Modifier.testTag(AppTestTags.TopBar.MENU_SETTINGS),
              )

              HorizontalDivider()

              DropdownMenuItem(
                text = {
                  Text(
                    if (state.automationsPaused) {
                      AppLabels.TopBar.resumeAutomationsMenu
                    } else {
                      AppLabels.TopBar.pauseAutomationsMenu
                    },
                  )
                },
                leadingIcon = {
                  Icon(
                    imageVector = if (state.automationsPaused) {
                      Icons.Default.PlayCircleFilled
                    } else {
                      Icons.Default.PauseCircleFilled
                    },
                    contentDescription = null,
                    tint = if (state.automationsPaused) {
                      MaterialTheme.colorScheme.primary
                    } else {
                      MaterialTheme.colorScheme.onSurfaceVariant
                    },
                  )
                },
                onClick = {
                  showGlobalOverflowMenu = false
                  onSetAutomationsPaused(!state.automationsPaused)
                },
                modifier = Modifier.testTag(AppTestTags.TopBar.MENU_PAUSE_AUTOMATIONS),
              )

              DropdownMenuItem(
                text = { Text(AppLabels.TopBar.transitionHistoryMenu) },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                  )
                },
                onClick = {
                  showGlobalOverflowMenu = false
                  globalSettingsDialogCategory = SettingsCategory.HISTORY
                },
                modifier = Modifier.testTag(AppTestTags.TopBar.MENU_HISTORY),
              )

              DropdownMenuItem(
                text = { Text(AppLabels.TopBar.permissionsDiagnosticMenu) },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                  )
                },
                onClick = {
                  showGlobalOverflowMenu = false
                  globalSettingsDialogCategory = SettingsCategory.PERMISSIONS
                },
                modifier = Modifier.testTag(AppTestTags.TopBar.MENU_PERMISSIONS),
              )

              DropdownMenuItem(
                text = { Text(AppLabels.TopBar.accessControlMenu) },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                  )
                },
                onClick = {
                  showGlobalOverflowMenu = false
                  openAccessControlOrChallenge()
                },
                modifier = Modifier.testTag(AppTestTags.TopBar.MENU_ACCESS_CONTROL),
              )

              DropdownMenuItem(
                text = { Text(AppLabels.TopBar.setupWizardMenu) },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                  )
                },
                onClick = {
                  showGlobalOverflowMenu = false
                  onReopenOnboarding()
                },
                modifier = Modifier.testTag(AppTestTags.TopBar.MENU_SETUP_WIZARD),
              )

              HorizontalDivider()

              DropdownMenuItem(
                text = {
                  Text(
                    text = AppLabels.TopBar.restoreFactoryDefaultsMenu,
                    color = MaterialTheme.colorScheme.error,
                  )
                },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Default.Restore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                  )
                },
                onClick = {
                  showGlobalOverflowMenu = false
                  if (!state.accessControlConfig.hasPermission(AppPermission.BACKUP_AND_FACTORY_RESET)) {
                    onShowStatusMessage(
                      AppLabels.AccessControl.permissionDeniedMessage(AppPermission.BACKUP_AND_FACTORY_RESET),
                    )
                  } else {
                    showGlobalResetConfirmDialog = true
                  }
                },
                modifier = Modifier.testTag(AppTestTags.TopBar.MENU_RESTORE_DEFAULTS),
              )

              DropdownMenuItem(
                text = { Text(AppLabels.TopBar.aboutAndPrivacyMenu) },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                  )
                },
                onClick = {
                  showGlobalOverflowMenu = false
                  globalSettingsDialogCategory = SettingsCategory.ABOUT
                },
                modifier = Modifier.testTag(AppTestTags.TopBar.MENU_ABOUT),
              )
            }
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
              if (!state.accessControlConfig.hasPermission(AppPermission.MANAGE_PROFILES)) {
                onShowStatusMessage(
                  AppLabels.AccessControl.permissionDeniedMessage(AppPermission.MANAGE_PROFILES),
                )
              } else {
                editingProfile = null
                showProfileBottomSheet = true
              }
            },
            shape = RoundedCornerShape(22.dp),
            containerColor = modeAccent,
            contentColor = state.currentMode.onActivePillColor(),
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text(AppLabels.Fab.newProfile, fontWeight = FontWeight.Bold) },
            modifier = Modifier.testTag(AppTestTags.Fab.SAVE_PROFILE_BUTTON),
          )
        }
        MainAppTab.SCHEDULES -> {
          ExtendedFloatingActionButton(
            onClick = {
              if (!state.accessControlConfig.hasPermission(AppPermission.MANAGE_SCHEDULES)) {
                onShowStatusMessage(
                  AppLabels.AccessControl.permissionDeniedMessage(AppPermission.MANAGE_SCHEDULES),
                )
              } else {
                editingScheduleRule = null
                showScheduleBottomSheet = true
              }
            },
            shape = RoundedCornerShape(22.dp),
            containerColor = modeAccent,
            contentColor = state.currentMode.onActivePillColor(),
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text(AppLabels.Fab.newSchedule, fontWeight = FontWeight.Bold) },
            modifier = Modifier.testTag(AppTestTags.Fab.ADD_SCHEDULE_FAB),
          )
        }
        MainAppTab.LOCATIONS -> {
          ExtendedFloatingActionButton(
            onClick = {
              if (!state.accessControlConfig.hasPermission(AppPermission.MANAGE_LOCATIONS)) {
                onShowStatusMessage(
                  AppLabels.AccessControl.permissionDeniedMessage(AppPermission.MANAGE_LOCATIONS),
                )
              } else if (!state.hasAcceptedLocationDisclosure) {
                showLocationDisclosureDialog = true
              } else {
                editingLocationRule = null
                showLocationBottomSheet = true
              }
            },
            shape = RoundedCornerShape(22.dp),
            containerColor = modeAccent,
            contentColor = state.currentMode.onActivePillColor(),
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text(AppLabels.Fab.newLocation, fontWeight = FontWeight.Bold) },
            modifier = Modifier.testTag(AppTestTags.Fab.ADD_LOCATION_FAB),
          )
        }
        else -> {}
      }
    },
    bottomBar = {
      ZenithFloatingPillNavigationBar(
        selectedTab = selectedTab,
        activeAccent = modeAccent,
        onSelectTab = { selectedTab = it },
      )
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
            if (state.accessControlConfig.hasPermission(AppPermission.CHANGE_SOUND_AND_TIMER)) {
              selectedTab = MainAppTab.NOW
            }
          },
          onEditProfile = { profile ->
            if (!state.accessControlConfig.hasPermission(AppPermission.MANAGE_PROFILES)) {
              onShowStatusMessage(
                AppLabels.AccessControl.permissionDeniedMessage(AppPermission.MANAGE_PROFILES),
              )
            } else {
              editingProfile = profile
              showProfileBottomSheet = true
            }
          },
          onDuplicateProfile = onDuplicateProfile,
          onDeleteProfile = onDeleteCustomProfile,
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
            if (!state.accessControlConfig.hasPermission(AppPermission.MANAGE_SCHEDULES)) {
              onShowStatusMessage(
                AppLabels.AccessControl.permissionDeniedMessage(AppPermission.MANAGE_SCHEDULES),
              )
            } else {
              editingScheduleRule = rule
              showScheduleBottomSheet = true
            }
          },
          onDuplicateRule = onDuplicateScheduleRule,
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
          onEditRule = { rule ->
            if (!state.accessControlConfig.hasPermission(AppPermission.MANAGE_LOCATIONS)) {
              onShowStatusMessage(
                AppLabels.AccessControl.permissionDeniedMessage(AppPermission.MANAGE_LOCATIONS),
              )
            } else {
              editingLocationRule = rule
              showLocationBottomSheet = true
            }
          },
          onDeleteRule = onDeleteLocationRule,
          onSyncGpsCoordinates = onSyncLocationsWithGps,
          onShowStatusMessage = onShowStatusMessage,
          hasAcceptedLocationDisclosure = state.hasAcceptedLocationDisclosure,
          onAcceptLocationDisclosure = onAcceptLocationDisclosure,
          onSyncBackgroundGeofencesNow = onSyncBackgroundGeofencesNow,
          modifier = Modifier.padding(innerPadding),
        )
      }
    }
  }
}

/**
 * Barra de Navegação Inferior em Pílula Flutuante ZENITH (`Floating Glass Pill Dock`).
 */
@Composable
private fun ZenithFloatingPillNavigationBar(
  selectedTab: MainAppTab,
  activeAccent: Color,
  onSelectTab: (MainAppTab) -> Unit,
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surfaceContainerLowest)
      .padding(horizontal = 16.dp, vertical = 10.dp),
    contentAlignment = Alignment.Center,
  ) {
    Surface(
      shape = RoundedCornerShape(30.dp),
      color = MaterialTheme.colorScheme.surfaceContainer,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        MainAppTab.entries.forEach { tab ->
          val selected = selectedTab == tab
          val pillBg by animateColorAsState(
            targetValue = if (selected) {
              activeAccent.copy(alpha = 0.16f)
            } else {
              Color.Transparent
            },
            animationSpec = tween(180),
            label = "zenithNavTabBg_${tab.name}",
          )
          val itemColor by animateColorAsState(
            targetValue = if (selected) {
              activeAccent
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant
            },
            animationSpec = tween(180),
            label = "zenithNavTabContent_${tab.name}",
          )

          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(24.dp))
              .background(pillBg)
              .clickable { onSelectTab(tab) }
              .padding(vertical = 8.dp, horizontal = 4.dp)
              .testTag(tab.tag),
            contentAlignment = Alignment.Center,
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
              Icon(
                imageVector = tab.icon,
                contentDescription = tab.label,
                tint = itemColor,
                modifier = Modifier.size(20.dp),
              )
              Text(
                text = tab.label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = itemColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }
        }
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
      .testTag(AppTestTags.Now.LIST),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
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

    // Bloco 1: Orbe Acústico ZENITH + Pílula Tripla de Modo + Timer Embutido
    item {
      UnifiedHeroAndModeCard(
        currentMode = state.currentMode,
        mediaPercent = state.streams.mediaPercent,
        activeProfile = activeProfile,
        activeTimer = state.activeTimer,
        activeRuleTitle = state.activeRuleTitle,
        activeRuleSubtitle = state.activeRuleSubtitle,
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

    // Bloco 2: 4 Cápsulas Líquidas de Volume ZENITH
    item {
      VolumeStreamsCard(
        streams = state.streams,
        currentMode = state.currentMode,
        onStreamVolumeChange = onStreamVolumeChange,
      )
    }
  }
}

/**
 * Orbe Acústico Respirante ZENITH (`ZenithOrbHeroCard`) + Seletor Triplo Flutuante + Gaveta de Timer.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun UnifiedHeroAndModeCard(
  currentMode: SoundMode,
  mediaPercent: Int,
  activeProfile: SoundProfile?,
  activeTimer: TimedModeSession?,
  activeRuleTitle: String?,
  activeRuleSubtitle: String?,
  onSelectMode: (SoundMode) -> Unit,
  onOpenProfilesTab: () -> Unit,
  onStartTimer: (Int) -> Unit,
  onExtendTimer: (Int) -> Unit,
  onCancelTimer: () -> Unit,
) {
  val animatedAccent by animateColorAsState(
    targetValue = currentMode.tonalAccentColor(),
    animationSpec = tween(durationMillis = 280),
    label = "zenithHeroAccent",
  )
  val animatedContainer by animateColorAsState(
    targetValue = currentMode.tonalContainerColor(),
    animationSpec = tween(durationMillis = 280),
    label = "zenithHeroContainer",
  )
  val animatedOnContainer by animateColorAsState(
    targetValue = currentMode.onTonalContainerColor(),
    animationSpec = tween(durationMillis = 280),
    label = "zenithHeroOnContainer",
  )
  val animatedGlow by animateColorAsState(
    targetValue = currentMode.orbGlowColor(),
    animationSpec = tween(durationMillis = 280),
    label = "zenithHeroGlow",
  )

  val showContextualTimerRow = currentMode != SoundMode.NORMAL || activeTimer != null

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag(AppTestTags.Now.HERO_MODE_CARD),
    shape = RoundedCornerShape(32.dp),
    border = BorderStroke(1.dp, animatedAccent.copy(alpha = 0.28f)),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      // Linha Superior: Badge de Status Atmosférico + Chip de Cena/Perfil Ativo
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Surface(
          shape = RoundedCornerShape(50),
          color = animatedContainer,
          border = BorderStroke(1.dp, animatedAccent.copy(alpha = 0.30f)),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Box(
              modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(animatedAccent),
            )
            Text(
              text = activeRuleTitle ?: "ZENITH ACOUSTIC",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = animatedOnContainer,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Chip Compacto do Perfil Ativo (Tocar abre a aba Perfis)
        Surface(
          modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onOpenProfilesTab)
            .testTag(AppTestTags.Now.ACTIVE_PROFILE_CHIP),
          shape = RoundedCornerShape(50),
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = activeProfile?.let { profileIconForKey(it.iconKey) } ?: Icons.Default.Style,
              contentDescription = null,
              tint = animatedAccent,
              modifier = Modifier.size(15.dp),
            )
            Text(
              text = activeProfile?.name ?: AppLabels.Tabs.profilesLabel,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              softWrap = false,
              overflow = TextOverflow.Ellipsis,
            )
            Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(15.dp),
            )
          }
        }
      }

      // Centro Gravitacional: Orbe Acústico ZENITH + Anéis Concêntricos
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(148.dp),
        contentAlignment = Alignment.Center,
      ) {
        Canvas(modifier = Modifier.size(148.dp)) {
          val center = Offset(size.width / 2f, size.height / 2f)
          val maxR = size.minDimension / 2f

          // Halo radial atmosférico
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(
                animatedGlow.copy(alpha = 0.34f),
                animatedGlow.copy(alpha = 0.10f),
                Color.Transparent,
              ),
              center = center,
              radius = maxR,
            ),
            radius = maxR,
            center = center,
          )

          // Anel acústico externo tracejado
          drawCircle(
            color = animatedAccent.copy(alpha = 0.22f),
            radius = maxR * 0.90f,
            center = center,
            style = Stroke(
              width = 1.2.dp.toPx(),
              pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f)),
            ),
          )

          // Anel acústico intermediário
          drawCircle(
            color = animatedAccent.copy(alpha = 0.36f),
            radius = maxR * 0.72f,
            center = center,
            style = Stroke(width = 1.5.dp.toPx()),
          )

          // Núcleo esférico do Orbe Zenith
          drawCircle(
            brush = Brush.radialGradient(
              colors = listOf(
                animatedAccent.copy(alpha = 0.42f),
                animatedContainer,
              ),
              center = Offset(center.x - maxR * 0.14f, center.y - maxR * 0.14f),
              radius = maxR * 0.56f,
            ),
            radius = maxR * 0.54f,
            center = center,
          )

          drawCircle(
            color = animatedAccent.copy(alpha = 0.75f),
            radius = maxR * 0.54f,
            center = center,
            style = Stroke(width = 2.dp.toPx()),
          )
        }

        // Ícone central dentro do Orbe (tocar alterna para o próximo modo)
        Box(
          modifier = Modifier
            .size(78.dp)
            .clip(CircleShape)
            .clickable {
              val modes = SoundMode.entries
              val nextMode = modes[(modes.indexOf(currentMode) + 1) % modes.size]
              onSelectMode(nextMode)
            },
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = currentMode.modeIcon(),
            contentDescription = null,
            tint = animatedOnContainer,
            modifier = Modifier.size(34.dp),
          )
        }
      }

      // Tipografia Central do Estado Acústico
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(
          text = currentMode.title,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          textAlign = TextAlign.Center,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        val cleanSubtitle = when {
          currentMode != SoundMode.NORMAL ->
            AppLabels.Now.modeWithMediaSubtitle(currentMode.subtitle, mediaPercent)
          activeRuleSubtitle != null -> activeRuleSubtitle
          else -> currentMode.subtitle
        }
        Text(
          text = cleanSubtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }

      // Pílula Tripla Flutuante dos 3 Modos (Som | Vibrar | Silêncio)
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(5.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          SoundMode.entries.forEach { mode ->
            val selected = mode == currentMode
            val modeAccent = mode.tonalAccentColor()
            val chipBg by animateColorAsState(
              targetValue = if (selected) {
                modeAccent
              } else {
                Color.Transparent
              },
              animationSpec = tween(180),
              label = "zenithModePillBg_${mode.name}",
            )
            val chipContent by animateColorAsState(
              targetValue = if (selected) {
                mode.onActivePillColor()
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
              animationSpec = tween(180),
              label = "zenithModePillContent_${mode.name}",
            )

            Surface(
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 44.dp)
                .clip(RoundedCornerShape(19.dp))
                .clickable { onSelectMode(mode) }
                .semantics { contentDescription = AppLabels.Now.modeContentDescription(mode) }
                .testTag(AppTestTags.Now.modeButton(mode)),
              shape = RoundedCornerShape(19.dp),
              color = chipBg,
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 6.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
              ) {
                Icon(
                  imageVector = mode.modeIcon(),
                  contentDescription = null,
                  tint = chipContent,
                  modifier = Modifier.size(17.dp),
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                  text = AppLabels.Now.modeShortPill(mode),
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                  color = chipContent,
                  maxLines = 1,
                  softWrap = false,
                  overflow = TextOverflow.Ellipsis,
                )
              }
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
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
          )
          FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Row(
              modifier = Modifier.padding(top = 4.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
              Icon(
                imageVector = Icons.Default.AccessTime,
                contentDescription = null,
                tint = animatedAccent,
                modifier = Modifier.size(16.dp),
              )
              val remainingMinutes = activeTimer?.remainingMinutes(System.currentTimeMillis())
              Text(
                text = if (activeTimer != null && remainingMinutes != null && remainingMinutes > 0) {
                  AppLabels.Now.remainingTimerLabel(remainingMinutes)
                } else {
                  AppLabels.Now.keepForLabel
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
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
                    .clickable { onExtendTimer(30) }
                    .testTag(AppTestTags.Now.EXTEND_TIMER_BUTTON),
                  shape = RoundedCornerShape(50),
                  color = MaterialTheme.colorScheme.surfaceContainerHigh,
                  border = BorderStroke(1.dp, animatedAccent.copy(alpha = 0.35f)),
                ) {
                  Text(
                    text = AppLabels.Now.extend30MinChip,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = animatedAccent,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                  )
                }
                FilledTonalButton(
                  onClick = onCancelTimer,
                  contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                  modifier = Modifier.testTag(AppTestTags.Now.CANCEL_TIMER_BUTTON),
                ) {
                  Text(
                    text = AppLabels.Common.restore,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    softWrap = false,
                  )
                }
              }
            } else {
              Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(15 to "15m", 30 to "30m", 60 to "1h", 120 to "2h").forEach { (minutes, label) ->
                  Surface(
                    modifier = Modifier
                      .clip(RoundedCornerShape(50))
                      .clickable { onStartTimer(minutes) }
                      .testTag(AppTestTags.Now.timerChip(minutes)),
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                  ) {
                    Text(
                      text = label,
                      style = MaterialTheme.typography.labelMedium,
                      fontWeight = FontWeight.SemiBold,
                      color = MaterialTheme.colorScheme.onSurface,
                      maxLines = 1,
                      softWrap = false,
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
      .testTag(AppTestTags.Now.MANUAL_OVERRIDE_BANNER),
    shape = RoundedCornerShape(24.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.40f)),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.tertiaryContainer,
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
          text = AppLabels.Now.manualOverrideTitle,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Text(
          text = AppLabels.Now.manualOverrideSubtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
        )
      }
      FilledTonalButton(
        onClick = onReapplyActiveRule,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.testTag(AppTestTags.Now.REAPPLY_RULE_BUTTON),
      ) {
        Icon(
          imageVector = Icons.Default.Refresh,
          contentDescription = null,
          modifier = Modifier.size(16.dp),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(AppLabels.Now.reapplyButton, fontWeight = FontWeight.Bold)
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
      .testTag(AppTestTags.Now.DND_PERMISSION_BANNER),
    shape = RoundedCornerShape(24.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
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
            text = AppLabels.Now.dndBannerTitle,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onErrorContainer,
          )
          Text(
            text = AppLabels.Now.dndBannerSubtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.85f),
          )
        }
      }
      Button(
        onClick = onRequestDndPermission,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.error,
          contentColor = MaterialTheme.colorScheme.onError,
        ),
        modifier = Modifier.testTag(AppTestTags.Now.DND_ENABLE_BUTTON),
      ) {
        Text(AppLabels.Common.enable, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun VolumeStreamsCard(
  streams: VolumeStreams,
  currentMode: SoundMode,
  onStreamVolumeChange: (VolumeStreamType, Int) -> Unit,
) {
  val activeAccent = currentMode.tonalAccentColor()

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(28.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = AppLabels.Now.volumesTitle,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
        )
        Surface(
          shape = RoundedCornerShape(50),
          color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
          Text(
            text = "4 CANAIS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
          )
        }
      }

      PillVolumeSliderRow(
        type = VolumeStreamType.RING,
        icon = Icons.Default.RingVolume,
        percent = streams.ringPercent,
        accentColor = activeAccent,
        onValueChangeFinished = { onStreamVolumeChange(VolumeStreamType.RING, it) },
      )
      PillVolumeSliderRow(
        type = VolumeStreamType.NOTIFICATION,
        icon = Icons.Default.Notifications,
        percent = streams.notificationPercent,
        accentColor = activeAccent,
        onValueChangeFinished = { onStreamVolumeChange(VolumeStreamType.NOTIFICATION, it) },
      )
      PillVolumeSliderRow(
        type = VolumeStreamType.MEDIA,
        icon = Icons.Default.MusicNote,
        percent = streams.mediaPercent,
        accentColor = activeAccent,
        onValueChangeFinished = { onStreamVolumeChange(VolumeStreamType.MEDIA, it) },
      )
      PillVolumeSliderRow(
        type = VolumeStreamType.ALARM,
        icon = Icons.Default.Alarm,
        percent = streams.alarmPercent,
        accentColor = activeAccent,
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
