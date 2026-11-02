package com.managesoundmode.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
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
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.managesoundmode.app.MainActivity
import com.managesoundmode.app.ManageSoundApplication
import com.managesoundmode.app.R
import com.managesoundmode.app.data.local.PersistedSoundPrefs
import com.managesoundmode.app.domain.model.DeviceSoundSnapshot
import com.managesoundmode.app.domain.model.SoundMode
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.domain.model.TransitionTriggerSource
import com.managesoundmode.app.domain.model.VolumeStreams
import kotlinx.coroutines.flow.first

class SoundModeWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = SoundModeGlanceWidget()
}

class SoundModeWidgetActionCallback : ActionCallback {
  override suspend fun onAction(
    context: Context,
    glanceId: GlanceId,
    parameters: ActionParameters,
  ) {
    val app = context.applicationContext as? ManageSoundApplication ?: return
    val repo = app.container.soundModeRepository

    val command = parameters[KEY_COMMAND] ?: return
    when (command) {
      CMD_SET_MODE -> {
        val rawMode = parameters[KEY_MODE] ?: return
        val mode = runCatching { SoundMode.valueOf(rawMode) }.getOrNull() ?: return
        repo.setSoundMode(mode, source = TransitionTriggerSource.WIDGET)
      }
      CMD_CYCLE_MODE -> {
        repo.cycleSoundModeFromQuickTile()
      }
      CMD_ACTIVATE_PROFILE -> {
        val profileId = parameters[KEY_PROFILE_ID] ?: return
        val profile = repo.profilesStream.first().firstOrNull { it.id == profileId } ?: return
        repo.activateProfile(profile, source = TransitionTriggerSource.WIDGET)
      }
    }

    SoundModeGlanceWidget().updateAll(context)
  }

  companion object {
    val KEY_COMMAND = ActionParameters.Key<String>("widget_cmd")
    val KEY_MODE = ActionParameters.Key<String>("widget_mode")
    val KEY_PROFILE_ID = ActionParameters.Key<String>("widget_profile_id")

    const val CMD_SET_MODE = "set_mode"
    const val CMD_CYCLE_MODE = "cycle_mode"
    const val CMD_ACTIVATE_PROFILE = "activate_profile"
  }
}

class SoundModeGlanceWidget : GlanceAppWidget() {

  override val sizeMode: SizeMode = SizeMode.Responsive(
    setOf(
      COMPACT_HERO,
      MEDIUM_HERO,
      EXPANDED_HERO,
    ),
  )

  override suspend fun provideGlance(context: Context, id: GlanceId) {
    val app = context.applicationContext as? ManageSoundApplication
    val repo = app?.container?.soundModeRepository
    val controller = app?.container?.soundSystemController

    val initialMode = controller?.readCurrentMode() ?: SoundMode.NORMAL
    val initialStreams = controller?.readVolumeStreams() ?: VolumeStreams()
    val initialPrefs = repo?.preferencesStream?.first() ?: PersistedSoundPrefs()
    val initialProfiles = repo?.profilesStream?.first() ?: SoundProfile.defaultPresets
    val initialSnapshot = DeviceSoundSnapshot(
      currentMode = initialMode,
      streams = initialStreams,
      hasDndPermission = controller?.hasDndPermission() ?: true,
      activeProfileId = initialPrefs.activeProfileId,
      activeTimer = initialPrefs.activeTimer,
    )

    provideContent {
      GlanceTheme {
        val snapshot by if (repo != null) {
          repo.snapshotStream.collectAsState(initial = initialSnapshot)
        } else {
          androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(initialSnapshot) }
        }
        val prefs by if (repo != null) {
          repo.preferencesStream.collectAsState(initial = initialPrefs)
        } else {
          androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(initialPrefs) }
        }
        val profiles by if (repo != null) {
          repo.profilesStream.collectAsState(initial = initialProfiles)
        } else {
          androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(initialProfiles) }
        }

        val activeProfile = profiles.firstOrNull { it.id == snapshot.activeProfileId }
        val remainingMinutes = prefs.activeTimer?.remainingMinutes(System.currentTimeMillis())

