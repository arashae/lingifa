package com.example.network

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.VocabularyItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class WritingEvaluationResult(
    val estimatedBand: String,
    val taskAchievementScore: String,
    val coherenceScore: String,
    val lexicalScore: String,
    val grammarScore: String,
    val overallFeedbackFa: String,
    val strengthsFa: List<String>,
    val mainIssuesFa: List<String>,
    val sentenceCorrections: List<SentenceCorrection>,
    val improvedVersion: String
)

data class SentenceCorrection(
    val original: String,
    val corrected: String,
    val explanationFa: String
)

data class SpeakingEvaluationResult(
    val estimatedBand: String,
    val fluencyFeedbackFa: String,
    val lexicalFeedbackFa: String,
    val grammarFeedbackFa: String,
    val pronunciationHintsFa: String,
    val betterPhrasings: List<String>
)

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    private fun getApiKey(): String = try {
        BuildConfig.GEMINI_API_KEY.trim()
    } catch (_: Exception) {
        ""
    }

    /** Gemini validity must only inspect the Gemini credential.
     * Calling AiPreferences.hasValidAiKey() here caused infinite mutual recursion when no DeepSeek key existed.
     */
    fun hasValidApiKey(): Boolean {
        val key = getApiKey()
        return key.isNotEmpty() && key != "MY_GEMINI_API_KEY"
    }

    suspend fun generateVocabularyList(userPrompt: String): Result<List<VocabularyItem>> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (!hasValidApiKey()) {
            return@withContext Result.failure(Exception("Gemini fallback is not configured."))
        }

        val systemInstruction = """
            You are an English lexicographer and Persian language-learning assistant for IELTS, TOEFL, and GRE.
            Return ONLY a valid JSON array where each object includes word, ipa, persianMeaning,
            englishDefinition, partOfSpeech, example, examplePersian, cefrLevel, synonyms, antonyms,
            collocations, wordFamily, commonMistakes, ieltsRelevance, toeflRelevance, and tags.
            Examples must naturally contain the target word or a normal inflected form.
            Do not claim that individual words guarantee any exam band or score.
        """.trimIndent()

        val promptJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", "System instruction: $systemInstruction\n\nUser Request: $userPrompt"))
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.3)
            })
        }

        runCatching {
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(promptJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (!response.isSuccessful || body == null) {
                    throw Exception("Gemini request failed (HTTP ${response.code}).")
                }

                val jsonResponse = JSONObject(body)
                val rawText = jsonResponse.optJSONArray("candidates")?.optJSONObject(0)
                    ?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)
                    ?.optString("text").orEmpty()
                val cleanJson = cleanJson(rawText)
                val array = if (cleanJson.startsWith("[")) {
                    JSONArray(cleanJson)
                } else {
                    JSONObject(cleanJson).optJSONArray("vocabulary") ?: JSONArray()
                }

                buildList {
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        val word = obj.optString("word").trim()
                        if (word.isBlank()) continue
                        add(
                            VocabularyItem(
                                id = 0L,
                                word = word,
                                normalizedWord = word.lowercase().trim(),
                                ipa = obj.optString("ipa"),
                                persianMeaning = obj.optString("persianMeaning"),
                                englishDefinition = obj.optString("englishDefinition"),
                                partOfSpeech = obj.optString("partOfSpeech", "word"),
                                example = obj.optString("example"),
                                examplePersian = obj.optString("examplePersian"),
                                cefrLevel = obj.optString("cefrLevel", "B2"),
                                synonyms = jsonStringList(obj.optJSONArray("synonyms")),
                                antonyms = jsonStringList(obj.optJSONArray("antonyms")),
                                collocations = jsonStringList(obj.optJSONArray("collocations")),
                                wordFamily = jsonStringList(obj.optJSONArray("wordFamily")),
                                commonMistakes = obj.optString("commonMistakes"),
                                ieltsRelevance = obj.optString("ieltsRelevance", "Medium"),
                                toeflRelevance = obj.optString("toeflRelevance", "Medium"),
                                tags = (jsonStringList(obj.optJSONArray("tags")) + listOf("ai-generated", "unverified")).distinct(),
                                source = "Gemini AI (unverified)"
                            )
                        )
                    }
                }
            }
        }.onFailure { Log.e(TAG, "Vocabulary generation failed", it) }
    }

    suspend fun enrichWord(word: String, optionalPersian: String = ""): Result<VocabularyItem> = withContext(Dispatchers.IO) {
        generateVocabularyList(
            "Word: '$word'. Optional Persian context: '$optionalPersian'. Return one accurate learner-dictionary entry."
        ).mapCatching { list ->
            list.firstOrNull() ?: throw Exception("No vocabulary data returned for '$word'.")
        }
    }

    suspend fun askTutor(userQuestion: String, conversationContext: String = ""): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (!hasValidApiKey()) {
            return@withContext Result.failure(Exception("Gemini fallback is not configured."))
        }

        val systemInstruction = """
            You are LinguaFa's English tutor for Persian speakers preparing for IELTS, TOEFL, and GRE.
            Explain primarily in fluent Persian, keep English examples natural, and encourage active recall,
            accurate collocation, paraphrasing, and self-correction.
        """.trimIndent()
        val userText = if (conversationContext.isBlank()) {
            userQuestion
        } else {
            "Context:\n$conversationContext\n\nStudent question:\n$userQuestion"
        }
        generateText(apiKey, "$systemInstruction\n\n$userText")
    }

    suspend fun evaluateEssay(taskPrompt: String, essayText: String): Result<WritingEvaluationResult> = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (!hasValidApiKey()) {
            return@withContext Result.failure(Exception("Gemini fallback is not configured."))
        }

        val prompt = """
            Evaluate this IELTS writing response conservatively using Task Response/Achievement,
            Coherence and Cohesion, Lexical Resource, and Grammatical Range and Accuracy.
            Return ONLY JSON with estimatedBand, taskAchievementScore, coherenceScore, lexicalScore,
            grammarScore, overallFeedbackFa, strengthsFa, mainIssuesFa, sentenceCorrections,
            and improvedVersion. Do not invent a score when evidence is insufficient.

            Task:
            $taskPrompt

            Essay:
            $essayText
        """.trimIndent()

        generateText(apiKey, prompt).mapCatching { raw ->
            val obj = JSONObject(cleanJson(raw))
            val corrections = mutableListOf<SentenceCorrection>()
            obj.optJSONArray("sentenceCorrections")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val item = arr.optJSONObject(i) ?: continue
                    corrections += SentenceCorrection(
                        original = item.optString("original"),
                        corrected = item.optString("corrected"),
                        explanationFa = item.optString("explanationFa")
                    )
                }
            }
            WritingEvaluationResult(
                estimatedBand = obj.optString("estimatedBand", "N/A"),
                taskAchievementScore = obj.optString("taskAchievementScore", "N/A"),
                coherenceScore = obj.optString("coherenceScore", "N/A"),
                lexicalScore = obj.optString("lexicalScore", "N/A"),
                grammarScore = obj.optString("grammarScore", "N/A"),
                overallFeedbackFa = obj.optString("overallFeedbackFa", "ارزیابی متنی انجام شد."),
                strengthsFa = jsonStringList(obj.optJSONArray("strengthsFa")),
                mainIssuesFa = jsonStringList(obj.optJSONArray("mainIssuesFa")),
                sentenceCorrections = corrections,
                improvedVersion = obj.optString("improvedVersion", essayText)
            )
        }
    }

    private suspend fun generateText(apiKey: String, prompt: String): Result<String> = withContext(Dispatchers.IO) {
        val payload = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().put("text", prompt))
                    })
                })
            })
            put("generationConfig", JSONObject().apply { put("temperature", 0.3) })
        }

        runCatching {
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(payload.toString().toRequestBody("application/json".toMediaType()))
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (!response.isSuccessful || body == null) {
                    throw Exception("Gemini request failed (HTTP ${response.code}).")
                }
                JSONObject(body).optJSONArray("candidates")?.optJSONObject(0)
                    ?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)
                    ?.optString("text")?.trim()?.takeIf { it.isNotBlank() }
                    ?: throw Exception("Gemini returned an empty response.")
            }
        }
    }

    private fun cleanJson(text: String): String = text.trim()
        .removePrefix("```json")
        .removePrefix("```")
        .removeSuffix("```")
        .trim()

    private fun jsonStringList(array: JSONArray?): List<String> = buildList {
        if (array == null) return@buildList
        for (i in 0 until array.length()) {
            array.optString(i).trim().takeIf { it.isNotBlank() }?.let(::add)
        }
    }
}
