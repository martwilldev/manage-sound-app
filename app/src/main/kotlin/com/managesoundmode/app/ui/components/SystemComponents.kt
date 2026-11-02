package com.managesoundmode.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.RingVolume
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.managesoundmode.app.domain.model.SoundMode
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.domain.model.VolumeStreamType
import com.managesoundmode.app.domain.model.VolumeStreams
import kotlin.math.roundToInt

/**
 * Padrão "Grouped List Cards" das Configurações Nativas do Android 15/16 (Estudo 04, Seção 1.3).
 */
fun groupedCardShape(index: Int, totalCount: Int): RoundedCornerShape {
  if (totalCount <= 1) return RoundedCornerShape(24.dp)
  return when (index) {
    0 -> RoundedCornerShape(
      topStart = 24.dp,
      topEnd = 24.dp,
      bottomStart = 6.dp,
      bottomEnd = 6.dp,
    )
    totalCount - 1 -> RoundedCornerShape(
      topStart = 6.dp,
      topEnd = 6.dp,
      bottomStart = 24.dp,
      bottomEnd = 24.dp,
    )
    else -> RoundedCornerShape(6.dp)
  }
}

fun profileIconForKey(iconKey: String): ImageVector = when (iconKey) {
  "sunny" -> Icons.Default.WbSunny
  "work" -> Icons.Default.Work
  "vibration" -> Icons.Default.Vibration
  "bedtime" -> Icons.Default.Bedtime
  "home" -> Icons.Default.Home
  "shield" -> Icons.Default.Shield
  else -> Icons.Default.Tune
}

fun SoundMode.modeIcon(): ImageVector = when (this) {
  SoundMode.NORMAL -> Icons.Default.NotificationsActive
  SoundMode.VIBRATE -> Icons.Default.Vibration
  SoundMode.SILENT -> Icons.Default.DoNotDisturbOn
}

/**
 * Switch Material 3 com ícone `Check` (`✓`) quando ativo e feedback tátil.
 */
@Composable
fun M3CheckSwitch(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptics = LocalHapticFeedback.current
  Switch(
    checked = checked,
    onCheckedChange = { next ->
      haptics.performHapticFeedback(HapticFeedbackType.LongPress)
      onCheckedChange(next)
    },
    thumbContent = if (checked) {
      {
        Icon(
          imageVector = Icons.Default.Check,
          contentDescription = null,
          modifier = Modifier.size(SwitchDefaults.IconSize),
        )
      }
    } else {
      null
    },
    modifier = modifier,
  )
}

/**
 * Slider Expressivo limpo do Material 3 (Android 15/16 Volume Panel style),
 * sem sobreposição duplicada de trilha, compacto e alinhado.
 */
@Composable
fun PillVolumeSliderRow(
  type: VolumeStreamType,
  icon: ImageVector,
  percent: Int,
  onValueChangeFinished: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  var sliderValue by remember(percent) { mutableStateOf(percent.toFloat()) }
  val currentPercent = sliderValue.roundToInt().coerceIn(0, 100)

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Box(
          modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(16.dp),
          )
        }
        Text(
          text = type.label,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
      Text(
        text = "$currentPercent%",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
      )
    }

    Slider(
      value = sliderValue,
      onValueChange = { sliderValue = it },
      onValueChangeFinished = { onValueChangeFinished(sliderValue.roundToInt()) },
      valueRange = 0f..100f,
      modifier = Modifier
        .fillMaxWidth()
        .testTag("slider_${type.name}"),
    )
  }
}

