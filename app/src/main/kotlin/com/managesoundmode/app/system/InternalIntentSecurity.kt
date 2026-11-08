package com.managesoundmode.app.system

import android.content.Context
import android.content.Intent
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Proteção criptográfica por instalação contra Intent Spoofing em componentes exportados.
 *
 * Como o código-fonte do repositório é público, qualquer aplicativo de terceiros instalado no
 * mesmo aparelho conhece os nomes das actions customizadas (`com.managesoundmode.app.action.*`).
 * Este utilitário gera um token aleatório de 256 bits (`SecureRandom`) exclusivo por instalação
 * no armazenamento interno privado (`MODE_PRIVATE`, excluído de backup em nuvem) e assina
 * todas as `PendingIntent`s internas antes de registrá-las no sistema.
 */
object InternalIntentSecurity {
  const val PREFS_FILE_NAME = "internal_security_prefs"
  private const val KEY_INSTALL_TOKEN = "install_intent_auth_token"
  const val EXTRA_INTERNAL_AUTH_TOKEN = "com.managesoundmode.app.extra.AUTH_TOKEN"

  @Volatile
  private var cachedToken: String? = null

  fun getOrCreateToken(context: Context): String {
    cachedToken?.let { return it }
    synchronized(this) {
      cachedToken?.let { return it }
      val appContext = context.applicationContext ?: context
      val prefs = appContext.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
      var token = prefs.getString(KEY_INSTALL_TOKEN, null)
      if (token.isNullOrBlank() || token.length < 64) {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        token = bytes.joinToString("") { "%02x".format(it) }
        prefs.edit().putString(KEY_INSTALL_TOKEN, token).apply()
      }
      cachedToken = token
      return token
    }
  }

  fun signIntent(context: Context, intent: Intent): Intent {
    intent.setPackage(context.packageName)
    intent.putExtra(EXTRA_INTERNAL_AUTH_TOKEN, getOrCreateToken(context))
    return intent
  }

  fun verifyIntent(context: Context, intent: Intent?): Boolean {
    if (intent == null) return false
    val provided = intent.getStringExtra(EXTRA_INTERNAL_AUTH_TOKEN) ?: return false
    val expected = getOrCreateToken(context)
    return MessageDigest.isEqual(
      provided.toByteArray(Charsets.UTF_8),
      expected.toByteArray(Charsets.UTF_8),
    )
  }
}
