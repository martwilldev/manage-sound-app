package com.managesoundmode.app.system

import android.app.AutomaticZenRule
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Build
import android.service.notification.Condition
import android.service.notification.ZenPolicy
import android.util.Log
import com.managesoundmode.app.MainActivity
import com.managesoundmode.app.R
import com.managesoundmode.app.domain.model.SoundMode
import com.managesoundmode.app.domain.model.SoundProfile
import com.managesoundmode.app.receiver.SystemAutomationReceiver

/**
 * [AUTO-04] Integração bidirecional com a API nativa `AutomaticZenRule` (Modos do Sistema no Android 15+).
 *
 * Publica os perfis do aplicativo nas Configurações do Sistema Android ("Modos" / "Não Perturbe"),
 * sincroniza o estado ativo (`Condition.STATE_TRUE` / `Condition.STATE_FALSE`) e responde a alterações
 * feitas pelo usuário diretamente no painel do sistema.
 */
object SystemZenRuleManager {
  private const val TAG = "SystemZenRuleManager"
  private const val CONDITION_SCHEME = "condition"
  private const val CONDITION_AUTHORITY = "com.managesoundmode.app"
  private const val CONDITION_PATH_PREFIX = "/profile/"
  private const val CONDITION_FULL_PREFIX = "$CONDITION_SCHEME://$CONDITION_AUTHORITY$CONDITION_PATH_PREFIX"

  fun buildConditionUriString(profileId: String): String =
    "$CONDITION_FULL_PREFIX$profileId"

  fun buildConditionUri(profileId: String): Uri =
    Uri.parse(buildConditionUriString(profileId))

  fun extractProfileIdFromConditionUri(rawUri: String?): String? {
    if (rawUri.isNullOrBlank()) return null
    return if (rawUri.startsWith(CONDITION_FULL_PREFIX)) {
      rawUri.removePrefix(CONDITION_FULL_PREFIX).takeIf { it.isNotBlank() }
    } else {
      null
    }
  }

  fun extractProfileIdFromConditionUri(conditionId: Uri?): String? =
    extractProfileIdFromConditionUri(conditionId?.toString())

  /**
   * Retorna quantas regras `AutomaticZenRule` do aplicativo estão registradas no sistema Android.
   */
  fun countRegisteredRules(context: Context): Int =
    runCatching {
      val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        ?: return@runCatching 0
      if (!nm.isNotificationPolicyAccessGranted) return@runCatching 0
      nm.automaticZenRules.values.count { rule ->
        extractProfileIdFromConditionUri(rule?.conditionId) != null
      }
    }.getOrDefault(0)

