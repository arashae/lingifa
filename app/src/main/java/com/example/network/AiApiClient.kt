package com.example.network

import android.content.Context
import com.example.data.model.VocabularyItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Unified AI Client for LinguaFa.
 * Intelligently prioritizes DeepSeek API when a key is provided (or defaults to the pre-configured key),
 * falling back to Gemini if available.
 */
object AiApiClient {

    fun hasValidApiKey(context: Context? = null): Boolean {
        return AiPreferences.getDeepSeekApiKey(context).isNotBlank() || GeminiClient.hasValidApiKey()
    }

    suspend fun testConnection(apiKey: String, model: String = "deepseek-chat"): Result<String> {
        return DeepSeekClient.testConnection(apiKey, model)
    }

    suspend fun askTutor(
        userMessage: String,
        contextInfo: String? = null,
        context: Context? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val dsKey = AiPreferences.getDeepSeekApiKey(context)
        val provider = AiPreferences.getPreferredProvider(context)
        val model = AiPreferences.getDeepSeekModel(context)

        if (provider == AiPreferences.PROVIDER_DEEPSEEK && dsKey.isNotBlank()) {
            val systemPrompt = """
                You are a knowledgeable, patient, and encouraging personal English tutor for IELTS, TOEFL, and GRE exams.
                Provide structured, clear explanations in Persian (فارسی). 
                Use English examples, phonetic cues, and clear contrast points.
            """.trimIndent()
            val prompt = if (!contextInfo.isNullOrBlank()) {
                "Context / Prior Discussion:\n$contextInfo\n\nStudent Question:\n$userMessage"
            } else {
                userMessage
            }
            val res = DeepSeekClient.chat(dsKey, systemPrompt, prompt, model)
            if (res.isSuccess) return@withContext res
        }

        // Fallback to Gemini if DeepSeek fails or not preferred
        if (GeminiClient.hasValidApiKey()) {
            return@withContext GeminiClient.askTutor(userMessage, contextInfo)
        }

        Result.failure(Exception("AI service unavailable. Please configure your DeepSeek API key in Profile."))
    }

    suspend fun generateVocabularyList(
        userPrompt: String,
        context: Context? = null
    ): Result<List<VocabularyItem>> = withContext(Dispatchers.IO) {
        val dsKey = AiPreferences.getDeepSeekApiKey(context)
        val model = AiPreferences.getDeepSeekModel(context)

        if (dsKey.isNotBlank()) {
            val res = DeepSeekClient.generateVocabularyList(dsKey, userPrompt, model)
            if (res.isSuccess) return@withContext res
        }

        if (GeminiClient.hasValidApiKey()) {
            return@withContext GeminiClient.generateVocabularyList(userPrompt)
        }

        Result.failure(Exception("AI service unavailable. Please configure your DeepSeek API key in Profile."))
    }

    suspend fun enrichWord(
        word: String,
        optionalPersian: String = "",
        context: Context? = null
    ): Result<VocabularyItem> = withContext(Dispatchers.IO) {
        val dsKey = AiPreferences.getDeepSeekApiKey(context)
        val model = AiPreferences.getDeepSeekModel(context)

        if (dsKey.isNotBlank()) {
            val res = DeepSeekClient.enrichWord(dsKey, word, optionalPersian, model)
            if (res.isSuccess) return@withContext res
        }

        if (GeminiClient.hasValidApiKey()) {
            return@withContext GeminiClient.enrichWord(word, optionalPersian)
        }

        Result.failure(Exception("AI service unavailable. Please configure your DeepSeek API key in Profile."))
    }

    suspend fun evaluateEssay(
        prompt: String,
        essay: String,
        context: Context? = null
    ): Result<WritingEvaluationResult> = withContext(Dispatchers.IO) {
        val dsKey = AiPreferences.getDeepSeekApiKey(context)
        val model = AiPreferences.getDeepSeekModel(context)

        if (dsKey.isNotBlank()) {
            val res = DeepSeekClient.evaluateEssay(dsKey, prompt, essay, model)
            if (res.isSuccess) return@withContext res
        }

        if (GeminiClient.hasValidApiKey()) {
            return@withContext GeminiClient.evaluateEssay(prompt, essay)
        }

        Result.failure(Exception("AI service unavailable. Please configure your DeepSeek API key in Profile."))
    }

    suspend fun evaluateSpeaking(
        prompt: String,
        transcript: String,
        context: Context? = null
    ): Result<SpeakingEvaluationResult> = withContext(Dispatchers.IO) {
        val dsKey = AiPreferences.getDeepSeekApiKey(context)
        val model = AiPreferences.getDeepSeekModel(context)

        if (dsKey.isNotBlank()) {
            val res = DeepSeekClient.evaluateSpeaking(dsKey, prompt, transcript, model)
            if (res.isSuccess) return@withContext res
        }

        if (GeminiClient.hasValidApiKey()) {
            return@withContext GeminiClient.evaluateSpeaking(prompt, transcript)
        }

        Result.failure(Exception("AI service unavailable. Please configure your DeepSeek API key in Profile."))
    }
}
