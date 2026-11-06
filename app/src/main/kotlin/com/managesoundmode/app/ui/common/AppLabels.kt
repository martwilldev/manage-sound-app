package com.managesoundmode.app.ui.common

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.managesoundmode.app.domain.model.AppAccessRole
import com.managesoundmode.app.domain.model.AppPermission
import com.managesoundmode.app.domain.model.RestorePolicy
import com.managesoundmode.app.domain.model.SoundMode
import com.managesoundmode.app.domain.model.ThemeMode
import com.managesoundmode.app.domain.model.TransitionTriggerSource
import com.managesoundmode.app.domain.model.VolumeStreamType
import com.managesoundmode.app.domain.model.VolumeStreams
import com.managesoundmode.app.domain.model.Weekday
import java.util.Locale

enum class AppLanguage(
  val code: String,
  val displayName: String,
  val shortLabel: String,
) {
  PT_BR("pt-BR", "Português (Brasil)", "Português"),
  EN("en", "English", "English"),
}

/**
 * Catálogo centralizado de Internacionalização (i18n) e Labels de UI do aplicativo.
 * Mantém todos os textos visíveis ao usuário organizados por domínio em um único arquivo,
 * com suporte nativo e reativo a Português (PT-BR) e Inglês (EN).
 */
object AppLabels {

  var currentLanguage: AppLanguage by mutableStateOf(AppLanguage.PT_BR)

  private fun tr(pt: String, en: String): String =
    when (currentLanguage) {
      AppLanguage.PT_BR -> pt
      AppLanguage.EN -> en
    }

  object Domain {
    fun soundModeTitle(mode: SoundMode): String =
      when (mode) {
        SoundMode.NORMAL -> tr("Som Ativo", "Active Sound")
        SoundMode.VIBRATE -> tr("Vibrar", "Vibrate")
        SoundMode.SILENT -> tr("Silencioso", "Silent")
      }

    fun soundModeSubtitle(mode: SoundMode): String =
      when (mode) {
        SoundMode.NORMAL -> tr("Toque e notificações audíveis", "Audible ring and notifications")
        SoundMode.VIBRATE -> tr("Silêncio sonoro, alerta tátil", "Silent audio, haptic alert")
        SoundMode.SILENT -> tr("Foco total, zero interrupções", "Total focus, zero interruptions")
      }

    fun streamTypeLabel(type: VolumeStreamType): String =
      when (type) {
        VolumeStreamType.RING -> tr("Toque (Ring)", "Ring")
        VolumeStreamType.NOTIFICATION -> tr("Notificações", "Notifications")
        VolumeStreamType.MEDIA -> tr("Mídia", "Media")
        VolumeStreamType.ALARM -> tr("Alarme", "Alarm")
      }

    fun weekdayShortLabel(day: Weekday): String =
      when (day) {
        Weekday.SUN -> tr("D", "S")
        Weekday.MON -> tr("S", "M")
        Weekday.TUE -> tr("T", "T")
        Weekday.WED -> tr("Q", "W")
        Weekday.THU -> tr("Q", "T")
        Weekday.FRI -> tr("S", "F")
        Weekday.SAT -> tr("S", "S")
      }

    fun weekdayFullLabel(day: Weekday): String =
      when (day) {
        Weekday.SUN -> tr("Dom", "Sun")
        Weekday.MON -> tr("Seg", "Mon")
        Weekday.TUE -> tr("Ter", "Tue")
        Weekday.WED -> tr("Qua", "Wed")
        Weekday.THU -> tr("Qui", "Thu")
        Weekday.FRI -> tr("Sex", "Fri")
        Weekday.SAT -> tr("Sáb", "Sat")
      }

    fun daysSummary(daysOfWeek: Set<Weekday>): String {
      if (daysOfWeek.size == 7) return tr("Todos os dias", "Every day")
      if (daysOfWeek == Weekday.weekdays) return tr("Seg a Sex", "Mon to Fri")
      if (daysOfWeek == Weekday.sleepWeeknights) return tr("Dom a Qui", "Sun to Thu")
      if (daysOfWeek == setOf(Weekday.SAT, Weekday.SUN)) return tr("Fim de semana", "Weekends")
      return Weekday.entries.filter { it in daysOfWeek }.joinToString(", ") { it.fullLabel }
    }

    fun themeModeLabel(mode: ThemeMode): String =
      when (mode) {
        ThemeMode.SYSTEM -> tr("Sistema", "System")
        ThemeMode.LIGHT -> tr("Claro", "Light")
        ThemeMode.DARK -> tr("Escuro", "Dark")
      }

    fun restorePolicyTitle(policy: RestorePolicy): String =
      when (policy) {
        RestorePolicy.DEFAULT_PROFILE -> tr("Perfil Padrão do Dia", "Default Day Profile")
        RestorePolicy.SNAPSHOT -> tr("Último Estado Manual (Snapshot)", "Last Manual State (Snapshot)")
      }

    fun restorePolicySubtitle(policy: RestorePolicy): String =
      when (policy) {
        RestorePolicy.DEFAULT_PROFILE ->
          tr(
            "Garante retorno ao perfil base (Som Ativo) ao encerrar qualquer período",
            "Guarantees return to the base profile (Active Sound) when any schedule ends",
          )
        RestorePolicy.SNAPSHOT ->
          tr(
            "Restaura exatamente o modo e os volumes que estavam antes da regra iniciar",
            "Restores the exact mode and volumes that were active before the rule started",
          )
      }

    fun triggerSourceBadge(source: TransitionTriggerSource): String =
      when (source) {
        TransitionTriggerSource.MANUAL -> tr("Manual", "Manual")
        TransitionTriggerSource.TIMER -> tr("Timer", "Timer")
        TransitionTriggerSource.SCHEDULE -> tr("Horário", "Schedule")
        TransitionTriggerSource.LOCATION -> tr("Local", "Location")
        TransitionTriggerSource.RESTORE -> tr("Auto-Restore", "Auto-Restore")
        TransitionTriggerSource.BOOT -> tr("Sistema / Boot", "System / Boot")
        TransitionTriggerSource.QUICK_TILE -> tr("Atalho Rápido", "Quick Tile")
        TransitionTriggerSource.NOTIFICATION -> tr("Notificação", "Notification")
        TransitionTriggerSource.WIDGET -> tr("Widget", "Widget")
        TransitionTriggerSource.HARDWARE_BUTTON -> tr("Botão de Volume", "Volume Button")
      }
  }

  object Common {
    val back: String get() = tr("Voltar", "Back")
    val create: String get() = tr("Criar", "Create")
    val cancel: String get() = tr("Cancelar", "Cancel")
    val ok: String get() = tr("OK", "OK")
    val restore: String get() = tr("Restaurar", "Restore")
    val allow: String get() = tr("Permitir", "Allow")
    val authorize: String get() = tr("Autorizar", "Authorize")
    val enable: String get() = tr("Ativar", "Enable")
    val clear: String get() = tr("Limpar", "Clear")
    val autoSavedSubtitle: String get() = tr("Salvo automaticamente", "Saved automatically")
  }

