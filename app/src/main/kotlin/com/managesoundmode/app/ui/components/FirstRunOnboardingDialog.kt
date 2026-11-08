package com.managesoundmode.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.managesoundmode.app.domain.model.ThemeMode
import com.managesoundmode.app.ui.common.AppLabels
import com.managesoundmode.app.ui.common.AppLanguage
import com.managesoundmode.app.ui.common.AppTestTags
import com.managesoundmode.app.ui.main.MainScreenUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FirstRunOnboardingDialog(
  state: MainScreenUiState.Ready,
  onCompleteOnboarding: () -> Unit,
  onSetAppLanguage: (AppLanguage) -> Unit,
  onSetThemeMode: (ThemeMode) -> Unit,
  onRequestDndPermission: () -> Unit,
  onRequestNotificationPermission: () -> Unit,
  onRequestExactAlarmPermission: () -> Unit,
  onRequestLocationPermission: () -> Unit,
  onSetDefaultDayProfileId: (String) -> Unit,
  onSetMuteMediaOnSilent: (Boolean) -> Unit,
  onSetSmartVolumeTriggerEnabled: (Boolean) -> Unit,
  onSetPersistentQuickBarEnabled: (Boolean) -> Unit,
) {
  var currentStep by rememberSaveable { mutableIntStateOf(1) }
  val totalSteps = 3

  Dialog(
    onDismissRequest = onCompleteOnboarding,
    properties = DialogProperties(
      usePlatformDefaultWidth = false,
      dismissOnClickOutside = false,
      dismissOnBackPress = true,
    ),
  ) {
    Scaffold(
      modifier = Modifier
        .fillMaxSize()
        .testTag(AppTestTags.Onboarding.DIALOG),
      containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
      topBar = {
        TopAppBar(
          navigationIcon = {
            if (currentStep > 1) {
              IconButton(
                onClick = { currentStep -= 1 },
                modifier = Modifier.testTag(AppTestTags.Onboarding.BACK_BUTTON),
              ) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = AppLabels.Common.back,
                )
              }
            }
          },
          title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text(
                text = AppLabels.Onboarding.stepIndicator(currentStep, totalSteps),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
              )
              Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
              ) {
                for (step in 1..totalSteps) {
                  val active = step <= currentStep
                  val pillColor by animateColorAsState(
                    targetValue = if (active) {
                      MaterialTheme.colorScheme.primary
                    } else {
                      MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    animationSpec = tween(200),
                    label = "onboardingStepPill_$step",
                  )
                  Box(
                    modifier = Modifier
                      .width(if (step == currentStep) 32.dp else 18.dp)
                      .height(6.dp)
                      .clip(RoundedCornerShape(50))
                      .background(pillColor)
                      .clickable { currentStep = step }
                      .testTag(AppTestTags.Onboarding.stepPill(step)),
                  )
                }
              }
            }
          },
          actions = {
            TextButton(
              onClick = onCompleteOnboarding,
              modifier = Modifier.testTag(AppTestTags.Onboarding.SKIP_BUTTON),
            ) {
              Text(
                text = AppLabels.Onboarding.skipButton,
                fontWeight = FontWeight.SemiBold,
              )
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
          ),
        )
      },
      bottomBar = {
        Surface(
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          tonalElevation = 3.dp,
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 20.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
          ) {
            if (currentStep < totalSteps) {
              Button(
                onClick = { currentStep += 1 },
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(min = 52.dp)
                  .testTag(AppTestTags.Onboarding.NEXT_BUTTON),
                shape = RoundedCornerShape(18.dp),
              ) {
                Text(
                  text = AppLabels.Onboarding.nextButton,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                  contentDescription = null,
                  modifier = Modifier.size(18.dp),
                )
              }
            } else {
              Button(
                onClick = onCompleteOnboarding,
                modifier = Modifier
                  .fillMaxWidth()
                  .heightIn(min = 52.dp)
                  .testTag(AppTestTags.Onboarding.FINISH_BUTTON),
                shape = RoundedCornerShape(18.dp),
              ) {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = null,
                  modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = AppLabels.Onboarding.finishButton,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Bold,
                )
              }
            }
          }
        }
      },
    ) { innerPadding ->
      LazyColumn(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        when (currentStep) {
          1 -> {
            item {
              OnboardingStep1WelcomeContent(
                state = state,
                onSetAppLanguage = onSetAppLanguage,
                onSetThemeMode = onSetThemeMode,
              )
            }
          }
          2 -> {
            item {
              OnboardingStep2PermissionsContent(
                state = state,
                onRequestDndPermission = onRequestDndPermission,
                onRequestNotificationPermission = onRequestNotificationPermission,
                onRequestExactAlarmPermission = onRequestExactAlarmPermission,
                onRequestLocationPermission = onRequestLocationPermission,
              )
            }
          }
          else -> {
            item {
              OnboardingStep3PreferencesContent(
                state = state,
                onSetDefaultDayProfileId = onSetDefaultDayProfileId,
                onSetMuteMediaOnSilent = onSetMuteMediaOnSilent,
                onSetSmartVolumeTriggerEnabled = onSetSmartVolumeTriggerEnabled,
                onSetPersistentQuickBarEnabled = onSetPersistentQuickBarEnabled,
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun OnboardingStep1WelcomeContent(
  state: MainScreenUiState.Ready,
  onSetAppLanguage: (AppLanguage) -> Unit,
  onSetThemeMode: (ThemeMode) -> Unit,
) {
  val currentLanguage = runCatching { AppLanguage.valueOf(state.appLanguageCode) }
    .getOrDefault(AppLabels.currentLanguage)

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    // Hero Banner de Boas-vindas Zenith
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(28.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
      ),
      border = BorderStroke(
        width = 1.dp,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
      ),
    ) {
      Column(
        modifier = Modifier.padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Surface(
          shape = RoundedCornerShape(50),
          color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
          border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
          ),
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.VolumeUp,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp),
            )
            Text(
              text = AppLabels.Onboarding.step1Badge,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }

        Text(
          text = AppLabels.Onboarding.step1Title,
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
        )

        Text(
          text = AppLabels.Onboarding.step1Subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }

    // Seleção Rápida de Idioma e Tema
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
      ),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = AppLabels.Settings.appLanguageTitle,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          AppLanguage.entries.forEach { language ->
            val selected = currentLanguage == language
            Surface(
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 42.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable { onSetAppLanguage(language) },
              shape = RoundedCornerShape(14.dp),
              color = if (selected) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
              },
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
              ) {
                Text(
                  text = language.shortLabel,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                  color = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                  } else {
                    MaterialTheme.colorScheme.onSurface
                  },
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

        Text(
          text = AppLabels.Settings.appThemeTitle,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          ThemeMode.entries.forEach { mode ->
            val selected = state.themeMode == mode
            Surface(
              modifier = Modifier
                .weight(1f)
                .heightIn(min = 42.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable { onSetThemeMode(mode) },
              shape = RoundedCornerShape(14.dp),
              color = if (selected) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
              },
            ) {
              Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
              ) {
                Text(
                  text = mode.label,
                  style = MaterialTheme.typography.labelLarge,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                  color = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                  } else {
                    MaterialTheme.colorScheme.onSurface
                  },
                  maxLines = 1,
                  overflow = TextOverflow.Ellipsis,
                )
              }
            }
          }
        }
      }
    }

    // 3 Pilares do Produto
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
      ),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        OnboardingFeatureRow(
          icon = Icons.Default.MusicOff,
          title = AppLabels.Onboarding.feature1Title,
          body = AppLabels.Onboarding.feature1Body,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        OnboardingFeatureRow(
          icon = Icons.Default.Schedule,
          title = AppLabels.Onboarding.feature2Title,
          body = AppLabels.Onboarding.feature2Body,
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
        OnboardingFeatureRow(
          icon = Icons.Default.Shield,
          title = AppLabels.Onboarding.feature3Title,
          body = AppLabels.Onboarding.feature3Body,
        )
      }
    }
  }
}

