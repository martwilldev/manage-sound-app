package com.managesoundmode.app.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.managesoundmode.app.domain.model.RestorePolicy
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.domain.model.TransitionLogEntry
import com.managesoundmode.app.ui.components.M3CheckSwitch
import com.managesoundmode.app.ui.components.groupedCardShape
import com.managesoundmode.app.ui.theme.SoundNormalEmerald
import com.managesoundmode.app.ui.theme.SoundVibrateAmber
import com.managesoundmode.app.ui.theme.accentColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun SettingsAndProfilesTabContent(
  state: MainScreenUiState.Ready,
  onActivateProfile: (SoundProfile) -> Unit,
  onDeleteCustomProfile: (String) -> Unit,
  onOpenSaveProfileDialog: () -> Unit,
  onSetRestorePolicy: (RestorePolicy) -> Unit,
  onSetUseDynamicColor: (Boolean) -> Unit,
  onRequestDndPermission: () -> Unit,
  onRequestExactAlarmPermission: () -> Unit,
  onClearLogs: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val restorePolicies = RestorePolicy.entries

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .testTag("settings_tab_list"),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 32.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    // 1. Status do Sistema (Health Check) — Padrão Android 15/16 Settings Grouped List Cards
    item {
      SectionHeader(
        title = "Status do Sistema (Health Check)",
        subtitle = "Permissões nativas para automação pontual e sem falhas",
      )
    }

    item {
      HealthCheckItemCard(
        title = "Acesso ao Não Perturbe (DND)",
        subtitle = if (state.hasDndPermission) {
          "Ativo • Permite ativar e sair do modo Silencioso automaticamente"
        } else {
          "Pendente • Toque para autorizar nas configurações do Android"
        },
        isHealthy = state.hasDndPermission,
        shape = groupedCardShape(index = 0, totalCount = 3),
        onClick = onRequestDndPermission,
      )
    }

    item {
      HealthCheckItemCard(
        title = "Alarmes Exatos (Horários Pontuais)",
        subtitle = if (state.canScheduleExactAlarms) {
          "Ativo • Trocas de modo ocorrem exatamente no minuto agendado"
        } else {
          "Pendente • Autorize para evitar atrasos em Doze Mode"
        },
        isHealthy = state.canScheduleExactAlarms,
        shape = groupedCardShape(index = 1, totalCount = 3),
        onClick = onRequestExactAlarmPermission,
      )
    }

    item {
      HealthCheckItemCard(
        title = "Privacidade 100% On-Device",
        subtitle = "Zero servidores • Regras e locais salvos apenas no seu aparelho",
        isHealthy = true,
        shape = groupedCardShape(index = 2, totalCount = 3),
        onClick = {},
      )
    }

    // 2. Comportamento ao Finalizar Período (RestorePolicy — Estudo 03 Seção 4)
    item {
      Spacer(modifier = Modifier.height(14.dp))
      SectionHeader(
        title = "Ao Encerrar um Período ou Local",
        subtitle = "O que fazer quando a última regra ativa termina (ex.: às 07:00 após Dormir)",
      )
    }

    itemsIndexed(
      items = restorePolicies,
      key = { _, policy -> policy.name },
    ) { index, policy ->
      val selected = state.restorePolicy == policy
      val shape = groupedCardShape(index = index, totalCount = restorePolicies.size)
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .clip(shape)
          .clickable { onSetRestorePolicy(policy) }
          .testTag("restore_policy_${policy.name}"),
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

    // 3. Aparência "App do Sistema" (Material You / Monet)
    item {
      Spacer(modifier = Modifier.height(14.dp))
      SectionHeader(
        title = "Aparência do Sistema",
        subtitle = "Integração visual com o Android 15/16",
      )
    }

    item {
      val shape = groupedCardShape(index = 0, totalCount = 1)
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
                text = "Cores Dinâmicas (Material You / Monet)",
                style = MaterialTheme.typography.titleMedium,
              )
              Text(
                text = "Absorve automaticamente a paleta de cores do papel de parede do Android.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          M3CheckSwitch(
            checked = state.useDynamicColor,
            onCheckedChange = onSetUseDynamicColor,
          )
        }
      }
    }

    // 4. Catálogo de Perfis de Som
    item {
      Spacer(modifier = Modifier.height(14.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Perfis de Som",
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            text = "Toque para ativar ou vincule a horários e locais",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        FilledTonalButton(onClick = onOpenSaveProfileDialog) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Criar do Estado Atual")
        }
      }
    }

    itemsIndexed(
      items = state.profiles,
      key = { _, profile -> "settings_${profile.id}" },
    ) { index, profile ->
      val isActive = state.activeProfileId == profile.id
      val shape = groupedCardShape(index = index, totalCount = state.profiles.size)
      val accent = profile.mode.accentColor()

      Card(
        modifier = Modifier
          .fillMaxWidth()
          .clip(shape)
          .clickable { onActivateProfile(profile) },
        shape = shape,
        colors = CardDefaults.cardColors(
          containerColor = if (isActive) {
            MaterialTheme.colorScheme.secondaryContainer
          } else {
            MaterialTheme.colorScheme.surfaceContainerLow
          },
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
                fontWeight = FontWeight.SemiBold,
              )
              Surface(
                shape = RoundedCornerShape(6.dp),
                color = accent.copy(alpha = 0.2f),
              ) {
                Text(
                  text = profile.mode.title,
                  style = MaterialTheme.typography.labelSmall,
                  color = accent,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                )
              }
            }
            Text(
              text = "Toque ${profile.streams.ringPercent}% • Notif ${profile.streams.notificationPercent}% • Mídia ${profile.streams.mediaPercent}% • Alarme ${profile.streams.alarmPercent}%",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }

          if (!profile.isPreset) {
            IconButton(onClick = { onDeleteCustomProfile(profile.id) }) {
              Icon(
                imageVector = Icons.Default.DeleteOutline,
                contentDescription = "Remover perfil",
              )
            }
          } else if (isActive) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = "Ativo",
              tint = accent,
            )
          }
        }
      }
    }

    // 5. Histórico Recente (Log Transparente — Estudo 04 Seção 2)
    item {
      Spacer(modifier = Modifier.height(14.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column {
          Text(
            text = "Histórico Recente de Transições",
            style = MaterialTheme.typography.titleMedium,
          )
          Text(
            text = "Registro transparente de todas as mudanças de modo",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        if (state.transitionLogs.isNotEmpty()) {
          TextButton(onClick = onClearLogs) {
            Text("Limpar")
          }
        }
      }
    }

    if (state.transitionLogs.isEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(24.dp),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
          ),
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
              text = "Nenhuma transição registrada ainda nesta sessão.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    } else {
      itemsIndexed(
        items = state.transitionLogs,
        key = { _, log -> log.id },
      ) { index, log ->
        val shape = groupedCardShape(index = index, totalCount = state.transitionLogs.size)
        TransitionLogCard(log = log, shape = shape)
      }
    }
  }
}

@Composable
private fun SectionHeader(
  title: String,
  subtitle: String,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(bottom = 8.dp),
    verticalArrangement = Arrangement.spacedBy(2.dp),
  ) {
    Text(
      text = title,
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onBackground,
    )
    Text(
      text = subtitle,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
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
        tint = if (isHealthy) SoundNormalEmerald else SoundVibrateAmber,
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
