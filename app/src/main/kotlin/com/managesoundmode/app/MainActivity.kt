package com.managesoundmode.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.managesoundmode.app.domain.model.SoundMode
import com.managesoundmode.app.domain.model.TransitionTriggerSource
import com.managesoundmode.app.system.AppShortcutsManager
import com.managesoundmode.app.ui.theme.ManageSoundModeTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    installSplashScreen()
    super.onCreate(savedInstanceState)
    handleShortcutIntent(intent)

    enableEdgeToEdge()
    setContent {
      ManageSoundModeTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background,
        ) {
          MainNavigation()
        }
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleShortcutIntent(intent)
  }

  private fun handleShortcutIntent(intent: Intent?) {
    val actionKey = intent?.getStringExtra(AppShortcutsManager.EXTRA_SHORTCUT_ACTION) ?: return
    intent.removeExtra(AppShortcutsManager.EXTRA_SHORTCUT_ACTION)
    val app = applicationContext as? ManageSoundApplication ?: return
    val repo = app.container.soundModeRepository

    lifecycleScope.launch {
      when (actionKey) {
        AppShortcutsManager.ACTION_SILENCE_1H -> {
          repo.startTimedSession(targetMode = SoundMode.VIBRATE, durationMinutes = 60)
        }
        else -> {
          val profileId = AppShortcutsManager.resolveProfileIdForShortcut(actionKey) ?: return@launch
          val profile = repo.profilesStream.first().firstOrNull { it.id == profileId } ?: return@launch
          repo.activateProfile(profile, source = TransitionTriggerSource.WIDGET)
        }
      }
    }
  }
}
