package com.example.network

import android.os.SystemClock
import android.util.Log
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

object DeepSeekClient {
    private const val TAG = "DeepSeekClient"
    private const val BASE_URL = "https://api.deepseek.com/chat/completions"

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    suspend fun testConnection(
        apiKey: String,
        model: String = AiPreferences.DEFAULT_MODEL
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("DeepSeek API key is empty."))
        }

        val requestJson = JSONObject().apply {
            put("model", model)
            put("messages", JSONArray().apply {
                put(JSONObject().apply {
                    put("role", "user")
                    put("content", "Reply only with Connected.")
                })
            })
            put("max_tokens", 8)
            put("temperature", 0)
        }

        runCatching {
            val request = Request.Builder()
                .url(BASE_URL)
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val started = SystemClock.elapsedRealtime()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (!response.isSuccessful || body == null) {
                    throw Exception("Connection failed: ${parseError(body, response.code)}")
                }
                val latencyMs = SystemClock.elapsedRealtime() - started
                "HTTP ${response.code} · ${latencyMs} ms · $model"
            }
        }.onFailure { Log.e(TAG, "DeepSeek test connection error", it) }
    }

    suspend fun chat(
        apiKey: String,
        systemPrompt: String,
        userMessage: String,
        model: String = AiPreferences.DEFAULT_MODEL,
        temperature: Double = 0.7
    ): Result<String> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            return@withContext Result.failure(Exception("DeepSeek API key is not configured."))
        }

        val messages = JSONArray().apply {
            if (systemPrompt.isNotBlank()) {
                put(JSONObject().apply {
                    put("role", "system")
                    put("content", systemPrompt)
                })
            }
            put(JSONObject().apply {
                put("role", "user")
                put("content", userMessage)
            })
        }

        val requestJson = JSONObject().apply {
            put("model", model)
            put("messages", messages)
            put("temperature", temperature)
        }

        runCatching {
            val request = Request.Builder()
                .url(BASE_URL)
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .post(requestJson.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string()
                if (!response.isSuccessful || body == null) {
                    throw Exception("DeepSeek error (${parseError(body, response.code)})")
                }

                val json = JSONObject(body)
                json.optJSONArray("choices")?.optJSONObject(0)
                    ?.optJSONObject("message")?.optString("content")
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: throw Exception("DeepSeek returned an empty response.")
            }
        }.onFailure { Log.e(TAG, "DeepSeek chat error", it) }
    }

    suspend fun generateVocabularyList(
        apiKey: String,
        userPrompt: String,
        model: String = AiPreferences.DEFAULT_MODEL
    ): Result<List<VocabularyItem>> = withContext(Dispatchers.IO) {
        val systemInstruction = """
            You are an English lexicographer and Persian language-learning assistant for IELTS, TOEFL, and GRE.
            Generate a concise, pedagogically useful vocabulary list based on the request.
            Return ONLY a valid JSON array where each object has these exact fields:
            - "word": string (lowercase)
            - "ipa": string
            - "persianMeaning": string
            - "englishDefinition": string
            - "partOfSpeech": string
            - "example": string that naturally contains the target word or a normal inflected form
            - "examplePersian": string
            - "cefrLevel": string (A2, B1, B2, C1, or C2)
            - "synonyms": array of strings
            - "antonyms": array of strings
            - "collocations": array of 2-4 natural candidate collocations
            - "wordFamily": array of strings
            - "commonMistakes": string
            - "ieltsRelevance": string (High, Medium, or Low)
            - "toeflRelevance": string (High, Medium, or Low)
            - "tags": array of strings
            Do not claim that any individual word guarantees an exam score. AI-generated lexical data will be
            marked unverified by the application until reviewed against a trusted source.
        """.trimIndent()

        chat(
            apiKey = apiKey,
            systemPrompt = systemInstruction,
            userMessage = userPrompt,
            model = model,
            temperature = 0.25
        ).mapCatching { rawText ->
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

                    val tags = jsonStringList(obj.optJSONArray("tags")) + listOf("ai-generated", "unverified")
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
                            tags = tags.distinct(),
                            source = "DeepSeek AI (unverified)"
                        )
                    )
                }
            }
        }
    }

    suspend fun enrichWord(
        apiKey: String,
        word: String,
        optionalPersian: String = "",
        model: String = AiPreferences.DEFAULT_MODEL
    ): Result<VocabularyItem> = withContext(Dispatchers.IO) {
        val prompt = "Word: '$word'. Optional Persian context: '$optionalPersian'. Return one accurate learner-dictionary entry."
        generateVocabularyList(apiKey, prompt, model).mapCatching { list ->
            list.firstOrNull() ?: throw Exception("No details found for '$word'.")
        }
    }

    suspend fun evaluateEssay(
        apiKey: String,
        taskPrompt: String,
        essayText: String,
        model: String = AiPreferences.DEFAULT_MODEL
    ): Result<WritingEvaluationResult> = withContext(Dispatchers.IO) {
        val systemPrompt = """
            Evaluate this IELTS essay using the official writing criteria: Task Response/Achievement,
            Coherence and Cohesion, Lexical Resource, and Grammatical Range and Accuracy.
            Be conservative: do not invent a score when evidence is insufficient. Explain primarily in Persian.
            Return ONLY valid JSON with estimatedBand, taskAchievementScore, coherenceScore, lexicalScore,
            grammarScore, overallFeedbackFa, strengthsFa, mainIssuesFa, sentenceCorrections, and improvedVersion.
            sentenceCorrections must be an array of objects with original, corrected, and explanationFa.
        """.trimIndent()

        chat(
            apiKey,
            systemPrompt,
            "Prompt: $taskPrompt\n\nEssay:\n$essayText",
            model,
            temperature = 0.2
        ).mapCatching { rawText ->
            val obj = JSONObject(cleanJson(rawText))
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

    suspend fun evaluateSpeaking(
        apiKey: String,
        taskPrompt: String,
        transcriptText: String,
        model: String = AiPreferences.DEFAULT_MODEL
    ): Result<SpeakingEvaluationResult> = withContext(Dispatchers.IO) {
        val systemPrompt = """
            You are reviewing ONLY a text transcript of an IELTS/TOEFL speaking response; no audio is available.
            Assess lexical choice, grammar, organization, and transcript evidence of fluency cautiously.
            Do NOT claim to hear pronunciation, accent, intonation, pauses, stress, or rhythm. In pronunciationHintsFa,
            explicitly state that pronunciation requires audio and provide only general practice suggestions.
            Return ONLY valid JSON with estimatedBand, fluencyFeedbackFa, lexicalFeedbackFa,
            grammarFeedbackFa, pronunciationHintsFa, and betterPhrasings.
        """.trimIndent()

        chat(
            apiKey,
            systemPrompt,
            "Task: $taskPrompt\n\nTranscript: $transcriptText",
            model,
            temperature = 0.2
        ).mapCatching { rawText ->
            val obj = JSONObject(cleanJson(rawText))
            SpeakingEvaluationResult(
                estimatedBand = obj.optString("estimatedBand", "N/A"),
                fluencyFeedbackFa = obj.optString("fluencyFeedbackFa"),
                lexicalFeedbackFa = obj.optString("lexicalFeedbackFa"),
                grammarFeedbackFa = obj.optString("grammarFeedbackFa"),
                pronunciationHintsFa = obj.optString(
                    "pronunciationHintsFa",
                    "برای ارزیابی تلفظ به فایل صوتی نیاز است؛ از روی متن نمی‌توان تلفظ را نمره‌گذاری کرد."
                ),
                betterPhrasings = jsonStringList(obj.optJSONArray("betterPhrasings"))
            )
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

    private fun parseError(body: String?, code: Int): String = try {
        JSONObject(body.orEmpty()).optJSONObject("error")?.optString("message")
            ?.takeIf { it.isNotBlank() }
            ?: "HTTP $code"
    } catch (_: Exception) {
        "HTTP $code"
    }
}
