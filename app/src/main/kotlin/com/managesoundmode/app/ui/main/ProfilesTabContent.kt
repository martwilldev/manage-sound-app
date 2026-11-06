package com.managesoundmode.app.ui.main

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.ui.common.AppLabels
import com.managesoundmode.app.ui.common.AppTestTags
import com.managesoundmode.app.ui.components.groupedCardShape
import com.managesoundmode.app.ui.components.profileIconForKey
import com.managesoundmode.app.ui.theme.onTonalContainerColor
import com.managesoundmode.app.ui.theme.tonalContainerColor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfilesTabContent(
  state: MainScreenUiState.Ready,
  onActivateProfile: (SoundProfile) -> Unit,
  onEditProfile: (SoundProfile) -> Unit,
  onSetDefaultDayProfileId: (String) -> Unit,
  onDeleteProfile: (String) -> Unit = {},
  onDuplicateProfile: (SoundProfile) -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val canDeleteProfiles = state.profiles.size > 1

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .testTag(AppTestTags.Profiles.LIST),
    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 104.dp),
    verticalArrangement = Arrangement.spacedBy(3.dp),
  ) {
    item {
      Text(
        text = AppLabels.Profiles.listHint,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 4.dp, vertical = 6.dp),
      )
    }

    itemsIndexed(
      items = state.profiles,
      key = { _, profile -> profile.id },
    ) { index, profile ->
      val isActive = state.activeProfileId == profile.id
      val isDefaultRestore = state.defaultDayProfileId == profile.id
      ProfileGroupedCard(
        profile = profile,
        isActive = isActive,
        isDefaultRestore = isDefaultRestore,
        canDelete = canDeleteProfiles,
        shape = groupedCardShape(index = index, totalCount = state.profiles.size),
        onActivate = { onActivateProfile(profile) },
        onEdit = { onEditProfile(profile) },
        onDuplicate = { onDuplicateProfile(profile) },
        onSetDefault = { onSetDefaultDayProfileId(profile.id) },
        onDelete = { onDeleteProfile(profile.id) },
      )
    }
  }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun ProfileGroupedCard(
  profile: SoundProfile,
  isActive: Boolean,
  isDefaultRestore: Boolean,
  canDelete: Boolean,
  shape: RoundedCornerShape,
  onActivate: () -> Unit,
  onEdit: () -> Unit,
  onDuplicate: () -> Unit,
  onSetDefault: () -> Unit,
  onDelete: () -> Unit,
) {
  val haptics = LocalHapticFeedback.current
  var showContextMenu by remember { mutableStateOf(false) }

  val containerColor = if (isActive) {
    MaterialTheme.colorScheme.secondaryContainer
  } else {
    MaterialTheme.colorScheme.surfaceContainerLow
  }

  Box(modifier = Modifier.fillMaxWidth()) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .clip(shape)
        .combinedClickable(
          onClick = onActivate,
          onLongClick = {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            showContextMenu = true
          },
        )
        .testTag(AppTestTags.Profiles.card(profile.id)),
      shape = shape,
      colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        // Linha 1: Ícone + Nome e Descrição com largura livre + Menu (⋮)
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(
                if (isActive) {
                  MaterialTheme.colorScheme.primaryContainer
                } else {
                  MaterialTheme.colorScheme.surfaceContainerHighest
                },
              ),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              imageVector = profileIconForKey(profile.iconKey),
              contentDescription = null,
              tint = if (isActive) {
                MaterialTheme.colorScheme.onPrimaryContainer
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
              modifier = Modifier.size(22.dp),
            )
          }

          Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
          ) {
            Text(
              text = profile.name,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onSurface,
              maxLines = 1,
              overflow = TextOverflow.Ellipsis,
            )
            if (profile.description.isNotBlank()) {
              Text(
                text = profile.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
              )
            }
          }

          Box {
            IconButton(
              onClick = { showContextMenu = true },
              modifier = Modifier
                .size(36.dp)
                .testTag(AppTestTags.Profiles.editButton(profile.id)),
            ) {
              Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = AppLabels.Profiles.optionsContentDescription(profile.name),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
              )
            }

            DropdownMenu(
              expanded = showContextMenu,
              onDismissRequest = { showContextMenu = false },
            ) {
              DropdownMenuItem(
                text = { Text(AppLabels.Profiles.editProfileMenu) },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = null,
                  )
                },
                onClick = {
                  showContextMenu = false
                  onEdit()
                },
                modifier = Modifier.testTag(AppTestTags.Profiles.menuEditItem(profile.id)),
              )
              DropdownMenuItem(
                text = { Text(AppLabels.Profiles.duplicateProfileMenu) },
                leadingIcon = {
                  Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                  )
                },
                onClick = {
                  showContextMenu = false
                  onDuplicate()
                },
                modifier = Modifier.testTag(AppTestTags.Profiles.menuDuplicateItem(profile.id)),
              )
              if (!isDefaultRestore) {
                DropdownMenuItem(
                  text = { Text(AppLabels.Profiles.setDefaultReturnMenu) },
                  leadingIcon = {
                    Icon(
                      imageVector = Icons.Filled.Check,
                      contentDescription = null,
                    )
                  },
                  onClick = {
                    showContextMenu = false
                    onSetDefault()
                  },
                  modifier = Modifier.testTag(AppTestTags.Profiles.menuSetDefaultItem(profile.id)),
                )
              }
              if (canDelete) {
                HorizontalDivider()
                DropdownMenuItem(
                  text = {
                    Text(
                      text = AppLabels.Profiles.deleteProfileMenu,
                      color = MaterialTheme.colorScheme.error,
                    )
                  },
                  leadingIcon = {
                    Icon(
                      imageVector = Icons.Default.DeleteOutline,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.error,
                    )
                  },
                  onClick = {
                    showContextMenu = false
                    onDelete()
                  },
                  modifier = Modifier.testTag(AppTestTags.Profiles.menuDeleteItem(profile.id)),
                )
              }
            }
          }
        }

        // Linha 2: Badges responsivos em FlowRow (nunca esmagam nem quebram verticalmente em zoom aumentado)
        FlowRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
          Surface(
            shape = RoundedCornerShape(50),
            color = profile.mode.tonalContainerColor(),
          ) {
            Text(
              text = profile.mode.title,
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Medium,
              color = profile.mode.onTonalContainerColor(),
              maxLines = 1,
              softWrap = false,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            )
          }

          if (isActive) {
            Surface(
              shape = RoundedCornerShape(50),
              color = MaterialTheme.colorScheme.primary,
            ) {
              Text(
                text = AppLabels.Profiles.activeBadge,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              )
            }
          }

          if (isDefaultRestore) {
            Surface(
              shape = RoundedCornerShape(50),
              color = MaterialTheme.colorScheme.surfaceContainerHighest,
            ) {
              Text(
                text = AppLabels.Profiles.defaultReturnBadge,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
              )
            }
          }
        }

        // Linha 3: Resumo completo dos 4 canais de volume (até 2 linhas em proporção aumentada)
        Text(
          text = AppLabels.Profiles.streamsSummaryLine(profile.streams),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}
