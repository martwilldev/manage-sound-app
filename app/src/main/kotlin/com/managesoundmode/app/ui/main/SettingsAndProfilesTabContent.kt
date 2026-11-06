package com.managesoundmode.app.ui.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PauseCircleFilled
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.managesoundmode.app.domain.model.AccessControlConfig
import com.managesoundmode.app.domain.model.AppAccessRole
import com.managesoundmode.app.domain.model.AppPermission
import com.managesoundmode.app.domain.model.RestorePolicy
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.domain.model.ThemeMode
import com.managesoundmode.app.domain.model.TransitionLogEntry
import com.managesoundmode.app.domain.model.TransitionTriggerSource
import com.managesoundmode.app.tile.QuickTileKind
import com.managesoundmode.app.ui.common.AppLabels
import com.managesoundmode.app.ui.common.AppLanguage
import com.managesoundmode.app.ui.common.AppTestTags
import com.managesoundmode.app.ui.components.M3CheckSwitch
import com.managesoundmode.app.ui.components.groupedCardShape
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class SettingsCategory {
  APPEARANCE,
  AUTOMATION,
  AUDIO_MEDIA,
  GADGETS_WIDGETS,
  PERMISSIONS,
  ACCESS_CONTROL,
  HISTORY,
  BACKUP_RESET,
  ABOUT,
}