@Composable
private fun OnboardingFeatureRow(
  icon: ImageVector,
  title: String,
  body: String,
) {
  Row(
    verticalAlignment = Alignment.Top,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Box(
      modifier = Modifier
        .size(40.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.secondaryContainer),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSecondaryContainer,
        modifier = Modifier.size(20.dp),
      )
    }
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        text = body,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun OnboardingStep2PermissionsContent(
  state: MainScreenUiState.Ready,
  onRequestDndPermission: () -> Unit,
  onRequestNotificationPermission: () -> Unit,
  onRequestExactAlarmPermission: () -> Unit,
  onRequestLocationPermission: () -> Unit,
) {
  val grantedCount = listOf(
    state.hasDndPermission,
    state.hasNotificationPermission,
    state.canScheduleExactAlarms,
    state.hasLocationPermission,
  ).count { it }

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(26.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
      ),
    ) {
      Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            Icon(
              imageVector = Icons.Default.Security,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.size(18.dp),
            )
            Text(
              text = AppLabels.Onboarding.step2Badge,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
          }

          Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.14f),
          ) {
            Text(
              text = AppLabels.Onboarding.permissionsProgressLabel(grantedCount, 4),
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            )
          }
        }

        Text(
          text = AppLabels.Onboarding.step2Title,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
        Text(
          text = AppLabels.Onboarding.step2Subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
        )
      }
    }

    OnboardingPermissionItemCard(
      icon = Icons.Default.DoNotDisturbOn,
      title = AppLabels.Onboarding.dndCardTitle,
      description = AppLabels.Onboarding.dndCardBody,
      badgeLabel = AppLabels.Onboarding.permissionRequiredTag,
      isGranted = state.hasDndPermission,
      actionButtonLabel = AppLabels.Common.enable,
      buttonTestTag = AppTestTags.Onboarding.GRANT_DND_BUTTON,
      onGrantClick = onRequestDndPermission,
    )

    OnboardingPermissionItemCard(
      icon = Icons.Default.NotificationsActive,
      title = AppLabels.Onboarding.notificationsCardTitle,
      description = AppLabels.Onboarding.notificationsCardBody,
      badgeLabel = AppLabels.Onboarding.permissionRecommendedTag,
      isGranted = state.hasNotificationPermission,
      actionButtonLabel = AppLabels.Common.allow,
      buttonTestTag = AppTestTags.Onboarding.GRANT_NOTIFICATIONS_BUTTON,
      onGrantClick = onRequestNotificationPermission,
    )

    OnboardingPermissionItemCard(
      icon = Icons.Default.Alarm,
      title = AppLabels.Onboarding.exactAlarmCardTitle,
      description = AppLabels.Onboarding.exactAlarmCardBody,
      badgeLabel = AppLabels.Onboarding.permissionRecommendedTag,
      isGranted = state.canScheduleExactAlarms,
      actionButtonLabel = AppLabels.Common.authorize,
      buttonTestTag = AppTestTags.Onboarding.GRANT_EXACT_ALARM_BUTTON,
      onGrantClick = onRequestExactAlarmPermission,
    )

    OnboardingPermissionItemCard(
      icon = Icons.Default.LocationOn,
      title = AppLabels.Onboarding.locationCardTitle,
      description = AppLabels.Onboarding.locationCardBody,
      badgeLabel = AppLabels.Onboarding.permissionOptionalTag,
      isGranted = state.hasLocationPermission,
      actionButtonLabel = AppLabels.Common.allow,
      buttonTestTag = AppTestTags.Onboarding.GRANT_LOCATION_BUTTON,
      onGrantClick = onRequestLocationPermission,
    )
  }
}

