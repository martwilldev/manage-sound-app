package com.managesoundmode.app.domain.model

import com.managesoundmode.app.ui.common.AppLabels
import java.security.MessageDigest
import kotlinx.serialization.Serializable

/**
 * Papéis internos de acesso (RBAC) do aplicativo.
 */
@Serializable
enum class AppAccessRole {
  ADMIN,
  OPERATOR,
  VIEWER,
  CUSTOM;

  val title: String
    get() = AppLabels.AccessControl.roleTitle(this)

  val subtitle: String
    get() = AppLabels.AccessControl.roleSubtitle(this)

  val shortBadge: String
    get() = AppLabels.AccessControl.roleShortBadge(this)

  fun defaultPermissions(): Set<AppPermission> =
    when (this) {
      ADMIN -> AppPermission.entries.toSet()
      OPERATOR -> setOf(AppPermission.CHANGE_SOUND_AND_TIMER)
      VIEWER -> emptySet()
      CUSTOM -> AppPermission.entries.toSet()
    }
}

/**
 * Permissões granulares por ação/domínio dentro do aplicativo.
 */
@Serializable
enum class AppPermission {
  CHANGE_SOUND_AND_TIMER,
  MANAGE_PROFILES,
  MANAGE_SCHEDULES,
  MANAGE_LOCATIONS,
  PAUSE_AUTOMATIONS,
  ACCESS_SETTINGS,
  BACKUP_AND_FACTORY_RESET;

  val title: String
    get() = AppLabels.AccessControl.permissionTitle(this)

  val subtitle: String
    get() = AppLabels.AccessControl.permissionSubtitle(this)
}

/**
 * Configuração persistida de Controle de Acesso (RBAC + PIN opcional).
 */
@Serializable
data class AccessControlConfig(
  val activeRole: AppAccessRole = AppAccessRole.ADMIN,
  val grantedPermissions: Set<AppPermission> = AppPermission.entries.toSet(),
  val pinCode: String? = null,
) {
  val isPinRequired: Boolean
    get() = !pinCode.isNullOrBlank()

  val isRestricted: Boolean
    get() = activeRole != AppAccessRole.ADMIN || grantedPermissions.size < AppPermission.entries.size

  fun hasPermission(permission: AppPermission): Boolean =
    if (activeRole == AppAccessRole.ADMIN) {
      true
    } else {
      permission in grantedPermissions
    }

  fun verifyPin(inputPin: String): Boolean {
    val stored = pinCode?.trim()
    if (stored.isNullOrBlank()) return true
    val candidate = inputPin.trim()
    return if (stored.startsWith(SHA256_PREFIX)) {
      val candidateHash = hashPin(candidate)
      MessageDigest.isEqual(
        stored.toByteArray(Charsets.UTF_8),
        candidateHash.toByteArray(Charsets.UTF_8),
      )
    } else {
      MessageDigest.isEqual(
        stored.toByteArray(Charsets.UTF_8),
        candidate.toByteArray(Charsets.UTF_8),
      )
    }
  }

  companion object {
    private const val SHA256_PREFIX = "sha256:"

    fun hashPin(rawPin: String): String {
      val digest = MessageDigest.getInstance("SHA-256")
        .digest(rawPin.trim().toByteArray(Charsets.UTF_8))
      return SHA256_PREFIX + digest.joinToString("") { "%02x".format(it) }
    }
  }

  fun withRole(newRole: AppAccessRole): AccessControlConfig =
    when (newRole) {
      AppAccessRole.ADMIN -> copy(
        activeRole = AppAccessRole.ADMIN,
        grantedPermissions = AppPermission.entries.toSet(),
      )
      AppAccessRole.OPERATOR -> copy(
        activeRole = AppAccessRole.OPERATOR,
        grantedPermissions = AppAccessRole.OPERATOR.defaultPermissions(),
      )
      AppAccessRole.VIEWER -> copy(
        activeRole = AppAccessRole.VIEWER,
        grantedPermissions = emptySet(),
      )
      AppAccessRole.CUSTOM -> copy(
        activeRole = AppAccessRole.CUSTOM,
      )
    }

  fun withPermissionEnabled(permission: AppPermission, enabled: Boolean): AccessControlConfig {
    val updated = if (enabled) {
      grantedPermissions + permission
    } else {
      grantedPermissions - permission
    }
    val resolvedRole = when {
      updated.size == AppPermission.entries.size -> AppAccessRole.ADMIN
      updated.isEmpty() -> AppAccessRole.VIEWER
      updated == AppAccessRole.OPERATOR.defaultPermissions() -> AppAccessRole.OPERATOR
      else -> AppAccessRole.CUSTOM
    }
    return copy(
      activeRole = resolvedRole,
      grantedPermissions = updated,
    )
  }
}
