package com.managesoundmode.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.managesoundmode.app.domain.model.SoundMode
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.domain.model.VolumeStreamType
import com.managesoundmode.app.domain.model.VolumeStreams
import com.managesoundmode.app.ui.common.AppLabels
import com.managesoundmode.app.ui.common.AppTestTags
import com.managesoundmode.app.ui.theme.onActivePillColor
import com.managesoundmode.app.ui.theme.onTonalContainerColor
import com.managesoundmode.app.ui.theme.tonalAccentColor
import com.managesoundmode.app.ui.theme.tonalContainerColor
import kotlin.math.roundToInt

/**
 * Formato de cartão orgânico ZENITH (cantos suaves atmosféricos).
 */
fun groupedCardShape(index: Int, totalCount: Int): RoundedCornerShape {
  if (totalCount <= 1) return RoundedCornerShape(24.dp)
  return when (index) {
    0 -> RoundedCornerShape(
      topStart = 24.dp,
      topEnd = 24.dp,
      bottomStart = 16.dp,
      bottomEnd = 16.dp,
    )
    totalCount - 1 -> RoundedCornerShape(
      topStart = 16.dp,
      topEnd = 16.dp,
      bottomStart = 24.dp,
      bottomEnd = 24.dp,
    )
    else -> RoundedCornerShape(16.dp)
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
 * Switch ZENITH com ícone `Check` (`✓`) quando ativo e feedback tátil.
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
    colors = SwitchDefaults.colors(
      checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
      checkedTrackColor = MaterialTheme.colorScheme.primary,
      uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
      uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
      uncheckedBorderColor = MaterialTheme.colorScheme.outlineVariant,
    ),
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
 * Cápsula Líquida de Volume ZENITH (`Zenith Capsule Slider`).
 * Exibe preenchimento líquido proporcional ao volume dentro de uma cápsula arredondada,
 * com badge circular do canal, percentual tabular e slider tátil integrado.
 */
@Composable
fun PillVolumeSliderRow(
  type: VolumeStreamType,
  icon: ImageVector,
  percent: Int,
  onValueChangeFinished: (Int) -> Unit,
  modifier: Modifier = Modifier,
  accentColor: Color = MaterialTheme.colorScheme.primary,
) {
  var sliderValue by remember { mutableFloatStateOf(percent.toFloat()) }
  var isDragging by remember { mutableStateOf(false) }

  LaunchedEffect(percent) {
    if (!isDragging) {
      sliderValue = percent.toFloat()
    }
  }

  val currentPercent = sliderValue.roundToInt().coerceIn(0, 100)
  val animatedFillFraction by animateFloatAsState(
    targetValue = (currentPercent / 100f).coerceIn(0f, 1f),
    animationSpec = tween(durationMillis = 150),
    label = "zenithCapsuleFill_${type.name}",
  )

  val capsuleShape = RoundedCornerShape(20.dp)

  Box(
    modifier = modifier
      .fillMaxWidth()
      .clip(capsuleShape)
      .background(MaterialTheme.colorScheme.surfaceContainerHigh)
      .border(
        width = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f),
        shape = capsuleShape,
      ),
  ) {
    // Camada de preenchimento líquido interno (Zenith Liquid Fill)
    Box(
      modifier = Modifier
        .matchParentSize(),
    ) {
      Box(
        modifier = Modifier
          .fillMaxHeight()
          .fillMaxWidth(fraction = animatedFillFraction)
          .background(
            brush = Brush.horizontalGradient(
              colors = listOf(
                accentColor.copy(alpha = 0.20f),
                accentColor.copy(alpha = 0.08f),
              ),
            ),
          ),
      )
    }

    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(start = 14.dp, end = 14.dp, top = 10.dp, bottom = 4.dp),
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
              .size(32.dp)
              .clip(CircleShape)
              .background(accentColor.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = icon,
              contentDescription = null,
              tint = accentColor,
              modifier = Modifier.size(16.dp),
            )
          }
          Text(
            text = type.label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
          )
        }

        Surface(
          shape = RoundedCornerShape(50),
          color = if (currentPercent == 0) {
            MaterialTheme.colorScheme.surfaceContainerHighest
          } else {
            accentColor.copy(alpha = 0.16f)
          },
        ) {
          Text(
            text = "$currentPercent%",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = if (currentPercent == 0) {
              MaterialTheme.colorScheme.onSurfaceVariant
            } else {
              accentColor
            },
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
          )
        }
      }

      Slider(
        value = sliderValue,
        onValueChange = {
          isDragging = true
          sliderValue = it
        },
        onValueChangeFinished = {
          isDragging = false
          onValueChangeFinished(sliderValue.roundToInt())
        },
        valueRange = 0f..100f,
        colors = SliderDefaults.colors(
          thumbColor = accentColor,
          activeTrackColor = accentColor,
          inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag(AppTestTags.Now.streamSlider(type)),
      )
    }
  }
}