  object Tabs {
    val nowLabel: String get() = tr("Agora", "Now")
    val nowTopBarTitle: String get() = tr("Modos de Som", "Sound Modes")
    val profilesLabel: String get() = tr("Perfis", "Profiles")
    val profilesTopBarTitle: String get() = tr("Perfis", "Profiles")
    val schedulesLabel: String get() = tr("Horários", "Schedules")
    val schedulesTopBarTitle: String get() = tr("Horários", "Schedules")
    val locationsLabel: String get() = tr("Locais", "Locations")
    val locationsTopBarTitle: String get() = tr("Locais", "Locations")
    val settingsLabel: String get() = tr("Configurações", "Settings")
    val settingsTopBarTitle: String get() = tr("Configurações", "Settings")
  }

  object TopBar {
    val enableDarkMode: String get() = tr("Ativar Modo Escuro", "Enable Dark Mode")
    val enableLightMode: String get() = tr("Ativar Modo Claro", "Enable Light Mode")
    val overflowMenuDescription: String get() = tr("Menu de opções", "More options")
    val openSettingsMenu: String get() = tr("Configurações", "Settings")
    val pauseAutomationsMenu: String
      get() = tr("Pausar Automações (Modo Férias)", "Pause Automations (Vacation Mode)")
    val resumeAutomationsMenu: String
      get() = tr("Retomar Automações", "Resume Automations")
    val transitionHistoryMenu: String
      get() = tr("Histórico de Transições", "Transition History")
    val permissionsDiagnosticMenu: String
      get() = tr("Diagnóstico de Permissões", "Permissions Diagnostic")
    val restoreFactoryDefaultsMenu: String
      get() = tr("Restaurar Padrões de Fábrica", "Restore Factory Defaults")
    val aboutAndPrivacyMenu: String
      get() = tr("Sobre & Privacidade", "About & Privacy")
    val setupWizardMenu: String
      get() = tr("Assistente de Setup & Permissões", "Setup & Permissions Wizard")
    val accessControlMenu: String
      get() = tr("Controle de Acesso & PIN", "Access Control & PIN")
  }

  object Fab {
    val newProfile: String get() = tr("Novo Perfil", "New Profile")
    val newSchedule: String get() = tr("Novo Horário", "New Schedule")
    val newLocation: String get() = tr("Novo Local", "New Location")
  }

  object Now {
    val keepForLabel: String get() = tr("Manter por:", "Keep for:")
    val extend30MinChip: String get() = "+30m"
    val volumesTitle: String get() = tr("Volumes", "Volumes")
    val manualOverrideTitle: String get() = tr("Ajustado manualmente", "Manually adjusted")
    val manualOverrideSubtitle: String
      get() = tr("Retoma a rotina no próximo horário", "Resumes routine at next schedule")
    val reapplyButton: String get() = tr("Reaplicar", "Reapply")
    val dndBannerTitle: String
      get() = tr("Permissão Não Perturbe", "Do Not Disturb Permission")
    val dndBannerSubtitle: String
      get() = tr("Necessária para o modo Silencioso", "Required for Silent mode")

    fun modeShortPill(mode: SoundMode): String =
      when (mode) {
        SoundMode.NORMAL -> tr("Som", "Sound")
        SoundMode.VIBRATE -> tr("Vibrar", "Vibrate")
        SoundMode.SILENT -> tr("Silêncio", "Silent")
      }

    fun modeContentDescription(mode: SoundMode): String =
      tr("Modo ${mode.title}", "Mode ${mode.title}")

    fun remainingTimerLabel(remainingMinutes: Int): String =
      tr("Volta em ${remainingMinutes}m", "Restores in ${remainingMinutes}m")

    fun modeWithMediaSubtitle(subtitle: String, mediaPercent: Int): String =
      tr("$subtitle • Mídia $mediaPercent%", "$subtitle • Media $mediaPercent%")
  }

  object Profiles {
    val listHint: String
      get() = tr(
        "Toque para ativar • Pressione e segure para editar ou excluir",
        "Tap to activate • Press and hold to edit or delete",
      )
    val defaultReturnSectionTitle: String
      get() = tr("Perfil Padrão de Retorno", "Default Return Profile")
    val defaultReturnSectionSubtitle: String
      get() = tr(
        "Aplicado automaticamente quando uma rotina de horário ou local chega ao fim.",
        "Applied automatically when a schedule or location routine ends.",
      )
    val activeBadge: String get() = tr("Ativo agora", "Active now")
    val defaultReturnBadge: String get() = tr("Padrão de retorno", "Default return")
    val editProfileMenu: String get() = tr("Editar perfil", "Edit profile")
    val duplicateProfileMenu: String get() = tr("Duplicar perfil", "Duplicate profile")
    val setDefaultReturnMenu: String
      get() = tr("Definir como padrão de retorno", "Set as default return profile")
    val deleteProfileMenu: String get() = tr("Excluir perfil", "Delete profile")
    val editProfileTitle: String get() = tr("Editar Perfil", "Edit Profile")
    val newProfileTitle: String get() = tr("Novo Perfil", "New Profile")
    val profileNameLabel: String get() = tr("Nome do perfil", "Profile name")
    val baseSoundModeSectionTitle: String
      get() = tr("Modo de toque", "Ringer mode")
    val channelsVolumeSectionTitle: String
      get() = tr("Volumes salvos neste perfil", "Volumes saved in this profile")
    val preserveMediaTitle: String
      get() = tr("Preservar mídia se música estiver tocando", "Preserve media if music is playing")
    val skipBluetoothTitle: String
      get() = tr("Não alterar mídia com fone Bluetooth", "Do not change media on Bluetooth headphones")
    val applyNowButton: String get() = tr("Ativar Este Perfil Agora", "Activate This Profile Now")
    val saveAndActivateButton: String get() = tr("Criar e Ativar", "Create & Activate")
    val createOnlyButton: String get() = tr("Criar Perfil", "Create Profile")

    fun iconOptions(): List<Pair<String, String>> = listOf(
      "work" to tr("Trabalho", "Work"),
      "bedtime" to tr("Dormir", "Sleep"),
      "vibration" to tr("Reunião", "Meeting"),
      "home" to tr("Casa", "Home"),
      "sunny" to tr("Dia", "Day"),
      "shield" to tr("Foco", "Focus"),
    )

    fun defaultStreamsDescription(streams: VolumeStreams): String =
      tr(
        "Toque ${streams.ringPercent}% • Mídia ${streams.mediaPercent}%",
        "Ring ${streams.ringPercent}% • Media ${streams.mediaPercent}%",
      )