private enum class HistorySourceFilter {
  ALL,
  MANUAL,
  SCHEDULE,
  LOCATION,
  SHORTCUTS,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsAndProfilesTabContent(
  state: MainScreenUiState.Ready,
  onSetRestorePolicy: (RestorePolicy) -> Unit,
  onSetThemeMode: (ThemeMode) -> Unit,
  onSetUseDynamicColor: (Boolean) -> Unit,
  onSetMuteMediaOnSilent: (Boolean) -> Unit,
  onSetPreserveMediaOnBluetooth: (Boolean) -> Unit,
  onSetSmartVolumeTriggerEnabled: (Boolean) -> Unit,
  onTestSmartNotification: () -> Unit,
  onPinHomeWidget: () -> Unit,
  onRequestDndPermission: () -> Unit,
  onRequestExactAlarmPermission: () -> Unit,
  onRequestNotificationPermission: () -> Unit,
  onClearLogs: () -> Unit,
  onSetPersistentQuickBarEnabled: (Boolean) -> Unit = {},
  onRequestAddQuickTile: (QuickTileKind, String?) -> Unit = { _, _ -> },
  onSetQuickTileSlotProfileId: (Int, String) -> Unit = { _, _ -> },
  onSetDefaultDayProfileId: (String) -> Unit = {},
  onSetAppLanguage: (AppLanguage) -> Unit = { AppLabels.currentLanguage = it },
  onSetAutomationsPaused: (Boolean) -> Unit = {},
  onSetHapticFeedbackEnabled: (Boolean) -> Unit = {},
  onSetNotifyOnAutoTransition: (Boolean) -> Unit = {},
  onSetDefaultTimerMinutes: (Int) -> Unit = {},
  onResetFactoryDefaults: () -> Unit = {},
  onExportBackupJson: ((String) -> Unit) -> Unit = {},
  onImportBackupJson: (String) -> Unit = {},
  onShowStatusMessage: (String) -> Unit = {},
  onRequestLocationPermission: () -> Unit = {},
  onReopenOnboarding: () -> Unit = {},
  onSetAccessRole: (AppAccessRole) -> Unit = {},
  onToggleAccessPermission: (AppPermission, Boolean) -> Unit = { _, _ -> },
  onSetAccessPin: (String?) -> Unit = {},
  onUnlockAdminWithPin: (String, () -> Unit) -> Unit = { _, onSuccess -> onSuccess() },
  onSetSystemZenRulesEnabled: (Boolean) -> Unit = {},
  onSyncSystemZenRulesNow: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  var openCategory by rememberSaveable { mutableStateOf<SettingsCategory?>(null) }
  var showResetConfirmDialog by rememberSaveable { mutableStateOf(false) }
  var showPinChallengeForAccessControl by rememberSaveable { mutableStateOf(false) }

  val defaultProfile = state.profiles.firstOrNull { it.id == state.defaultDayProfileId }
    ?: state.profiles.firstOrNull()
  val slot1Profile = state.profiles.firstOrNull { it.id == state.quickTileSlot1ProfileId }
    ?: state.profiles.firstOrNull()
  val slot2Profile = state.profiles.firstOrNull { it.id == state.quickTileSlot2ProfileId }
    ?: state.profiles.getOrNull(1)
    ?: state.profiles.firstOrNull()

  val grantedPermissionsCount = listOf(
    state.hasDndPermission,
    state.hasNotificationPermission,
    state.canScheduleExactAlarms,
    true, // Armazenamento 100% On-Device
  ).count { it }

  if (showPinChallengeForAccessControl) {
    AccessPinChallengeDialog(
      config = state.accessControlConfig,
      onDismiss = { showPinChallengeForAccessControl = false },
      onVerifyAndOpenSubScreen = {
        showPinChallengeForAccessControl = false
        openCategory = SettingsCategory.ACCESS_CONTROL
      },
      onUnlockAdminWithPin = { pin ->
        onUnlockAdminWithPin(pin) {
          showPinChallengeForAccessControl = false
          openCategory = SettingsCategory.ACCESS_CONTROL
        }
      },
      onShowStatusMessage = onShowStatusMessage,
    )
  }

  if (showResetConfirmDialog) {
    FactoryResetConfirmDialog(
      onConfirm = {
        showResetConfirmDialog = false
        openCategory = null
        onResetFactoryDefaults()
      },
      onDismiss = { showResetConfirmDialog = false },
    )
  }

  if (openCategory != null) {
    SettingsCategoryFullScreenDialog(
      category = openCategory!!,
      state = state,
      onDismiss = { openCategory = null },
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
      onRequestFactoryResetConfirm = {
        if (!state.accessControlConfig.hasPermission(AppPermission.BACKUP_AND_FACTORY_RESET)) {
          onShowStatusMessage(
            AppLabels.AccessControl.permissionDeniedMessage(AppPermission.BACKUP_AND_FACTORY_RESET),
          )
        } else {
          showResetConfirmDialog = true
        }
      },
      onExportBackupJson = onExportBackupJson,
      onImportBackupJson = onImportBackupJson,
      onShowStatusMessage = onShowStatusMessage,
      onRequestLocationPermission = onRequestLocationPermission,
      onReopenOnboarding = {
        openCategory = null
        onReopenOnboarding()
      },
      onSetAccessRole = onSetAccessRole,
      onToggleAccessPermission = onToggleAccessPermission,
      onSetAccessPin = onSetAccessPin,
      onSetSystemZenRulesEnabled = onSetSystemZenRulesEnabled,
      onSyncSystemZenRulesNow = onSyncSystemZenRulesNow,
    )
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .testTag(AppTestTags.Settings.LIST),
    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 36.dp),
    verticalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    // 0. Card de Topo — Chave Geral de Automação (Modo Férias)
    item {
      MasterAutomationBannerCard(
        automationsPaused = state.automationsPaused,
        onToggleAutomationsPaused = { enabled -> onSetAutomationsPaused(!enabled) },
      )
      Spacer(modifier = Modifier.height(14.dp))
    }

    // Grupo 1: Preferências e Comportamento (4 Categorias)
    item {
      SectionHeader(title = AppLabels.Settings.hubPreferencesGroupTitle)
    }

    item {
      SettingsCategoryRowCard(
        icon = Icons.Default.Palette,
        title = AppLabels.Settings.categoryAppearanceTitle,
        subtitle = AppLabels.Settings.appearanceSummary(
          themeLabel = state.themeMode.label,
          languageLabel = AppLabels.currentLanguage.shortLabel,
          dynamicColor = state.useDynamicColor,
        ),
        shape = groupedCardShape(index = 0, totalCount = 4),
        testTag = AppTestTags.Settings.CATEGORY_APPEARANCE,
        onClick = { openCategory = SettingsCategory.APPEARANCE },
      )
    }

    item {
      SettingsCategoryRowCard(
        icon = Icons.Default.Autorenew,
        title = AppLabels.Settings.categoryAutomationTitle,
        subtitle = AppLabels.Settings.automationSummary(
          policyTitle = state.restorePolicy.title,
          defaultProfileName = defaultProfile?.name ?: "-",
          paused = state.automationsPaused,
        ),
        shape = groupedCardShape(index = 1, totalCount = 4),
        testTag = AppTestTags.Settings.CATEGORY_AUTOMATION,
        onClick = { openCategory = SettingsCategory.AUTOMATION },
      )
    }

    item {
      SettingsCategoryRowCard(
        icon = Icons.AutoMirrored.Filled.VolumeUp,
        title = AppLabels.Settings.categoryAudioMediaTitle,
        subtitle = AppLabels.Settings.audioMediaSummary(
          muteMedia = state.muteMediaOnSilent,
          preserveBt = state.preserveMediaOnBluetooth,
          defaultTimerMinutes = state.defaultTimerMinutes,
        ),
        shape = groupedCardShape(index = 2, totalCount = 4),
        testTag = AppTestTags.Settings.CATEGORY_AUDIO_MEDIA,
        onClick = { openCategory = SettingsCategory.AUDIO_MEDIA },
      )
    }

    item {
      SettingsCategoryRowCard(
        icon = Icons.Default.Widgets,
        title = AppLabels.Settings.categoryGadgetsWidgetsTitle,
        subtitle = AppLabels.Settings.gadgetsSummary(
          quickBarEnabled = state.persistentQuickBarEnabled,
          slot1Name = slot1Profile?.name ?: "-",
          slot2Name = slot2Profile?.name ?: "-",
        ),
        shape = groupedCardShape(index = 3, totalCount = 4),
        testTag = AppTestTags.Settings.CATEGORY_GADGETS_WIDGETS,
        onClick = { openCategory = SettingsCategory.GADGETS_WIDGETS },
      )
    }

    // Grupo 2: Sistema, Dados e Informações (5 Categorias)
    item {
      Spacer(modifier = Modifier.height(14.dp))
      SectionHeader(title = AppLabels.Settings.hubSystemGroupTitle)
    }

    item {
      SettingsCategoryRowCard(
        icon = Icons.Default.Security,
        title = AppLabels.Settings.categoryPermissionsTitle,
        subtitle = AppLabels.Settings.permissionsSummary(
          grantedCount = grantedPermissionsCount,
          totalCount = 4,
        ),
        shape = groupedCardShape(index = 0, totalCount = 5),
        testTag = AppTestTags.Settings.CATEGORY_PERMISSIONS,
        hasWarningBadge = grantedPermissionsCount < 4,
        onClick = { openCategory = SettingsCategory.PERMISSIONS },
      )
    }

    item {
      val allowedCount = if (state.accessControlConfig.activeRole == AppAccessRole.ADMIN) {
        AppPermission.entries.size
      } else {
        state.accessControlConfig.grantedPermissions.size
      }
      SettingsCategoryRowCard(
        icon = Icons.Default.Lock,
        title = AppLabels.Settings.categoryAccessControlTitle,
        subtitle = AppLabels.Settings.accessControlSummary(
          roleTitle = state.accessControlConfig.activeRole.shortBadge,
          grantedCount = allowedCount,
          totalCount = AppPermission.entries.size,
          hasPin = state.accessControlConfig.isPinRequired,
        ),
        shape = groupedCardShape(index = 1, totalCount = 5),
        testTag = AppTestTags.Settings.CATEGORY_ACCESS_CONTROL,
        hasWarningBadge = state.accessControlConfig.isRestricted,
        onClick = {
          if (state.accessControlConfig.isPinRequired && state.accessControlConfig.activeRole != AppAccessRole.ADMIN) {
            showPinChallengeForAccessControl = true
          } else {
            openCategory = SettingsCategory.ACCESS_CONTROL
          }
        },
      )
    }

    item {
      SettingsCategoryRowCard(
        icon = Icons.Default.History,
        title = AppLabels.Settings.categoryHistoryTitle,
        subtitle = AppLabels.Settings.historySummary(state.transitionLogs.size),
        shape = groupedCardShape(index = 2, totalCount = 5),
        testTag = AppTestTags.Settings.CATEGORY_HISTORY,
        onClick = { openCategory = SettingsCategory.HISTORY },
      )
    }

    item {
      SettingsCategoryRowCard(
        icon = Icons.Default.Backup,
        title = AppLabels.Settings.categoryBackupResetTitle,
        subtitle = AppLabels.Settings.backupSummary(
          profilesCount = state.profiles.size,
          schedulesCount = state.scheduleRules.size,
          locationsCount = state.locationRules.size,
        ),
        shape = groupedCardShape(index = 3, totalCount = 5),
        testTag = AppTestTags.Settings.CATEGORY_BACKUP_RESET,
        onClick = { openCategory = SettingsCategory.BACKUP_RESET },
      )
    }

    item {
      SettingsCategoryRowCard(
        icon = Icons.Default.Info,
        title = AppLabels.Settings.categoryAboutTitle,
        subtitle = AppLabels.Settings.aboutVersionSubtitle,
        shape = groupedCardShape(index = 4, totalCount = 5),
        testTag = AppTestTags.Settings.CATEGORY_ABOUT,
        onClick = { openCategory = SettingsCategory.ABOUT },
      )
    }
  }
}

@Composable
internal fun FactoryResetConfirmDialog(
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.Restore,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.error,
      )
    },
    title = {
      Text(
        text = AppLabels.Settings.factoryResetDialogTitle,
        fontWeight = FontWeight.SemiBold,
      )
    },
    text = {
      Text(text = AppLabels.Settings.factoryResetDialogMessage)
    },
    confirmButton = {
      Button(
        onClick = onConfirm,
        colors = ButtonDefaults.buttonColors(
          containerColor = MaterialTheme.colorScheme.error,
          contentColor = MaterialTheme.colorScheme.onError,
        ),
        modifier = Modifier.testTag(AppTestTags.Settings.CONFIRM_RESET_DEFAULTS_BUTTON),
      ) {
        Text(AppLabels.Common.restore)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(AppLabels.Common.cancel)
      }
    },
  )
}

