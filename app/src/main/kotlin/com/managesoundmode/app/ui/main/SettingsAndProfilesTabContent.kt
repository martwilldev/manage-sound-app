package com.managesoundmode.app.ui.main

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.managesoundmode.app.domain.model.ThemeMode
import com.managesoundmode.app.domain.model.TransitionLogEntry
import com.managesoundmode.app.ui.components.M3CheckSwitch
import com.managesoundmode.app.ui.components.groupedCardShape
import com.managesoundmode.app.ui.components.profileIconForKey
import com.managesoundmode.app.ui.theme.onTonalContainerColor
import com.managesoundmode.app.ui.theme.tonalContainerColor
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsAndProfilesTabContent(
  state: MainScreenUiState.Ready,
  onActivateProfile: (SoundProfile) -> Unit,
  onEditProfile: (SoundProfile) -> Unit,
  onCreateNewProfile: () -> Unit,
  onSetRestorePolicy: (RestorePolicy) -> Unit,
  onSetThemeMode: (ThemeMode) -> Unit,
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
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 36.dp),
    verticalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    // 1. Aparência & Tema (Modo Claro, Modo Escuro, Sistema + Monet)
    item {
      SectionHeader(title = "Aparência")
    }

    item {
      val shape = groupedCardShape(index = 0, totalCount = 2)
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(18.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Text(
            text = "Tema do Aplicativo",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
          )
          SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            ThemeMode.entries.forEachIndexed { idx, mode ->
              SegmentedButton(
                selected = state.themeMode == mode,
                onClick = { onSetThemeMode(mode) },
                shape = SegmentedButtonDefaults.itemShape(
                  index = idx,
                  count = ThemeMode.entries.size,
                ),
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
                modifier = Modifier.testTag("theme_button_${mode.name}"),
              ) {
                Text(mode.label)
              }
            }
          }
        }
      }
    }

    item {
      val shape = groupedCardShape(index = 1, totalCount = 2)
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
                text = "Cores Dinâmicas (Material You)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
              )
              Text(
                text = "Adapta as cores ao papel de parede do Android",
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

    // 2. Gestão de Perfis de Som
    item {
      Spacer(modifier = Modifier.height(14.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Perfis de Som",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
        )
        FilledTonalButton(onClick = onCreateNewProfile) {
          Icon(
            imageVector = Icons.Default.Add,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text("Novo Perfil")
        }
      }
    }

    itemsIndexed(
      items = state.profiles,
      key = { _, profile -> "settings_${profile.id}" },
    ) { index, profile ->
      val isActive = state.activeProfileId == profile.id
      val shape = groupedCardShape(index = index, totalCount = state.profiles.size)

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
            .padding(horizontal = 18.dp, vertical = 14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(profile.mode.tonalContainerColor()),
              contentAlignment = Alignment.Center,
            ) {
              Icon(
                imageVector = profileIconForKey(profile.iconKey),
                contentDescription = null,
                tint = profile.mode.onTonalContainerColor(),
                modifier = Modifier.size(20.dp),
              )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                  text = profile.name,
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.SemiBold,
                )
                if (isActive) {
                  Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary,
                  ) {
                    Text(
                      text = "Ativo",
                      style = MaterialTheme.typography.labelSmall,
                      color = MaterialTheme.colorScheme.onPrimary,
                      modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                  }
                }
              }
              Text(
                text = "${profile.mode.title} • ${profile.summaryLabel()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }

          IconButton(onClick = { onEditProfile(profile) }) {
            Icon(
              imageVector = Icons.Default.Edit,
              contentDescription = "Editar perfil ${profile.name}",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
        }
      }
    }

    // 3. Comportamento ao Finalizar Período (RestorePolicy)
    item {
      Spacer(modifier = Modifier.height(14.dp))
      SectionHeader(title = "Ao Encerrar um Período")
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

    // 4. Status do Sistema (Health Check)
    item {
      Spacer(modifier = Modifier.height(14.dp))
      SectionHeader(title = "Permissões do Sistema")
    }

    item {
      HealthCheckItemCard(
        title = "Acesso ao Não Perturbe (DND)",
        subtitle = if (state.hasDndPermission) "Concedido" else "Pendente • Toque para autorizar",
        isHealthy = state.hasDndPermission,
        shape = groupedCardShape(index = 0, totalCount = 3),
        onClick = onRequestDndPermission,
      )
    }

    item {
      HealthCheckItemCard(
        title = "Alarmes Exatos",
        subtitle = if (state.canScheduleExactAlarms) "Concedido" else "Pendente • Toque para autorizar",
        isHealthy = state.canScheduleExactAlarms,
        shape = groupedCardShape(index = 1, totalCount = 3),
        onClick = onRequestExactAlarmPermission,
      )
    }

    item {
      HealthCheckItemCard(
        title = "Armazenamento 100% On-Device",
        subtitle = "Ativo • Zero dados enviados à nuvem",
        isHealthy = true,
        shape = groupedCardShape(index = 2, totalCount = 3),
        onClick = {},
      )
    }

    // 5. Histórico Recente
    item {
      Spacer(modifier = Modifier.height(14.dp))
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(
          text = "Histórico Recente",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
        )
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
              text = "Nenhuma transição registrada ainda.",
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