    fun streamsSummaryLine(streams: VolumeStreams): String =
      tr(
        "Toque ${streams.ringPercent}% • Notif ${streams.notificationPercent}% • Mídia ${streams.mediaPercent}% • Alarme ${streams.alarmPercent}%",
        "Ring ${streams.ringPercent}% • Notif ${streams.notificationPercent}% • Media ${streams.mediaPercent}% • Alarm ${streams.alarmPercent}%",
      )

    fun optionsContentDescription(name: String): String =
      tr("Opções do perfil $name", "Options for profile $name")
  }

  object Schedules {
    val exactAlarmBannerTitle: String
      get() = tr(
        "Autorize alarmes exatos para pontualidade",
        "Allow exact alarms for punctuality",
      )
    val listHint: String
      get() = tr(
        "Toque para editar • Pressione e segure para opções ou excluir",
        "Tap to edit • Press and hold for options or delete",
      )
    val highPriorityBadge: String get() = tr("Prioritário", "High Priority")
    val editScheduleMenu: String get() = tr("Editar horário", "Edit schedule")
    val duplicateScheduleMenu: String get() = tr("Duplicar horário", "Duplicate schedule")
    val enableScheduleMenu: String get() = tr("Ativar horário", "Enable schedule")
    val disableScheduleMenu: String get() = tr("Desativar horário", "Disable schedule")
    val deleteScheduleMenu: String get() = tr("Excluir horário", "Delete schedule")
    val editScheduleTitle: String get() = tr("Editar Horário", "Edit Schedule")
    val newScheduleTitle: String get() = tr("Novo Horário", "New Schedule")
    val defaultScheduleName: String get() = tr("Rotina Agendada", "Scheduled Routine")
    val scheduleNameLabel: String
      get() = tr("Nome da rotina (ex.: Dormir, Trabalho)", "Schedule name (e.g. Sleep, Work)")
    val startTimeLabel: String get() = tr("INÍCIO", "START")
    val endTimeLabel: String get() = tr("FIM", "END")
    val daysOfWeekSectionTitle: String get() = tr("Repetir nos dias", "Repeat on days")
    val targetProfileSectionTitle: String
      get() = tr("Perfil de som aplicado", "Applied sound profile")
    val highPriorityTitle: String
      get() = tr("Alta Prioridade (Sobrepor outros)", "High Priority (Override others)")
    val highPrioritySubtitle: String
      get() = tr(
        "Vence conflitos mesmo quando configurado para Som Alto",
        "Wins conflicts even when configured for Loud Sound",
      )
    val createScheduleButton: String get() = tr("Criar Horário", "Create Schedule")
    val selectStartTimeTitle: String
      get() = tr("Horário de Início", "Start Time")
    val selectEndTimeTitle: String
      get() = tr("Horário de Término", "End Time")

    fun optionsContentDescription(name: String): String =
      tr("Opções do horário $name", "Options for schedule $name")
  }

  object Locations {
    val geofenceHeaderTitle: String
      get() = tr("Cercas Geográficas (OpenStreetMap)", "Geofences (OpenStreetMap)")
    val geofenceHeaderSubtitle: String
      get() = tr(
        "Toque em um local para ajustar o pino e o raio no mapa ou pressione e segure para excluir.",
        "Tap a location to adjust its map pin and radius, or press and hold to delete.",
      )
    val readingGpsButton: String get() = tr("Lendo GPS...", "Reading GPS...")
    val verifyGpsButton: String get() = tr("Verificar pelo GPS", "Verify via GPS")
    val gpsReadErrorMessage: String
      get() = tr(
        "Não foi possível ler o GPS agora. Verifique se a localização do aparelho está ativa.",
        "Could not read GPS right now. Please check if device location is enabled.",
      )
    val gpsPermissionRequiredMessage: String
      get() = tr(
        "Permissão de localização necessária para verificar cercas pelo GPS.",
        "Location permission is required to verify geofences via GPS.",
      )
    val insideBadge: String get() = tr("No local", "At location")
    val editLocationMenu: String get() = tr("Editar local e mapa", "Edit location & map")
    val markOutsideMenu: String
      get() = tr("Marcar como fora deste local", "Mark as outside this location")
    val simulateInsideMenu: String
      get() = tr("Simular presença neste local", "Simulate presence at this location")
    val enableLocationMenu: String get() = tr("Ativar local", "Enable location")
    val disableLocationMenu: String get() = tr("Desativar local", "Disable location")
    val deleteLocationMenu: String get() = tr("Excluir local", "Delete location")
    val editLocationTitle: String get() = tr("Editar Local", "Edit Location")
    val newLocationTitle: String get() = tr("Novo Local", "New Location")
    val defaultLocationName: String get() = tr("Local Personalizado", "Custom Location")
    val locationNameLabel: String
      get() = tr(
        "Nome do local (ex.: Escritório, Faculdade)",
        "Location name (e.g. Office, College)",
      )
    val radiusSectionTitle: String
      get() = tr("Raio da Cerca Geográfica", "Geofence Radius")
    val wifiSsidLabel: String
      get() = tr(
        "Rede Wi-Fi opcional (confirmação indoor)",
        "Optional Wi-Fi SSID (indoor confirmation)",
      )
    val targetProfileSectionTitle: String
      get() = tr("Perfil ao permanecer no local", "Profile while at this location")
    val testPresenceButton: String
      get() = tr("Testar presença neste local agora", "Test presence at this location now")
    val createLocationButton: String get() = tr("Criar Local", "Create Location")

    fun radiusLabel(meters: Int): String = tr("Raio ${meters}m", "Radius ${meters}m")
    fun metersValue(meters: Int): String = tr("$meters metros", "$meters meters")
    fun optionsContentDescription(name: String): String =
      tr("Opções do local $name", "Options for location $name")

    val geofenceStatusTitle: String
      get() = tr("Geofencing Passivo em 2º Plano", "Passive Background Geofencing")
    fun geofenceStatusSubtitle(activeCount: Int): String =
      tr(
        "$activeCount cerca(s) ativa(s) • Dwell 2 min + Confirmação Wi-Fi SSID",
        "$activeCount active geofence(s) • 2-min Dwell + Wi-Fi SSID confirmation",
      )
    val syncGeofencesButton: String
      get() = tr("Sincronizar Cercas", "Sync Geofences")
    fun geofencesSyncedMessage(count: Int): String =
      tr(
        "Geofencing em 2º plano sincronizado ($count cerca(s) ativa(s))",
        "Background geofencing synced ($count active geofence(s))",
      )
  }

