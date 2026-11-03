package com.managesoundmode.app.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
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
import com.managesoundmode.app.domain.model.ThemeMode
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
      CMD_CYCLE_PROFILE -> {
        val profiles = repo.profilesStream.first()
        if (profiles.isNotEmpty()) {
          val currentId = repo.preferencesStream.first().activeProfileId
          val currentIndex = profiles.indexOfFirst { it.id == currentId }
          val nextProfile = profiles[(currentIndex + 1) % profiles.size]
          repo.activateProfile(nextProfile, source = TransitionTriggerSource.WIDGET)
        }
      }
    }

    SoundModeGlanceWidget().updateAll(context)
  }

  companion object {
    val KEY_COMMAND = ActionParameters.Key<String>("widget_cmd")
    val KEY_MODE = ActionParameters.Key<String>("widget_mode")

    const val CMD_SET_MODE = "set_mode"
    const val CMD_CYCLE_MODE = "cycle_mode"
    const val CMD_CYCLE_PROFILE = "cycle_profile"
  }
}

private data class WidgetPalette(
  val outerSurface: ColorProvider,
  val bannerBg: ColorProvider,
  val bannerTitle: ColorProvider,
  val bannerSubtitle: ColorProvider,
  val badgeBg: ColorProvider,
  val badgeContent: ColorProvider,
  val activeButtonBg: ColorProvider,
  val activeButtonContent: ColorProvider,
  val inactiveButtonBg: ColorProvider,
  val inactiveButtonContent: ColorProvider,
)

class SoundModeGlanceWidget : GlanceAppWidget() {