@Composable
private fun MasterAutomationBannerCard(
  automationsPaused: Boolean,
  onToggleAutomationsPaused: (Boolean) -> Unit,
) {
  val isAutomationEnabled = !automationsPaused
  val containerColor = if (isAutomationEnabled) {
    MaterialTheme.colorScheme.secondaryContainer
  } else {
    MaterialTheme.colorScheme.tertiaryContainer
  }
  val contentColor = if (isAutomationEnabled) {
    MaterialTheme.colorScheme.onSecondaryContainer
  } else {
    MaterialTheme.colorScheme.onTertiaryContainer
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag(AppTestTags.Settings.MASTER_AUTOMATION_CARD),
    shape = RoundedCornerShape(26.dp),
    colors = CardDefaults.cardColors(containerColor = containerColor),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 18.dp, vertical = 16.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = Modifier
          .weight(1f)
          .padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Icon(
          imageVector = if (isAutomationEnabled) {
            Icons.Default.PlayCircleFilled
          } else {
            Icons.Default.PauseCircleFilled
          },
          contentDescription = null,
          tint = contentColor,
          modifier = Modifier.size(30.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = AppLabels.Settings.masterAutomationTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = contentColor,
          )
          Text(
            text = if (isAutomationEnabled) {
              AppLabels.Settings.masterAutomationActiveSubtitle
            } else {
              AppLabels.Settings.masterAutomationPausedSubtitle
            },
            style = MaterialTheme.typography.bodySmall,
            color = contentColor.copy(alpha = 0.85f),
          )
        }
      }
      M3CheckSwitch(
        checked = isAutomationEnabled,
        onCheckedChange = onToggleAutomationsPaused,
        modifier = Modifier.testTag(AppTestTags.Settings.MASTER_AUTOMATION_SWITCH),
      )
    }
  }
}

@Composable
private fun SettingsCategoryRowCard(
  icon: ImageVector,
  title: String,
  subtitle: String,
  shape: RoundedCornerShape,
  testTag: String,
  hasWarningBadge: Boolean = false,
  onClick: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(shape)
      .clickable(onClick = onClick)
      .testTag(testTag),
    shape = shape,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 15.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
      Box(
        modifier = Modifier
          .size(42.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSecondaryContainer,
          modifier = Modifier.size(21.dp),
        )
      }

      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(2.dp),
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurface,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }

      if (hasWarningBadge) {
        Icon(
          imageVector = Icons.Default.WarningAmber,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.error,
          modifier = Modifier.size(20.dp),
        )
      }

      Icon(
        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun SettingsCategoryFullScreenDialog(
  category: SettingsCategory,
  state: MainScreenUiState.Ready,
  onDismiss: () -> Unit,
  onSetRestorePolicy: (RestorePolicy) -> Unit = {},
  onSetDefaultDayProfileId: (String) -> Unit = {},
  onSetThemeMode: (ThemeMode) -> Unit = {},
  onSetUseDynamicColor: (Boolean) -> Unit = {},
  onSetAppLanguage: (AppLanguage) -> Unit = { AppLabels.currentLanguage = it },
  onSetAutomationsPaused: (Boolean) -> Unit = {},
  onSetHapticFeedbackEnabled: (Boolean) -> Unit = {},
  onSetNotifyOnAutoTransition: (Boolean) -> Unit = {},
  onSetDefaultTimerMinutes: (Int) -> Unit = {},
  onSetMuteMediaOnSilent: (Boolean) -> Unit = {},
  onSetPreserveMediaOnBluetooth: (Boolean) -> Unit = {},
  onSetSmartVolumeTriggerEnabled: (Boolean) -> Unit = {},
  onTestSmartNotification: () -> Unit = {},
  onSetPersistentQuickBarEnabled: (Boolean) -> Unit = {},
  onRequestAddQuickTile: (QuickTileKind, String?) -> Unit = { _, _ -> },
  onSetQuickTileSlotProfileId: (Int, String) -> Unit = { _, _ -> },
  onPinHomeWidget: () -> Unit = {},
  onRequestDndPermission: () -> Unit = {},
  onRequestExactAlarmPermission: () -> Unit = {},
  onRequestNotificationPermission: () -> Unit = {},
  onClearLogs: () -> Unit = {},
  onRequestFactoryResetConfirm: () -> Unit = {},
  onExportBackupJson: ((String) -> Unit) -> Unit = {},
  onImportBackupJson: (String) -> Unit = {},
  onShowStatusMessage: (String) -> Unit = {},
  onRequestLocationPermission: () -> Unit = {},
  onReopenOnboarding: () -> Unit = {},
  onSetAccessRole: (AppAccessRole) -> Unit = {},
  onToggleAccessPermission: (AppPermission, Boolean) -> Unit = { _, _ -> },
  onSetAccessPin: (String?) -> Unit = {},
  onSetSystemZenRulesEnabled: (Boolean) -> Unit = {},
  onSyncSystemZenRulesNow: () -> Unit = {},
) {
  val context = LocalContext.current
  val title = when (category) {
    SettingsCategory.APPEARANCE -> AppLabels.Settings.categoryAppearanceTitle
    SettingsCategory.AUTOMATION -> AppLabels.Settings.categoryAutomationTitle
    SettingsCategory.AUDIO_MEDIA -> AppLabels.Settings.categoryAudioMediaTitle
    SettingsCategory.GADGETS_WIDGETS -> AppLabels.Settings.categoryGadgetsWidgetsTitle
    SettingsCategory.PERMISSIONS -> AppLabels.Settings.categoryPermissionsTitle
    SettingsCategory.ACCESS_CONTROL -> AppLabels.Settings.categoryAccessControlTitle
    SettingsCategory.HISTORY -> AppLabels.Settings.categoryHistoryTitle
    SettingsCategory.BACKUP_RESET -> AppLabels.Settings.categoryBackupResetTitle
    SettingsCategory.ABOUT -> AppLabels.Settings.categoryAboutTitle
  }

  Dialog(
    onDismissRequest = onDismiss,
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
              onClick = onDismiss,
              modifier = Modifier.testTag(AppTestTags.Settings.CLOSE_SUBSCREEN_BUTTON),
            ) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = AppLabels.Common.back,
              )
            }
          },
          title = {
            Text(
              text = title,
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.SemiBold,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          },
          actions = {
            if (category == SettingsCategory.HISTORY && state.transitionLogs.isNotEmpty()) {
              TextButton(
                onClick = onClearLogs,
                modifier = Modifier.testTag(AppTestTags.Settings.CLEAR_HISTORY_BUTTON),
              ) {
                Text(AppLabels.Common.clear)
              }
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
          ),
        )
      },
    ) { innerPadding ->
      when (category) {
        SettingsCategory.APPEARANCE -> {
          AppearanceSettingsSubScreen(
            state = state,
            onSetThemeMode = onSetThemeMode,
            onSetAppLanguage = onSetAppLanguage,
            onSetUseDynamicColor = onSetUseDynamicColor,
            onSetHapticFeedbackEnabled = onSetHapticFeedbackEnabled,
            modifier = Modifier.padding(innerPadding),
          )
        }

        SettingsCategory.AUTOMATION -> {
          AutomationSettingsSubScreen(
            state = state,
            onSetAutomationsPaused = onSetAutomationsPaused,
            onSetRestorePolicy = onSetRestorePolicy,
            onSetDefaultDayProfileId = onSetDefaultDayProfileId,
            onSetNotifyOnAutoTransition = onSetNotifyOnAutoTransition,
            onSetSystemZenRulesEnabled = onSetSystemZenRulesEnabled,
            onSyncSystemZenRulesNow = onSyncSystemZenRulesNow,
            modifier = Modifier.padding(innerPadding),
          )
        }

        SettingsCategory.AUDIO_MEDIA -> {
          AudioAndTimerSettingsSubScreen(
            state = state,
            onSetSmartVolumeTriggerEnabled = onSetSmartVolumeTriggerEnabled,
            onTestSmartNotification = onTestSmartNotification,
            onSetMuteMediaOnSilent = onSetMuteMediaOnSilent,
            onSetPreserveMediaOnBluetooth = onSetPreserveMediaOnBluetooth,
            onSetDefaultTimerMinutes = onSetDefaultTimerMinutes,
            modifier = Modifier.padding(innerPadding),
          )
        }

        SettingsCategory.GADGETS_WIDGETS -> {
          GadgetsAndWidgetsSubScreen(
            state = state,
            onSetPersistentQuickBarEnabled = onSetPersistentQuickBarEnabled,
            onRequestAddQuickTile = onRequestAddQuickTile,
            onSetQuickTileSlotProfileId = onSetQuickTileSlotProfileId,
            onPinHomeWidget = onPinHomeWidget,
            modifier = Modifier.padding(innerPadding),
          )
        }

        SettingsCategory.PERMISSIONS -> {
          PermissionsDiagnosticSubScreen(
            state = state,
            onRequestDndPermission = onRequestDndPermission,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onRequestExactAlarmPermission = onRequestExactAlarmPermission,
            onRequestLocationPermission = onRequestLocationPermission,
            onReopenOnboarding = {
              onDismiss()
              onReopenOnboarding()
            },
            modifier = Modifier.padding(innerPadding),
          )
        }

        SettingsCategory.ACCESS_CONTROL -> {
          AccessControlSubScreen(
            config = state.accessControlConfig,
            onSetAccessRole = onSetAccessRole,
            onToggleAccessPermission = onToggleAccessPermission,
            onSetAccessPin = onSetAccessPin,
            modifier = Modifier.padding(innerPadding),
          )
        }

        SettingsCategory.HISTORY -> {
          TransitionHistorySubScreen(
            logs = state.transitionLogs,
            modifier = Modifier.padding(innerPadding),
          )
        }

        SettingsCategory.BACKUP_RESET -> {
          BackupAndResetSubScreen(
            context = context,
            onExportBackupJson = onExportBackupJson,
            onImportBackupJson = onImportBackupJson,
            onRequestFactoryResetConfirm = onRequestFactoryResetConfirm,
            onShowStatusMessage = onShowStatusMessage,
            modifier = Modifier.padding(innerPadding),
          )
        }

        SettingsCategory.ABOUT -> {
          AboutAndPrivacySubScreen(
            modifier = Modifier.padding(innerPadding),
          )
        }
      }
    }
  }
}