  object GeofenceMap {
    val mapCardTitle: String
      get() = tr("Mapa da Cerca Geográfica", "Geofence Map")
    val mapCardSubtitle: String
      get() = tr(
        "Toque no mapa, arraste o pino ou busque o endereço para posicionar o perímetro.",
        "Tap the map, drag the pin, or search an address to position the perimeter.",
      )
    val fetchingGpsLabel: String get() = tr("Obtendo GPS...", "Getting GPS...")
    val useCurrentGpsLabel: String get() = tr("Usar GPS atual", "Use current GPS")
    val searchPlaceholder: String
      get() = tr(
        "Buscar rua, bairro, empresa ou cidade...",
        "Search street, neighborhood, place or city...",
      )
    val searchContentDescription: String get() = tr("Buscar endereço", "Search address")
    val searchResultsHint: String
      get() = tr(
        "Toque em um endereço para posicionar no mapa:",
        "Tap an address to position on the map:",
      )
    val zoomInContentDescription: String get() = tr("Aproximar mapa", "Zoom in")
    val zoomOutContentDescription: String get() = tr("Afastar mapa", "Zoom out")
    val recenterContentDescription: String get() = tr("Centralizar pino", "Recenter pin")

    fun coordinatesAndRadiusBadge(lat: Double, lng: Double, radiusMeters: Int): String =
      String.format(
        Locale.ROOT,
        tr("%.4f, %.4f • Raio %dm", "%.4f, %.4f • Radius %dm"),
        lat,
        lng,
        radiusMeters,
      )
  }

  object Disclosure {
    val title: String
      get() = tr(
        "Automação por Localização (100% On-Device)",
        "Location Automation (100% On-Device)",
      )
    val body: String
      get() = tr(
        "O Modos de Som coleta dados de localização em segundo plano para ativar e desativar automaticamente seus perfis de som (Silencioso, Vibrar ou Volume Alto) quando você entra ou sai dos locais configurados, mesmo quando o aplicativo está fechado ou não está em uso.",
        "Sound Modes collects location data in the background to automatically enable and disable your sound profiles (Silent, Vibrate, or High Volume) when you enter or leave configured locations, even when the app is closed or not in use.",
      )
    val privacyHighlight: String
      get() = tr(
        "Privacidade Total: Suas coordenadas ficam salvas exclusivamente no seu aparelho. Nenhum dado de GPS é enviado para servidores.",
        "Total Privacy: Your coordinates are stored exclusively on your device. No GPS data is ever sent to external servers.",
      )
    val acceptButton: String get() = tr("Entendi e Continuar", "I Understand & Continue")
    val dismissButton: String get() = tr("Agora não", "Not now")
  }

  object Settings {
    val appearanceSection: String get() = tr("Aparência", "Appearance")
    val appThemeTitle: String get() = tr("Tema do Aplicativo", "App Theme")
    val appLanguageTitle: String get() = tr("Idioma do Aplicativo", "App Language")
    val dynamicColorsTitle: String
      get() = tr("Cores Dinâmicas (Material You)", "Dynamic Colors (Material You)")
    val dynamicColorsSubtitle: String
      get() = tr(
        "Adapta as cores ao papel de parede do Android",
        "Adapts colors to your Android wallpaper",
      )
    val volumeNotificationsMediaSection: String
      get() = tr("Botões de Volume, Notificações & Mídia", "Volume Buttons, Notifications & Media")
    val suggestTimerTitle: String
      get() = tr("Sugerir Timer ao Silenciar", "Suggest Timer When Silencing")
    val suggestTimerSubtitle: String
      get() = tr(
        "Exibe notificação com [30m] [1h] [2h] quando você silencia pelo botão físico",
        "Shows notification with [30m] [1h] [2h] when silencing via hardware button",
      )
    val testSmartNotificationButton: String
      get() = tr("Testar notificação inteligente", "Test smart notification")
    val muteMediaOnSilentTitle: String
      get() = tr("Zerar Mídia ao Silenciar", "Mute Media When Silencing")
    val muteMediaOnSilentSubtitle: String
      get() = tr(
        "Zera vídeos/jogos ao entrar em Vibrar ou Silencioso e devolve ao voltar",
        "Mutes videos/games when entering Vibrate or Silent and restores on return",
      )
    val preserveBluetoothTitle: String
      get() = tr("Preservar Mídia no Bluetooth", "Preserve Media on Bluetooth")
    val preserveBluetoothSubtitle: String
      get() = tr(
        "Mantém o volume do fone ou som automotivo conectado",
        "Keeps volume on connected headphones or car audio",
      )
    val widgetsAndGadgetsSection: String
      get() = tr("Widgets & Gadgets do Sistema", "System Widgets & Gadgets")
    val persistentQuickBarTitle: String
      get() = tr("Gadget Fixo na Barra de Notificações", "Pinned Notification Bar Gadget")
    val persistentQuickBarSubtitle: String
      get() = tr(
        "Exibe barra silenciosa com botões [Som] [Vibrar] [Silêncio] ao puxar as notificações",
        "Shows silent bar with [Sound] [Vibrate] [Silent] buttons in the notification shade",
      )
    val headerModeTilesTitle: String
      get() = tr("Blocos de Modo no Header (Trio ou Ciclo)", "Header Mode Tiles (Trio or Cycle)")
    val headerModeTilesSubtitle: String
      get() = tr(
        "Adicione os 3 blocos independentes (apenas o modo atual fica colorido no topo e qualquer troca leva 1 toque) ou o bloco único de ciclo.",
        "Add the 3 independent tiles (only the active mode is highlighted and switching takes 1 tap) or the single cycle tile.",
      )
    val addTileNormalButton: String get() = tr("+ Som", "+ Sound")
    val addTileVibrateButton: String get() = tr("+ Vibrar", "+ Vibrate")
    val addTileSilentButton: String get() = tr("+ Silêncio", "+ Silent")
    val addCycleTileButton: String
      get() = tr(
        "Adicionar Bloco Único (Ciclo: Som → Vibrar → Silêncio)",
        "Add Single Tile (Cycle: Sound → Vibrate → Silent)",
      )
    val favoriteProfileTilesTitle: String
      get() = tr("Blocos de Perfis Favoritos no Header", "Favorite Profile Tiles in Header")
    val favoriteProfileTilesSubtitle: String
      get() = tr(
        "Escolha até 2 perfis para acionar no topo do Android aplicando os 4 canais de volume em 1 toque.",
        "Choose up to 2 profiles to trigger from the Android header, applying all 4 volume channels in 1 tap.",
      )
    val utilityTilesTitle: String
      get() = tr("Blocos de Timer Rápido & Zero Mídia", "Quick Timer & Zero Media Tiles")
    val utilityTilesSubtitle: String
      get() = tr(
        "Atalhos de 1 toque no Header para iniciar Timer de Silêncio (30m/1h/2h) ou zerar a Mídia (0%) antes de abrir vídeos.",
        "1-tap header shortcuts to start Silent Timer (30m/1h/2h) or mute Media (0%) before opening videos.",
      )
    val addTimerTileButton: String get() = tr("+ Timer (30m/1h)", "+ Timer (30m/1h)")
    val addMediaMuteTileButton: String get() = tr("+ Zero Mídia (0%)", "+ Zero Media (0%)")
    val homeWidgetTitle: String
      get() = tr("Widget Material You (2x1, 4x1 e 4x2)", "Material You Widget (2x1, 4x1 & 4x2)")
    val homeWidgetSubtitle: String
      get() = tr(
        "Troque entre Som, Vibrar, Silêncio e Perfis em 1 toque direto na tela inicial ou segure o ícone do app para ver os 4 atalhos rápidos.",
        "Switch between Sound, Vibrate, Silent and Profiles in 1 tap on your home screen or long-press the app icon for 4 quick shortcuts.",
      )
    val pinHomeWidgetButton: String
      get() = tr("Fixar Widget na Tela Inicial", "Pin Widget to Home Screen")
    val restorePolicySection: String
      get() = tr("Ao Encerrar um Período", "When a Schedule Ends")
    val systemPermissionsSection: String
      get() = tr("Permissões do Sistema", "System Permissions")
    val dndPermissionTitle: String
      get() = tr("Acesso ao Não Perturbe (DND)", "Do Not Disturb Access (DND)")
    val grantedStatus: String get() = tr("Concedido", "Granted")
    val pendingTapToAuthorizeStatus: String
      get() = tr("Pendente • Toque para autorizar", "Pending • Tap to authorize")
    val smartNotificationsTitle: String
      get() = tr("Notificações Inteligentes", "Smart Notifications")
    val smartNotificationsGrantedSubtitle: String
      get() = tr("Concedido • Timer e sugestões ativos", "Granted • Timer and suggestions active")
    val smartNotificationsPendingSubtitle: String
      get() = tr("Pendente • Toque para permitir notificações", "Pending • Tap to allow notifications")
    val exactAlarmsTitle: String get() = tr("Alarmes Exatos", "Exact Alarms")
    val onDeviceStorageTitle: String
      get() = tr("Armazenamento 100% On-Device", "100% On-Device Storage")
    val onDeviceStorageSubtitle: String
      get() = tr("Ativo • Zero dados enviados à nuvem", "Active • Zero data sent to the cloud")
    val recentHistorySection: String get() = tr("Histórico Recente", "Recent History")
    val emptyHistoryMessage: String
      get() = tr("Nenhuma transição registrada ainda.", "No transitions recorded yet.")