/**
 * BottomSheet nativo do Material 3 para criar ou editar qualquer Perfil de Som
 * (incluindo os presets Trabalho, Dormir, Reunião, Casa e Padrão).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditorBottomSheet(
  initialProfile: SoundProfile?,
  defaultMode: SoundMode,
  defaultStreams: VolumeStreams,
  onSaveProfile: (profile: SoundProfile, applyNow: Boolean) -> Unit,
  onDeleteProfile: ((profileId: String) -> Unit)? = null,
  onDismiss: () -> Unit,
) {
  val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
  var name by rememberSaveable { mutableStateOf(initialProfile?.name ?: "") }
  var iconKey by rememberSaveable { mutableStateOf(initialProfile?.iconKey ?: "work") }
  var selectedMode by rememberSaveable { mutableStateOf(initialProfile?.mode ?: defaultMode) }
  var ringPercent by rememberSaveable {
    mutableIntStateOf(initialProfile?.streams?.ringPercent ?: defaultStreams.ringPercent)
  }
  var notifPercent by rememberSaveable {
    mutableIntStateOf(
      initialProfile?.streams?.notificationPercent ?: defaultStreams.notificationPercent,
    )
  }
  var mediaPercent by rememberSaveable {
    mutableIntStateOf(initialProfile?.streams?.mediaPercent ?: defaultStreams.mediaPercent)
  }
  var alarmPercent by rememberSaveable {
    mutableIntStateOf(initialProfile?.streams?.alarmPercent ?: defaultStreams.alarmPercent)
  }
  var preserveMedia by rememberSaveable {
    mutableStateOf(initialProfile?.preserveMediaIfPlaying ?: true)
  }
  var skipBluetooth by rememberSaveable {
    mutableStateOf(initialProfile?.skipMediaOnBluetooth ?: true)
  }

  val iconOptions = listOf(
    "work" to "Trabalho",
    "bedtime" to "Dormir",
    "vibration" to "Reunião",
    "home" to "Casa",
    "sunny" to "Dia",
    "shield" to "Foco",
  )

  fun buildProfile(): SoundProfile {
    val resolvedName = name.trim().ifEmpty { "Novo Perfil" }
    val streams = VolumeStreams(
      ringPercent = ringPercent,
      notificationPercent = notifPercent,
      mediaPercent = mediaPercent,
      alarmPercent = alarmPercent,
    )
    return SoundProfile(
      id = initialProfile?.id ?: "custom_${System.currentTimeMillis()}",
      name = resolvedName,
      description = initialProfile?.description ?: "",
      mode = selectedMode,
      streams = streams,
      isPreset = initialProfile?.isPreset ?: false,
      iconKey = iconKey,
      preserveMediaIfPlaying = preserveMedia,
      skipMediaOnBluetooth = skipBluetooth,
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
          text = if (initialProfile == null) "Novo Perfil de Som" else "Editar '${initialProfile.name}'",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.SemiBold,
        )
        if (initialProfile != null && !initialProfile.isPreset && onDeleteProfile != null) {
          TextButton(onClick = { onDeleteProfile(initialProfile.id) }) {
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

      // Seletor rápido de ícone
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        iconOptions.forEach { (key, label) ->
          val selected = iconKey == key
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
              .clip(RoundedCornerShape(14.dp))
              .clickable { iconKey = key }
              .padding(6.dp),
          ) {
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(
                  if (selected) {
                    MaterialTheme.colorScheme.primary
                  } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                  },
                ),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = profileIconForKey(key),
                contentDescription = label,
                tint = if (selected) {
                  MaterialTheme.colorScheme.onPrimary
                } else {
                  MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(22.dp),
              )
            }
            Text(
              text = label,
              style = MaterialTheme.typography.labelSmall,
              color = if (selected) {
                MaterialTheme.colorScheme.primary
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
            )
          }
        }
      }

      OutlinedTextField(
        value = name,
        onValueChange = { name = it },
        label = { Text("Nome do perfil") },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("profile_name_input"),
      )

      // Modo de Toque do Perfil (SegmentedButton nativo M3)
      Text(
        text = "Modo de toque",
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
        SoundMode.entries.forEachIndexed { idx, mode ->
          SegmentedButton(
            selected = selectedMode == mode,
            onClick = { selectedMode = mode },
            shape = SegmentedButtonDefaults.itemShape(index = idx, count = SoundMode.entries.size),
            icon = {
              Icon(
                imageVector = mode.modeIcon(),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
              )
            },
          ) {
            Text(mode.title)
          }
        }
      }

      // Sliders dos 4 Canais do Perfil
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Text(
            text = "Volumes salvos neste perfil",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          PillVolumeSliderRow(
            type = VolumeStreamType.RING,
            icon = Icons.Default.RingVolume,
            percent = ringPercent,
            onValueChangeFinished = { ringPercent = it },
          )
          PillVolumeSliderRow(
            type = VolumeStreamType.NOTIFICATION,
            icon = Icons.Default.Notifications,
            percent = notifPercent,
            onValueChangeFinished = { notifPercent = it },
          )
          PillVolumeSliderRow(
            type = VolumeStreamType.MEDIA,
            icon = Icons.Default.MusicNote,
            percent = mediaPercent,
            onValueChangeFinished = { mediaPercent = it },
          )
          PillVolumeSliderRow(
            type = VolumeStreamType.ALARM,
            icon = Icons.Default.Alarm,
            percent = alarmPercent,
            onValueChangeFinished = { alarmPercent = it },
          )
        }
      }

      // Proteções de Mídia e Bluetooth
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Preservar mídia se música estiver tocando",
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.weight(1f),
        )
        M3CheckSwitch(
          checked = preserveMedia,
          onCheckedChange = { preserveMedia = it },
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Não alterar mídia com fone Bluetooth",
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.weight(1f),
        )
        M3CheckSwitch(
          checked = skipBluetooth,
          onCheckedChange = { skipBluetooth = it },
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        FilledTonalButton(
          onClick = { onSaveProfile(buildProfile(), false) },
          enabled = name.trim().isNotEmpty() || initialProfile != null,
          modifier = Modifier
            .weight(1f)
            .height(50.dp)
            .testTag("confirm_save_profile_button"),
        ) {
          Text("Salvar")
        }
        Button(
          onClick = { onSaveProfile(buildProfile(), true) },
          enabled = name.trim().isNotEmpty() || initialProfile != null,
          modifier = Modifier
            .weight(1f)
            .height(50.dp),
        ) {
          Text("Salvar e Ativar")
        }
      }
    }
  }
}

/**
 * Modal de Prominent Disclosure exigido pela política da Google Play Store (Estudo 02, Seção 4).
 */
@Composable
fun ProminentLocationDisclosureDialog(
  onAcceptAndContinue: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    icon = {
      Icon(
        imageVector = Icons.Default.LocationOn,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
      )
    },
    title = {
      Text(
        text = "Automação por Localização (100% On-Device)",
        style = MaterialTheme.typography.titleLarge,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = "O Manage Sound Mode coleta dados de localização em segundo plano para ativar e desativar automaticamente seus perfis de som (Silencioso, Vibrar ou Volume Alto) quando você entra ou sai dos locais configurados, mesmo quando o aplicativo está fechado ou não está em uso.",
          style = MaterialTheme.typography.bodyMedium,
        )
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.secondaryContainer,
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
              text = "Privacidade Total: Suas coordenadas ficam salvas exclusivamente no seu aparelho. Nenhum dado de GPS é enviado para servidores.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onAcceptAndContinue,
        modifier = Modifier.testTag("accept_location_disclosure_button"),
      ) {
        Text("Entendi e Continuar")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Agora não")
      }
    },
  )
}