@Composable
internal fun AccessPinChallengeDialog(
  config: AccessControlConfig,
  onDismiss: () -> Unit,
  onVerifyAndOpenSubScreen: () -> Unit,
  onUnlockAdminWithPin: (String) -> Unit,
  onShowStatusMessage: (String) -> Unit,
) {
  var pinInput by rememberSaveable { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    modifier = Modifier.testTag(AppTestTags.AccessControl.PIN_CHALLENGE_DIALOG),
    icon = {
      Icon(
        imageVector = Icons.Default.Lock,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
      )
    },
    title = {
      Text(
        text = AppLabels.AccessControl.pinChallengeDialogTitle,
        fontWeight = FontWeight.SemiBold,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = AppLabels.AccessControl.pinChallengeDialogSubtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
          value = pinInput,
          onValueChange = { raw ->
            pinInput = raw.filter { it.isDigit() }.take(4)
          },
          label = { Text(AppLabels.AccessControl.pinChallengeInputLabel) },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag(AppTestTags.AccessControl.PIN_CHALLENGE_INPUT),
        )
      }
    },
    confirmButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalButton(
          onClick = { onUnlockAdminWithPin(pinInput) },
          modifier = Modifier.testTag(AppTestTags.AccessControl.PIN_CHALLENGE_QUICK_ADMIN_BUTTON),
        ) {
          Text(AppLabels.AccessControl.quickUnlockAdminButton)
        }
        Button(
          onClick = {
            if (config.verifyPin(pinInput)) {
              onVerifyAndOpenSubScreen()
            } else {
              onShowStatusMessage(AppLabels.AccessControl.pinChallengeWrongError)
            }
          },
          modifier = Modifier.testTag(AppTestTags.AccessControl.PIN_CHALLENGE_UNLOCK_BUTTON),
        ) {
          Text(AppLabels.AccessControl.pinChallengeUnlockButton)
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(AppLabels.Common.cancel)
      }
    },
  )
}