    // --- Categorized Settings Hub & New Sub-Screens ---
    val masterAutomationTitle: String
      get() = tr("Automação Inteligente", "Smart Automation")
    val masterAutomationActiveSubtitle: String
      get() = tr(
        "Ativa • Horários e locais funcionando automaticamente",
        "Active • Schedules and locations running automatically",
      )
    val masterAutomationPausedSubtitle: String
      get() = tr(
        "Modo Férias ativo • Todas as rotinas automáticas pausadas",
        "Vacation Mode active • All automatic routines paused",
      )
    val hubPreferencesGroupTitle: String
      get() = tr("Preferências e Comportamento", "Preferences & Behavior")
    val hubSystemGroupTitle: String
      get() = tr("Sistema, Dados e Informações", "System, Data & Info")

    val categoryAppearanceTitle: String
      get() = tr("Aparência, Tema & Idioma", "Appearance, Theme & Language")
    val categoryAutomationTitle: String
      get() = tr("Automação & Retorno de Rotina", "Automation & Routine Return")
    val categoryAudioMediaTitle: String
      get() = tr("Botões de Volume, Mídia & Timer", "Volume Buttons, Media & Timer")
    val categoryGadgetsWidgetsTitle: String
      get() = tr("Atalhos no Header & Widgets", "Header Shortcuts & Widgets")
    val categoryPermissionsTitle: String
      get() = tr("Permissões & Diagnóstico", "Permissions & Diagnostics")
    val categoryAccessControlTitle: String
      get() = tr("Controle de Acesso & PIN (RBAC)", "Access Control & PIN (RBAC)")
    val categoryHistoryTitle: String
      get() = tr("Histórico de Transições", "Transition History")
    val categoryBackupResetTitle: String
      get() = tr("Backup & Restaurar Padrões", "Backup & Factory Reset")
    val categoryAboutTitle: String
      get() = tr("Sobre o Modos de Som & Privacidade", "About Sound Modes & Privacy")

    val hapticFeedbackTitle: String
      get() = tr("Vibração Tátil nos Controles", "Haptic Feedback on Controls")
    val hapticFeedbackSubtitle: String
      get() = tr(
        "Retorno tátil suave ao alternar modos, perfis e sliders",
        "Gentle tactile feedback when switching modes, profiles and sliders",
      )
    val notifyOnAutoTransitionTitle: String
      get() = tr("Notificar ao Ativar Rotina Automática", "Notify on Automatic Routine")
    val notifyOnAutoTransitionSubtitle: String
      get() = tr(
        "Exibe aviso silencioso quando um horário ou local mudar o modo de som",
        "Shows silent alert when a schedule or location changes the sound mode",
      )
    val defaultTimerSectionTitle: String
      get() = tr("Duração Padrão do Timer Rápido", "Default Quick Timer Duration")
    val defaultTimerSectionSubtitle: String
      get() = tr(
        "Usada ao iniciar o timer pelo atalho rápido do Header ou Widget",
        "Used when starting a timer from the Header quick tile or Widget",
      )

    val locationPermissionTitle: String
      get() = tr("Localização para Cercas (GPS)", "Location for Geofences (GPS)")
    val locationPermissionGrantedSubtitle: String
      get() = tr("Concedido • Cercas geográficas ativas", "Granted • Geofences active")
    val locationPermissionPendingSubtitle: String
      get() = tr("Opcional • Conceda na aba Locais se usar cercas", "Optional • Grant in Locations tab if using geofences")

    val filterAll: String get() = tr("Todos", "All")
    val filterManual: String get() = tr("Manual", "Manual")
    val filterSchedule: String get() = tr("Horário", "Schedule")
    val filterLocation: String get() = tr("Local", "Location")
    val filterShortcuts: String get() = tr("Atalhos", "Shortcuts")

    val backupSectionTitle: String
      get() = tr("Exportar & Importar Configurações (JSON)", "Export & Import Settings (JSON)")
    val backupSectionSubtitle: String
      get() = tr(
        "Salve uma cópia dos seus perfis, horários, locais e preferências ou restaure em outro aparelho.",
        "Save a copy of your profiles, schedules, locations and preferences or restore on another device.",
      )
    val exportBackupButton: String
      get() = tr("Copiar Backup JSON", "Copy JSON Backup")
    val importBackupButton: String
      get() = tr("Importar JSON", "Import JSON")
    val importBackupInputLabel: String
      get() = tr("Cole aqui o JSON de backup exportado", "Paste exported backup JSON here")
    val backupCopiedMessage: String
      get() = tr("Backup JSON copiado para a área de transferência", "Backup JSON copied to clipboard")

