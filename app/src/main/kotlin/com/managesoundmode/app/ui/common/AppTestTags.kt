package com.managesoundmode.app.ui.common

import com.managesoundmode.app.domain.model.AppAccessRole
import com.managesoundmode.app.domain.model.AppPermission
import com.managesoundmode.app.domain.model.RestorePolicy
import com.managesoundmode.app.domain.model.SoundMode
import com.managesoundmode.app.domain.model.ThemeMode
import com.managesoundmode.app.domain.model.VolumeStreamType

/**
 * Catálogo centralizado de identificadores de teste (Test IDs / testTag) de toda a interface.
 * Nunca utilize strings literais em Modifier.testTag(...) fora deste arquivo.
 */
object AppTestTags {

  object Tabs {
    const val NOW = "tab_now"
    const val PROFILES = "tab_profiles"
    const val SCHEDULES = "tab_schedules"
    const val LOCATIONS = "tab_locations"
    const val SETTINGS = "tab_settings"
  }

  object TopBar {
    const val QUICK_THEME_TOGGLE = "quick_theme_toggle"
    const val OVERFLOW_MENU_BUTTON = "topbar_overflow_menu_button"
    const val MENU_SETTINGS = "topbar_menu_settings"
    const val MENU_PAUSE_AUTOMATIONS = "topbar_menu_pause_automations"
    const val MENU_HISTORY = "topbar_menu_history"
    const val MENU_PERMISSIONS = "topbar_menu_permissions"
    const val MENU_RESTORE_DEFAULTS = "topbar_menu_restore_defaults"
    const val MENU_ABOUT = "topbar_menu_about"
    const val MENU_SETUP_WIZARD = "topbar_menu_setup_wizard"
    const val MENU_ACCESS_CONTROL = "topbar_menu_access_control"
    const val ROLE_BADGE_CHIP = "topbar_role_badge_chip"
  }

  object Fab {
    const val SAVE_PROFILE_BUTTON = "save_profile_button"
    const val ADD_SCHEDULE_FAB = "add_schedule_fab"
    const val ADD_LOCATION_FAB = "add_location_fab"
  }

  object Now {
    const val LIST = "now_tab_list"
    const val HERO_MODE_CARD = "hero_mode_card"
    const val ACTIVE_PROFILE_CHIP = "active_profile_chip"
    const val EXTEND_TIMER_BUTTON = "extend_timer_button"
    const val CANCEL_TIMER_BUTTON = "cancel_timer_button"
    const val MANUAL_OVERRIDE_BANNER = "manual_override_banner"
    const val REAPPLY_RULE_BUTTON = "reapply_rule_button"
    const val DND_PERMISSION_BANNER = "dnd_permission_banner"
    const val DND_ENABLE_BUTTON = "dnd_enable_button"

    fun modeButton(mode: SoundMode): String = "mode_button_${mode.name}"
    fun timerChip(minutes: Int): String = "timer_chip_$minutes"
    fun streamSlider(type: VolumeStreamType): String = "slider_${type.name}"
  }

  object Profiles {
    const val LIST = "profiles_tab_list"
    const val DELETE_TOP_BUTTON = "delete_profile_top_button"
    const val CONFIRM_SAVE_BUTTON = "confirm_save_profile_button"
    const val NAME_INPUT = "profile_name_input"

    fun card(profileId: String): String = "profile_card_$profileId"
    fun editButton(profileId: String): String = "edit_profile_$profileId"
    fun menuEditItem(profileId: String): String = "profile_menu_edit_$profileId"
    fun menuDuplicateItem(profileId: String): String = "profile_menu_duplicate_$profileId"
    fun menuSetDefaultItem(profileId: String): String = "profile_menu_set_default_$profileId"
    fun menuDeleteItem(profileId: String): String = "profile_menu_delete_$profileId"
  }

  object Schedules {
    const val LIST = "schedules_tab_list"
    const val EXACT_ALARM_BANNER = "schedules_exact_alarm_banner"
    const val AUTHORIZE_EXACT_ALARM_BUTTON = "schedules_authorize_exact_alarm_button"
    const val DELETE_TOP_BUTTON = "delete_schedule_top_button"
    const val CONFIRM_SAVE_BUTTON = "confirm_save_schedule_button"
    const val NAME_INPUT = "schedule_name_input"

