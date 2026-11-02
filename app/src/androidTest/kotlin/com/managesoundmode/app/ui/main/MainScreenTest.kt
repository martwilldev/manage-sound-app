package com.managesoundmode.app.ui.main

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.managesoundmode.app.domain.model.SoundMode
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.domain.model.VolumeStreams
import com.managesoundmode.app.ui.theme.ManageSoundModeTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MainScreenTest {
  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun dashboardContent_displaysCurrentModeAndProfiles() {
    composeTestRule.setContent {
      ManageSoundModeTheme {
        MainDashboardContent(
          state = MainScreenUiState.Ready(
            currentMode = SoundMode.NORMAL,
            streams = VolumeStreams(),
            hasDndPermission = false,
            profiles = SoundProfile.defaultPresets,
          ),
          onSelectMode = {},
          onStreamVolumeChange = { _, _ -> },
          onActivateProfile = {},
          onStartTimer = { _, _ -> },
          onCancelTimer = {},
          onSaveProfile = { _, _ -> },
          onDismissMessage = {},
          onRequestDndPermission = {},
        )
      }
    }

    composeTestRule.onNodeWithTag("hero_mode_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("dnd_permission_banner").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mode_button_NORMAL").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mode_button_VIBRATE").assertIsDisplayed()
    composeTestRule.onNodeWithTag("mode_button_SILENT").assertIsDisplayed()
    composeTestRule.onNodeWithTag("tab_profiles").assertIsDisplayed()
    composeTestRule.onNodeWithText("Volumes").assertIsDisplayed()
  }

  @Test
  fun profilesTab_displaysFabAndSelectingProfileReturnsToHomeTab() {
    var activatedProfileId: String? = null
    composeTestRule.setContent {
      ManageSoundModeTheme {
        MainDashboardContent(
          state = MainScreenUiState.Ready(
            currentMode = SoundMode.NORMAL,
            streams = VolumeStreams(),
            hasDndPermission = true,
            profiles = SoundProfile.defaultPresets,
          ),
          onSelectMode = {},
          onStreamVolumeChange = { _, _ -> },
          onActivateProfile = { profile -> activatedProfileId = profile.id },
          onStartTimer = { _, _ -> },
          onCancelTimer = {},
          onSaveProfile = { _, _ -> },
          onDismissMessage = {},
          onRequestDndPermission = {},
        )
      }
    }

    // Open Perfis tab
    composeTestRule.onNodeWithTag("tab_profiles").performClick()
    composeTestRule.onNodeWithTag("save_profile_button").assertIsDisplayed()
    composeTestRule.onNodeWithTag("profile_card_preset_work_high").assertIsDisplayed()

    // Click a profile card -> should activate profile and automatically return to Agora (Hero Card visible)
    composeTestRule.onNodeWithTag("profile_card_preset_work_high").performClick()
    assertEquals("preset_work_high", activatedProfileId)
    composeTestRule.onNodeWithTag("hero_mode_card").assertIsDisplayed()
  }
}