    val factoryResetSectionTitle: String
      get() = tr("Restaurar Padrões de Fábrica", "Restore Factory Defaults")
    val factoryResetSectionSubtitle: String
      get() = tr(
        "Recria os 5 perfis originais (Trabalho, Reunião, Dormir, Casa e Padrão), restaura horários/locais de fábrica e redefine preferências.",
        "Recreates the 5 original profiles (Work, Meeting, Sleep, Home and Default), restores default schedules/locations and resets preferences.",
      )
    val factoryResetButton: String
      get() = tr("Restaurar Perfis e Regras de Fábrica", "Restore Factory Profiles & Rules")
    val factoryResetDialogTitle: String
      get() = tr("Restaurar padrões de fábrica?", "Restore factory defaults?")
    val factoryResetDialogMessage: String
      get() = tr(
        "Todos os perfis predefinidos excluídos serão recuperados e suas regras de horário e local voltarão para a configuração original.",
        "All deleted preset profiles will be recovered and your schedule and location rules will return to their original configuration.",
      )

    val aboutVersionSubtitle: String
      get() = tr("Versão 0.1.0 (MCP1) • Android 8 a 16", "Version 0.1.0 (MCP1) • Android 8 to 16")
    val aboutTagline: String
      get() = tr(
        "Controle inteligente de som, perfis multicanal, horários e cercas geográficas sem nunca perder chamadas importantes.",
        "Smart sound control, multi-channel profiles, schedules and geofences without ever missing important calls.",
      )
    val priorityHierarchyTitle: String
      get() = tr("Como Funciona a Prioridade de Regras", "How Rule Priority Works")
    val priorityStep1: String
      get() = tr(
        "1. Ajuste Manual ou Timer Temporário: Sua ação manual sempre tem prioridade imediata.",
        "1. Manual Adjustment or Temporary Timer: Your manual action always takes immediate priority.",
      )
    val priorityStep2: String
      get() = tr(
        "2. Regra com Alta Prioridade: Vence conflitos entre regras simultâneas.",
        "2. High Priority Rule: Wins conflicts between simultaneous rules.",
      )
    val priorityStep3: String
      get() = tr(
        "3. Mais Restritiva Vence: Silencioso > Vibrar > Som Ativo.",
        "3. Most Restrictive Wins: Silent > Vibrate > Active Sound.",
      )
    val priorityStep4: String
      get() = tr(
        "4. Retorno Seguro: Ao fim da rotina, restaura seu Perfil Padrão ou Snapshot.",
        "4. Safe Return: When a routine ends, restores your Default Profile or Snapshot.",
      )
    val privacyCommitmentTitle: String
      get() = tr("Compromisso de Privacidade (100% On-Device)", "Privacy Commitment (100% On-Device)")
    val privacyCommitmentBody: String
      get() = tr(
        "Zero servidores, zero rastreadores e zero anúncios. Todos os seus perfis, horários, coordenadas de cercas geográficas e logs de transição permanecem armazenados exclusivamente na memória local do seu dispositivo.",
        "Zero servers, zero trackers and zero ads. All your profiles, schedules, geofence coordinates and transition logs remain stored exclusively in your device's local storage.",
      )

    fun appearanceSummary(themeLabel: String, languageLabel: String, dynamicColor: Boolean): String =
      tr(
        "Tema: $themeLabel • $languageLabel${if (dynamicColor) " • Material You" else ""}",
        "Theme: $themeLabel • $languageLabel${if (dynamicColor) " • Material You" else ""}",
      )

    fun automationSummary(policyTitle: String, defaultProfileName: String, paused: Boolean): String =
      if (paused) {
        tr("Pausada (Modo Férias) • Padrão: $defaultProfileName", "Paused (Vacation Mode) • Default: $defaultProfileName")
      } else {
        tr("Retorno: $policyTitle ($defaultProfileName)", "Return: $policyTitle ($defaultProfileName)")
      }

    fun audioMediaSummary(muteMedia: Boolean, preserveBt: Boolean, defaultTimerMinutes: Int): String =
      tr(
        "Zerar mídia: ${if (muteMedia) "Sim" else "Não"} • BT: ${if (preserveBt) "Protegido" else "Normal"} • Timer ${defaultTimerMinutes}m",
        "Mute media: ${if (muteMedia) "On" else "Off"} • BT: ${if (preserveBt) "Protected" else "Normal"} • Timer ${defaultTimerMinutes}m",
      )

    fun gadgetsSummary(quickBarEnabled: Boolean, slot1Name: String, slot2Name: String): String =
      tr(
        "Barra: ${if (quickBarEnabled) "Ativa" else "Inativa"} • Favoritos: $slot1Name, $slot2Name",
        "Bar: ${if (quickBarEnabled) "Active" else "Off"} • Favorites: $slot1Name, $slot2Name",
      )

    fun permissionsSummary(grantedCount: Int, totalCount: Int): String =
      tr(
        "$grantedCount de $totalCount permissões essenciais concedidas",
        "$grantedCount of $totalCount essential permissions granted",
      )

    fun historySummary(count: Int): String =
      if (count == 0) {
        tr("Nenhuma transição registrada ainda", "No transitions recorded yet")
      } else {
        tr("$count transições recentes registradas", "$count recent transitions recorded")
      }

    fun backupSummary(profilesCount: Int, schedulesCount: Int, locationsCount: Int): String =
      tr(
        "$profilesCount perfis • $schedulesCount horários • $locationsCount locais salvos",
        "$profilesCount profiles • $schedulesCount schedules • $locationsCount saved locations",
      )

    fun slotLabel(slotNumber: Int, profileName: String?): String =
      tr(
        "Slot $slotNumber: ${profileName ?: "Nenhum"}",
        "Slot $slotNumber: ${profileName ?: "None"}",
      )

    fun addSlotButton(slotNumber: Int): String =
      tr("+ Adicionar Slot $slotNumber", "+ Add Slot $slotNumber")

    fun quickProfileFallbackName(slotNumber: Int): String =
      tr("Perfil Rápido $slotNumber", "Quick Profile $slotNumber")

    val reopenOnboardingButton: String
      get() = tr("Abrir Assistente de Setup Inicial", "Open Initial Setup Wizard")

    fun accessControlSummary(
      roleTitle: String,
      grantedCount: Int,
      totalCount: Int,
      hasPin: Boolean,
    ): String =
      tr(
        "$roleTitle • $grantedCount/$totalCount ações liberadas • PIN: ${if (hasPin) "Ativo" else "Desativado"}",
        "$roleTitle • $grantedCount/$totalCount actions allowed • PIN: ${if (hasPin) "Active" else "Off"}",
      )