/**
 * Tela dedicada Full-Screen ZENITH para criar ou editar qualquer Cena / Perfil de Som.
 * - Zero fechamento acidental ao arrastar sliders.
 * - Quando editando um perfil existente (initialProfile != null), salva automaticamente qualquer alteração.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileEditorBottomSheet(
  initialProfile: SoundProfile?,
  defaultMode: SoundMode,
  defaultStreams: VolumeStreams,
  onSaveProfile: (profile: SoundProfile, applyNow: Boolean) -> Unit,
  onAutoSaveProfile: (profile: SoundProfile) -> Unit = {},
  onDeleteProfile: ((profileId: String) -> Unit)? = null,
  onDismiss: () -> Unit,
) {
  val isEditing = initialProfile != null
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

  val iconOptions = AppLabels.Profiles.iconOptions()
  val activeAccent = selectedMode.tonalAccentColor()

  fun buildProfile(): SoundProfile {
    val resolvedName = name.trim().ifEmpty {
      initialProfile?.name ?: AppLabels.Profiles.newProfileTitle
    }
    val streams = VolumeStreams(
      ringPercent = ringPercent,
      notificationPercent = notifPercent,
      mediaPercent = mediaPercent,
      alarmPercent = alarmPercent,
    )
    return SoundProfile(
      id = initialProfile?.id ?: "custom_${System.currentTimeMillis()}",
      name = resolvedName,
      description = initialProfile?.description
        ?: AppLabels.Profiles.defaultStreamsDescription(streams),
      mode = selectedMode,
      streams = streams,
      isPreset = initialProfile?.isPreset ?: false,
      iconKey = iconKey,
      preserveMediaIfPlaying = preserveMedia,
      skipMediaOnBluetooth = skipBluetooth,
    )
  }

  fun notifyAutoSaveIfEditing() {
    if (isEditing) {
      onAutoSaveProfile(buildProfile())
    }
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
                  AppLabels.Profiles.editProfileTitle
                } else {
                  AppLabels.Profiles.newProfileTitle
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
            if (isEditing && onDeleteProfile != null) {
              IconButton(
                onClick = { onDeleteProfile(initialProfile.id) },
                modifier = Modifier.testTag(AppTestTags.Profiles.DELETE_TOP_BUTTON),
              ) {
                Icon(
                  imageVector = Icons.Default.DeleteOutline,
                  contentDescription = AppLabels.Profiles.deleteProfileMenu,
                  tint = MaterialTheme.colorScheme.error,
                )
              }
            } else if (!isEditing) {
              TextButton(
                onClick = { onSaveProfile(buildProfile(), false) },
                enabled = name.trim().isNotEmpty(),
                modifier = Modifier.testTag(AppTestTags.Profiles.CONFIRM_SAVE_BUTTON),
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
        // Seletor de ícone ZENITH com halo semântico
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          FlowRow(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 12.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            iconOptions.forEach { (key, label) ->
              val selected = iconKey == key
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier
                  .clip(RoundedCornerShape(16.dp))
                  .clickable {
                    iconKey = key
                    notifyAutoSaveIfEditing()
                  }
                  .padding(6.dp),
              ) {
                Box(
                  modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                      if (selected) {
                        activeAccent
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
                      selectedMode.onActivePillColor()
                    } else {
                      MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(22.dp),
                  )
                }
                Text(
                  text = label,
                  style = MaterialTheme.typography.labelSmall,
                  fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                  maxLines = 1,
                  softWrap = false,
                  color = if (selected) {
                    activeAccent
                  } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                  },
                )
              }
            }
          }
        }

        OutlinedTextField(
          value = name,
          onValueChange = {
            name = it
            notifyAutoSaveIfEditing()
          },
          label = { Text(AppLabels.Profiles.profileNameLabel) },
          singleLine = true,
          shape = RoundedCornerShape(18.dp),
          modifier = Modifier
            .fillMaxWidth()
            .testTag(AppTestTags.Profiles.NAME_INPUT),
        )

        // Seletor Triplo de Modo ZENITH
        Text(
          text = AppLabels.Profiles.baseSoundModeSectionTitle,
          style = MaterialTheme.typography.titleSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
          shape = RoundedCornerShape(22.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
          ) {
            SoundMode.entries.forEach { mode ->
              val selected = selectedMode == mode
              val modeAccent = mode.tonalAccentColor()
              val bg by animateColorAsState(
                targetValue = if (selected) modeAccent else Color.Transparent,
                animationSpec = tween(180),
                label = "profileEditorModeBg_${mode.name}",
              )
              val contentColor by animateColorAsState(
                targetValue = if (selected) {
                  mode.onActivePillColor()
                } else {
                  MaterialTheme.colorScheme.onSurfaceVariant
                },
                animationSpec = tween(180),
                label = "profileEditorModeContent_${mode.name}",
              )

              Surface(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(16.dp))
                  .clickable {
                    selectedMode = mode
                    notifyAutoSaveIfEditing()
                  },
                shape = RoundedCornerShape(16.dp),
                color = bg,
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 12.dp),
                  horizontalArrangement = Arrangement.Center,
                  verticalAlignment = Alignment.CenterVertically,
                ) {
                  Icon(
                    imageVector = mode.modeIcon(),
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(18.dp),
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = AppLabels.Now.modeShortPill(mode),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                    color = contentColor,
                    maxLines = 1,
                    softWrap = false,
                  )
                }
              }
            }
          }
        }

        // Cápsulas dos 4 Canais do Perfil
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = AppLabels.Profiles.channelsVolumeSectionTitle,
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            PillVolumeSliderRow(
              type = VolumeStreamType.RING,
              icon = Icons.Default.RingVolume,
              percent = ringPercent,
              accentColor = activeAccent,
              onValueChangeFinished = {
                ringPercent = it
                notifyAutoSaveIfEditing()
              },
            )
            PillVolumeSliderRow(
              type = VolumeStreamType.NOTIFICATION,
              icon = Icons.Default.Notifications,
              percent = notifPercent,
              accentColor = activeAccent,
              onValueChangeFinished = {
                notifPercent = it
                notifyAutoSaveIfEditing()
              },
            )
            PillVolumeSliderRow(
              type = VolumeStreamType.MEDIA,
              icon = Icons.Default.MusicNote,
              percent = mediaPercent,
              accentColor = activeAccent,
              onValueChangeFinished = {
                mediaPercent = it
                notifyAutoSaveIfEditing()
              },
            )
            PillVolumeSliderRow(
              type = VolumeStreamType.ALARM,
              icon = Icons.Default.Alarm,
              percent = alarmPercent,
              accentColor = activeAccent,
              onValueChangeFinished = {
                alarmPercent = it
                notifyAutoSaveIfEditing()
              },
            )
          }
        }

        // Proteções de Mídia e Bluetooth em Cartão ZENITH
        Surface(
          shape = RoundedCornerShape(22.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
          border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = AppLabels.Profiles.preserveMediaTitle,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                  .weight(1f)
                  .padding(end = 12.dp),
              )
              M3CheckSwitch(
                checked = preserveMedia,
                onCheckedChange = {
                  preserveMedia = it
                  notifyAutoSaveIfEditing()
                },
              )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically,
            ) {
              Text(
                text = AppLabels.Profiles.skipBluetoothTitle,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
                  .weight(1f)
                  .padding(end = 12.dp),
              )
              M3CheckSwitch(
                checked = skipBluetooth,
                onCheckedChange = {
                  skipBluetooth = it
                  notifyAutoSaveIfEditing()
                },
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (!isEditing) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            FilledTonalButton(
              onClick = { onSaveProfile(buildProfile(), false) },
              enabled = name.trim().isNotEmpty(),
              shape = RoundedCornerShape(18.dp),
              modifier = Modifier
                .weight(1f)
                .height(52.dp),
            ) {
              Text(AppLabels.Profiles.createOnlyButton, fontWeight = FontWeight.SemiBold)
            }
            Button(
              onClick = { onSaveProfile(buildProfile(), true) },
              enabled = name.trim().isNotEmpty(),
              shape = RoundedCornerShape(18.dp),
              colors = ButtonDefaults.buttonColors(
                containerColor = activeAccent,
                contentColor = selectedMode.onActivePillColor(),
              ),
              modifier = Modifier
                .weight(1f)
                .height(52.dp),
            ) {
              Text(AppLabels.Profiles.saveAndActivateButton, fontWeight = FontWeight.Bold)
            }
          }
        } else {
          Button(
            onClick = { onSaveProfile(buildProfile(), true) },
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = activeAccent,
              contentColor = selectedMode.onActivePillColor(),
            ),
            modifier = Modifier
              .fillMaxWidth()
              .height(52.dp),
          ) {
            Text(AppLabels.Profiles.applyNowButton, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

/**
 * Modal de Prominent Disclosure exigido pela política da Google Play Store no estilo ZENITH.
 */
