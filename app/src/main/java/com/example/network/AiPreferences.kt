package com.example.network

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object AiPreferences {
    const val PREFS_NAME = "linguafa_ai_preferences"

    private const val LEGACY_PLAINTEXT_KEY = "deepseek_api_key"
    private const val KEY_DEEPSEEK_CIPHERTEXT = "deepseek_api_key_ciphertext"
    private const val KEY_DEEPSEEK_IV = "deepseek_api_key_iv"
    private const val KEY_PROVIDER = "preferred_ai_provider"
    private const val KEY_MODEL = "deepseek_model"
    private const val KEYSTORE_ALIAS = "linguafa_deepseek_api_key_v1"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"

    const val PROVIDER_DEEPSEEK = "DEEPSEEK"
    const val PROVIDER_GEMINI = "GEMINI"
    const val MODEL_FLASH = "deepseek-flash"
    const val MODEL_PRO = "deepseek-v4-pro"
    const val DEFAULT_MODEL = MODEL_FLASH

    private val supportedModels = setOf(MODEL_FLASH, MODEL_PRO)

    @Volatile
    private var applicationContext: Context? = null

    fun init(context: Context) {
        if (applicationContext == null) {
            applicationContext = context.applicationContext
        }
    }

    private fun getContext(context: Context? = applicationContext): Context? =
        (context ?: applicationContext)?.applicationContext

    private fun getPrefs(context: Context? = applicationContext): SharedPreferences? =
        getContext(context)?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Returns the user-entered DeepSeek key decrypted with a device-bound Android Keystore key.
     * No API key is bundled with the app. A legacy plaintext preference is migrated once and removed.
     */
    fun getDeepSeekApiKey(context: Context? = applicationContext): String {
        val ctx = getContext(context) ?: return ""
        val prefs = getPrefs(ctx) ?: return ""

        migrateLegacyPlaintextKeyIfNeeded(ctx, prefs)

        val ciphertext = prefs.getString(KEY_DEEPSEEK_CIPHERTEXT, null) ?: return ""
        val iv = prefs.getString(KEY_DEEPSEEK_IV, null) ?: return ""

        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateSecretKey(),
                GCMParameterSpec(128, Base64.decode(iv, Base64.NO_WRAP))
            )
            String(
                cipher.doFinal(Base64.decode(ciphertext, Base64.NO_WRAP)),
                Charsets.UTF_8
            ).trim()
        }.getOrElse {
            // If the keystore entry was invalidated, fail closed rather than exposing or guessing a key.
            ""
        }
    }

    fun getRawStoredKey(context: Context? = applicationContext): String = getDeepSeekApiKey(context)

    fun setDeepSeekApiKey(context: Context? = applicationContext, key: String) {
        val ctx = getContext(context) ?: return
        val prefs = getPrefs(ctx) ?: return
        val normalized = key.trim()
        if (normalized.isBlank()) {
            clearDeepSeekApiKey(ctx)
            return
        }

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val encrypted = cipher.doFinal(normalized.toByteArray(Charsets.UTF_8))

        prefs.edit()
            .putString(KEY_DEEPSEEK_CIPHERTEXT, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .putString(KEY_DEEPSEEK_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .remove(LEGACY_PLAINTEXT_KEY)
            .apply()
    }

    fun clearDeepSeekApiKey(context: Context? = applicationContext) {
        getPrefs(context)?.edit()
            ?.remove(KEY_DEEPSEEK_CIPHERTEXT)
            ?.remove(KEY_DEEPSEEK_IV)
            ?.remove(LEGACY_PLAINTEXT_KEY)
            ?.apply()
    }

    fun getPreferredProvider(context: Context? = applicationContext): String {
        return getPrefs(context)?.getString(KEY_PROVIDER, PROVIDER_DEEPSEEK)
            ?.takeIf { it == PROVIDER_DEEPSEEK || it == PROVIDER_GEMINI }
            ?: PROVIDER_DEEPSEEK
    }

    fun setPreferredProvider(context: Context? = applicationContext, provider: String) {
        val safeProvider = if (provider == PROVIDER_GEMINI) PROVIDER_GEMINI else PROVIDER_DEEPSEEK
        getPrefs(context)?.edit()?.putString(KEY_PROVIDER, safeProvider)?.apply()
    }

    fun getDeepSeekModel(context: Context? = applicationContext): String {
        val stored = getPrefs(context)?.getString(KEY_MODEL, DEFAULT_MODEL).orEmpty()
        return when (stored) {
            MODEL_FLASH, MODEL_PRO -> stored
            // Retired aliases from older builds are migrated to the current Flash endpoint.
            "deepseek-chat", "deepseek-reasoner", "deepseek-v4-flash" -> MODEL_FLASH
            else -> DEFAULT_MODEL
        }
    }

    fun setDeepSeekModel(context: Context? = applicationContext, model: String) {
        val safeModel = model.takeIf { it in supportedModels } ?: DEFAULT_MODEL
        getPrefs(context)?.edit()?.putString(KEY_MODEL, safeModel)?.apply()
    }

    fun hasValidAiKey(context: Context? = applicationContext): Boolean {
        return getDeepSeekApiKey(context).isNotBlank() || GeminiClient.hasValidApiKey()
    }

    private fun migrateLegacyPlaintextKeyIfNeeded(context: Context, prefs: SharedPreferences) {
        if (prefs.contains(KEY_DEEPSEEK_CIPHERTEXT)) {
            if (prefs.contains(LEGACY_PLAINTEXT_KEY)) {
                prefs.edit().remove(LEGACY_PLAINTEXT_KEY).apply()
            }
            return
        }

        val legacy = prefs.getString(LEGACY_PLAINTEXT_KEY, null)?.trim().orEmpty()
        if (legacy.isNotBlank()) {
            runCatching { setDeepSeekApiKey(context, legacy) }
                .onFailure { prefs.edit().remove(LEGACY_PLAINTEXT_KEY).apply() }
        }
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(KEYSTORE_ALIAS, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEYSTORE_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}
