package com.managesoundmode.app.system

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.managesoundmode.app.MainActivity
import com.managesoundmode.app.R
import com.managesoundmode.app.domain.model.SoundProfile

object AppShortcutsManager {

  const val EXTRA_SHORTCUT_ACTION = "shortcut_action"
  const val ACTION_SILENCE_1H = "shortcut_silence_1h"
  const val ACTION_PROFILE_WORK = "shortcut_profile_work"
  const val ACTION_PROFILE_SLEEP = "shortcut_profile_sleep"
  const val ACTION_RESTORE_NORMAL = "shortcut_restore_normal"

  fun publishDynamicShortcuts(context: Context) {
    runCatching {
      val icon = IconCompat.createWithResource(context, R.drawable.ic_qs_sound_mode)

      val shortcuts = listOf(
        ShortcutInfoCompat.Builder(context, ACTION_SILENCE_1H)
          .setShortLabel("Vibrar por 1h")
          .setLongLabel("Silenciar em Vibrar por 1 hora")
          .setIcon(icon)
          .setIntent(shortcutIntent(context, ACTION_SILENCE_1H))
          .build(),
        ShortcutInfoCompat.Builder(context, ACTION_PROFILE_WORK)
          .setShortLabel("Perfil Trabalho")
          .setLongLabel("Ativar perfil Trabalho")
          .setIcon(icon)
          .setIntent(shortcutIntent(context, ACTION_PROFILE_WORK))
          .build(),
        ShortcutInfoCompat.Builder(context, ACTION_PROFILE_SLEEP)
          .setShortLabel("Perfil Dormir")
          .setLongLabel("Ativar perfil Dormir")
          .setIcon(icon)
          .setIntent(shortcutIntent(context, ACTION_PROFILE_SLEEP))
          .build(),
        ShortcutInfoCompat.Builder(context, ACTION_RESTORE_NORMAL)
          .setShortLabel("Som Normal")
          .setLongLabel("Restaurar perfil Padrão (Dia)")
          .setIcon(icon)
          .setIntent(shortcutIntent(context, ACTION_RESTORE_NORMAL))
          .build(),
      )

      ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }
  }

  private fun shortcutIntent(context: Context, actionKey: String): Intent =
    Intent(context, MainActivity::class.java).apply {
      action = Intent.ACTION_VIEW
      putExtra(EXTRA_SHORTCUT_ACTION, actionKey)
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }

  fun resolveProfileIdForShortcut(actionKey: String): String? = when (actionKey) {
    ACTION_PROFILE_WORK -> "preset_work_high"
    ACTION_PROFILE_SLEEP -> "preset_sleep"
    ACTION_RESTORE_NORMAL -> SoundProfile.DEFAULT_DAY_PROFILE_ID
    else -> null
  }
}
