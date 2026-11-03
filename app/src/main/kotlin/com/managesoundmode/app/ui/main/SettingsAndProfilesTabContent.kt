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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BluetoothAudio
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MusicOff
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SettingsBrightness
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.managesoundmode.app.domain.model.RestorePolicy
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.domain.model.ThemeMode
import com.managesoundmode.app.domain.model.TransitionLogEntry
import com.managesoundmode.app.tile.QuickTileKind
import com.managesoundmode.app.ui.components.M3CheckSwitch
import com.managesoundmode.app.ui.components.groupedCardShape
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
  modifier: Modifier = Modifier,
) {
  val restorePolicies = RestorePolicy.entries

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .testTag("settings_tab_list"),
    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 36.dp),
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

    // 2. Gatilho de Botão de Volume, Notificações & Inteligência de Mídia
    item {
      Spacer(modifier = Modifier.height(14.dp))
      SectionHeader(title = "Botões de Volume, Notificações & Mídia")
    }

    item {
      val shape = groupedCardShape(index = 0, totalCount = 3)
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
                  text = "Sugerir Timer ao Silenciar",
                  style = MaterialTheme.typography.titleMedium,
                  fontWeight = FontWeight.Medium,
                )
                Text(
                  text = "Exibe notificação com [30m] [1h] [2h] quando você silencia pelo botão físico",
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
              }
            }
            M3CheckSwitch(
              checked = state.smartVolumeTriggerEnabled,
              onCheckedChange = onSetSmartVolumeTriggerEnabled,
            )
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
          ) {
            TextButton(onClick = onTestSmartNotification) {
              Text("Testar notificação inteligente")
            }
          }
        }
      }
    }

    item {
      val shape = groupedCardShape(index = 1, totalCount = 3)
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
              imageVector = Icons.Default.MusicOff,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Column {
              Text(
                text = "Zerar Mídia ao Silenciar",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
              )
              Text(
                text = "Zera vídeos/jogos ao entrar em Vibrar ou Silencioso e devolve ao voltar",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          M3CheckSwitch(
            checked = state.muteMediaOnSilent,
            onCheckedChange = onSetMuteMediaOnSilent,
          )
        }
      }
    }

    item {
      val shape = groupedCardShape(index = 2, totalCount = 3)
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
              imageVector = Icons.Default.BluetoothAudio,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Column {
              Text(
                text = "Preservar Mídia no Bluetooth",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
              )
              Text(
                text = "Mantém o volume do fone ou som automotivo conectado",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          M3CheckSwitch(
            checked = state.preserveMediaOnBluetooth,
            onCheckedChange = onSetPreserveMediaOnBluetooth,
          )
        }
      }
    }

    // 3. Widgets & Gadgets do Sistema
    item {
      Spacer(modifier = Modifier.height(14.dp))
      SectionHeader(title = "Widgets & Gadgets do Sistema")
    }

    // 3.1 Barra Rápida Persistente na Gaveta de Notificações
    item {
      val shape = groupedCardShape(index = 0, totalCount = 5)
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("persistent_quick_bar_card"),
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
              imageVector = Icons.Default.NotificationsActive,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
            )
            Column {
              Text(
                text = "Gadget Fixo na Barra de Notificações",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
              )
              Text(
                text = "Exibe barra silenciosa com botões [Som] [Vibrar] [Silêncio] ao puxar as notificações",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
              )
            }
          }
          M3CheckSwitch(
            checked = state.persistentQuickBarEnabled,
            onCheckedChange = onSetPersistentQuickBarEnabled,
            modifier = Modifier.testTag("persistent_quick_bar_switch"),
          )
        }
      }
    }

    // 3.2 Opção A — Trio de Modos Diretos no Header (Som | Vibrar | Silêncio) + Bloco Ciclo
    item {
      val shape = groupedCardShape(index = 1, totalCount = 5)
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("quick_settings_tile_card"),
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
                text = "Blocos de Modo no Header (Trio ou Ciclo)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
              )
              Text(
                text = "Adicione os 3 blocos independentes (apenas o modo atual fica colorido no topo e qualquer troca leva 1 toque) ou o bloco único de ciclo.",
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
                .testTag("add_tile_normal"),
            ) {
              Text("+ Som", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            FilledTonalButton(
              onClick = { onRequestAddQuickTile(QuickTileKind.MODE_VIBRATE, null) },
              shape = RoundedCornerShape(14.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("add_tile_vibrate"),
            ) {
              Text("+ Vibrar", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            FilledTonalButton(
              onClick = { onRequestAddQuickTile(QuickTileKind.MODE_SILENT, null) },
              shape = RoundedCornerShape(14.dp),
              contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("add_tile_silent"),
            ) {
              Text("+ Silêncio", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }

          OutlinedButton(
            onClick = { onRequestAddQuickTile(QuickTileKind.CYCLE, null) },
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("add_quick_tile_button"),
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = null,
              modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text("Adicionar Bloco Único (Ciclo: Som → Vibrar → Silêncio)")
          }
        }
      }
    }

    // 3.3 Opção B — Slots de Perfis Favoritos no Header (Slot 1 e Slot 2)
    item {
      val shape = groupedCardShape(index = 2, totalCount = 5)
      val slot1Profile = state.profiles.firstOrNull { it.id == state.quickTileSlot1ProfileId }
        ?: state.profiles.firstOrNull()
      val slot2Profile = state.profiles.firstOrNull { it.id == state.quickTileSlot2ProfileId }
        ?: state.profiles.getOrNull(1)
        ?: state.profiles.firstOrNull()

      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("quick_profile_tiles_card"),
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
                text = "Blocos de Perfis Favoritos no Header",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
              )
              Text(
                text = "Escolha até 2 perfis para acionar no topo do Android aplicando os 4 canais de volume em 1 toque.",
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
                slot1Profile?.name ?: "Perfil Rápido 1",
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
                slot2Profile?.name ?: "Perfil Rápido 2",
              )
            },
          )
        }
      }
    }

    // 3.4 Opções C & D — Blocos de Timer Rápido (30m/1h/2h) e Zero Mídia (Mudo 0%)
    item {
      val shape = groupedCardShape(index = 3, totalCount = 5)
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .testTag("quick_utility_tiles_card"),
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
                text = "Blocos de Timer Rápido & Zero Mídia",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
              )
              Text(
                text = "Atalhos de 1 toque no Header para iniciar Timer de Silêncio (30m/1h/2h) ou zerar a Mídia (0%) antes de abrir vídeos.",
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
                .testTag("add_tile_timer"),
            ) {
              Text("+ Timer (30m/1h)", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            FilledTonalButton(
              onClick = { onRequestAddQuickTile(QuickTileKind.MEDIA_MUTE, null) },
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .weight(1f)
                .testTag("add_tile_media_mute"),
            ) {
              Text("+ Zero Mídia (0%)", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
          }
        }
      }
    }

    // 3.5 Widget Material You na Tela Inicial (2x1, 4x1 e 4x2)
    item {
      val shape = groupedCardShape(index = 4, totalCount = 5)
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
                text = "Widget Material You (2x1, 4x1 e 4x2)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
              )
              Text(
                text = "Troque entre Som, Vibrar, Silêncio e Perfis em 1 toque direto na tela inicial ou segure o ícone do app para ver os 4 atalhos rápidos.",
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
              .testTag("pin_home_widget_button"),
          ) {
            Text("Fixar Widget na Tela Inicial")
          }
        }
      }
    }

    // 4. Comportamento ao Finalizar Período (RestorePolicy)
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

    // 5. Status do Sistema (Health Check)
    item {
      Spacer(modifier = Modifier.height(14.dp))
      SectionHeader(title = "Permissões do Sistema")
    }

    item {
      HealthCheckItemCard(
        title = "Acesso ao Não Perturbe (DND)",
        subtitle = if (state.hasDndPermission) "Concedido" else "Pendente • Toque para autorizar",
        isHealthy = state.hasDndPermission,
        shape = groupedCardShape(index = 0, totalCount = 4),
        onClick = onRequestDndPermission,
      )
    }

    item {
      HealthCheckItemCard(
        title = "Notificações Inteligentes",
        subtitle = if (state.hasNotificationPermission) {
          "Concedido • Timer e sugestões ativos"
        } else {
          "Pendente • Toque para permitir notificações"
        },
        isHealthy = state.hasNotificationPermission,
        shape = groupedCardShape(index = 1, totalCount = 4),
        onClick = onRequestNotificationPermission,
      )
    }

    item {
      HealthCheckItemCard(
        title = "Alarmes Exatos",
        subtitle = if (state.canScheduleExactAlarms) "Concedido" else "Pendente • Toque para autorizar",
        isHealthy = state.canScheduleExactAlarms,
        shape = groupedCardShape(index = 2, totalCount = 4),
        onClick = onRequestExactAlarmPermission,
      )
    }

    item {
      HealthCheckItemCard(
        title = "Armazenamento 100% On-Device",
        subtitle = "Ativo • Zero dados enviados à nuvem",
        isHealthy = true,
        shape = groupedCardShape(index = 3, totalCount = 4),
        onClick = {},
      )
    }

    // 6. Histórico Recente
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
          text = "Slot $slotNumber: ${selectedProfile?.name ?: "Nenhum"}",
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
        modifier = Modifier.testTag("add_tile_profile_slot_$slotNumber"),
      ) {
        Text("+ Adicionar Slot $slotNumber")
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
          modifier = Modifier.testTag("slot_${slotNumber}_profile_${profile.id}"),
        )
      }
    }
  }
}
