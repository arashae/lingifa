package com.example.network

import android.content.Context
import android.content.SharedPreferences

object AiPreferences {
    private const val PREFS_NAME = "linguafa_ai_preferences"
    private const val KEY_DEEPSEEK_KEY = "deepseek_api_key"
    private const val KEY_PROVIDER = "preferred_ai_provider"
    private const val KEY_MODEL = "deepseek_model"

    const val PROVIDER_DEEPSEEK = "DEEPSEEK"
    const val PROVIDER_GEMINI = "GEMINI"
    const val DEFAULT_MODEL = "deepseek-chat"
    const val DEFAULT_FALLBACK_KEY = "sk-4063620adf83409e8fc409c8ce8428bd"

    @Volatile
    private var applicationContext: Context? = null

    fun init(context: Context) {
        if (applicationContext == null) {
            applicationContext = context.applicationContext
        }
    }

    private fun getPrefs(context: Context? = applicationContext): SharedPreferences? {
        val ctx = context ?: applicationContext ?: return null
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getDeepSeekApiKey(context: Context? = applicationContext): String {
        val stored = getPrefs(context)?.getString(KEY_DEEPSEEK_KEY, null)?.trim()
        return if (!stored.isNullOrEmpty()) stored else DEFAULT_FALLBACK_KEY
    }

    fun getRawStoredKey(context: Context? = applicationContext): String {
        return getPrefs(context)?.getString(KEY_DEEPSEEK_KEY, "")?.trim().orEmpty()
    }

    fun setDeepSeekApiKey(context: Context? = applicationContext, key: String) {
        getPrefs(context)?.edit()?.putString(KEY_DEEPSEEK_KEY, key.trim())?.apply()
    }

    fun clearDeepSeekApiKey(context: Context? = applicationContext) {
        getPrefs(context)?.edit()?.remove(KEY_DEEPSEEK_KEY)?.apply()
    }

    fun getPreferredProvider(context: Context? = applicationContext): String {
        return getPrefs(context)?.getString(KEY_PROVIDER, PROVIDER_DEEPSEEK) ?: PROVIDER_DEEPSEEK
    }

    fun setPreferredProvider(context: Context? = applicationContext, provider: String) {
        getPrefs(context)?.edit()?.putString(KEY_PROVIDER, provider)?.apply()
    }

    fun getDeepSeekModel(context: Context? = applicationContext): String {
        return getPrefs(context)?.getString(KEY_MODEL, DEFAULT_MODEL) ?: DEFAULT_MODEL
    }

    fun setDeepSeekModel(context: Context? = applicationContext, model: String) {
        getPrefs(context)?.edit()?.putString(KEY_MODEL, model)?.apply()
    }

    fun hasValidAiKey(context: Context? = applicationContext): Boolean {
        return getDeepSeekApiKey(context).isNotEmpty() || GeminiClient.hasValidApiKey()
    }
}
