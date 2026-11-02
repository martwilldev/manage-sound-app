package com.managesoundmode.app.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.ui.components.profileIconForKey
import com.managesoundmode.app.ui.theme.onTonalContainerColor
import com.managesoundmode.app.ui.theme.tonalContainerColor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfilesTabContent(
  state: MainScreenUiState.Ready,
  onActivateProfile: (SoundProfile) -> Unit,
  onEditProfile: (SoundProfile) -> Unit,
  onCreateNewProfile: () -> Unit,
  onSetDefaultDayProfileId: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .testTag("profiles_tab_list"),
    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Toque para ativar ou ajuste os volumes de cada perfil",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        FilledTonalButton(
          onClick = onCreateNewProfile,
          shape = RoundedCornerShape(18.dp),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
          modifier = Modifier.testTag("save_profile_button"),
        ) {
          Icon(
            imageVector = Icons.Filled.Add,
            contentDescription = null,
            modifier = Modifier
              .padding(end = 6.dp)
              .size(18.dp),
          )
          Text("Novo", fontWeight = FontWeight.SemiBold)
        }
      }
    }

    items(state.profiles, key = { it.id }) { profile ->
      val isActive = state.activeProfileId == profile.id
      val isDefaultRestore = state.defaultDayProfileId == profile.id
      ProfileSceneCard(
        profile = profile,
        isActive = isActive,
        isDefaultRestore = isDefaultRestore,
        onActivate = { onActivateProfile(profile) },
        onEdit = { onEditProfile(profile) },
      )
    }

    item {
      Column(
        modifier = Modifier.padding(top = 8.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        Text(
          text = "Perfil padrão de retorno",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
          ) {
            Text(
              text = "Usado automaticamente quando uma rotina de horário ou local chega ao fim.",
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
                )
              }
            }
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProfileSceneCard(
  profile: SoundProfile,
  isActive: Boolean,
  isDefaultRestore: Boolean,
  onActivate: () -> Unit,
  onEdit: () -> Unit,
) {
  val containerColor = if (isActive) {
    profile.mode.tonalContainerColor()
  } else {
    MaterialTheme.colorScheme.surfaceContainerLow
  }
  val contentColor = if (isActive) {
    profile.mode.onTonalContainerColor()
  } else {
    MaterialTheme.colorScheme.onSurface
  }
  val secondaryColor = if (isActive) {
    contentColor.copy(alpha = 0.8f)
  } else {
    MaterialTheme.colorScheme.onSurfaceVariant
  }

  Surface(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(24.dp))
      .clickable(onClick = onActivate)
      .testTag("profile_card_${profile.id}"),
    shape = RoundedCornerShape(24.dp),
    color = containerColor,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 18.dp, vertical = 16.dp),
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
          Surface(
            modifier = Modifier.size(42.dp),
            shape = CircleShape,
            color = if (isActive) {
              contentColor.copy(alpha = 0.14f)
            } else {
              MaterialTheme.colorScheme.surfaceContainerHighest
            },
          ) {
            Box(contentAlignment = Alignment.Center) {
              Icon(
                imageVector = profileIconForKey(profile.iconKey),
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(20.dp),
              )
            }
          }

          Column(modifier = Modifier.weight(1f)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
              Text(
                text = profile.name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
              )
              if (isActive) {
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = contentColor.copy(alpha = 0.16f),
                ) {
                  Text(
                    text = "Ativo",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = contentColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                  )
                }
              } else if (isDefaultRestore) {
                Surface(
                  shape = RoundedCornerShape(10.dp),
                  color = MaterialTheme.colorScheme.surfaceContainerHighest,
                ) {
                  Text(
                    text = "Retorno padrão",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                  )
                }
              }
            }
            Text(
              text = "${profile.mode.title} • ${profile.description}",
              style = MaterialTheme.typography.bodySmall,
              color = secondaryColor,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
          }
        }

        IconButton(
          onClick = onEdit,
          modifier = Modifier
            .size(36.dp)
            .testTag("edit_profile_${profile.id}"),
        ) {
          Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = "Editar perfil ${profile.name}",
            tint = secondaryColor,
            modifier = Modifier.size(18.dp),
          )
        }
      }

      FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
      ) {
        MiniStreamBadge(
          label = "Toque ${profile.streams.ringPercent}%",
          isActive = isActive,
          contentColor = contentColor,
        )
        MiniStreamBadge(
          label = "Notif ${profile.streams.notificationPercent}%",
          isActive = isActive,
          contentColor = contentColor,
        )
        MiniStreamBadge(
          label = "Mídia ${profile.streams.mediaPercent}%",
          isActive = isActive,
          contentColor = contentColor,
        )
        MiniStreamBadge(
          label = "Alarme ${profile.streams.alarmPercent}%",
          isActive = isActive,
          contentColor = contentColor,
        )
      }
    }
  }
}

@Composable
private fun MiniStreamBadge(
  label: String,
  isActive: Boolean,
  contentColor: androidx.compose.ui.graphics.Color,
) {
  Surface(
    shape = RoundedCornerShape(10.dp),
    color = if (isActive) {
      contentColor.copy(alpha = 0.12f)
    } else {
      MaterialTheme.colorScheme.surfaceContainerHigh
    },
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Medium,
      color = if (isActive) contentColor else MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
    )
  }
}