    val systemZenRulesTitle: String
      get() = tr(
        "Modos do Sistema Android 15+ (AutomaticZenRule)",
        "Android 15+ System Modes (AutomaticZenRule)",
      )
    val systemZenRulesSubtitle: String
      get() = tr(
        "Registra seus perfis de som no menu nativo 'Modos' do Android com ativação bidirecional.",
        "Registers your sound profiles in Android's native 'Modes' menu with bidirectional activation.",
      )
    val syncZenRulesButton: String
      get() = tr("Sincronizar Modos com o Sistema", "Sync Modes with System")
    fun zenRulesSyncedMessage(count: Int): String =
      tr(
        "$count modo(s) sincronizado(s) com o sistema Android",
        "$count mode(s) synced with Android system",
      )
  }

  object AccessControl {
    val sectionRoleTitle: String
      get() = tr("Papel de Acesso Ativo (Role)", "Active Access Role")
    val sectionRoleSubtitle: String
      get() = tr(
        "Escolha um nível predefinido de acesso ou personalize cada permissão individualmente abaixo.",
        "Choose a preset access level or customize each individual permission below.",
      )
    val sectionPinTitle: String
      get() = tr("Proteção por PIN (Opcional)", "PIN Protection (Optional)")
    val sectionPinSubtitle: String
      get() = tr(
        "Exige um PIN de 4 dígitos para voltar ao modo Administrador ou alterar permissões quando o app estiver em modo restrito.",
        "Requires a 4-digit PIN to return to Administrator mode or modify permissions while restricted.",
      )
    val pinInputLabel: String
      get() = tr("PIN numérico (4 dígitos)", "Numeric PIN (4 digits)")
    val savePinButton: String
      get() = tr("Salvar PIN", "Save PIN")
    val removePinButton: String
      get() = tr("Remover PIN", "Remove PIN")
    val pinActiveBadge: String
      get() = tr("PIN Ativo", "PIN Active")
    val pinInactiveBadge: String
      get() = tr("Sem PIN", "No PIN")
    val pinSavedMessage: String
      get() = tr("PIN de controle de acesso atualizado com sucesso", "Access control PIN updated")
    val pinRemovedMessage: String
      get() = tr("Proteção por PIN removida", "PIN protection removed")
    val pinInvalidLengthMessage: String
      get() = tr("Informe um PIN numérico de exatamente 4 dígitos", "Enter a 4-digit numeric PIN")
    val sectionMatrixTitle: String
      get() = tr("Matriz Granular de Permissões (RBAC)", "Granular Permissions Matrix (RBAC)")
    val sectionMatrixSubtitle: String
      get() = tr(
        "Ative ou desative cada ação permitida no aplicativo:",
        "Enable or disable each allowed action in the app:",
      )
    val pinChallengeDialogTitle: String
      get() = tr("Autorização Necessária (PIN)", "Authorization Required (PIN)")
    val pinChallengeDialogSubtitle: String
      get() = tr(
        "Digite o PIN de 4 dígitos para desbloquear o Controle de Acesso ou restaurar o papel de Administrador.",
        "Enter the 4-digit PIN to unlock Access Control or restore Administrator role.",
      )
    val pinChallengeInputLabel: String
      get() = tr("Digite o PIN de 4 dígitos", "Enter 4-digit PIN")
    val pinChallengeUnlockButton: String
      get() = tr("Desbloquear", "Unlock")
    val pinChallengeWrongError: String
      get() = tr("PIN incorreto. Tente novamente.", "Incorrect PIN. Please try again.")
    val quickUnlockAdminButton: String
      get() = tr("Desbloquear Administrador", "Unlock Administrator")
    val unlockedAsAdminMessage: String
      get() = tr("Acesso Administrador restaurado", "Administrator access restored")

    fun roleTitle(role: AppAccessRole): String =
      when (role) {
        AppAccessRole.ADMIN -> tr("Administrador (Acesso Total)", "Administrator (Full Access)")
        AppAccessRole.OPERATOR -> tr("Operador (Uso Rápido)", "Operator (Quick Use)")
        AppAccessRole.VIEWER -> tr("Somente Leitura (Bloqueado)", "View Only (Locked)")
        AppAccessRole.CUSTOM -> tr("Personalizado (Matriz Granular)", "Custom (Granular Matrix)")
      }

    fun roleSubtitle(role: AppAccessRole): String =
      when (role) {
        AppAccessRole.ADMIN ->
          tr(
            "Libera todas as funções de som, perfis, horários, locais, configurações e backup",
            "Unlocks all sound, profile, schedule, location, settings and backup actions",
          )
        AppAccessRole.OPERATOR ->
          tr(
            "Permite apenas trocar modo/volumes e usar o Timer na aba Agora; bloqueia edições e exclusões",
            "Allows switching modes/volumes and Timer in Now tab; blocks editing and deletion",
          )
        AppAccessRole.VIEWER ->
          tr(
            "Bloqueia qualquer alteração manual de som, perfis, rotinas e configurações",
            "Blocks all manual changes to sound, profiles, routines and settings",
          )
        AppAccessRole.CUSTOM ->
          tr(
            "Permissões selecionadas manualmente na matriz de controle de acesso",
            "Permissions manually selected in the access control matrix",
          )
      }

    fun roleShortBadge(role: AppAccessRole): String =
      when (role) {
        AppAccessRole.ADMIN -> tr("Admin", "Admin")
        AppAccessRole.OPERATOR -> tr("Operador", "Operator")
        AppAccessRole.VIEWER -> tr("Leitura", "Read-Only")
        AppAccessRole.CUSTOM -> tr("Restrito", "Restricted")
      }

    fun permissionTitle(permission: AppPermission): String =
      when (permission) {
        AppPermission.CHANGE_SOUND_AND_TIMER ->
          tr("Alternar Modos, Volumes e Timer", "Switch Modes, Volumes & Timer")
        AppPermission.MANAGE_PROFILES ->
          tr("Criar, Editar e Excluir Perfis", "Create, Edit & Delete Profiles")
        AppPermission.MANAGE_SCHEDULES ->
          tr("Criar, Editar e Excluir Horários", "Create, Edit & Delete Schedules")
        AppPermission.MANAGE_LOCATIONS ->
          tr("Criar, Editar e Excluir Locais", "Create, Edit & Delete Locations")
        AppPermission.PAUSE_AUTOMATIONS ->
          tr("Pausar Automações (Modo Férias)", "Pause Automations (Vacation Mode)")
        AppPermission.ACCESS_SETTINGS ->
          tr("Alterar Configurações do App", "Change App Settings")
        AppPermission.BACKUP_AND_FACTORY_RESET ->
          tr("Backup JSON & Restaurar Padrões", "JSON Backup & Factory Reset")
      }