@Composable
private fun AccessControlSubScreen(
  config: AccessControlConfig,
  onSetAccessRole: (AppAccessRole) -> Unit,
  onToggleAccessPermission: (AppPermission, Boolean) -> Unit,
  onSetAccessPin: (String?) -> Unit,
  modifier: Modifier = Modifier,
) {
  var pinDraft by rememberSaveable(config.pinCode) {
    mutableStateOf(config.pinCode.orEmpty())
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp, vertical = 12.dp)
      .testTag(AppTestTags.AccessControl.SUBSCREEN),
    verticalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    // 1. Seleção de Papel (RBAC Roles)
    SectionHeader(title = AppLabels.AccessControl.sectionRoleTitle)
    Text(
      text = AppLabels.AccessControl.sectionRoleSubtitle,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(bottom = 8.dp),
    )

    AppAccessRole.entries.forEachIndexed { idx, role ->
      val isSelected = config.activeRole == role
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .clip(groupedCardShape(index = idx, totalCount = AppAccessRole.entries.size))
          .clickable { onSetAccessRole(role) }
          .testTag(AppTestTags.AccessControl.roleCard(role)),
        shape = groupedCardShape(index = idx, totalCount = AppAccessRole.entries.size),
        colors = CardDefaults.cardColors(
          containerColor = if (isSelected) {
            MaterialTheme.colorScheme.secondaryContainer
          } else {
            MaterialTheme.colorScheme.surfaceContainerLow
          },
        ),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
            imageVector = if (isSelected) {
              Icons.Default.RadioButtonChecked
            } else {
              Icons.Default.RadioButtonUnchecked
            },
            contentDescription = null,
            tint = if (isSelected) {
              MaterialTheme.colorScheme.primary
            } else {
              MaterialTheme.colorScheme.onSurfaceVariant
            },
          )
          Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
              text = role.title,
              style = MaterialTheme.typography.bodyLarge,
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              text = role.subtitle,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 2. Proteção por PIN de 4 dígitos (Opcional)
    SectionHeader(title = AppLabels.AccessControl.sectionPinTitle)
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            text = AppLabels.AccessControl.sectionPinSubtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f).padding(end = 12.dp),
          )
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = if (config.isPinRequired) {
              MaterialTheme.colorScheme.primaryContainer
            } else {
              MaterialTheme.colorScheme.surfaceContainerHighest
            },
          ) {
            Text(
              text = if (config.isPinRequired) {
                AppLabels.AccessControl.pinActiveBadge
              } else {
                AppLabels.AccessControl.pinInactiveBadge
              },
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }
        }

        OutlinedTextField(
          value = pinDraft,
          onValueChange = { raw ->
            pinDraft = raw.filter { it.isDigit() }.take(4)
          },
          label = { Text(AppLabels.AccessControl.pinInputLabel) },
          singleLine = true,
          modifier = Modifier
            .fillMaxWidth()
            .testTag(AppTestTags.AccessControl.PIN_INPUT),
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Button(
            onClick = { onSetAccessPin(pinDraft) },
            modifier = Modifier
              .weight(1f)
              .testTag(AppTestTags.AccessControl.SAVE_PIN_BUTTON),
          ) {
            Text(AppLabels.AccessControl.savePinButton)
          }
          if (config.isPinRequired) {
            OutlinedButton(
              onClick = {
                pinDraft = ""
                onSetAccessPin(null)
              },
              modifier = Modifier.testTag(AppTestTags.AccessControl.REMOVE_PIN_BUTTON),
            ) {
              Text(AppLabels.AccessControl.removePinButton)
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 3. Matriz Granular de Permissões (7 ações)
    SectionHeader(title = AppLabels.AccessControl.sectionMatrixTitle)
    Text(
      text = AppLabels.AccessControl.sectionMatrixSubtitle,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(bottom = 8.dp),
    )

    AppPermission.entries.forEachIndexed { idx, permission ->
      val isGranted = config.hasPermission(permission)
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = groupedCardShape(index = idx, totalCount = AppPermission.entries.size),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(
            modifier = Modifier
              .weight(1f)
              .padding(end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
          ) {
            Text(
              text = permission.title,
              style = MaterialTheme.typography.bodyLarge,
              fontWeight = FontWeight.Medium,
            )
            Text(
              text = permission.subtitle,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          M3CheckSwitch(
            checked = isGranted,
            onCheckedChange = { enabled ->
              onToggleAccessPermission(permission, enabled)
            },
            modifier = Modifier.testTag(AppTestTags.AccessControl.permissionSwitch(permission)),
          )
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppearanceSettingsSubScreen(
  state: MainScreenUiState.Ready,
  onSetThemeMode: (ThemeMode) -> Unit,
  onSetAppLanguage: (AppLanguage) -> Unit,
  onSetUseDynamicColor: (Boolean) -> Unit,
  onSetHapticFeedbackEnabled: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    // 1. Tema
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = groupedCardShape(index = 0, totalCount = 4),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = AppLabels.Settings.appThemeTitle,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Medium,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          ThemeMode.entries.forEachIndexed { idx, mode ->
            SegmentedButton(
              selected = state.themeMode == mode,
              onClick = { onSetThemeMode(mode) },
              shape = SegmentedButtonDefaults.itemShape(index = idx, count = ThemeMode.entries.size),
              icon = {
                Icon(
                  imageVector = when (mode) {
                    ThemeMode.SYSTEM -> Icons.Default.SettingsBrightness
                    ThemeMode.LIGHT -> Icons.Default.LightMode
                    ThemeMode.DARK -> Icons.Default.DarkMode
                  },
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
              },
              modifier = Modifier.testTag(AppTestTags.Settings.themeButton(mode)),
            ) {
              Text(mode.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }
        }
      }
    }

    // 2. Idioma
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = groupedCardShape(index = 1, totalCount = 4),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = AppLabels.Settings.appLanguageTitle,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Medium,
        )
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
          AppLanguage.entries.forEachIndexed { idx, language ->
            SegmentedButton(
              selected = AppLabels.currentLanguage == language,
              onClick = { onSetAppLanguage(language) },
              shape = SegmentedButtonDefaults.itemShape(index = idx, count = AppLanguage.entries.size),
              modifier = Modifier.testTag(AppTestTags.Settings.languageButton(language)),
            ) {
              Text(language.shortLabel)
            }
          }
        }
      }
    }

    // 3. Cores Dinâmicas (Material You)
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = groupedCardShape(index = 2, totalCount = 4),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Palette,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Column {
            Text(
              text = AppLabels.Settings.dynamicColorsTitle,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Medium,
            )
            Text(
              text = AppLabels.Settings.dynamicColorsSubtitle,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        M3CheckSwitch(
          checked = state.useDynamicColor,
          onCheckedChange = onSetUseDynamicColor,
          modifier = Modifier.testTag(AppTestTags.Settings.DYNAMIC_COLOR_SWITCH),
        )
      }
    }

    // 4. Vibração Tátil nos Controles
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = groupedCardShape(index = 3, totalCount = 4),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Vibration,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Column {
            Text(
              text = AppLabels.Settings.hapticFeedbackTitle,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Medium,
            )
            Text(
              text = AppLabels.Settings.hapticFeedbackSubtitle,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        M3CheckSwitch(
          checked = state.hapticFeedbackEnabled,
          onCheckedChange = onSetHapticFeedbackEnabled,
          modifier = Modifier.testTag(AppTestTags.Settings.HAPTIC_FEEDBACK_SWITCH),
        )
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AutomationSettingsSubScreen(
  state: MainScreenUiState.Ready,
  onSetAutomationsPaused: (Boolean) -> Unit,
  onSetRestorePolicy: (RestorePolicy) -> Unit,
  onSetDefaultDayProfileId: (String) -> Unit,
  onSetNotifyOnAutoTransition: (Boolean) -> Unit,
  onSetSystemZenRulesEnabled: (Boolean) -> Unit = {},
  onSyncSystemZenRulesNow: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val restorePolicies = RestorePolicy.entries
  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    MasterAutomationBannerCard(
      automationsPaused = state.automationsPaused,
      onToggleAutomationsPaused = { enabled -> onSetAutomationsPaused(!enabled) },
    )

    SectionHeader(title = AppLabels.Settings.restorePolicySection)

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
      restorePolicies.forEachIndexed { index, policy ->
        val selected = state.restorePolicy == policy
        val shape = groupedCardShape(index = index, totalCount = restorePolicies.size)
        Card(
          modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable { onSetRestorePolicy(policy) }
            .testTag(AppTestTags.Settings.restorePolicy(policy)),
          shape = shape,
          colors = CardDefaults.cardColors(
            containerColor = if (selected) {
              MaterialTheme.colorScheme.secondaryContainer
            } else {
              MaterialTheme.colorScheme.surfaceContainerLow
            },
          ),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(
              imageVector = if (selected) {
                Icons.Default.RadioButtonChecked
              } else {
                Icons.Default.RadioButtonUnchecked
              },
              contentDescription = null,
              tint = if (selected) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = policy.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
              )
              Text(
                text = policy.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
        }
      }
    }

    // Seleção unificada do Perfil Padrão de Retorno
    SectionHeader(title = AppLabels.Profiles.defaultReturnSectionTitle)
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
        Text(
          text = AppLabels.Profiles.defaultReturnSectionSubtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          state.profiles.forEach { profile ->
            val selected = profile.id == state.defaultDayProfileId
            FilterChip(
              selected = selected,
              onClick = { onSetDefaultDayProfileId(profile.id) },
              label = { Text(profile.name) },
              leadingIcon = if (selected) {
                {
                  Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                  )
                }
              } else {
                null
              },
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier.testTag(AppTestTags.Settings.defaultReturnProfileChip(profile.id)),
            )
          }
        }
      }
    }

    // Notificar ao ativar rotina automática
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
            imageVector = Icons.Default.NotificationsActive,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Column {
            Text(
              text = AppLabels.Settings.notifyOnAutoTransitionTitle,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Medium,
            )
            Text(
              text = AppLabels.Settings.notifyOnAutoTransitionSubtitle,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        M3CheckSwitch(
          checked = state.notifyOnAutoTransition,
          onCheckedChange = onSetNotifyOnAutoTransition,
          modifier = Modifier.testTag(AppTestTags.Settings.NOTIFY_AUTO_TRANSITION_SWITCH),
        )
      }
    }

    // [AUTO-04] Modos do Sistema Android 15+ (AutomaticZenRule)
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(
              imageVector = Icons.Default.Autorenew,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Column {
              Text(
                text = AppLabels.Settings.systemZenRulesTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
              )
              Text(
                text = AppLabels.Settings.systemZenRulesSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          M3CheckSwitch(
            checked = state.systemZenRulesEnabled,
            onCheckedChange = onSetSystemZenRulesEnabled,
            modifier = Modifier.testTag(AppTestTags.Settings.SYSTEM_ZEN_RULES_SWITCH),
          )
        }

        FilledTonalButton(
          onClick = onSyncSystemZenRulesNow,
          modifier = Modifier
            .fillMaxWidth()
            .testTag(AppTestTags.Settings.SYNC_ZEN_RULES_BUTTON),
        ) {
          Text(AppLabels.Settings.syncZenRulesButton)
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AudioAndTimerSettingsSubScreen(
  state: MainScreenUiState.Ready,
  onSetSmartVolumeTriggerEnabled: (Boolean) -> Unit,
  onTestSmartNotification: () -> Unit,
  onSetMuteMediaOnSilent: (Boolean) -> Unit,
  onSetPreserveMediaOnBluetooth: (Boolean) -> Unit,
  onSetDefaultTimerMinutes: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val timerOptions = listOf(15, 30, 60, 120)

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    // 1. Sugerir Timer ao Silenciar
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = groupedCardShape(index = 0, totalCount = 4),
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
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Icon(
              imageVector = Icons.Default.NotificationsActive,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Column {
              Text(
                text = AppLabels.Settings.suggestTimerTitle,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
              )
              Text(
                text = AppLabels.Settings.suggestTimerSubtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          M3CheckSwitch(
            checked = state.smartVolumeTriggerEnabled,
            onCheckedChange = onSetSmartVolumeTriggerEnabled,
            modifier = Modifier.testTag(AppTestTags.Settings.SUGGEST_TIMER_SWITCH),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End,
        ) {
          TextButton(
            onClick = onTestSmartNotification,
            modifier = Modifier.testTag(AppTestTags.Settings.TEST_SMART_NOTIFICATION_BUTTON),
          ) {
            Text(AppLabels.Settings.testSmartNotificationButton)
          }
        }
      }
    }

    // 2. Zerar Mídia ao Silenciar
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = groupedCardShape(index = 1, totalCount = 4),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
            imageVector = Icons.Default.MusicOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Column {
            Text(
              text = AppLabels.Settings.muteMediaOnSilentTitle,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Medium,
            )
            Text(
              text = AppLabels.Settings.muteMediaOnSilentSubtitle,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        M3CheckSwitch(
          checked = state.muteMediaOnSilent,
          onCheckedChange = onSetMuteMediaOnSilent,
          modifier = Modifier.testTag(AppTestTags.Settings.MUTE_MEDIA_SWITCH),
        )
      }
    }

    // 3. Preservar Mídia no Bluetooth
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = groupedCardShape(index = 2, totalCount = 4),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
            imageVector = Icons.Default.BluetoothAudio,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Column {
            Text(
              text = AppLabels.Settings.preserveBluetoothTitle,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Medium,
            )
            Text(
              text = AppLabels.Settings.preserveBluetoothSubtitle,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        M3CheckSwitch(
          checked = state.preserveMediaOnBluetooth,
          onCheckedChange = onSetPreserveMediaOnBluetooth,
          modifier = Modifier.testTag(AppTestTags.Settings.PRESERVE_BT_SWITCH),
        )
      }
    }

    // 4. Duração Padrão do Timer Rápido
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = groupedCardShape(index = 3, totalCount = 4),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = AppLabels.Settings.defaultTimerSectionTitle,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Medium,
            )
            Text(
              text = AppLabels.Settings.defaultTimerSectionSubtitle,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          timerOptions.forEach { minutes ->
            val selected = state.defaultTimerMinutes == minutes
            val labelText = when (minutes) {
              60 -> "1h"
              120 -> "2h"
              else -> "${minutes}m"
            }
            FilterChip(
              selected = selected,
              onClick = { onSetDefaultTimerMinutes(minutes) },
              label = { Text(labelText) },
              modifier = Modifier.testTag(AppTestTags.Settings.defaultTimerChip(minutes)),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun GadgetsAndWidgetsSubScreen(
  state: MainScreenUiState.Ready,
  onSetPersistentQuickBarEnabled: (Boolean) -> Unit,
  onRequestAddQuickTile: (QuickTileKind, String?) -> Unit,
  onSetQuickTileSlotProfileId: (Int, String) -> Unit,
  onPinHomeWidget: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val slot1Profile = state.profiles.firstOrNull { it.id == state.quickTileSlot1ProfileId }
    ?: state.profiles.firstOrNull()
  val slot2Profile = state.profiles.firstOrNull { it.id == state.quickTileSlot2ProfileId }
    ?: state.profiles.getOrNull(1)
    ?: state.profiles.firstOrNull()

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    // 1. Barra Rápida Persistente na Gaveta de Notificações
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag(AppTestTags.Settings.PERSISTENT_QUICK_BAR_CARD),
      shape = groupedCardShape(index = 0, totalCount = 5),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
            imageVector = Icons.Default.NotificationsActive,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Column {
            Text(
              text = AppLabels.Settings.persistentQuickBarTitle,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Medium,
            )
            Text(
              text = AppLabels.Settings.persistentQuickBarSubtitle,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        M3CheckSwitch(
          checked = state.persistentQuickBarEnabled,
          onCheckedChange = onSetPersistentQuickBarEnabled,
          modifier = Modifier.testTag(AppTestTags.Settings.PERSISTENT_QUICK_BAR_SWITCH),
        )
      }
    }

    // 2. Trio de Modos Diretos no Header (Som | Vibrar | Silêncio) + Bloco Ciclo
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag(AppTestTags.Settings.QUICK_SETTINGS_TILE_CARD),
      shape = groupedCardShape(index = 1, totalCount = 5),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
            imageVector = Icons.Default.SettingsBrightness,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = AppLabels.Settings.headerModeTilesTitle,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Medium,
            )
            Text(
              text = AppLabels.Settings.headerModeTilesSubtitle,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilledTonalButton(
            onClick = { onRequestAddQuickTile(QuickTileKind.MODE_NORMAL, null) },
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier
              .weight(1f)
              .testTag(AppTestTags.Settings.ADD_TILE_NORMAL),
          ) {
            Text(AppLabels.Settings.addTileNormalButton, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
          FilledTonalButton(
            onClick = { onRequestAddQuickTile(QuickTileKind.MODE_VIBRATE, null) },
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier
              .weight(1f)
              .testTag(AppTestTags.Settings.ADD_TILE_VIBRATE),
          ) {
            Text(AppLabels.Settings.addTileVibrateButton, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
          FilledTonalButton(
            onClick = { onRequestAddQuickTile(QuickTileKind.MODE_SILENT, null) },
            shape = RoundedCornerShape(14.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
            modifier = Modifier
              .weight(1f)
              .testTag(AppTestTags.Settings.ADD_TILE_SILENT),
          ) {
            Text(AppLabels.Settings.addTileSilentButton, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
        }

        OutlinedButton(
          onClick = { onRequestAddQuickTile(QuickTileKind.CYCLE, null) },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag(AppTestTags.Settings.ADD_QUICK_TILE_BUTTON),
        ) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.size(6.dp))
          Text(AppLabels.Settings.addCycleTileButton)
        }
      }
    }

    // 3. Slots de Perfis Favoritos no Header (Slot 1 e Slot 2)
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag(AppTestTags.Settings.QUICK_PROFILE_TILES_CARD),
      shape = groupedCardShape(index = 2, totalCount = 5),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Style,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = AppLabels.Settings.favoriteProfileTilesTitle,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Medium,
            )
            Text(
              text = AppLabels.Settings.favoriteProfileTilesSubtitle,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        ProfileQuickTileSlotConfigRow(
          slotNumber = 1,
          profiles = state.profiles,
          selectedProfile = slot1Profile,
          onSelectProfile = { profileId -> onSetQuickTileSlotProfileId(1, profileId) },
          onAddTile = {
            onRequestAddQuickTile(
              QuickTileKind.PROFILE_SLOT_1,
              slot1Profile?.name ?: AppLabels.Settings.quickProfileFallbackName(1),
            )
          },
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        ProfileQuickTileSlotConfigRow(
          slotNumber = 2,
          profiles = state.profiles,
          selectedProfile = slot2Profile,
          onSelectProfile = { profileId -> onSetQuickTileSlotProfileId(2, profileId) },
          onAddTile = {
            onRequestAddQuickTile(
              QuickTileKind.PROFILE_SLOT_2,
              slot2Profile?.name ?: AppLabels.Settings.quickProfileFallbackName(2),
            )
          },
        )
      }
    }

    // 4. Blocos de Timer Rápido e Zero Mídia
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .testTag(AppTestTags.Settings.QUICK_UTILITY_TILES_CARD),
      shape = groupedCardShape(index = 3, totalCount = 5),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = AppLabels.Settings.utilityTilesTitle,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Medium,
            )
            Text(
              text = AppLabels.Settings.utilityTilesSubtitle,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          FilledTonalButton(
            onClick = { onRequestAddQuickTile(QuickTileKind.QUICK_TIMER, null) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .weight(1f)
              .testTag(AppTestTags.Settings.ADD_TILE_TIMER),
          ) {
            Text(AppLabels.Settings.addTimerTileButton, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
          FilledTonalButton(
            onClick = { onRequestAddQuickTile(QuickTileKind.MEDIA_MUTE, null) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .weight(1f)
              .testTag(AppTestTags.Settings.ADD_TILE_MEDIA_MUTE),
          ) {
            Text(AppLabels.Settings.addMediaMuteTileButton, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
        }
      }
    }

    // 5. Widget Material You na Tela Inicial
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = groupedCardShape(index = 4, totalCount = 5),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Widgets,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = AppLabels.Settings.homeWidgetTitle,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Medium,
            )
            Text(
              text = AppLabels.Settings.homeWidgetSubtitle,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
        FilledTonalButton(
          onClick = onPinHomeWidget,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .align(Alignment.End)
            .testTag(AppTestTags.Settings.PIN_HOME_WIDGET_BUTTON),
        ) {
          Text(AppLabels.Settings.pinHomeWidgetButton)
        }
      }
    }
  }
}

@Composable
private fun PermissionsDiagnosticSubScreen(
  state: MainScreenUiState.Ready,
  onRequestDndPermission: () -> Unit,
  onRequestNotificationPermission: () -> Unit,
  onRequestExactAlarmPermission: () -> Unit,
  onRequestLocationPermission: () -> Unit = {},
  onReopenOnboarding: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    HealthCheckItemCard(
      title = AppLabels.Settings.dndPermissionTitle,
      subtitle = if (state.hasDndPermission) {
        AppLabels.Settings.grantedStatus
      } else {
        AppLabels.Settings.pendingTapToAuthorizeStatus
      },
      isHealthy = state.hasDndPermission,
      shape = groupedCardShape(index = 0, totalCount = 5),
      onClick = onRequestDndPermission,
    )

    HealthCheckItemCard(
      title = AppLabels.Settings.smartNotificationsTitle,
      subtitle = if (state.hasNotificationPermission) {
        AppLabels.Settings.smartNotificationsGrantedSubtitle
      } else {
        AppLabels.Settings.smartNotificationsPendingSubtitle
      },
      isHealthy = state.hasNotificationPermission,
      shape = groupedCardShape(index = 1, totalCount = 5),
      onClick = onRequestNotificationPermission,
    )

    HealthCheckItemCard(
      title = AppLabels.Settings.exactAlarmsTitle,
      subtitle = if (state.canScheduleExactAlarms) {
        AppLabels.Settings.grantedStatus
      } else {
        AppLabels.Settings.pendingTapToAuthorizeStatus
      },
      isHealthy = state.canScheduleExactAlarms,
      shape = groupedCardShape(index = 2, totalCount = 5),
      onClick = onRequestExactAlarmPermission,
    )

    HealthCheckItemCard(
      title = AppLabels.Settings.locationPermissionTitle,
      subtitle = if (state.hasLocationPermission) {
        AppLabels.Settings.locationPermissionGrantedSubtitle
      } else {
        AppLabels.Settings.locationPermissionPendingSubtitle
      },
      isHealthy = state.hasLocationPermission,
      shape = groupedCardShape(index = 3, totalCount = 5),
      onClick = onRequestLocationPermission,
    )

    HealthCheckItemCard(
      title = AppLabels.Settings.onDeviceStorageTitle,
      subtitle = AppLabels.Settings.onDeviceStorageSubtitle,
      isHealthy = true,
      shape = groupedCardShape(index = 4, totalCount = 5),
      onClick = {},
    )

    Spacer(modifier = Modifier.height(14.dp))

    FilledTonalButton(
      onClick = onReopenOnboarding,
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier
        .fillMaxWidth()
        .testTag(AppTestTags.Settings.REOPEN_ONBOARDING_BUTTON),
    ) {
      Icon(
        imageVector = Icons.Default.Security,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
      )
      Spacer(modifier = Modifier.size(8.dp))
      Text(AppLabels.Settings.reopenOnboardingButton)
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TransitionHistorySubScreen(
  logs: List<TransitionLogEntry>,
  modifier: Modifier = Modifier,
) {
  var selectedFilter by rememberSaveable { mutableStateOf(HistorySourceFilter.ALL) }

  val filteredLogs = logs.filter { entry ->
    when (selectedFilter) {
      HistorySourceFilter.ALL -> true
      HistorySourceFilter.MANUAL -> entry.source in setOf(
        TransitionTriggerSource.MANUAL,
        TransitionTriggerSource.HARDWARE_BUTTON,
        TransitionTriggerSource.TIMER,
      )
      HistorySourceFilter.SCHEDULE -> entry.source in setOf(
        TransitionTriggerSource.SCHEDULE,
        TransitionTriggerSource.RESTORE,
      )
      HistorySourceFilter.LOCATION -> entry.source == TransitionTriggerSource.LOCATION
      HistorySourceFilter.SHORTCUTS -> entry.source in setOf(
        TransitionTriggerSource.QUICK_TILE,
        TransitionTriggerSource.WIDGET,
        TransitionTriggerSource.NOTIFICATION,
      )
    }
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    item {
      FlowRow(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        HistorySourceFilter.entries.forEach { filter ->
          val label = when (filter) {
            HistorySourceFilter.ALL -> AppLabels.Settings.filterAll
            HistorySourceFilter.MANUAL -> AppLabels.Settings.filterManual
            HistorySourceFilter.SCHEDULE -> AppLabels.Settings.filterSchedule
            HistorySourceFilter.LOCATION -> AppLabels.Settings.filterLocation
            HistorySourceFilter.SHORTCUTS -> AppLabels.Settings.filterShortcuts
          }
          FilterChip(
            selected = selectedFilter == filter,
            onClick = { selectedFilter = filter },
            label = { Text(label) },
            modifier = Modifier.testTag(AppTestTags.Settings.historyFilterChip(filter.name)),
          )
        }
      }
    }

    if (filteredLogs.isEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        ) {
          Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Default.History,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
              text = AppLabels.Settings.emptyHistoryMessage,
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    } else {
      itemsIndexed(
        items = filteredLogs,
        key = { _, log -> log.id },
      ) { index, log ->
        val shape = groupedCardShape(index = index, totalCount = filteredLogs.size)
        TransitionLogCard(log = log, shape = shape)
      }
    }
  }
}

@Composable
private fun BackupAndResetSubScreen(
  context: Context,
  onExportBackupJson: ((String) -> Unit) -> Unit,
  onImportBackupJson: (String) -> Unit,
  onRequestFactoryResetConfirm: () -> Unit,
  onShowStatusMessage: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var backupJsonText by rememberSaveable { mutableStateOf("") }

  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    // 1. Exportar e Importar JSON
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Backup,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = AppLabels.Settings.backupSectionTitle,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              text = AppLabels.Settings.backupSectionSubtitle,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        OutlinedTextField(
          value = backupJsonText,
          onValueChange = { backupJsonText = it },
          label = { Text(AppLabels.Settings.importBackupInputLabel) },
          maxLines = 5,
          shape = RoundedCornerShape(16.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag(AppTestTags.Settings.IMPORT_BACKUP_INPUT),
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          FilledTonalButton(
            onClick = {
              onExportBackupJson { exportedJson ->
                backupJsonText = exportedJson
                runCatching {
                  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                  clipboard?.setPrimaryClip(ClipData.newPlainText("ManageSoundModeBackup", exportedJson))
                }
                onShowStatusMessage(AppLabels.Settings.backupCopiedMessage)
              }
            },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .weight(1f)
              .testTag(AppTestTags.Settings.EXPORT_BACKUP_BUTTON),
          ) {
            Icon(
              imageVector = Icons.Default.ContentCopy,
              contentDescription = null,
              modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(AppLabels.Settings.exportBackupButton, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }

          Button(
            onClick = {
              if (backupJsonText.isNotBlank()) {
                onImportBackupJson(backupJsonText)
              }
            },
            enabled = backupJsonText.isNotBlank(),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .weight(1f)
              .testTag(AppTestTags.Settings.IMPORT_BACKUP_BUTTON),
          ) {
            Text(AppLabels.Settings.importBackupButton, maxLines = 1, overflow = TextOverflow.Ellipsis)
          }
        }
      }
    }

    // 2. Restaurar Padrões de Fábrica
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Restore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
          )
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = AppLabels.Settings.factoryResetSectionTitle,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              text = AppLabels.Settings.factoryResetSectionSubtitle,
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }

        OutlinedButton(
          onClick = onRequestFactoryResetConfirm,
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag(AppTestTags.Settings.RESET_DEFAULTS_BUTTON),
        ) {
          Icon(
            imageVector = Icons.Default.Restore,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.size(8.dp))
          Text(
            text = AppLabels.Settings.factoryResetButton,
            color = MaterialTheme.colorScheme.error,
          )
        }
      }
    }
  }
}

@Composable
private fun AboutAndPrivacySubScreen(
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 20.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp),
  ) {
    // 1. Identidade do Produto
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(26.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = AppLabels.Tabs.nowTopBarTitle,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Text(
          text = AppLabels.Settings.aboutVersionSubtitle,
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
        )
        Text(
          text = AppLabels.Settings.aboutTagline,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
      }
    }

    // 2. Hierarquia de Prioridade de Regras
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
        Text(
          text = AppLabels.Settings.priorityHierarchyTitle,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
        )
        Text(
          text = AppLabels.Settings.priorityStep1,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = AppLabels.Settings.priorityStep2,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = AppLabels.Settings.priorityStep3,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          text = AppLabels.Settings.priorityStep4,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    // 3. Compromisso de Privacidade (100% On-Device)
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
          )
          Text(
            text = AppLabels.Settings.privacyCommitmentTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
          )
        }
        Text(
          text = AppLabels.Settings.privacyCommitmentBody,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun SectionHeader(title: String) {
  Text(
    text = title,
    style = MaterialTheme.typography.titleMedium,
    fontWeight = FontWeight.SemiBold,
    color = MaterialTheme.colorScheme.onBackground,
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 6.dp),
  )
}

@Composable
private fun HealthCheckItemCard(
  title: String,
  subtitle: String,
  isHealthy: Boolean,
  shape: RoundedCornerShape,
  onClick: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(shape)
      .clickable(onClick = onClick),
    shape = shape,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Icon(
        imageVector = if (isHealthy) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
        contentDescription = null,
        tint = if (isHealthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
      )
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = title,
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Medium,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun TransitionLogCard(
  log: TransitionLogEntry,
  shape: RoundedCornerShape,
) {
  val formattedTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    .format(Date(log.timestampEpochMillis))

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = shape,
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
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
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = formattedTime,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
          )
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
          ) {
            Text(
              text = log.source.badgeLabel,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            )
          }
        }
        Text(
          text = log.title,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium,
        )
        Text(
          text = log.detail,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun ProfileQuickTileSlotConfigRow(
  slotNumber: Int,
  profiles: List<SoundProfile>,
  selectedProfile: SoundProfile?,
  onSelectProfile: (String) -> Unit,
  onAddTile: () -> Unit,
) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = AppLabels.Settings.slotLabel(slotNumber, selectedProfile?.name),
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold,
        )
        if (selectedProfile != null) {
          Text(
            text = selectedProfile.summaryLabel(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      FilledTonalButton(
        onClick = onAddTile,
        shape = RoundedCornerShape(14.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.testTag(AppTestTags.Settings.addTileProfileSlot(slotNumber)),
      ) {
        Text(AppLabels.Settings.addSlotButton(slotNumber))
      }
    }

    LazyRow(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      items(
        items = profiles,
        key = { it.id },
      ) { profile ->
        val isSelected = selectedProfile?.id == profile.id
        FilterChip(
          selected = isSelected,
          onClick = { onSelectProfile(profile.id) },
          label = { Text(profile.name) },
          modifier = Modifier.testTag(AppTestTags.Settings.slotProfileChip(slotNumber, profile.id)),
        )
      }
    }
  }
}