    fun card(ruleId: String): String = "schedule_card_$ruleId"
    fun switch(ruleId: String): String = "schedule_switch_$ruleId"
    fun optionsButton(ruleId: String): String = "schedule_options_$ruleId"
    fun menuEditItem(ruleId: String): String = "schedule_menu_edit_$ruleId"
    fun menuDuplicateItem(ruleId: String): String = "schedule_menu_duplicate_$ruleId"
    fun menuToggleItem(ruleId: String): String = "schedule_menu_toggle_$ruleId"
    fun menuDeleteItem(ruleId: String): String = "schedule_menu_delete_$ruleId"
  }

  object Locations {
    const val LIST = "locations_tab_list"
    const val SYNC_GPS_CARD = "sync_gps_locations_card"
    const val VERIFY_GPS_BUTTON = "verify_gps_proximity_button"
    const val GEOFENCE_STATUS_CARD = "locations_geofence_status_card"
    const val SYNC_GEOFENCES_BUTTON = "locations_sync_geofences_button"
    const val DELETE_TOP_BUTTON = "delete_location_top_button"
    const val CONFIRM_SAVE_BUTTON = "confirm_save_location_button"
    const val NAME_INPUT = "location_name_input"
    const val RADIUS_SLIDER = "geofence_radius_slider"
    const val ACCEPT_DISCLOSURE_BUTTON = "accept_location_disclosure_button"

    fun card(ruleId: String): String = "location_card_$ruleId"
    fun switch(ruleId: String): String = "location_switch_$ruleId"
    fun togglePresenceButton(ruleId: String): String = "toggle_location_presence_$ruleId"
    fun optionsButton(ruleId: String): String = "location_options_$ruleId"
    fun menuEditItem(ruleId: String): String = "location_menu_edit_$ruleId"
    fun menuPresenceItem(ruleId: String): String = "location_menu_presence_$ruleId"
    fun menuToggleItem(ruleId: String): String = "location_menu_toggle_$ruleId"
    fun menuDeleteItem(ruleId: String): String = "location_menu_delete_$ruleId"
  }

  object GeofenceMap {
    const val CARD = "geofence_map_picker_card"
    const val USE_CURRENT_GPS_BUTTON = "use_current_gps_button"
    const val SEARCH_ADDRESS_BUTTON = "search_address_button"
    const val SEARCH_ADDRESS_INPUT = "search_address_input"
    const val OSM_WEBVIEW = "osm_interactive_webview"

    fun searchResult(index: Int): String = "address_search_result_$index"
  }

  object Settings {
    const val LIST = "settings_tab_list"
    const val CLOSE_SETTINGS_BUTTON = "settings_close_button"
    const val CLOSE_SUBSCREEN_BUTTON = "settings_subscreen_close_button"
    const val MASTER_AUTOMATION_CARD = "settings_master_automation_card"
    const val MASTER_AUTOMATION_SWITCH = "settings_master_automation_switch"
    const val CATEGORY_APPEARANCE = "settings_category_appearance"
    const val CATEGORY_AUTOMATION = "settings_category_automation"
    const val CATEGORY_AUDIO_MEDIA = "settings_category_audio_media"
    const val CATEGORY_GADGETS_WIDGETS = "settings_category_gadgets_widgets"
    const val CATEGORY_PERMISSIONS = "settings_category_permissions"
    const val CATEGORY_ACCESS_CONTROL = "settings_category_access_control"
    const val CATEGORY_HISTORY = "settings_category_history"
    const val CATEGORY_BACKUP_RESET = "settings_category_backup_reset"
    const val CATEGORY_ABOUT = "settings_category_about"
    const val DYNAMIC_COLOR_SWITCH = "settings_dynamic_color_switch"
    const val HAPTIC_FEEDBACK_SWITCH = "settings_haptic_feedback_switch"
    const val NOTIFY_AUTO_TRANSITION_SWITCH = "settings_notify_auto_transition_switch"
    const val SUGGEST_TIMER_SWITCH = "settings_suggest_timer_switch"
    const val TEST_SMART_NOTIFICATION_BUTTON = "settings_test_smart_notification_button"
    const val MUTE_MEDIA_SWITCH = "settings_mute_media_switch"
    const val PRESERVE_BT_SWITCH = "settings_preserve_bt_switch"
    const val CLEAR_HISTORY_BUTTON = "settings_clear_history_button"
    const val IMPORT_BACKUP_INPUT = "settings_import_backup_input"
    const val RESET_DEFAULTS_BUTTON = "settings_reset_defaults_button"
    const val CONFIRM_RESET_DEFAULTS_BUTTON = "settings_confirm_reset_defaults_button"
    const val EXPORT_BACKUP_BUTTON = "settings_export_backup_button"
    const val IMPORT_BACKUP_BUTTON = "settings_import_backup_button"
    const val PERSISTENT_QUICK_BAR_CARD = "persistent_quick_bar_card"
    const val PERSISTENT_QUICK_BAR_SWITCH = "persistent_quick_bar_switch"
    const val QUICK_SETTINGS_TILE_CARD = "quick_settings_tile_card"
    const val ADD_TILE_NORMAL = "add_tile_normal"
    const val ADD_TILE_VIBRATE = "add_tile_vibrate"
    const val ADD_TILE_SILENT = "add_tile_silent"
    const val ADD_QUICK_TILE_BUTTON = "add_quick_tile_button"
    const val QUICK_PROFILE_TILES_CARD = "quick_profile_tiles_card"
    const val QUICK_UTILITY_TILES_CARD = "quick_utility_tiles_card"
    const val ADD_TILE_TIMER = "add_tile_timer"
    const val ADD_TILE_MEDIA_MUTE = "add_tile_media_mute"
    const val PIN_HOME_WIDGET_BUTTON = "pin_home_widget_button"

