package com.managesoundmode.app.ui.main

import android.content.Intent
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.RingVolume
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
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import com.managesoundmode.app.domain.model.SoundMode
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.domain.model.TimedModeSession
import com.managesoundmode.app.domain.model.VolumeStreamType
import com.managesoundmode.app.domain.model.VolumeStreams
import com.managesoundmode.app.ui.theme.ManageSoundModeTheme
import com.managesoundmode.app.ui.theme.SoundSilentRose
import com.managesoundmode.app.ui.theme.SoundTimerBlue
import com.managesoundmode.app.ui.theme.SoundTimerContainerDark
import com.managesoundmode.app.ui.theme.accentColor
import com.managesoundmode.app.ui.theme.containerColor
import kotlin.math.roundToInt

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
        modifier = modifier,
      )
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
  modifier: Modifier = Modifier,
) {
  val haptics = LocalHapticFeedback.current
  val snackbarHostState = remember { SnackbarHostState() }
  var showSaveProfileDialog by rememberSaveable { mutableStateOf(false) }

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

  Scaffold(
    modifier = modifier.fillMaxSize(),
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.titleLarge,
          )
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background,
          titleContentColor = MaterialTheme.colorScheme.onBackground,
        ),
      )
    },
  ) { innerPadding ->
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding),
      contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      item {
        CurrentModeHeroCard(
          currentMode = state.currentMode,
          activeTimer = state.activeTimer,
        )
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
            text = "Perfis de Som",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
          )
          FilledTonalButton(
            onClick = { showSaveProfileDialog = true },
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

      items(
        items = state.profiles,
        key = { it.id },
      ) { profile ->
        SoundProfileCard(
          profile = profile,
          isActive = state.activeProfileId == profile.id,
          onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            onActivateProfile(profile)
          },
        )
      }
    }
  }
}

@Composable
private fun CurrentModeHeroCard(
  currentMode: SoundMode,
  activeTimer: TimedModeSession?,
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
    shape = RoundedCornerShape(24.dp),
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
            .size(48.dp)
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
            text = "MODO ATUAL",
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
        text = currentMode.subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = 0.85f),
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
private fun DndPermissionBanner(
  onRequestDndPermission: () -> Unit,
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .testTag("dnd_permission_banner"),
    shape = RoundedCornerShape(18.dp),
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
          targetValue = if (selected) accent.copy(alpha = 0.22f) else MaterialTheme.colorScheme.surfaceVariant,
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
            .clip(RoundedCornerShape(18.dp))
            .clickable { onSelectMode(mode) }
            .semantics { contentDescription = "Modo ${mode.title}" }
            .testTag("mode_button_${mode.name}"),
          shape = RoundedCornerShape(18.dp),
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
              color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
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
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
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
        text = "Silenciar ou vibrar temporariamente e voltar ao modo anterior sozinho.",
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
    shape = RoundedCornerShape(20.dp),
    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Text(
        text = "Canais de Volume Independentes",
        style = MaterialTheme.typography.titleMedium,
      )

      VolumeStreamSliderRow(
        type = VolumeStreamType.RING,
        icon = Icons.Default.RingVolume,
        percent = streams.ringPercent,
        onValueChangeFinished = { onStreamVolumeChange(VolumeStreamType.RING, it) },
      )
      VolumeStreamSliderRow(
        type = VolumeStreamType.NOTIFICATION,
        icon = Icons.Default.Notifications,
        percent = streams.notificationPercent,
        onValueChangeFinished = { onStreamVolumeChange(VolumeStreamType.NOTIFICATION, it) },
      )
      VolumeStreamSliderRow(
        type = VolumeStreamType.MEDIA,
        icon = Icons.Default.MusicNote,
        percent = streams.mediaPercent,
        onValueChangeFinished = { onStreamVolumeChange(VolumeStreamType.MEDIA, it) },
      )
      VolumeStreamSliderRow(
        type = VolumeStreamType.ALARM,
        icon = Icons.Default.Alarm,
        percent = streams.alarmPercent,
        onValueChangeFinished = { onStreamVolumeChange(VolumeStreamType.ALARM, it) },
      )
    }
  }
}

@Composable
private fun VolumeStreamSliderRow(
  type: VolumeStreamType,
  icon: ImageVector,
  percent: Int,
  onValueChangeFinished: (Int) -> Unit,
) {
  var sliderValue by remember(percent) { mutableStateOf(percent.toFloat()) }

  Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
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
          imageVector = icon,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(20.dp),
        )
        Text(
          text = type.label,
          style = MaterialTheme.typography.bodyMedium,
        )
      }
      Text(
        text = "${sliderValue.roundToInt()}%",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
      )
    }
    Slider(
      value = sliderValue,
      onValueChange = { sliderValue = it },
      onValueChangeFinished = { onValueChangeFinished(sliderValue.roundToInt()) },
      valueRange = 0f..100f,
      colors = SliderDefaults.colors(
        thumbColor = MaterialTheme.colorScheme.primary,
        activeTrackColor = MaterialTheme.colorScheme.primary,
      ),
      modifier = Modifier.testTag("slider_${type.name}"),
    )
  }
}

@Composable
private fun SoundProfileCard(
  profile: SoundProfile,
  isActive: Boolean,
  onClick: () -> Unit,
) {
  val modeAccent = profile.mode.accentColor()
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .clickable(onClick = onClick)
      .testTag("profile_card_${profile.id}"),
    shape = RoundedCornerShape(18.dp),
    colors = CardDefaults.cardColors(
      containerColor = if (isActive) {
        modeAccent.copy(alpha = 0.16f)
      } else {
        MaterialTheme.colorScheme.surface
      },
    ),
    border = BorderStroke(
      width = if (isActive) 1.5.dp else 1.dp,
      color = if (isActive) modeAccent else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
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
          label = { Text("Nome do perfil (ex.: Cinema, Academia)") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Descrição curta") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
      }
    },
    confirmButton = {
      Button(
        onClick = { onConfirm(name, description) },
        enabled = name.isNotBlank(),
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
private fun MainScreenPreview() {
  ManageSoundModeTheme {
    MainDashboardContent(
      state = MainScreenUiState.Ready(
        currentMode = SoundMode.NORMAL,
        streams = VolumeStreams(),
        hasDndPermission = false,
        profiles = SoundProfile.defaultPresets,
        activeProfileId = "preset_home",
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