  /**
   * Sincroniza os perfis de som do aplicativo com os Modos do Sistema Android (`AutomaticZenRule`).
   *
   * @return Quantidade de regras sincronizadas no `NotificationManager`.
   */
  fun syncProfilesToSystemZenRules(
    context: Context,
    profiles: List<SoundProfile>,
    activeProfileId: String?,
    enabled: Boolean = true,
  ): Int {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
      ?: return 0
    if (!nm.isNotificationPolicyAccessGranted) return 0

    return runCatching {
      val existingRules: Map<String, AutomaticZenRule> = nm.automaticZenRules ?: emptyMap()
      val existingByProfileId = mutableMapOf<String, Pair<String, AutomaticZenRule>>()
      existingRules.forEach { (ruleId, rule) ->
        val pid = extractProfileIdFromConditionUri(rule.conditionId)
        if (pid != null) {
          existingByProfileId[pid] = ruleId to rule
        }
      }

      if (!enabled) {
        existingByProfileId.values.forEach { (ruleId, _) ->
          runCatching { nm.removeAutomaticZenRule(ruleId) }
        }
        return@runCatching 0
      }

      val validProfileIds = profiles.map { it.id }.toSet()
      // Remove regras de perfis excluídos
      existingByProfileId.forEach { (pid, pair) ->
        if (pid !in validProfileIds) {
          runCatching { nm.removeAutomaticZenRule(pair.first) }
        }
      }

      var syncedCount = 0
      for (profile in profiles) {
        val conditionUri = buildConditionUri(profile.id)
        val zenRule = buildZenRuleForProfile(context, profile, conditionUri)
        val existingEntry = existingByProfileId[profile.id]
        val systemRuleId = if (existingEntry != null) {
          runCatching { nm.updateAutomaticZenRule(existingEntry.first, zenRule) }
          existingEntry.first
        } else {
          runCatching { nm.addAutomaticZenRule(zenRule) }.getOrNull()
        }

        if (!systemRuleId.isNullOrBlank()) {
          syncedCount++
          if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val isActive = profile.id == activeProfileId
            val condition = Condition(
              conditionUri,
              profile.summaryLabel(),
              if (isActive) Condition.STATE_TRUE else Condition.STATE_FALSE,
            )
            runCatching { nm.setAutomaticZenRuleState(systemRuleId, condition) }
          }
        }
      }
      syncedCount
    }.onFailure { error ->
      Log.w(TAG, "Falha ao sincronizar AutomaticZenRules com o sistema", error)
    }.getOrDefault(0)
  }

  /**
   * Resolve qual `profileId` corresponde a um `systemRuleId` enviado pelo broadcast
   * `ACTION_AUTOMATIC_ZEN_RULE_STATUS_CHANGED`.
   */
  fun resolveProfileIdForSystemRuleId(context: Context, systemRuleId: String): String? =
    runCatching {
      val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        ?: return@runCatching null
      val rule = nm.getAutomaticZenRule(systemRuleId) ?: return@runCatching null
      extractProfileIdFromConditionUri(rule.conditionId)
    }.getOrNull()

  private fun buildZenRuleForProfile(
    context: Context,
    profile: SoundProfile,
    conditionUri: Uri,
  ): AutomaticZenRule {
    val ruleName = "Modos de Som • ${profile.name}"
    val configActivity = ComponentName(context, MainActivity::class.java)
    val ownerReceiver = ComponentName(context, SystemAutomationReceiver::class.java)
    val interruptionFilter = when (profile.mode) {
      SoundMode.SILENT -> NotificationManager.INTERRUPTION_FILTER_PRIORITY
      SoundMode.VIBRATE, SoundMode.NORMAL -> NotificationManager.INTERRUPTION_FILTER_ALL
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
      val zenType = when {
        profile.id.contains("sleep", ignoreCase = true) ||
          profile.name.contains("sono", ignoreCase = true) -> AutomaticZenRule.TYPE_BEDTIME
        profile.id.contains("work", ignoreCase = true) ||
          profile.name.contains("trabalho", ignoreCase = true) -> AutomaticZenRule.TYPE_SCHEDULE_TIME
        else -> AutomaticZenRule.TYPE_OTHER
      }
      val zenPolicy = ZenPolicy.Builder()
        .allowAlarms(true)
        .allowMedia(profile.streams.mediaPercent > 0)
        .allowCalls(
          if (profile.streams.ringPercent > 0) {
            ZenPolicy.PEOPLE_TYPE_ANYONE
          } else {
            ZenPolicy.PEOPLE_TYPE_STARRED
          },
        )
        .build()

      return AutomaticZenRule.Builder(ruleName, conditionUri)
        .setType(zenType)
        .setIconResId(R.drawable.ic_qs_sound_mode)
        .setTriggerDescription(profile.description.ifBlank { profile.summaryLabel() })
        .setConfigurationActivity(configActivity)
        .setOwner(ownerReceiver)
        .setInterruptionFilter(interruptionFilter)
        .setZenPolicy(zenPolicy)
        .setEnabled(true)
        .build()
    }

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val zenPolicy = ZenPolicy.Builder()
        .allowAlarms(true)
        .allowMedia(profile.streams.mediaPercent > 0)
        .build()
      AutomaticZenRule(
        ruleName,
        ownerReceiver,
        configActivity,
        conditionUri,
        zenPolicy,
        interruptionFilter,
        true,
      )
    } else {
      @Suppress("DEPRECATION")
      AutomaticZenRule(
        ruleName,
        ownerReceiver,
        conditionUri,
        interruptionFilter,
        true,
      )
    }
  }
}
