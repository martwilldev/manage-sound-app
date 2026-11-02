package com.managesoundmode.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.managesoundmode.app.domain.model.VolumeStreamType
import kotlin.math.roundToInt

/**
 * Padrão "Grouped List Cards" das Configurações Nativas do Android 15/16 (Estudo 04, Seção 1.3).
 * - Item único: 24.dp em todos os cantos.
 * - Primeiro item: topo 24.dp, base 4.dp.
 * - Item intermediário: 4.dp em todos os cantos.
 * - Último item: topo 4.dp, base 24.dp.
 */
fun groupedCardShape(index: Int, totalCount: Int): RoundedCornerShape {
  if (totalCount <= 1) return RoundedCornerShape(24.dp)
  return when (index) {
    0 -> RoundedCornerShape(
      topStart = 24.dp,
      topEnd = 24.dp,
      bottomStart = 4.dp,
      bottomEnd = 4.dp,
    )
    totalCount - 1 -> RoundedCornerShape(
      topStart = 4.dp,
      topEnd = 4.dp,
      bottomStart = 24.dp,
      bottomEnd = 24.dp,
    )
    else -> RoundedCornerShape(4.dp)
  }
}

/**
 * Switch Material 3 com ícone `Check` (`✓`) quando ativo e feedback tátil (Estudo 04, Seção 1.4).
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
 * Pill Slider espesso inspirado no Painel de Volume do Android 14/15 (Estudo 04, Seção 1.4).
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
    verticalArrangement = Arrangement.spacedBy(6.dp),
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
            .background(MaterialTheme.colorScheme.secondaryContainer),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(18.dp),
          )
        }
        Text(
          text = type.label,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium,
          color = MaterialTheme.colorScheme.onSurface,
        )
      }
      Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
      ) {
        Text(
          text = "$currentPercent%",
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
        )
      }
    }

    // Pill Track Visual + Slider Interativo
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(36.dp),
      contentAlignment = Alignment.Center,
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(22.dp)
          .clip(RoundedCornerShape(50))
          .background(MaterialTheme.colorScheme.surfaceContainerHighest),
      ) {
        Box(
          modifier = Modifier
            .fillMaxWidth(fraction = (currentPercent / 100f).coerceIn(0.04f, 1f))
            .height(22.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)),
        )
      }
      Slider(
        value = sliderValue,
        onValueChange = { sliderValue = it },
        onValueChangeFinished = { onValueChangeFinished(sliderValue.roundToInt()) },
        valueRange = 0f..100f,
        colors = SliderDefaults.colors(
          thumbColor = MaterialTheme.colorScheme.onPrimaryContainer,
          activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0f),
          inactiveTrackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f),
        ),
        modifier = Modifier
          .fillMaxWidth()
          .testTag("slider_${type.name}"),
      )
    }
  }
}

/**
 * Modal de Prominent Disclosure exigido pela política da Google Play Store (Estudo 02, Seção 4)
 * antes de solicitar permissão de localização em segundo plano.
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