    fun themeButton(mode: ThemeMode): String = "theme_button_${mode.name}"
    fun languageButton(language: AppLanguage): String = "language_button_${language.name}"
    fun restorePolicy(policy: RestorePolicy): String = "restore_policy_${policy.name}"
    fun defaultReturnProfileChip(profileId: String): String =
      "settings_default_return_profile_$profileId"
    fun addTileProfileSlot(slotNumber: Int): String = "add_tile_profile_slot_$slotNumber"
    fun slotProfileChip(slotNumber: Int, profileId: String): String =
      "slot_${slotNumber}_profile_$profileId"
    fun defaultTimerChip(minutes: Int): String = "settings_default_timer_chip_$minutes"
    fun historyFilterChip(filterName: String): String = "settings_history_filter_$filterName"
    const val REOPEN_ONBOARDING_BUTTON = "settings_reopen_onboarding_button"
    const val SYSTEM_ZEN_RULES_SWITCH = "settings_system_zen_rules_switch"
    const val SYNC_ZEN_RULES_BUTTON = "settings_sync_zen_rules_button"
  }

  object Onboarding {
    const val DIALOG = "onboarding_wizard_dialog"
    const val SKIP_BUTTON = "onboarding_skip_button"
    const val BACK_BUTTON = "onboarding_back_button"
    const val NEXT_BUTTON = "onboarding_next_button"
    const val FINISH_BUTTON = "onboarding_finish_button"
    const val GRANT_DND_BUTTON = "onboarding_grant_dnd_button"
    const val GRANT_NOTIFICATIONS_BUTTON = "onboarding_grant_notifications_button"
    const val GRANT_EXACT_ALARM_BUTTON = "onboarding_grant_exact_alarm_button"
    const val GRANT_LOCATION_BUTTON = "onboarding_grant_location_button"

    fun stepPill(stepIndex: Int): String = "onboarding_step_pill_$stepIndex"
    fun defaultProfileChip(profileId: String): String = "onboarding_default_profile_$profileId"
  }

  object AccessControl {
    const val SUBSCREEN = "access_control_subscreen"
    const val PIN_INPUT = "access_control_pin_input"
    const val SAVE_PIN_BUTTON = "access_control_save_pin_button"
    const val REMOVE_PIN_BUTTON = "access_control_remove_pin_button"
    const val PIN_CHALLENGE_DIALOG = "access_control_pin_challenge_dialog"
    const val PIN_CHALLENGE_INPUT = "access_control_pin_challenge_input"
    const val PIN_CHALLENGE_UNLOCK_BUTTON = "access_control_pin_challenge_unlock_button"
    const val PIN_CHALLENGE_QUICK_ADMIN_BUTTON = "access_control_pin_challenge_quick_admin_button"

    fun roleCard(role: AppAccessRole): String = "access_role_card_${role.name}"
    fun permissionSwitch(permission: AppPermission): String = "access_permission_switch_${permission.name}"
  }
}
