package com.example.network

import android.content.Context
import com.example.data.model.VocabularyItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Unified AI client for LinguaFa.
 * DeepSeek is preferred when the learner has configured a key; Gemini remains an optional fallback.
 * No provider credential is bundled in the APK.
 */
object AiApiClient {

    fun hasValidApiKey(context: Context? = null): Boolean {
        return AiPreferences.getDeepSeekApiKey(context).isNotBlank() || GeminiClient.hasValidApiKey()
    }

    suspend fun testConnection(apiKey: String, model: String = AiPreferences.DEFAULT_MODEL): Result<String> {
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
                You are LinguaFa's English tutor for Persian speakers preparing for IELTS, TOEFL, and GRE.
                Explain clearly in Persian, keep English examples natural, and prefer retrieval practice,
                accurate collocations, paraphrasing, and self-correction over memorizing impressive words.
            """.trimIndent()
            val prompt = if (!contextInfo.isNullOrBlank()) {
                "Context / Prior Discussion:\n$contextInfo\n\nStudent Question:\n$userMessage"
            } else {
                userMessage
            }
            val result = DeepSeekClient.chat(dsKey, systemPrompt, prompt, model)
            if (result.isSuccess) return@withContext result
        }

        if (GeminiClient.hasValidApiKey()) {
            return@withContext GeminiClient.askTutor(userMessage, contextInfo.orEmpty())
        }

        Result.failure(Exception("AI service unavailable. Add a DeepSeek API key in Profile > AI Configuration."))
    }

    suspend fun generateVocabularyList(
        userPrompt: String,
        context: Context? = null
    ): Result<List<VocabularyItem>> = withContext(Dispatchers.IO) {
        val dsKey = AiPreferences.getDeepSeekApiKey(context)
        val model = AiPreferences.getDeepSeekModel(context)

        if (dsKey.isNotBlank()) {
            val result = DeepSeekClient.generateVocabularyList(dsKey, userPrompt, model)
            if (result.isSuccess) return@withContext result
        }

        if (GeminiClient.hasValidApiKey()) {
            return@withContext GeminiClient.generateVocabularyList(userPrompt)
        }

        Result.failure(Exception("AI service unavailable. Add a DeepSeek API key in Profile > AI Configuration."))
    }

    suspend fun enrichWord(
        word: String,
        optionalPersian: String = "",
        context: Context? = null
    ): Result<VocabularyItem> = withContext(Dispatchers.IO) {
        val dsKey = AiPreferences.getDeepSeekApiKey(context)
        val model = AiPreferences.getDeepSeekModel(context)

        if (dsKey.isNotBlank()) {
            val result = DeepSeekClient.enrichWord(dsKey, word, optionalPersian, model)
            if (result.isSuccess) return@withContext result
        }

        if (GeminiClient.hasValidApiKey()) {
            return@withContext GeminiClient.enrichWord(word, optionalPersian)
        }

        Result.failure(Exception("AI service unavailable. Add a DeepSeek API key in Profile > AI Configuration."))
    }

    suspend fun evaluateEssay(
        prompt: String,
        essay: String,
        context: Context? = null
    ): Result<WritingEvaluationResult> = withContext(Dispatchers.IO) {
        val dsKey = AiPreferences.getDeepSeekApiKey(context)
        val model = AiPreferences.getDeepSeekModel(context)

        if (dsKey.isNotBlank()) {
            val result = DeepSeekClient.evaluateEssay(dsKey, prompt, essay, model)
            if (result.isSuccess) return@withContext result
        }

        if (GeminiClient.hasValidApiKey()) {
            return@withContext GeminiClient.evaluateEssay(prompt, essay)
        }

        Result.failure(Exception("AI evaluation unavailable. Add a DeepSeek API key in Profile > AI Configuration."))
    }

    suspend fun evaluateSpeaking(
        prompt: String,
        transcript: String,
        context: Context? = null
    ): Result<SpeakingEvaluationResult> = withContext(Dispatchers.IO) {
        val dsKey = AiPreferences.getDeepSeekApiKey(context)
        val model = AiPreferences.getDeepSeekModel(context)

        if (dsKey.isNotBlank()) {
            return@withContext DeepSeekClient.evaluateSpeaking(dsKey, prompt, transcript, model)
        }

        // GeminiClient has no audio-aware speaking evaluator. Avoid fabricating pronunciation
        // assessment from a transcript-only fallback.
        Result.failure(
            Exception("Speaking text analysis requires a configured DeepSeek key. Pronunciation cannot be scored from transcript text alone.")
        )
    }
}