@Composable
private fun OnboardingPermissionItemCard(
  icon: ImageVector,
  title: String,
  description: String,
  badgeLabel: String,
  isGranted: Boolean,
  actionButtonLabel: String,
  buttonTestTag: String,
  onGrantClick: () -> Unit,
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(22.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isGranted) {
        MaterialTheme.colorScheme.surfaceContainerLow
      } else {
        MaterialTheme.colorScheme.surfaceContainerHigh
      },
    ),
  ) {
    Column(
      modifier = Modifier.padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          modifier = Modifier.weight(1f),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(
                if (isGranted) {
                  MaterialTheme.colorScheme.primaryContainer
                } else {
                  MaterialTheme.colorScheme.secondaryContainer
                },
              ),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = if (isGranted) Icons.Default.CheckCircle else icon,
              contentDescription = null,
              tint = if (isGranted) {
                MaterialTheme.colorScheme.onPrimaryContainer
              } else {
                MaterialTheme.colorScheme.onSecondaryContainer
              },
              modifier = Modifier.size(20.dp),
            )
          }

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = title,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.SemiBold,
            )
            Text(
              text = badgeLabel,
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.primary,
            )
          }
        }

        Spacer(modifier = Modifier.width(8.dp))

        if (isGranted) {
          Surface(
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.primaryContainer,
          ) {
            Text(
              text = AppLabels.Onboarding.permissionGrantedBadge,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer,
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
          }
        } else {
          FilledTonalButton(
            onClick = onGrantClick,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
            modifier = Modifier.testTag(buttonTestTag),
          ) {
            Text(
              text = actionButtonLabel,
              style = MaterialTheme.typography.labelLarge,
              fontWeight = FontWeight.SemiBold,
            )
          }
        }
      }

      Text(
        text = description,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OnboardingStep3PreferencesContent(
  state: MainScreenUiState.Ready,
  onSetDefaultDayProfileId: (String) -> Unit,
  onSetMuteMediaOnSilent: (Boolean) -> Unit,
  onSetSmartVolumeTriggerEnabled: (Boolean) -> Unit,
  onSetPersistentQuickBarEnabled: (Boolean) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(26.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
      ),
    ) {
      Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = AppLabels.Onboarding.step3Badge,
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Text(
          text = AppLabels.Onboarding.step3Title,
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onTertiaryContainer,
        )
        Text(
          text = AppLabels.Onboarding.step3Subtitle,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
        )
      }
    }

    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
      ),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Text(
          text = AppLabels.Onboarding.defaultProfileSectionTitle,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
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
              leadingIcon = {
                Icon(
                  imageVector = if (selected) {
                    Icons.Default.Check
                  } else {
                    profileIconForKey(profile.iconKey)
                  },
                  contentDescription = null,
                  modifier = Modifier.size(16.dp),
                )
              },
              modifier = Modifier.testTag(AppTestTags.Onboarding.defaultProfileChip(profile.id)),
            )
          }
        }
      }
    }

    Card(
      modifier = Modifier.fillMaxWidth(),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
      ),
    ) {
      Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
      ) {
        Text(
          text = AppLabels.Onboarding.smartBehaviorsSectionTitle,
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold,
        )

        OnboardingSwitchRow(
          title = AppLabels.Settings.muteMediaOnSilentTitle,
          subtitle = AppLabels.Settings.muteMediaOnSilentSubtitle,
          checked = state.muteMediaOnSilent,
          onCheckedChange = onSetMuteMediaOnSilent,
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

        OnboardingSwitchRow(
          title = AppLabels.Settings.suggestTimerTitle,
          subtitle = AppLabels.Settings.suggestTimerSubtitle,
          checked = state.smartVolumeTriggerEnabled,
          onCheckedChange = onSetSmartVolumeTriggerEnabled,
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))

        OnboardingSwitchRow(
          title = AppLabels.Settings.persistentQuickBarTitle,
          subtitle = AppLabels.Settings.persistentQuickBarSubtitle,
          checked = state.persistentQuickBarEnabled,
          onCheckedChange = onSetPersistentQuickBarEnabled,
        )
      }
    }
  }
}

@Composable
private fun OnboardingSwitchRow(
  title: String,
  subtitle: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
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
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    M3CheckSwitch(
      checked = checked,
      onCheckedChange = onCheckedChange,
    )
  }
}