@Composable
fun ProminentLocationDisclosureDialog(
  onAcceptAndContinue: () -> Unit,
  onDismiss: () -> Unit,
) {
  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    icon = {
      Box(
        modifier = Modifier
          .size(48.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = Icons.Default.LocationOn,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.primary,
          modifier = Modifier.size(26.dp),
        )
      }
    },
    title = {
      Text(
        text = AppLabels.Disclosure.title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
          text = AppLabels.Disclosure.body,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = SoundMode.NORMAL.tonalContainerColor(),
          border = BorderStroke(1.dp, SoundMode.NORMAL.tonalAccentColor().copy(alpha = 0.35f)),
        ) {
          Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Icon(
              imageVector = Icons.Default.Shield,
              contentDescription = null,
              tint = SoundMode.NORMAL.tonalAccentColor(),
            )
            Text(
              text = AppLabels.Disclosure.privacyHighlight,
              style = MaterialTheme.typography.bodySmall,
              color = SoundMode.NORMAL.onTonalContainerColor(),
            )
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = onAcceptAndContinue,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.testTag(AppTestTags.Locations.ACCEPT_DISCLOSURE_BUTTON),
      ) {
        Text(AppLabels.Disclosure.acceptButton, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text(AppLabels.Disclosure.dismissButton)
      }
    },
  )
}