    fun permissionSubtitle(permission: AppPermission): String =
      when (permission) {
        AppPermission.CHANGE_SOUND_AND_TIMER ->
          tr(
            "Trocar entre Som, Vibrar e Silêncio, mover sliders de volume, ativar perfil e usar o Timer na aba Agora",
            "Switch between Sound, Vibrate and Silent, adjust volume sliders, activate profiles and use Timer",
          )
        AppPermission.MANAGE_PROFILES ->
          tr(
            "Cadastrar novos perfis, duplicar, editar canais, definir padrão de retorno ou remover perfis",
            "Create new profiles, duplicate, edit streams, set default return or delete profiles",
          )
        AppPermission.MANAGE_SCHEDULES ->
          tr(
            "Criar, editar, duplicar, ativar/desativar ou excluir rotinas de horário",
            "Create, edit, duplicate, toggle or delete schedule routines",
          )
        AppPermission.MANAGE_LOCATIONS ->
          tr(
            "Criar, editar mapa/raio, ativar/desativar, simular presença ou excluir locais",
            "Create, edit map/radius, toggle, simulate presence or delete locations",
          )
        AppPermission.PAUSE_AUTOMATIONS ->
          tr(
            "Suspender ou retomar todas as rotinas automáticas pelo Modo Férias",
            "Suspend or resume all automatic routines via Vacation Mode",
          )
        AppPermission.ACCESS_SETTINGS ->
          tr(
            "Modificar tema, idioma, política de retorno, proteção de mídia, gadgets e limpar histórico",
            "Modify theme, language, restore policy, media protection, gadgets and clear history",
          )
        AppPermission.BACKUP_AND_FACTORY_RESET ->
          tr(
            "Exportar ou importar configurações via JSON e executar restauração de fábrica",
            "Export or import settings via JSON and execute factory reset",
          )
      }

    fun permissionDeniedMessage(permission: AppPermission): String =
      tr(
        "Acesso bloqueado pelo Controle de Acesso: '${permission.title}' não está habilitado no papel atual.",
        "Blocked by Access Control: '${permission.title}' is not enabled for the current role.",
      )
  }

  object Onboarding {
    val skipButton: String get() = tr("Pular", "Skip")
    val nextButton: String get() = tr("Continuar", "Continue")
    val finishButton: String get() = tr("Concluir e Começar", "Finish & Get Started")

    fun stepIndicator(current: Int, total: Int): String =
      tr("Passo $current de $total", "Step $current of $total")

    // Passo 1: Boas-vindas, Idioma & Tema
    val step1Badge: String
      get() = tr("Boas-vindas • 100% On-Device", "Welcome • 100% On-Device")
    val step1Title: String
      get() = tr("Controle inteligente de som sem perder chamadas", "Smart sound control without missing calls")
    val step1Subtitle: String
      get() = tr(
        "Configure seu idioma, aparência e permissões em menos de 1 minuto para automatizar sua rotina com segurança.",
        "Set up your language, appearance and permissions in under 1 minute to safely automate your routine.",
      )
    val feature1Title: String
      get() = tr("Proteção de Mídia & Timer Rápido", "Media Protection & Quick Timer")
    val feature1Body: String
      get() = tr(
        "Zera a mídia automaticamente ao silenciar e restaura seu som ao fim de reuniões ou aulas.",
        "Automatically mutes media when silencing and restores your sound after meetings or classes.",
      )
    val feature2Title: String
      get() = tr("Rotinas de Horário e Locais (GPS + Wi-Fi)", "Schedule & Location Routines (GPS + Wi-Fi)")
    val feature2Body: String
      get() = tr(
        "Ativa perfis ao chegar no trabalho ou dormir e respeita alterações manuais nos botões físicos.",
        "Activates profiles when arriving at work or sleeping and respects manual volume button changes.",
      )
    val feature3Title: String
      get() = tr("Privacidade Absoluta (Zero Nuvem)", "Absolute Privacy (Zero Cloud)")
    val feature3Body: String
      get() = tr(
        "Seus horários, locais e histórico nunca saem do aparelho.",
        "Your schedules, locations and history never leave your device.",
      )

    // Passo 2: Setup Rápido de Permissões
    val step2Badge: String
      get() = tr("Permissões do Sistema", "System Permissions")
    val step2Title: String
      get() = tr("Libere os controles essenciais do Android", "Enable essential Android controls")
    val step2Subtitle: String
      get() = tr(
        "O Android exige autorização explícita para alternar o modo Não Perturbe e disparar rotinas no horário exato.",
        "Android requires explicit authorization to toggle Do Not Disturb and trigger routines on time.",
      )
    val permissionGrantedBadge: String get() = tr("Ativo ✓", "Active ✓")
    val permissionRequiredTag: String get() = tr("Essencial", "Essential")
    val permissionRecommendedTag: String get() = tr("Recomendado", "Recommended")
    val permissionOptionalTag: String get() = tr("Opcional", "Optional")

    val dndCardTitle: String
      get() = tr("Acesso ao Não Perturbe (DND)", "Do Not Disturb Access (DND)")
    val dndCardBody: String
      get() = tr(
        "Indispensável para ativar o Modo Silencioso e controlar o volume de toque e notificações.",
        "Required to activate Silent Mode and control ring and notification volumes.",
      )
    val notificationsCardTitle: String
      get() = tr("Notificações Inteligentes & Gadget", "Smart Notifications & Gadget")
    val notificationsCardBody: String
      get() = tr(
        "Permite sugerir timer ao silenciar pelos botões físicos e exibir a barra rápida no topo.",
        "Allows suggesting a timer when silencing via hardware buttons and showing the top quick bar.",
      )
    val exactAlarmCardTitle: String
      get() = tr("Alarmes Exatos para Horários", "Exact Alarms for Schedules")
    val exactAlarmCardBody: String
      get() = tr(
        "Garante que suas rotinas de Trabalho e Sono comecem e terminem no minuto exato.",
        "Ensures your Work and Sleep routines start and end at the exact minute.",
      )
    val locationCardTitle: String
      get() = tr("Localização para Cercas (GPS)", "Location for Geofences (GPS)")
    val locationCardBody: String
      get() = tr(
        "Usada apenas se você quiser mudar o modo de som ao chegar ou sair de locais salvos.",
        "Used only if you want to switch sound modes when arriving at or leaving saved places.",
      )

    fun permissionsProgressLabel(grantedCount: Int, totalCount: Int): String =
      tr(
        "$grantedCount de $totalCount permissões ativas no aparelho",
        "$grantedCount of $totalCount permissions active on device",
      )

    // Passo 3: Perfil Padrão & Preferências Iniciais
    val step3Badge: String
      get() = tr("Toque Final", "Final Touch")
    val step3Title: String
      get() = tr("Como o app deve se comportar no dia a dia?", "How should the app behave day to day?")
    val step3Subtitle: String
      get() = tr(
        "Escolha o perfil para o qual seu celular deve voltar quando timers ou compromissos terminarem.",
        "Choose the profile your phone should return to when timers or appointments finish.",
      )
    val defaultProfileSectionTitle: String
      get() = tr("Perfil Padrão de Retorno", "Default Return Profile")
    val smartBehaviorsSectionTitle: String
      get() = tr("Automações Protetoras Ativas", "Active Protective Automations")
  }
}
