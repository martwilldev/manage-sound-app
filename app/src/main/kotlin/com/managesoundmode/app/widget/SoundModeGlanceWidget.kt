package com.managesoundmode.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionSendBroadcast
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.managesoundmode.app.MainActivity
import com.managesoundmode.app.ManageSoundApplication
import com.managesoundmode.app.domain.model.SoundMode
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.receiver.SystemAutomationReceiver
import kotlinx.coroutines.flow.first

class SoundModeWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = SoundModeGlanceWidget()
}

class SoundModeGlanceWidget : GlanceAppWidget() {

  override val sizeMode: SizeMode = SizeMode.Responsive(
    setOf(
      SMALL_PILL,
      MEDIUM_BAR,
      LARGE_HUB,
    ),
  )

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val app = context.applicationContext as? ManageSoundApplication
    val currentMode = app?.container?.soundSystemController?.readCurrentMode() ?: SoundMode.NORMAL
    val streams = app?.container?.soundSystemController?.readVolumeStreams()
    val prefs = app?.container?.preferencesDataSource?.prefsFlow?.first()
    val profiles = app?.container?.soundModeRepository?.profilesStream?.first()
      ?: SoundProfile.defaultPresets

    val activeProfile = profiles.firstOrNull { it.id == prefs?.activeProfileId }
    val activeTimer = prefs?.activeTimer
    val remainingMinutes = activeTimer?.remainingMinutes(System.currentTimeMillis())