  override val sizeMode: SizeMode = SizeMode.Responsive(
    setOf(
      COMPACT_BAR,
      HERO_CARD_STANDARD,
      HERO_CARD_LARGE,
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
        val isDark = resolveDarkTheme(context, prefs.themeMode)
        val palette = rememberWidgetPalette(snapshot.currentMode, isDark)

        PureHeroWidgetLayout(
          context = context,
          size = LocalSize.current,
          currentMode = snapshot.currentMode,
          streams = snapshot.streams,
          activeProfile = activeProfile,
          remainingTimerMinutes = remainingMinutes?.takeIf { it > 0 },
          palette = palette,
        )
      }
    }
  }

  @Composable
  private fun PureHeroWidgetLayout(
    context: Context,
    size: DpSize,
    currentMode: SoundMode,
    streams: VolumeStreams,
    activeProfile: SoundProfile?,
    remainingTimerMinutes: Int?,
    palette: WidgetPalette,
  ) {
    // Se o usuário redimensionou para 1 linha fina (< 105.dp), usa a barra compacta
    if (size.height < 105.dp) {
      CompactSingleRowHeroWidget(
        context = context,
        currentMode = currentMode,
        streams = streams,
        activeProfile = activeProfile,
        palette = palette,
      )
      return
    }

    // Proposta A (Recomendada): Pure Hero Card 2 Andares (idêntico ao Hero Card da aba Agora)
    Column(
      modifier = GlanceModifier
        .fillMaxSize()
        .cornerRadius(28.dp)
        .background(palette.outerSurface)
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Andar 1: Banner Tonal do Modo Atual + Chip de Perfil Ativo
      HeroTonalBanner(
        context = context,
        currentMode = currentMode,
        streams = streams,
        activeProfile = activeProfile,
        remainingTimerMinutes = remainingTimerMinutes,
        palette = palette,
      )

      Spacer(modifier = GlanceModifier.height(10.dp))

      // Andar 2: Dock Expressivo dos 3 Modos (Som | Vibrar | Silêncio)
      HeroModesSelectorRow(
        currentMode = currentMode,
        palette = palette,
      )
    }
  }

  @Composable
  private fun HeroTonalBanner(
    context: Context,
    currentMode: SoundMode,
    streams: VolumeStreams,
    activeProfile: SoundProfile?,
    remainingTimerMinutes: Int?,
    palette: WidgetPalette,
  ) {
    // Subtítulo curto e útil que NUNCA trunca com reticências
    val cleanSubtitle = when {
      remainingTimerMinutes != null -> "Timer: ${remainingTimerMinutes}m • Mídia ${streams.mediaPercent}%"
      currentMode == SoundMode.NORMAL -> "Toque ${streams.ringPercent}% • Mídia ${streams.mediaPercent}%"
      currentMode == SoundMode.VIBRATE -> "Alerta tátil • Mídia ${streams.mediaPercent}%"
      SoundMode.SILENT == currentMode -> "Zero ruído • Alarme ${streams.alarmPercent}%"
      else -> "Mídia ${streams.mediaPercent}%"
    }

    Row(
      modifier = GlanceModifier
        .fillMaxWidth()
        .cornerRadius(22.dp)
        .background(palette.bannerBg)
        .clickable(actionStartActivity(Intent(context, MainActivity::class.java)))
        .padding(horizontal = 14.dp, vertical = 14.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      // Avatar Circular do Modo (44.dp)
      Box(
        modifier = GlanceModifier
          .size(44.dp)
          .cornerRadius(22.dp)
          .background(palette.badgeBg),
        contentAlignment = Alignment.Center,
      ) {
        Image(
          provider = ImageProvider(modeDrawableRes(currentMode)),
          contentDescription = currentMode.title,
          colorFilter = ColorFilter.tint(palette.badgeContent),
          modifier = GlanceModifier.size(22.dp),
        )
      }

      Spacer(modifier = GlanceModifier.width(12.dp))

      // Título + Métricas Diretas (sem truncar)
      Column(modifier = GlanceModifier.defaultWeight()) {
        Text(
          text = currentMode.title,
          style = TextStyle(
            color = palette.bannerTitle,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
          ),
          maxLines = 1,
        )
        Text(
          text = cleanSubtitle,
          style = TextStyle(
            color = palette.bannerSubtitle,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
          ),
          maxLines = 1,
        )
      }

      Spacer(modifier = GlanceModifier.width(8.dp))

      // Chip do Perfil Ativo à direita (tocar alterna entre os perfis direto no widget!)
      Box(
        modifier = GlanceModifier
          .cornerRadius(16.dp)
          .background(palette.badgeBg)
          .clickable(
            actionRunCallback<SoundModeWidgetActionCallback>(
              actionParametersOf(
                SoundModeWidgetActionCallback.KEY_COMMAND to SoundModeWidgetActionCallback.CMD_CYCLE_PROFILE,
              ),
            ),
          )
          .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Image(
            provider = ImageProvider(R.drawable.ic_widget_style),
            contentDescription = null,
            colorFilter = ColorFilter.tint(palette.badgeContent),
            modifier = GlanceModifier.size(14.dp),
          )
          Spacer(modifier = GlanceModifier.width(5.dp))
          Text(
            text = activeProfile?.name ?: "Perfis",
            style = TextStyle(
              color = palette.badgeContent,
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
  private fun HeroModesSelectorRow(
    currentMode: SoundMode,
    palette: WidgetPalette,
  ) {
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
        palette = palette,
      )
      Spacer(modifier = GlanceModifier.width(8.dp))
      HeroModeButton(
        modifier = GlanceModifier.defaultWeight(),
        label = "Vibrar",
        iconRes = R.drawable.ic_widget_vibration,
        selected = currentMode == SoundMode.VIBRATE,
        targetMode = SoundMode.VIBRATE,
        palette = palette,
      )
      Spacer(modifier = GlanceModifier.width(8.dp))
      HeroModeButton(
        modifier = GlanceModifier.defaultWeight(),
        label = "Silêncio",
        iconRes = R.drawable.ic_widget_silent,
        selected = currentMode == SoundMode.SILENT,
        targetMode = SoundMode.SILENT,
        palette = palette,
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
    palette: WidgetPalette,
  ) {
    val bgColor = if (selected) palette.activeButtonBg else palette.inactiveButtonBg
    val contentColor = if (selected) palette.activeButtonContent else palette.inactiveButtonContent

    Box(
      modifier = modifier
        .height(48.dp)
        .cornerRadius(if (selected) 20.dp else 16.dp)
        .background(bgColor)
        .clickable(
          actionRunCallback<SoundModeWidgetActionCallback>(
            actionParametersOf(
              SoundModeWidgetActionCallback.KEY_COMMAND to SoundModeWidgetActionCallback.CMD_SET_MODE,
              SoundModeWidgetActionCallback.KEY_MODE to targetMode.name,
            ),
          ),
        )
        .padding(horizontal = 10.dp),
      contentAlignment = Alignment.Center,
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Image(
          provider = ImageProvider(iconRes),
          contentDescription = null,
          colorFilter = ColorFilter.tint(contentColor),
          modifier = GlanceModifier.size(18.dp),
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
  private fun CompactSingleRowHeroWidget(
    context: Context,
    currentMode: SoundMode,
    streams: VolumeStreams,
    activeProfile: SoundProfile?,
    palette: WidgetPalette,
  ) {
    Row(
      modifier = GlanceModifier
        .fillMaxSize()
        .cornerRadius(26.dp)
        .background(palette.outerSurface)
        .padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Row(
        modifier = GlanceModifier
          .defaultWeight()
          .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier = GlanceModifier
            .size(38.dp)
            .cornerRadius(19.dp)
            .background(palette.bannerBg),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            provider = ImageProvider(modeDrawableRes(currentMode)),
            contentDescription = currentMode.title,
            colorFilter = ColorFilter.tint(palette.bannerTitle),
            modifier = GlanceModifier.size(20.dp),
          )
        }
        Spacer(modifier = GlanceModifier.width(10.dp))
        Column {
          Text(
            text = currentMode.title,
            style = TextStyle(
              color = palette.bannerTitle,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
            ),
            maxLines = 1,
          )
          Text(
            text = "${activeProfile?.name ?: "Manual"} • Mídia ${streams.mediaPercent}%",
            style = TextStyle(
              color = palette.inactiveButtonContent,
              fontSize = 11.sp,
            ),
            maxLines = 1,
          )
        }
      }

      CompactIconPill(
        iconRes = R.drawable.ic_widget_volume_up,
        selected = currentMode == SoundMode.NORMAL,
        targetMode = SoundMode.NORMAL,
        palette = palette,
      )
      Spacer(modifier = GlanceModifier.width(6.dp))
      CompactIconPill(
        iconRes = R.drawable.ic_widget_vibration,
        selected = currentMode == SoundMode.VIBRATE,
        targetMode = SoundMode.VIBRATE,
        palette = palette,
      )
      Spacer(modifier = GlanceModifier.width(6.dp))
      CompactIconPill(
        iconRes = R.drawable.ic_widget_silent,
        selected = currentMode == SoundMode.SILENT,
        targetMode = SoundMode.SILENT,
        palette = palette,
      )
    }
  }

  @Composable
  private fun CompactIconPill(
    iconRes: Int,
    selected: Boolean,
    targetMode: SoundMode,
    palette: WidgetPalette,
  ) {
    val bg = if (selected) palette.activeButtonBg else palette.inactiveButtonBg
    val tint = if (selected) palette.activeButtonContent else palette.inactiveButtonContent
    Box(
      modifier = GlanceModifier
        .size(40.dp)
        .cornerRadius(14.dp)
        .background(bg)
        .clickable(
          actionRunCallback<SoundModeWidgetActionCallback>(
            actionParametersOf(
              SoundModeWidgetActionCallback.KEY_COMMAND to SoundModeWidgetActionCallback.CMD_SET_MODE,
              SoundModeWidgetActionCallback.KEY_MODE to targetMode.name,
            ),
          ),
        ),
      contentAlignment = Alignment.Center,
    ) {
      Image(
        provider = ImageProvider(iconRes),
        contentDescription = targetMode.title,
        colorFilter = ColorFilter.tint(tint),
        modifier = GlanceModifier.size(18.dp),
      )
    }
  }

  private fun resolveDarkTheme(context: Context, themeMode: ThemeMode): Boolean {
    return when (themeMode) {
      ThemeMode.DARK -> true
      ThemeMode.LIGHT -> false
      ThemeMode.SYSTEM -> {
        val nightMask = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        nightMask == Configuration.UI_MODE_NIGHT_YES
      }
    }
  }

  private fun rememberWidgetPalette(mode: SoundMode, isDark: Boolean): WidgetPalette {
    if (isDark) {
      val (bannerBg, bannerTitle, bannerSub, badgeBg, badgeContent, activeBtnBg) = when (mode) {
        SoundMode.NORMAL -> Sextuple(
          Color(0xFF1B365D),
          Color(0xFFEAF2FF),
          Color(0xFFB8D1F8),
          Color(0xFF284B7E),
          Color(0xFFEAF2FF),
          Color(0xFF4C8DF6),
        )
        SoundMode.VIBRATE -> Sextuple(
          Color(0xFF392759),
          Color(0xFFF5ECFF),
          Color(0xFFD6BFF6),
          Color(0xFF4E3778),
          Color(0xFFF5ECFF),
          Color(0xFF9B72CF),
        )
        SoundMode.SILENT -> Sextuple(
          Color(0xFF4F1E27),
          Color(0xFFFFECEE),
          Color(0xFFF8B8C0),
          Color(0xFF6B2A36),
          Color(0xFFFFECEE),
          Color(0xFFE25C67),
        )
      }
      return WidgetPalette(
        outerSurface = ColorProvider(Color(0xFF131820)),
        bannerBg = ColorProvider(bannerBg),
        bannerTitle = ColorProvider(bannerTitle),
        bannerSubtitle = ColorProvider(bannerSub),
        badgeBg = ColorProvider(badgeBg),
        badgeContent = ColorProvider(badgeContent),
        activeButtonBg = ColorProvider(activeBtnBg),
        activeButtonContent = ColorProvider(Color(0xFF0B1118)),
        inactiveButtonBg = ColorProvider(Color(0xFF1F2733)),
        inactiveButtonContent = ColorProvider(Color(0xFFC5CFDC)),
      )
    } else {
      val (bannerBg, bannerTitle, bannerSub, badgeBg, badgeContent, activeBtnBg) = when (mode) {
        SoundMode.NORMAL -> Sextuple(
          Color(0xFFD7E6FF),
          Color(0xFF0E294B),
          Color(0xFF2E4D75),
          Color(0xFFFFFFFF),
          Color(0xFF16437E),
          Color(0xFF1E5AA8),
        )
        SoundMode.VIBRATE -> Sextuple(
          Color(0xFFECDCFF),
          Color(0xFF2C1649),
          Color(0xFF513873),
          Color(0xFFFFFFFF),
          Color(0xFF4D2C7A),
          Color(0xFF653E9C),
        )
        SoundMode.SILENT -> Sextuple(
          Color(0xFFFFDAD9),
          Color(0xFF410E14),
          Color(0xFF702931),
          Color(0xFFFFFFFF),
          Color(0xFF8C1D28),
          Color(0xFFB32634),
        )
      }
      return WidgetPalette(
        outerSurface = ColorProvider(Color(0xFFF2F5FA)),
        bannerBg = ColorProvider(bannerBg),
        bannerTitle = ColorProvider(bannerTitle),
        bannerSubtitle = ColorProvider(bannerSub),
        badgeBg = ColorProvider(badgeBg),
        badgeContent = ColorProvider(badgeContent),
        activeButtonBg = ColorProvider(activeBtnBg),
        activeButtonContent = ColorProvider(Color(0xFFFFFFFF)),
        inactiveButtonBg = ColorProvider(Color(0xFFE2E7F0)),
        inactiveButtonContent = ColorProvider(Color(0xFF394352)),
      )
    }
  }

  private data class Sextuple<A, B, C, D, E, F>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F,
  )

  private fun modeDrawableRes(mode: SoundMode): Int = when (mode) {
    SoundMode.NORMAL -> R.drawable.ic_widget_volume_up
    SoundMode.VIBRATE -> R.drawable.ic_widget_vibration
    SoundMode.SILENT -> R.drawable.ic_widget_silent
  }

  companion object {
    private val COMPACT_BAR = DpSize(180.dp, 72.dp)
    private val HERO_CARD_STANDARD = DpSize(260.dp, 130.dp)
    private val HERO_CARD_LARGE = DpSize(320.dp, 160.dp)

    suspend fun refreshAllWidgets(context: Context) {
      runCatching { SoundModeGlanceWidget().updateAll(context) }
    }

    fun requestPinWidget(context: Context): Boolean {
      val appWidgetManager = context.getSystemService(AppWidgetManager::class.java) ?: return false
      if (!appWidgetManager.isRequestPinAppWidgetSupported) return false
      val provider = ComponentName(context, SoundModeWidgetReceiver::class.java)
      return appWidgetManager.requestPinAppWidget(provider, null, null)
    }
  }
}
