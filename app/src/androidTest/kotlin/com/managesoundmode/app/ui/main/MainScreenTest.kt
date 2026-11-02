package com.managesoundmode.app.ui.main

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import com.managesoundmode.app.domain.model.SoundMode
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.domain.model.VolumeStreams
import com.managesoundmode.app.ui.theme.ManageSoundModeTheme
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
}