        HeroCardWidgetLayout(
          context = context,
          size = LocalSize.current,
          currentMode = snapshot.currentMode,
          mediaPercent = snapshot.streams.mediaPercent,
          muteMediaOnSilent = prefs.muteMediaOnSilent,
          activeProfile = activeProfile,
          activeRuleTitle = snapshot.activeRuleTitle,
          remainingTimerMinutes = remainingMinutes?.takeIf { it > 0 },
          profiles = profiles.take(4),
        )
      }
    }
  }

  @Composable
  private fun HeroCardWidgetLayout(
    context: Context,
    size: DpSize,
    currentMode: SoundMode,
    mediaPercent: Int,
    muteMediaOnSilent: Boolean,
    activeProfile: SoundProfile?,
    activeRuleTitle: String?,
    remainingTimerMinutes: Int?,
    profiles: List<SoundProfile>,
  ) {
    val showProfilesStrip = size.height >= 150.dp

    // Container externo idêntico ao Hero Card (28.dp)
    Column(
      modifier = GlanceModifier
        .fillMaxSize()
        .cornerRadius(28.dp)
        .background(GlanceTheme.colors.surface)
        .padding(12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // 1. Cabeçalho Tonal Expressivo do Hero Card (20.dp)
      HeroTonalBanner(
        context = context,
        currentMode = currentMode,
        mediaPercent = mediaPercent,
        muteMediaOnSilent = muteMediaOnSilent,
        activeProfile = activeProfile,
        activeRuleTitle = activeRuleTitle,
        remainingTimerMinutes = remainingTimerMinutes,
      )

      Spacer(modifier = GlanceModifier.height(10.dp))

      // 2. Seletor dos 3 Modos (Som | Vibrar | Silêncio) com ícones reais
      HeroModesSelectorRow(currentMode = currentMode)

      // 3. Se o widget for expandido verticalmente (4x2 grande), exibe também os Perfis Rápidos
      if (showProfilesStrip && profiles.isNotEmpty()) {
        Spacer(modifier = GlanceModifier.height(8.dp))
        HeroProfilesRow(
          profiles = profiles,
          activeProfileId = activeProfile?.id,
        )
      }
    }
  }

  @Composable
  private fun HeroTonalBanner(
    context: Context,
    currentMode: SoundMode,
    mediaPercent: Int,
    muteMediaOnSilent: Boolean,
    activeProfile: SoundProfile?,
    activeRuleTitle: String?,
    remainingTimerMinutes: Int?,
  ) {
    val bannerBg: ColorProvider = when (currentMode) {
      SoundMode.NORMAL -> GlanceTheme.colors.primaryContainer
      SoundMode.VIBRATE -> GlanceTheme.colors.secondaryContainer
      SoundMode.SILENT -> GlanceTheme.colors.tertiaryContainer
    }
    val bannerContent: ColorProvider = when (currentMode) {
      SoundMode.NORMAL -> GlanceTheme.colors.onPrimaryContainer
      SoundMode.VIBRATE -> GlanceTheme.colors.onSecondaryContainer
      SoundMode.SILENT -> GlanceTheme.colors.onTertiaryContainer
    }
    val badgeBg: ColorProvider = when (currentMode) {
      SoundMode.NORMAL -> GlanceTheme.colors.surface
      SoundMode.VIBRATE -> GlanceTheme.colors.surface
      SoundMode.SILENT -> GlanceTheme.colors.surface
    }

    Row(
      modifier = GlanceModifier
        .fillMaxWidth()
        .cornerRadius(20.dp)
        .background(bannerBg)
        .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
        .padding(horizontal = 14.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Círculo do Ícone do Modo (40.dp)
      Box(
        modifier = GlanceModifier
          .size(40.dp)
          .cornerRadius(20.dp)
          .background(badgeBg),
        contentAlignment = Alignment.Center,
      ) {
        Image(
          provider = ImageProvider(modeDrawableRes(currentMode)),
          contentDescription = currentMode.title,
          colorFilter = ColorFilter.tint(bannerContent),
          modifier = GlanceModifier.size(22.dp),
        )
      }

      Spacer(modifier = GlanceModifier.width(12.dp))

      // Título + Subtítulo do Hero Card
      Column(modifier = GlanceModifier.defaultWeight()) {
        Text(
          text = currentMode.title,
          style = TextStyle(
            color = bannerContent,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
          ),
          maxLines = 1,
        )
        val subtitle = when {
          remainingTimerMinutes != null -> "Volta em ${remainingTimerMinutes}m • Mídia $mediaPercent%"
          activeRuleTitle != null -> activeRuleTitle
          currentMode != SoundMode.NORMAL && muteMediaOnSilent ->
            "${currentMode.subtitle} • Mídia 0%"
          else -> "${currentMode.subtitle} • Mídia $mediaPercent%"
        }
        Text(
          text = subtitle,
          style = TextStyle(
            color = bannerContent,
            fontSize = 12.sp,
          ),
          maxLines = 1,
        )
      }

      Spacer(modifier = GlanceModifier.width(8.dp))

      // Chip Compacto do Perfil Ativo à direita (Idêntico ao Hero Card da tela inicial)
      Box(
        modifier = GlanceModifier
          .cornerRadius(14.dp)
          .background(badgeBg)
          .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Image(
            provider = ImageProvider(R.drawable.ic_widget_style),
            contentDescription = null,
            colorFilter = ColorFilter.tint(bannerContent),
            modifier = GlanceModifier.size(14.dp),
          )
          Spacer(modifier = GlanceModifier.width(5.dp))
          Text(
            text = activeProfile?.name ?: "Perfis",
            style = TextStyle(
              color = bannerContent,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
          )
        }
      }
    }
  }

  @Composable
  private fun HeroModesSelectorRow(currentMode: SoundMode) {
    Row(
      modifier = GlanceModifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      HeroModeButton(
        modifier = GlanceModifier.defaultWeight(),
        label = "Som",
        iconRes = R.drawable.ic_widget_volume_up,
        selected = currentMode == SoundMode.NORMAL,
        targetMode = SoundMode.NORMAL,
      )
      Spacer(modifier = GlanceModifier.width(8.dp))
      HeroModeButton(
        modifier = GlanceModifier.defaultWeight(),
        label = "Vibrar",
        iconRes = R.drawable.ic_widget_vibration,
        selected = currentMode == SoundMode.VIBRATE,
        targetMode = SoundMode.VIBRATE,
      )
      Spacer(modifier = GlanceModifier.width(8.dp))
      HeroModeButton(
        modifier = GlanceModifier.defaultWeight(),
        label = "Silêncio",
        iconRes = R.drawable.ic_widget_silent,
        selected = currentMode == SoundMode.SILENT,
        targetMode = SoundMode.SILENT,
      )
    }
  }

  @Composable
  private fun HeroModeButton(
    modifier: GlanceModifier,
    label: String,
    iconRes: Int,
    selected: Boolean,
    targetMode: SoundMode,
  ) {
    val bgColor = if (selected) {
      GlanceTheme.colors.primary
    } else {
      GlanceTheme.colors.surfaceVariant
    }
    val contentColor = if (selected) {
      GlanceTheme.colors.onPrimary
    } else {
      GlanceTheme.colors.onSurfaceVariant
    }

    Box(
      modifier = modifier
        .cornerRadius(16.dp)
        .background(bgColor)
        .clickable(
          actionRunCallback<SoundModeWidgetActionCallback>(
            actionParametersOf(
              SoundModeWidgetActionCallback.KEY_COMMAND to SoundModeWidgetActionCallback.CMD_SET_MODE,
              SoundModeWidgetActionCallback.KEY_MODE to targetMode.name,
            ),
          ),
        )
        .padding(horizontal = 8.dp, vertical = 10.dp),
      contentAlignment = Alignment.Center,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
          provider = ImageProvider(iconRes),
          contentDescription = null,
          colorFilter = ColorFilter.tint(contentColor),
          modifier = GlanceModifier.size(16.dp),
        )
        Spacer(modifier = GlanceModifier.width(6.dp))
        Text(
          text = label,
          style = TextStyle(
            color = contentColor,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
          ),
          maxLines = 1,
        )
      }
    }
  }

  @Composable
  private fun HeroProfilesRow(
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
              if (selected) GlanceTheme.colors.secondaryContainer
              else GlanceTheme.colors.surfaceVariant,
            )
            .clickable(
              actionRunCallback<SoundModeWidgetActionCallback>(
                actionParametersOf(
                  SoundModeWidgetActionCallback.KEY_COMMAND to SoundModeWidgetActionCallback.CMD_ACTIVATE_PROFILE,
                  SoundModeWidgetActionCallback.KEY_PROFILE_ID to profile.id,
                ),
              ),
            )
            .padding(vertical = 8.dp, horizontal = 6.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = profile.name,
            style = TextStyle(
              color = if (selected) GlanceTheme.colors.onSecondaryContainer
              else GlanceTheme.colors.onSurfaceVariant,
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

  private fun modeDrawableRes(mode: SoundMode): Int = when (mode) {
    SoundMode.NORMAL -> R.drawable.ic_widget_volume_up
    SoundMode.VIBRATE -> R.drawable.ic_widget_vibration
    SoundMode.SILENT -> R.drawable.ic_widget_silent
  }

  companion object {
    private val COMPACT_HERO = DpSize(180.dp, 100.dp)
    private val MEDIUM_HERO = DpSize(280.dp, 120.dp)
    private val EXPANDED_HERO = DpSize(280.dp, 165.dp)

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