    provideContent {
      GlanceTheme {
        val size = LocalSize.current
        WidgetContent(
          context = context,
          size = size,
          currentMode = currentMode,
          mediaPercent = streams?.mediaPercent ?: 0,
          activeProfileName = activeProfile?.name,
          activeProfileId = activeProfile?.id,
          remainingTimerMinutes = remainingMinutes?.takeIf { it > 0 },
          profiles = profiles.take(4),
        )
      }
    }
  }

  @Composable
  private fun WidgetContent(
    context: Context,
    size: DpSize,
    currentMode: SoundMode,
    mediaPercent: Int,
    activeProfileName: String?,
    activeProfileId: String?,
    remainingTimerMinutes: Int?,
    profiles: List<SoundProfile>,
  ) {
    val isLargeHub = size.height >= 120.dp
    val isCompactPill = size.width < 240.dp && !isLargeHub

    Column(
      modifier = GlanceModifier
        .fillMaxSize()
        .cornerRadius(24.dp)
        .background(GlanceTheme.colors.surface)
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      if (isCompactPill) {
        CompactPillLayout(
          context = context,
          currentMode = currentMode,
          activeProfileName = activeProfileName,
          remainingTimerMinutes = remainingTimerMinutes,
        )
      } else {
        HeaderAndModesRow(
          context = context,
          currentMode = currentMode,
          mediaPercent = mediaPercent,
          activeProfileName = activeProfileName,
          remainingTimerMinutes = remainingTimerMinutes,
        )

        if (isLargeHub) {
          Spacer(modifier = GlanceModifier.height(10.dp))
          ProfilesRow(
            context = context,
            profiles = profiles,
            activeProfileId = activeProfileId,
          )
        }
      }
    }
  }

  @Composable
  private fun CompactPillLayout(
    context: Context,
    currentMode: SoundMode,
    activeProfileName: String?,
    remainingTimerMinutes: Int?,
  ) {
    Row(
      modifier = GlanceModifier
        .fillMaxWidth()
        .clickable(actionSendBroadcast(cycleModeIntent(context))),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(modifier = GlanceModifier.defaultWeight()) {
        Text(
          text = activeProfileName ?: currentMode.title,
          style = TextStyle(
            color = GlanceTheme.colors.onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
          ),
          maxLines = 1,
        )
        Text(
          text = if (remainingTimerMinutes != null) {
            "${currentMode.title} • ${remainingTimerMinutes}m"
          } else {
            "${currentMode.title} • Toque p/ alternar"
          },
          style = TextStyle(
            color = GlanceTheme.colors.onSurfaceVariant,
            fontSize = 12.sp,
          ),
          maxLines = 1,
        )
      }
    }
  }

  @Composable
  private fun HeaderAndModesRow(
    context: Context,
    currentMode: SoundMode,
    mediaPercent: Int,
    activeProfileName: String?,
    remainingTimerMinutes: Int?,
  ) {
    Row(
      modifier = GlanceModifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Column(
        modifier = GlanceModifier
          .defaultWeight()
          .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
      ) {
        Text(
          text = activeProfileName ?: currentMode.title,
          style = TextStyle(
            color = GlanceTheme.colors.onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
          ),
          maxLines = 1,
        )
        val detail = when {
          remainingTimerMinutes != null -> "${currentMode.title} • Volta em ${remainingTimerMinutes}m"
          else -> "${currentMode.title} • Mídia $mediaPercent%"
        }
        Text(
          text = detail,
          style = TextStyle(
            color = GlanceTheme.colors.onSurfaceVariant,
            fontSize = 12.sp,
          ),
          maxLines = 1,
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        ModePillButton(
          label = "Som",
          selected = currentMode == SoundMode.NORMAL,
          intent = setModeIntent(context, SoundMode.NORMAL),
        )
        Spacer(modifier = GlanceModifier.width(6.dp))
        ModePillButton(
          label = "Vibrar",
          selected = currentMode == SoundMode.VIBRATE,
          intent = setModeIntent(context, SoundMode.VIBRATE),
        )
        Spacer(modifier = GlanceModifier.width(6.dp))
        ModePillButton(
          label = "Silêncio",
          selected = currentMode == SoundMode.SILENT,
          intent = setModeIntent(context, SoundMode.SILENT),
        )
      }
    }
  }

  @Composable
  private fun ProfilesRow(
    context: Context,
    profiles: List<SoundProfile>,
    activeProfileId: String?,
  ) {
    Row(
      modifier = GlanceModifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      profiles.forEachIndexed { index, profile ->
        val selected = profile.id == activeProfileId
        Box(
          modifier = GlanceModifier
            .defaultWeight()
            .cornerRadius(14.dp)
            .background(
              if (selected) GlanceTheme.colors.primaryContainer
              else GlanceTheme.colors.secondaryContainer,
            )
            .clickable(actionSendBroadcast(activateProfileIntent(context, profile.id)))
            .padding(vertical = 8.dp, horizontal = 6.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = profile.name,
            style = TextStyle(
              color = if (selected) GlanceTheme.colors.onPrimaryContainer
              else GlanceTheme.colors.onSecondaryContainer,
              fontSize = 12.sp,
              fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            ),
            maxLines = 1,
          )
        }
        if (index < profiles.lastIndex) {
          Spacer(modifier = GlanceModifier.width(6.dp))
        }
      }
    }
  }

  @Composable
  private fun ModePillButton(
    label: String,
    selected: Boolean,
    intent: Intent,
  ) {
    Box(
      modifier = GlanceModifier
        .cornerRadius(14.dp)
        .background(
          if (selected) GlanceTheme.colors.primary
          else GlanceTheme.colors.surfaceVariant,
        )
        .clickable(actionSendBroadcast(intent))
        .padding(horizontal = 10.dp, vertical = 7.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = label,
        style = TextStyle(
          color = if (selected) GlanceTheme.colors.onPrimary
          else GlanceTheme.colors.onSurfaceVariant,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold,
        ),
        maxLines = 1,
      )
    }
  }

  private fun setModeIntent(context: Context, mode: SoundMode): Intent =
    Intent(context, SystemAutomationReceiver::class.java).apply {
      action = SystemAutomationReceiver.ACTION_WIDGET_SET_MODE
      putExtra(SystemAutomationReceiver.EXTRA_SOUND_MODE, mode.name)
    }

  private fun cycleModeIntent(context: Context): Intent =
    Intent(context, SystemAutomationReceiver::class.java).apply {
      action = SystemAutomationReceiver.ACTION_WIDGET_CYCLE_MODE
    }

  private fun activateProfileIntent(context: Context, profileId: String): Intent =
    Intent(context, SystemAutomationReceiver::class.java).apply {
      action = SystemAutomationReceiver.ACTION_ACTIVATE_PROFILE
      putExtra(SystemAutomationReceiver.EXTRA_PROFILE_ID, profileId)
    }

  companion object {
    private val SMALL_PILL = DpSize(140.dp, 60.dp)
    private val MEDIUM_BAR = DpSize(280.dp, 72.dp)
    private val LARGE_HUB = DpSize(280.dp, 136.dp)

    suspend fun refreshAllWidgets(context: Context) {
      runCatching {
        SoundModeGlanceWidget().updateAll(context)
      }
    }

    fun requestPinWidget(context: Context): Boolean {
      val appWidgetManager = context.getSystemService(AppWidgetManager::class.java) ?: return false
      if (!appWidgetManager.isRequestPinAppWidgetSupported) return false
      val provider = ComponentName(context, SoundModeWidgetReceiver::class.java)
      return appWidgetManager.requestPinAppWidget(provider, null, null)
    }
  }
}
